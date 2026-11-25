package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.Device
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for real file/photos/videos/documents transfer. */
class FileTransferViewModel(app: Application) : AndroidViewModel(app) {
    private val container = AppContainer.getInstance(app)
    private val manager = container.fileTransferManager

    val transfers = manager.transfers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val incomingRequest = manager.incomingRequest
    val lastEvent = manager.lastEvent

    /** Only peers with an active Nearby connection can receive files. */
    val connectedDevices: StateFlow<List<Device>> = combine(
        container.connectionManager.discoveredDevices,
        container.connectionManager.connectedEndpoints
    ) { devices, endpoints ->
        devices.filter { device -> container.connectionManager.endpointIdForDevice(device.deviceId) in endpoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun send(uri: Uri, recipientDeviceId: String) {
        viewModelScope.launch { manager.sendFile(uri, recipientDeviceId) }
    }

    fun accept() = manager.acceptIncoming()
    fun reject() = manager.rejectIncoming()
    fun cancel(transferId: String) = manager.cancel(transferId)
}
