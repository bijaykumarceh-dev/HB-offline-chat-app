package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.data.local.MessageEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for one-to-one chat backed by Room + encrypted transport. */
class ChatViewModel(
    app: Application,
    private val peerId: String
) : AndroidViewModel(app) {

    private val container = AppContainer.getInstance(app)
    private val repository = container.messageRepository
    private val connection = container.connectionManager

    val messages: StateFlow<List<MessageEntity>> = repository.observeMessages(peerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendText(
                peerId = peerId,
                peerPublicKey = connection.publicKeyForDevice(peerId),
                endpointId = connection.endpointIdForDevice(peerId),
                text = text
            )
        }
    }

    fun delete(messageId: String) {
        viewModelScope.launch { repository.deleteMessage(messageId) }
    }

    fun retryFailed() {
        viewModelScope.launch { repository.retryFailed() }
    }

    fun retryQueued() {
        val endpointId = connection.endpointIdForDevice(peerId)
        if (endpointId != null) {
            viewModelScope.launch {
                repository.retryQueued(peerId, connection.publicKeyForDevice(peerId), endpointId)
            }
        }
    }

    companion object {
        fun factory(peerId: String, app: Application): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(app, peerId) as T
            }
    }
}
