package com.hb.offlinechat.connectivity.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.hb.offlinechat.domain.model.ConnectionState
import com.hb.offlinechat.domain.model.Device
import com.hb.offlinechat.domain.model.EndpointInfo
import com.hb.offlinechat.domain.model.TransportType
import com.hb.offlinechat.service.notification.NotificationHelper
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** An incoming connection request awaiting the user's accept/reject decision. */
data class PendingConnection(val endpointId: String, val info: EndpointInfo)

/**
 * Google Nearby Connections transport — real local discovery and messaging.
 * Requires Google Play services; the caller is notified if unavailable.
 *
 * Both advertising and discovery run together (standard Nearby pattern): every
 * device advertises its own endpoint and discovers peers. Payloads are delivered
 * through a buffered [SharedFlow] so rapid file/audio chunks are never conflated.
 */
class NearbyTransport(private val context: Context) {

    private var client: ConnectionsClient? = null
    private val endpointInfo = mutableMapOf<String, EndpointInfo>()
    private val blockedDeviceIds = MutableStateFlow<Set<String>>(emptySet())

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingConnection = MutableStateFlow<PendingConnection?>(null)
    val pendingConnection: StateFlow<PendingConnection?> = _pendingConnection.asStateFlow()

    private val _payloads = MutableSharedFlow<Pair<String, ByteArray>>(
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    /** Incoming byte payloads from connected peers (first = endpoint id). */
    val payloads: SharedFlow<Pair<String, ByteArray>> = _payloads.asSharedFlow()

    /** Progress updates from Nearby for file transfer surfaces. */
    data class TransferUpdate(
        val endpointId: String,
        val payloadId: Long,
        val status: Int,
        val bytesTransferred: Long,
        val totalBytes: Long
    )

    private val _transferUpdates = MutableSharedFlow<TransferUpdate>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val transferUpdates: SharedFlow<TransferUpdate> = _transferUpdates.asSharedFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                _payloads.tryEmit(endpointId to bytes)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            _transferUpdates.tryEmit(
                TransferUpdate(
                    endpointId,
                    update.payloadId,
                    update.status,
                    update.bytesTransferred,
                    update.totalBytes
                )
            )
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val parsed = parseEndpointName(info.endpointName)
            if (parsed == null || parsed.deviceId in blockedDeviceIds.value) {
                client?.rejectConnection(endpointId)
                return
            }
            endpointInfo[endpointId] = parsed
            _connectionState.value = ConnectionState.VERIFYING
            NotificationHelper.post(context, "Connection request", parsed.deviceName, 3001)
            // Surface the request; UI must explicitly accept or reject.
            _pendingConnection.value = PendingConnection(endpointId, parsed)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            _pendingConnection.value = null
            if (result.status.isSuccess) {
                _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                _connectionState.value = ConnectionState.CONNECTED
            } else {
                _connectionState.value = ConnectionState.FAILED
                _error.value = "Connection rejected or failed"
            }
        }

        override fun onDisconnected(endpointId: String) {
            _pendingConnection.value = null
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            endpointInfo.remove(endpointId)
            if (_connectedEndpoints.value.isEmpty()) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val parsed = parseEndpointName(info.endpointName) ?: return
            if (parsed.deviceId in blockedDeviceIds.value) return
            endpointInfo[endpointId] = parsed
            val device = Device(
                deviceId = parsed.deviceId,
                deviceName = parsed.deviceName,
                devicePublicKey = parsed.publicKey,
                transportType = TransportType.NEARBY,
                lastSeen = System.currentTimeMillis()
            )
            _discoveredDevices.value =
                (_discoveredDevices.value.filterNot { it.deviceId == device.deviceId } + device)
                    .distinctBy { it.deviceId }
        }

