package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.ConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Live connection status for a specific discovered peer. */
class ConnectionViewModel(
    app: Application,
    private val deviceId: String
) : AndroidViewModel(app) {
    private val connection = AppContainer.getInstance(app).connectionManager

    val state = connection.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.DISCONNECTED)

    val endpointId: String? get() = connection.endpointIdForDevice(deviceId)

    val isConnected = connection.connectedEndpoints
        .map { endpoints -> endpointId?.let { it in endpoints } ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun retry() {
        endpointId?.let { connection.requestConnection(it) }
    }

    fun disconnect() = connection.disconnectDevice(deviceId)

    companion object {
        fun factory(app: Application, deviceId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ConnectionViewModel(app, deviceId) as T
            }
    }
}
