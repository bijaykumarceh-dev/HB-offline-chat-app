package com.hb.offlinechat.connectivity.connection

import android.util.Log
import com.hb.offlinechat.connectivity.nearby.NearbyTransport
import com.hb.offlinechat.core.security.CryptoManager
import com.hb.offlinechat.core.security.DeviceIdentity
import com.hb.offlinechat.core.security.ReplayProtection
import com.hb.offlinechat.domain.model.ConnectionState
import com.hb.offlinechat.domain.model.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import java.util.UUID
import javax.crypto.SecretKey

/**
 * Unified connection facade: owns the selected transport, establishes the
 * application-level session key, and exposes authenticated/encrypted messaging
 * primitives for every feature (text, files, voice, groups, calls).
 */
class ConnectionManager(
    private val transport: NearbyTransport,
    private val identity: DeviceIdentity,
    scope: CoroutineScope
) {
    private val replay = ReplayProtection()
    private val sessionKeys = mutableMapOf<String, SecretKey>()

    val discoveredDevices: Flow<List<Device>> = transport.discoveredDevices
    val connectionState: Flow<ConnectionState> = transport.connectionState
    val connectedEndpoints: StateFlow<Set<String>> = transport.connectedEndpoints
    val error: Flow<String?> = transport.error
    val pendingConnection = transport.pendingConnection
    val transferUpdates = transport.transferUpdates

    val myDeviceId: String get() = identity.deviceId
    val myDeviceName: String get() = identity.deviceName

    fun startDiscovery(): Boolean =
        transport.start(identity.deviceId, identity.deviceName, identity.publicKeyBase64)

    fun requestConnection(endpointId: String) =
        transport.requestConnection(endpointId, identity.deviceName)

    fun acceptConnection(endpointId: String) = transport.acceptConnection(endpointId)
    fun rejectConnection(endpointId: String) = transport.rejectConnection(endpointId)

    fun endpointIdForDevice(deviceId: String): String? = transport.endpointIdForDevice(deviceId)
    fun deviceIdForEndpoint(endpointId: String): String? = transport.infoFor(endpointId)?.deviceId
    fun connectedEndpointIds(): Set<String> = transport.connectedEndpoints.value

    fun publicKeyForDevice(deviceId: String): String =
        endpointIdForDevice(deviceId)?.let { transport.publicKeyFor(it) } ?: ""

    fun disconnectDevice(deviceId: String) {
        endpointIdForDevice(deviceId)?.let { transport.disconnect(it) }
    }

    fun stop() {
        transport.stop()
        sessionKeys.clear()
    }

    /** Establish the session key with a peer using ECDH + HKDF. */
    private fun ensureSessionKey(endpointId: String, peerPublicKey: String): SecretKey? {
        sessionKeys[endpointId]?.let { return it }
        return try {
            val secret = identity.deriveSharedSecret(peerPublicKey)
            val key = CryptoManager.deriveSessionKey(secret, SALT, INFO)
            sessionKeys[endpointId] = key
            key
        } catch (e: Exception) {
            Log.w(TAG, "Session key unavailable (legacy device / no key agreement): ${e.message}")
            null
        }
    }

    /**
     * Encrypt and send a payload of any kind to a connected peer.
     * [messageId] may be supplied by callers that need to correlate replies.
     */
    fun sendWire(
        endpointId: String,
        peerPublicKey: String,
        kind: WireKind,
        plainPayload: String,
        messageId: String = UUID.randomUUID().toString()
    ): WireMessage? {
        val key = ensureSessionKey(endpointId, peerPublicKey) ?: return null
        val wire = WireMessage(
            kind = kind,
            senderId = identity.deviceId,
            messageId = messageId,
            timestamp = System.currentTimeMillis(),
            payload = CryptoManager.encrypt(plainPayload, key)
        )
        transport.send(endpointId, WireProtocol.encode(wire))
        return wire
    }

    /** Address a peer by the app-generated device id rather than the Nearby endpoint id. */
    fun sendWireToDevice(deviceId: String, kind: WireKind, plainPayload: String): Boolean {
        val endpointId = endpointIdForDevice(deviceId) ?: return false
        return sendWire(endpointId, transport.publicKeyFor(endpointId), kind, plainPayload) != null
    }

    /**
     * Broadcast to all connected peers. Each peer gets its own ECDH-derived key,
     * so encryption is performed per recipient (the group coordinator role).
     */
    fun sendWireToAll(kind: WireKind, plainPayload: String, messageId: String = UUID.randomUUID().toString()): Int {
        var sent = 0
        connectedEndpoints.value.forEach { endpointId ->
            if (sendWire(endpointId, transport.publicKeyFor(endpointId), kind, plainPayload, messageId) != null) {
                sent++
            }
        }
        return sent
    }

    /**
     * All authenticated, replay-checked incoming wire messages.
     * Decoded exactly once so replay protection is not consumed by multiple
     * feature collectors (chat, files, groups, calls).
     */
    val incomingWire: SharedFlow<WireMessage> = transport.payloads
        .mapNotNull { (endpointId, bytes) -> decryptWire(bytes, transport.publicKeyFor(endpointId)) }
        .shareIn(scope, SharingStarted.WhileSubscribed(5000), replay = 0)

    private fun decryptWire(bytes: ByteArray, peerPublicKey: String): WireMessage? {
        return try {
            val envelope = WireProtocol.decode(bytes) ?: return null
            if (!replay.isFresh(envelope.messageId, envelope.timestamp)) return null
            val key = ensureSessionKey(envelope.senderId, peerPublicKey) ?: return null
            val plain = CryptoManager.decrypt(envelope.payload, key) ?: return null
            envelope.copy(payload = plain)
        } catch (e: Exception) {
            null
        }
    }

    // ---- Backwards-compatible text-chat API --------------------------------

    fun sendText(endpointId: String, peerPublicKey: String, text: String): WireMessage? =
        sendWire(endpointId, peerPublicKey, WireKind.TEXT, text)

    fun sendTextToDevice(deviceId: String, text: String): Boolean =
        sendWireToDevice(deviceId, WireKind.TEXT, text)

    fun receive(bytes: ByteArray, peerPublicKey: String): DecodedMessage? {
        val wire = decryptWire(bytes, peerPublicKey) ?: return null
        if (wire.kind != WireKind.TEXT) return null
        return DecodedMessage(wire.messageId, wire.senderId, wire.payload, wire.timestamp)
    }

    val incomingMessages: Flow<DecodedMessage> = incomingWire.filter { it.kind == WireKind.TEXT }
        .map { DecodedMessage(it.messageId, it.senderId, it.payload, it.timestamp) }

    data class DecodedMessage(
        val messageId: String,
        val senderId: String,
        val text: String,
        val timestamp: Long
    )

    companion object {
        private const val TAG = "ConnectionManager"
        private val SALT = "hb-offline-chat-v1-salt".toByteArray()
        private val INFO = "hb-session-key".toByteArray()
    }
}
