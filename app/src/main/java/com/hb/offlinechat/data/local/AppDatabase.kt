package com.hb.offlinechat.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val avatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val deviceName: String,
    val publicKey: String = "",
    val transportType: String = "NEARBY",
    val lastSeen: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isTrusted: Boolean = false
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerId: String,
    val title: String,
    val isGroup: Boolean = false,
    val lastMessage: String? = null,
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val messageType: String,
    val encryptedPayload: String? = null,
    val plainText: String? = null,
    val timestamp: Long,
    val deliveryStatus: String,
    val isDeleted: Boolean = false
)

@Entity(tableName = "groups", primaryKeys = ["groupId"])
data class GroupEntity(
    val groupId: String,
    val name: String,
    val avatarUri: String? = null,
    val coordinatorId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "deviceId"])
data class GroupMemberEntity(
    val groupId: String,
    val deviceId: String,
    val displayName: String,
    val role: String = "MEMBER"
)

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val transferId: String,
    val peerId: String,
    val fileName: String,
    val fileSize: Long,
    val direction: String,
    val status: String,
    val localUri: String? = null,
    val sha256: String? = null,
    val progress: Int = 0,
    val startedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey val callId: String,
    val peerId: String,
    val startTime: Long,
    val durationSec: Long = 0,
    val direction: String,
    val wasMissed: Boolean = false
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDevice(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Query("UPDATE devices SET isBlocked = :blocked WHERE deviceId = :deviceId")
    suspend fun setBlocked(deviceId: String, blocked: Boolean)

    @Query("UPDATE devices SET isTrusted = :trusted WHERE deviceId = :deviceId")
    suspend fun setTrusted(deviceId: String, trusted: Boolean)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun page(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE messageId = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("UPDATE messages SET deliveryStatus = 'QUEUED' WHERE deliveryStatus = 'FAILED'")
    suspend fun requeueFailed()

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun softDelete(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteConversationMessages(conversationId: String)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroup(groupId: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: GroupMemberEntity)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND deviceId = :deviceId")
    suspend fun removeMember(groupId: String, deviceId: String)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY startedAt DESC")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: TransferEntity)

    @Query("UPDATE transfers SET status = :status, progress = :progress WHERE transferId = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int)
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_history ORDER BY startTime DESC")
    fun observeCalls(): Flow<List<CallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallHistoryEntity)
}

@Database(
    entities = [
        UserEntity::class, DeviceEntity::class, ConversationEntity::class,
        MessageEntity::class, GroupEntity::class, GroupMemberEntity::class,
        TransferEntity::class, CallHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun transferDao(): TransferDao
    abstract fun callDao(): CallDao
}
