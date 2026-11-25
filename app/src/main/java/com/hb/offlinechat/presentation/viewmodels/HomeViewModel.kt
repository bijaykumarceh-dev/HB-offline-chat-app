package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.ConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Home state sourced from Room and the live connection transport. */
class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = AppContainer.getInstance(app)

    val conversations = container.database.conversationDao().observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionState = container.connectionManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.DISCONNECTED)

    val myDeviceName: String get() = container.connectionManager.myDeviceName
}
