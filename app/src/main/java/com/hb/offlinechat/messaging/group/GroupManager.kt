package com.hb.offlinechat.messaging.group

import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.connectivity.connection.WireKind
import com.hb.offlinechat.data.local.ConversationDao
import com.hb.offlinechat.data.local.ConversationEntity
import com.hb.offlinechat.data.local.GroupDao
import com.hb.offlinechat.data.local.GroupEntity
import com.hb.offlinechat.data.local.GroupMemberEntity
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
 * Host-based group messaging.
 *
 * The group creator is the coordinator. Members send to the coordinator, which
 * forwards to every connected peer with the original message id, giving natural
 * duplicate detection. Transport is single-hop Nearby Connections P2P_CLUSTER —
 * there is no multi-hop mesh routing (documented limitation).
 */
class GroupManager(
    private val connection: ConnectionManager,
    private val groupDao: GroupDao,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val scope: CoroutineScope
) {
    companion object {
        private const val RS = "\u001E"
    }

    val groups: Flow<List<GroupEntity>> = groupDao.observeGroups()
    fun members(groupId: String): Flow<List<GroupMemberEntity>> = groupDao.observeMembers(groupId)
    fun messages(groupId: String): Flow<List<MessageEntity>> = messageDao.observeMessages(conversationId(groupId))

    val myDeviceId: String get() = connection.myDeviceId

    init {
        scope.launch(Dispatchers.IO) {
            connection.incomingWire.collect { wire ->
                if (wire.kind != WireKind.GROUP_TEXT) return@collect
                val parts = wire.payload.split(RS, limit = 2)
                if (parts.size != 2) return@collect
                val groupId = parts[0]
                val text = parts[1]
                persist(groupId, wire.senderId, wire.messageId, text)

                val coordinator = isCoordinator(groupId)
                if (coordinator) {
                    // Forward to all other connected peers preserving messageId.
                    val peers = connection.connectedEndpoints.value.filter { endpoint ->
                        connection.deviceIdForEndpoint(endpoint) != wire.senderId
                    }
                    peers.forEach { endpoint ->
                        connection.sendWire(
                            endpoint,
                            connection.publicKeyForDevice(
                                connection.deviceIdForEndpoint(endpoint) ?: ""
                            ),
                            WireKind.GROUP_TEXT,
                            wire.payload,
                            wire.messageId
                        )
                    }
                }
            }
        }
    }

    suspend fun createGroup(name: String): String {
        val groupId = UUID.randomUUID().toString()
        groupDao.upsertGroup(
            GroupEntity(
                groupId = groupId,
                name = name,
                coordinatorId = connection.myDeviceId
            )
        )
        groupDao.upsertMember(
            GroupMemberEntity(
                groupId = groupId,
                deviceId = connection.myDeviceId,
                displayName = connection.myDeviceName,
                role = "COORDINATOR"
            )
        )
        return groupId
    }

    suspend fun addMember(groupId: String, deviceId: String, displayName: String) {
        groupDao.upsertMember(
            GroupMemberEntity(groupId, deviceId, displayName, "MEMBER")
        )
    }

    suspend fun removeMember(groupId: String, deviceId: String) =
        groupDao.removeMember(groupId, deviceId)

    suspend fun leaveGroup(groupId: String) = groupDao.removeMember(groupId, connection.myDeviceId)

    /** Send a group message, routing through the coordinator when this device is a member. */
    suspend fun send(groupId: String, text: String) {
        val messageId = UUID.randomUUID().toString()
        val payload = listOf(groupId, text).joinToString(RS)
        persist(groupId, connection.myDeviceId, messageId, text)

        if (isCoordinator(groupId)) {
            connection.sendWireToAll(WireKind.GROUP_TEXT, payload, messageId)
        } else {
            val coordinatorId = coordinatorId(groupId)
            if (coordinatorId != null) {
                connection.sendWireToDevice(coordinatorId, WireKind.GROUP_TEXT, payload)
            }
        }
    }

    private suspend fun persist(groupId: String, senderId: String, messageId: String, text: String) {
        val now = System.currentTimeMillis()
        messageDao.upsert(
            MessageEntity(
                messageId = messageId,
                conversationId = conversationId(groupId),
                senderId = senderId,
                receiverId = groupId,
                messageType = MessageType.TEXT.name,
                plainText = text,
                timestamp = now,
                deliveryStatus = DeliveryStatus.DELIVERED.name
            )
        )
        conversationDao.upsert(
            ConversationEntity(
                conversationId = conversationId(groupId),
                peerId = groupId,
                title = "Group ${groupId.take(6)}",
                isGroup = true,
                lastMessage = text,
                lastMessageAt = now
            )
        )
    }

    private suspend fun isCoordinator(groupId: String): Boolean =
        groupDao.getGroup(groupId)?.coordinatorId == connection.myDeviceId

    private suspend fun coordinatorId(groupId: String): String? =
        groupDao.getGroup(groupId)?.coordinatorId

    fun conversationId(groupId: String): String = "grp_$groupId"
}
