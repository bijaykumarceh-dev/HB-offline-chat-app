package com.hb.offlinechat.di

import android.content.Context
import androidx.room.Room
import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.connectivity.nearby.NearbyTransport
import com.hb.offlinechat.core.security.DeviceIdentity
import com.hb.offlinechat.data.local.AppDatabase
import com.hb.offlinechat.data.local.PreferencesManager
import com.hb.offlinechat.data.repository.DeviceRepository
import com.hb.offlinechat.data.repository.MessageRepository
import com.hb.offlinechat.messaging.group.GroupManager
import com.hb.offlinechat.transfer.files.FileTransferManager
import com.hb.offlinechat.voice.calling.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight manual dependency container.
 * Provides singletons for database, identity, transport and preferences.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "hb_offline_chat.db"
        ).build()
    }

    val preferences: PreferencesManager by lazy { PreferencesManager(context.applicationContext) }

    val deviceIdentity: DeviceIdentity by lazy { DeviceIdentity(context.applicationContext) }

    val nearbyTransport: NearbyTransport by lazy { NearbyTransport(context.applicationContext) }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val deviceRepository: DeviceRepository by lazy {
        DeviceRepository(nearbyTransport, database.deviceDao(), repositoryScope)
    }

    val connectionManager: ConnectionManager by lazy {
        ConnectionManager(nearbyTransport, deviceIdentity, repositoryScope)
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepository(
            context = context.applicationContext,
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            connectionManager = connectionManager,
            scope = repositoryScope
        )
    }

    val fileTransferManager: FileTransferManager by lazy {
        FileTransferManager(
            context = context.applicationContext,
            connection = connectionManager,
            transferDao = database.transferDao(),
            scope = repositoryScope
        )
    }

    val groupManager: GroupManager by lazy {
        GroupManager(
            connection = connectionManager,
            groupDao = database.groupDao(),
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            scope = repositoryScope
        )
    }

    val callManager: CallManager by lazy {
        CallManager(context.applicationContext, connectionManager, repositoryScope)
    }

    init {
        // Start device persistence and block-list synchronisation at app start.
        deviceRepository
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
            }
    }
}
