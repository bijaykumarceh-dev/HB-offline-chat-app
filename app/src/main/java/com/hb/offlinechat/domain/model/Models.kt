package com.hb.offlinechat.domain.model

import android.os.Build

/** How a device was reached. */
enum class TransportType { NEARBY, WIFI_DIRECT, BLUETOOTH, UNKNOWN }

/** Connection lifecycle states shared by all transports. */
enum class ConnectionState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    VERIFYING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/** Delivery status of a locally stored message. */
enum class DeliveryStatus {
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

/** Message content kinds. */
enum class MessageType { TEXT, EMOJI, IMAGE, VIDEO, AUDIO, DOCUMENT, VOICE_MESSAGE, CONTROL }

/**
 * Nearby device. Only an app-generated identifier and display name are shared;
 * no private personal information is broadcast.
 */
data class Device(
    val deviceId: String,
    val deviceName: String,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val devicePublicKey: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val transportType: TransportType = TransportType.NEARBY
)

/** Transport endpoint information carried in Nearby Connections endpoint info. */
data class EndpointInfo(
    val deviceId: String,
    val deviceName: String,
    val publicKey: String,
    val protocolVersion: Int = 1
)

/** A chat message persisted in Room and exchanged over the transport. */
data class Message(
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val messageType: MessageType,
    val encryptedPayload: String,
    val timestamp: Long,
    val deliveryStatus: DeliveryStatus,
    val plainTextForUi: String? = null
)

/** Version-aware runtime permission planning. */
object RequiredPermissions {
    fun forDiscovery(): List<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += android.Manifest.permission.BLUETOOTH_SCAN
            perms += android.Manifest.permission.BLUETOOTH_CONNECT
            perms += android.Manifest.permission.BLUETOOTH_ADVERTISE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += android.Manifest.permission.NEARBY_WIFI_DEVICES
                perms += android.Manifest.permission.POST_NOTIFICATIONS
            }
        } else {
            perms += android.Manifest.permission.ACCESS_FINE_LOCATION
        }
        return perms
    }

    fun forRecording(): List<String> = listOf(android.Manifest.permission.RECORD_AUDIO)

    fun forFileTransfer(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
}
