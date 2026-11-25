package com.hb.offlinechat.data.repository

import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.data.local.MessageDao
import com.hb.offlinechat.data.local.MessageEntity
import com.hb.offlinechat.domain.model.DeliveryStatus
import com.hb.offlinechat.domain.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Message repository: persists messages in Room, sends them over the encrypted
 * connection, and keeps an offline outgoing queue for disconnected peers.
 */
class MessageRepository(
    private val messageDao: MessageDao,
    private val connectionManager: ConnectionManager,
    private val scope: CoroutineScope
) {

    init {
        // Persist every authenticated incoming message.
        scope.launch(Dispatchers.IO) {
            connectionManager.incomingMessages.collect { incoming ->
                val conversationId = conversationId(incoming.senderId)
                messageDao.upsert(
                    MessageEntity(
                        messageId = incoming.messageId.ifBlank { UUID.randomUUID().toString() },
                        conversationId = conversationId,
                        senderId = incoming.senderId,
                        receiverId = connectionManager.myDeviceId,
                        messageType = MessageType.TEXT.name,
                        encryptedPayload = null,
                        plainText = incoming.text,
                        timestamp = incoming.timestamp,
                        deliveryStatus = DeliveryStatus.DELIVERED.name
                    )
                )
            }
        }
    }

    fun observeMessages(peerId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(conversationId(peerId))

    /**
     * Queue and send a text message. If no transport session is available the
     * message stays QUEUED and is retried by [retryQueued].
     */
    suspend fun sendText(
        peerId: String,
        peerPublicKey: String,
        endpointId: String?,
        text: String
    ): MessageEntity {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val envelope = if (endpointId != null) {
            connectionManager.sendText(endpointId, peerPublicKey, text)
        } else null

        val entity = MessageEntity(
            messageId = id,
            conversationId = conversationId(peerId),
            senderId = connectionManager.myDeviceId,
            receiverId = peerId,
            messageType = MessageType.TEXT.name,
            encryptedPayload = envelope?.payload,
            plainText = text,
            timestamp = now,
            deliveryStatus = if (envelope != null) DeliveryStatus.SENT.name else DeliveryStatus.QUEUED.name
        )
        messageDao.upsert(entity)
        return entity
    }

    /** Retry all queued/failed messages when a peer reconnects. */
    suspend fun retryQueued(peerId: String, peerPublicKey: String, endpointId: String) {
        messageDao.page(conversationId(peerId), 500, 0)
            .filter { it.deliveryStatus == DeliveryStatus.QUEUED.name || it.deliveryStatus == DeliveryStatus.FAILED.name }
            .forEach { msg ->
                val plain = msg.plainText
                if (plain != null) {
                    val envelope = connectionManager.sendText(endpointId, peerPublicKey, plain)
                    if (envelope != null) {
                        messageDao.updateStatus(msg.messageId, DeliveryStatus.SENT.name)
                    }
                }
            }
    }

    suspend fun retryFailed() = messageDao.requeueFailed()

    suspend fun deleteMessage(messageId: String) = messageDao.softDelete(messageId)

    fun conversationId(peerId: String): String {
        val ids = listOf(connectionManager.myDeviceId, peerId).sorted()
        return "dm_${ids[0]}_${ids[1]}"
    }
}