        override fun onEndpointLost(endpointId: String) {
            endpointInfo.remove(endpointId)
            val deviceId = _discoveredDevices.value.firstOrNull { it.deviceId == endpointId }?.deviceId
            if (deviceId != null) {
                _discoveredDevices.value = _discoveredDevices.value.filterNot { it.deviceId == deviceId }
            }
        }
    }

    /** Start advertising + discovery. Requires permissions already granted by the caller. */
    fun start(localDeviceId: String, localDeviceName: String, localPublicKey: String): Boolean {
        return try {
            if (client == null) client = Nearby.getConnectionsClient(context)
            val name = encodeEndpointName(localDeviceId, localDeviceName, localPublicKey)
            client?.startAdvertising(name, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                ?.addOnFailureListener { _error.value = "Advertising failed: ${it.message}" }
            client?.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                ?.addOnSuccessListener { _connectionState.value = ConnectionState.DISCOVERING }
                ?.addOnFailureListener { _error.value = "Discovery failed: ${it.message}" }
            true
        } catch (e: Exception) {
            _error.value = "Nearby Connections unavailable: ${e.message}"
            false
        }
    }

    fun requestConnection(endpointId: String, localDeviceName: String) {
        _connectionState.value = ConnectionState.CONNECTING
        client?.requestConnection(localDeviceName, endpointId, connectionLifecycleCallback)
            ?.addOnFailureListener {
                _connectionState.value = ConnectionState.FAILED
                _error.value = "Connection request failed: ${it.message}"
            }
    }

    /** User-approved connection acceptance. Blocked peers are always rejected. */
    fun acceptConnection(endpointId: String) {
        val info = endpointInfo[endpointId]
        if (info != null && info.deviceId in blockedDeviceIds.value) {
            rejectConnection(endpointId)
            return
        }
        client?.acceptConnection(endpointId, payloadCallback)
            ?.addOnFailureListener { _error.value = "Accept failed: ${it.message}" }
    }

    /** User-driven rejection. */
    fun rejectConnection(endpointId: String) {
        client?.rejectConnection(endpointId)
        _pendingConnection.value = null
    }

    /** Applied by DeviceRepository from the persisted block list. */
    fun setBlockedDevices(deviceIds: Set<String>) {
        blockedDeviceIds.value = deviceIds
        _discoveredDevices.value = _discoveredDevices.value.filterNot { it.deviceId in deviceIds }
        deviceIds.forEach { id ->
            endpointInfo.entries.firstOrNull { it.value.deviceId == id }?.key?.let { endpointId ->
                if (endpointId in _connectedEndpoints.value) {
                    client?.disconnectFromEndpoint(endpointId)
                }
            }
        }
    }

    fun infoFor(endpointId: String): EndpointInfo? = endpointInfo[endpointId]

    /** Public key advertised by a discovered/connected endpoint. */
    fun publicKeyFor(endpointId: String): String = endpointInfo[endpointId]?.publicKey ?: ""

    /** Nearby internal endpoint id for a discovered app device id. */
    fun endpointIdForDevice(deviceId: String): String? =
        endpointInfo.entries.firstOrNull { it.value.deviceId == deviceId }?.key

    fun send(endpointId: String, bytes: ByteArray) {
        client?.sendPayload(listOf(endpointId), Payload.fromBytes(bytes))
    }

    /** Send to every currently connected peer (used by the group coordinator). */
    fun sendToAll(bytes: ByteArray) {
        val targets = _connectedEndpoints.value.toList()
        if (targets.isNotEmpty()) client?.sendPayload(targets, Payload.fromBytes(bytes))
    }

    /** Disconnect a single peer; Nearby callbacks update connection state. */
    fun disconnect(endpointId: String) {
        client?.disconnectFromEndpoint(endpointId)
    }

    fun stop() {
        _connectedEndpoints.value.forEach { endpointId ->
            client?.disconnectFromEndpoint(endpointId)
        }
        client?.stopAdvertising()
        client?.stopDiscovery()
        client = null
        endpointInfo.clear()
        _discoveredDevices.value = emptyList()
        _connectedEndpoints.value = emptySet()
        _pendingConnection.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    companion object {
        private const val TAG = "NearbyTransport"
        private const val SERVICE_ID = "com.hb.offlinechat.NEARBY"
        private val STRATEGY = Strategy.P2P_CLUSTER
        private val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        private val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        private const val PREFIX = "HBCHAT|"
        private const val SEP = "|"

        fun encodeEndpointName(deviceId: String, deviceName: String, publicKey: String): String =
            "$PREFIX$deviceId$SEP$deviceName$SEP$publicKey"

        fun parseEndpointName(name: String): EndpointInfo? {
            if (!name.startsWith(PREFIX)) return null
            val parts = name.removePrefix(PREFIX).split(SEP, limit = 3)
            return if (parts.size == 3) EndpointInfo(parts[0], parts[1], parts[2]) else null
        }
    }
}
