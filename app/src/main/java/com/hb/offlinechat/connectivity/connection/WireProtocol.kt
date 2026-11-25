package com.hb.offlinechat.connectivity.connection

/**
 * Discriminator for every message that crosses the local transport.
 *
 * All payloads are carried over a single Nearby Connections byte channel, so a
 * small envelope with a stable [kind] lets text, files, voice, groups and call
 * signalling coexist without launching competing transports.
 */
enum class WireKind {
    TEXT,
    READ_RECEIPT,
    TYPING,
    GROUP_TEXT,
    FILE_META,
    FILE_ACCEPT,
    FILE_REJECT,
    FILE_CHUNK,
    FILE_CANCEL,
    VOICE_META,
    VOICE_CHUNK,
    CALL_INVITE,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_END,
    CALL_AUDIO
}

/**
 * A decoded wire message. [payload] is the *decrypted* UTF-8 / Base64 content;
 * the [messageId] and [timestamp] are authenticated as part of the envelope.
 */
data class WireMessage(
    val kind: WireKind,
    val senderId: String,
    val messageId: String,
    val timestamp: Long,
    val payload: String
)

/** Wire envelope codec. Format is versioned and delimiter-safe. */
object WireProtocol {
    private const val VERSION = 1
    private const val FIELD = "\u001F"

    fun encode(message: WireMessage): ByteArray =
        listOf(
            VERSION.toString(),
            message.kind.name,
            message.messageId,
            message.senderId,
            message.timestamp.toString(),
            message.payload
        ).joinToString(FIELD).toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): WireMessage? {
        return try {
            val parts = String(bytes, Charsets.UTF_8).split(FIELD, limit = 6)
            if (parts.size != 6 || parts[0] != VERSION.toString()) return null
            WireMessage(
                kind = WireKind.valueOf(parts[1]),
                messageId = parts[2],
                senderId = parts[3],
                timestamp = parts[4].toLong(),
                payload = parts[5]
            )
        } catch (e: Exception) {
            null
        }
    }
}
