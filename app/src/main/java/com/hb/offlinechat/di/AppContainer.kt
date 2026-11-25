package com.hb.offlinechat.di

import android.content.Context
import androidx.room.Room
import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.connectivity.nearby.NearbyTransport
import com.hb.offlinechat.core.security.DeviceIdentity
import com.hb.offlinechat.data.local.AppDatabase
import com.hb.offlinechat.data.local.PreferencesManager
import com.hb.offlinechat.data.repository.MessageRepository
import com.hb.offlinechat.transfer.files.FileTransferManager
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

    val connectionManager: ConnectionManager by lazy {
        ConnectionManager(nearbyTransport, deviceIdentity)
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepository(database.messageDao(), connectionManager, repositoryScope)
    }

    val fileTransferManager: FileTransferManager by lazy {
        FileTransferManager(
            context = context.applicationContext,
            connection = connectionManager,
            transferDao = database.transferDao(),
            scope = repositoryScope
        )
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
