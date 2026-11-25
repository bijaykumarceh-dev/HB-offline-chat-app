package com.hb.offlinechat.transfer.files

import android.content.Context
import android.net.Uri
import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.connectivity.connection.WireKind
import com.hb.offlinechat.core.security.CryptoManager
import com.hb.offlinechat.core.utils.FileUtils
import com.hb.offlinechat.data.local.TransferDao
import com.hb.offlinechat.data.local.TransferEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** A pending receive request shown to the user for accept/reject. */
data class IncomingFileRequest(
    val transferId: String,
    val senderId: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val sha256: String
)

/**
 * Real chunked file transfer over the encrypted wire protocol.
 *
 * Metadata, accept/reject decisions and data chunks are all authenticated with
 * the per-peer session key. SHA-256 is verified after reassembly. Files are
 * saved to the app-visible Downloads directory (no broad storage permission).
 */
class FileTransferManager(
    private val context: Context,
    private val connection: ConnectionManager,
    private val transferDao: TransferDao,
    private val scope: CoroutineScope
) {
    companion object {
        private const val CHUNK_SIZE = 16 * 1024
        const val MAX_FILE_SIZE = 200L * 1024 * 1024
        private const val RS = "\u001E"
        private const val TAG = "FileTransfer"
    }

    private val sendDecisions = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val receiveStates = ConcurrentHashMap<String, ReceiveState>()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()

    private class ReceiveState(
        val file: File,
        val name: String,
        val chunkCount: Int,
        val sha256: String,
        val peerId: String,
        var received: Int = 0
    )

    private val _incomingRequest = MutableStateFlow<IncomingFileRequest?>(null)
    val incomingRequest: StateFlow<IncomingFileRequest?> = _incomingRequest.asStateFlow()

    private val _lastEvent = MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent.asStateFlow()

    val transfers = transferDao.observeTransfers()

    init {
        scope.launch(Dispatchers.IO) {
            connection.incomingWire.collect { message ->
                when (message.kind) {
                    WireKind.FILE_META -> onFileMeta(message.senderId, message.payload)
                    WireKind.FILE_ACCEPT -> sendDecisions[decodeSingle(message.payload)]?.complete(true)
                    WireKind.FILE_REJECT -> sendDecisions[decodeSingle(message.payload)]?.complete(false)
                    WireKind.FILE_CHUNK -> onChunk(message.payload)
                    WireKind.FILE_CANCEL -> onCancel(decodeSingle(message.payload))
                    else -> Unit
                }
            }
        }
    }

    /** Read a SAF URI, hash it, ask the receiver, then stream encrypted chunks. */
    suspend fun sendFile(uri: Uri, recipientDeviceId: String) = withContext(Dispatchers.IO) {
        val info = FileUtils.info(context, uri)
        if (info.size > MAX_FILE_SIZE) {
            _lastEvent.value = "File too large (max ${FileUtils.readableSize(MAX_FILE_SIZE)})"
            return@withContext
        }
        val temp = File(context.cacheDir, "upload_${UUID.randomUUID()}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            } ?: run {
                _lastEvent.value = "Cannot open selected file"
                return@withContext
            }
            transfer(temp, info.name, info.mimeType, recipientDeviceId)
        } finally {
            temp.delete()
        }
    }

    /** Send an app-generated file (e.g. a voice recording) through the same pipeline. */
    suspend fun sendFileFromPath(
        path: String,
        displayName: String,
        mimeType: String,
        recipientDeviceId: String
    ) = withContext(Dispatchers.IO) {
        val source = File(path)
        if (!source.exists()) {
            _lastEvent.value = "File not found"
            return@withContext
        }
        if (source.length() > MAX_FILE_SIZE) {
            _lastEvent.value = "File too large"
            return@withContext
        }
        val temp = File(context.cacheDir, "upload_${UUID.randomUUID()}")
        try {
            source.inputStream().use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            }
            transfer(temp, displayName, mimeType, recipientDeviceId)
        } finally {
            temp.delete()
        }
    }

    private suspend fun transfer(temp: File, name: String, mime: String, recipientDeviceId: String) {
        val endpointId = connection.endpointIdForDevice(recipientDeviceId)
        if (endpointId == null) {
            _lastEvent.value = "Recipient is not connected"
            return
        }
        val peerKey = connection.publicKeyForDevice(recipientDeviceId)
        val hash = hashFile(temp)
        val chunkCount = maxOf(1, ((temp.length() + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt())
        val transferId = UUID.randomUUID().toString()

        transferDao.upsert(
            TransferEntity(
                transferId = transferId,
                peerId = recipientDeviceId,
                fileName = name,
                fileSize = temp.length(),
                direction = "OUT",
                status = "PENDING",
                sha256 = hash
            )
        )

        val meta = listOf(
            transferId, name, temp.length().toString(), mime, hash, chunkCount.toString()
        ).joinToString(RS)
        if (connection.sendWire(endpointId, peerKey, WireKind.FILE_META, meta) == null) {
            _lastEvent.value = "Could not send transfer request"
            return
        }

        val decision = CompletableDeferred<Boolean>()
        sendDecisions[transferId] = decision
        _lastEvent.value = "Waiting for $name acceptance…"
        val accepted = decision.await()
        sendDecisions.remove(transferId)

        if (!accepted) {
            transferDao.updateProgress(transferId, "REJECTED", 0)
            _lastEvent.value = "Transfer rejected"
            return
        }

        transferDao.updateProgress(transferId, "TRANSFERRING", 0)
        temp.inputStream().use { input ->
            var index = 0
            val buffer = ByteArray(CHUNK_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                if (cancelled.contains(transferId)) {
                    transferDao.updateProgress(transferId, "CANCELLED", 0)
                    _lastEvent.value = "Transfer cancelled"
                    return
                }
                val chunk = listOf(
                    transferId, index.toString(), Base64.getEncoder().encodeToString(buffer.copyOf(read))
                ).joinToString(RS)
                connection.sendWire(endpointId, peerKey, WireKind.FILE_CHUNK, chunk)
                index++
                transferDao.updateProgress(transferId, "TRANSFERRING", (index * 100) / chunkCount)
            }
        }
        transferDao.updateProgress(transferId, "COMPLETED", 100)
        _lastEvent.value = "Sent $name"
    }

    /** Receiver accepts the announced transfer and starts collecting chunks. */
    fun acceptIncoming() {
        scope.launch(Dispatchers.IO) {
            val request = _incomingRequest.value ?: return@launch
            val file = File(context.cacheDir, "download_${request.transferId}")
            receiveStates[request.transferId] = ReceiveState(
                file = file,
                name = request.name,
                chunkCount = ((request.size + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt().coerceAtLeast(1),
                sha256 = request.sha256,
                peerId = request.senderId
            )
            transferDao.upsert(
                TransferEntity(
                    transferId = request.transferId,
                    peerId = request.senderId,
                    fileName = request.name,
                    fileSize = request.size,
                    direction = "IN",
                    status = "TRANSFERRING",
                    sha256 = request.sha256
                )
            )
            connection.sendWireToDevice(request.senderId, WireKind.FILE_ACCEPT, request.transferId)
            _incomingRequest.value = null
            _lastEvent.value = "Receiving ${request.name}…"
        }
    }

    /** Receiver rejects the announced transfer. */
    fun rejectIncoming() {
        scope.launch(Dispatchers.IO) {
            val request = _incomingRequest.value ?: return@launch
            connection.sendWireToDevice(request.senderId, WireKind.FILE_REJECT, request.transferId)
            transferDao.upsert(
                TransferEntity(
                    transferId = request.transferId,
                    peerId = request.senderId,
                    fileName = request.name,
                    fileSize = request.size,
                    direction = "IN",
                    status = "REJECTED",
                    sha256 = request.sha256
                )
            )
            _incomingRequest.value = null
            _lastEvent.value = "Transfer rejected"
        }
    }

    /** Cancel an in-progress transfer (either direction). */
    fun cancel(transferId: String) {
        scope.launch(Dispatchers.IO) {
            cancelled.add(transferId)
            val state = receiveStates.remove(transferId)
            state?.file?.delete()
            val peerId = state?.peerId
            transferDao.upsert(
                TransferEntity(
                    transferId = transferId,
                    peerId = peerId ?: "",
                    fileName = "",
                    fileSize = 0,
                    direction = "IN",
                    status = "CANCELLED"
                )
            )
            if (peerId != null) connection.sendWireToDevice(peerId, WireKind.FILE_CANCEL, transferId)
            _lastEvent.value = "Transfer cancelled"
        }
    }

    private suspend fun onFileMeta(senderId: String, payload: String) {
        val parts = payload.split(RS, limit = 6)
        if (parts.size != 6) return
        val request = IncomingFileRequest(
            transferId = parts[0],
            senderId = senderId,
            name = parts[1],
            size = parts[2].toLongOrNull() ?: return,
            mimeType = parts[3],
            sha256 = parts[4]
        )
        if (request.size > MAX_FILE_SIZE) {
            connection.sendWireToDevice(senderId, WireKind.FILE_REJECT, request.transferId)
            _lastEvent.value = "Rejected oversized file"
            return
        }
        _incomingRequest.value = request
    }

    private suspend fun onChunk(payload: String) {
        val parts = payload.split(RS, limit = 3)
        if (parts.size != 3) return
        val transferId = parts[0]
        val state = receiveStates[transferId] ?: return
        if (cancelled.contains(transferId)) return
        val index = parts[1].toIntOrNull() ?: return
        val bytes = try {
            Base64.getDecoder().decode(parts[2])
        } catch (e: Exception) {
            return
        }
        try {
            RandomAccessFile(state.file, "rw").use { raf ->
                raf.seek(index.toLong() * CHUNK_SIZE)
                raf.write(bytes)
            }
            state.received++
            val progress = (state.received * 100) / state.chunkCount
            transferDao.updateProgress(transferId, "TRANSFERRING", progress)
            if (state.received >= state.chunkCount) finalize(transferId, state)
        } catch (e: Exception) {
            _lastEvent.value = "Write failed: ${e.message}"
        }
    }

    private suspend fun finalize(transferId: String, state: ReceiveState) {
        val actualHash = hashFile(state.file)
        if (actualHash != state.sha256) {
            state.file.delete()
            transferDao.updateProgress(transferId, "FAILED", 0)
            _lastEvent.value = "Integrity check failed — file discarded"
            receiveStates.remove(transferId)
            return
        }
        val destination = FileUtils.uniqueFile(FileUtils.downloadsDir(context), state.name)
        if (state.file.renameTo(destination)) {
            transferDao.upsert(
                TransferEntity(
                    transferId = transferId,
                    peerId = state.peerId,
                    fileName = destination.name,
                    fileSize = destination.length(),
                    direction = "IN",
                    status = "COMPLETED",
                    localUri = destination.absolutePath,
                    sha256 = actualHash,
                    progress = 100
                )
            )
            _lastEvent.value = "Received ${destination.name}"
        } else {
            transferDao.updateProgress(transferId, "FAILED", 0)
            _lastEvent.value = "Could not save file"
        }
        receiveStates.remove(transferId)
    }

    private suspend fun onCancel(transferId: String) {
        if (cancelled.add(transferId)) {
            val state = receiveStates.remove(transferId)
            state?.file?.delete()
            transferDao.updateProgress(transferId, "CANCELLED", 0)
            _lastEvent.value = "Transfer cancelled by peer"
        }
    }

    private fun decodeSingle(payload: String): String = payload

    private fun hashFile(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
