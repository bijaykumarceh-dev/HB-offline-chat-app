package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.Device
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for a single host-coordinated group conversation. */
class GroupChatViewModel(
    app: Application,
    val groupId: String
) : AndroidViewModel(app) {

    private val container = AppContainer.getInstance(app)
    private val manager = container.groupManager
    private val connection = container.connectionManager

    val messages = manager.messages(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members = manager.members(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups = manager.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectedDevices: StateFlow<List<Device>> = combine(
        connection.discoveredDevices,
        connection.connectedEndpoints
    ) { devices, endpoints ->
        devices.filter { connection.endpointIdForDevice(it.deviceId) in endpoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myDeviceId: String get() = connection.myDeviceId

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { manager.send(groupId, text) }
    }

    fun addMember(device: Device) {
        viewModelScope.launch { manager.addMember(groupId, device.deviceId, device.deviceName) }
    }

    fun removeMember(deviceId: String) {
        viewModelScope.launch { manager.removeMember(groupId, deviceId) }
    }

    companion object {
        fun factory(app: Application, groupId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GroupChatViewModel(app, groupId) as T
            }
    }
}

/** ViewModel for creating and listing groups. */
class GroupListViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = AppContainer.getInstance(app).groupManager
    val groups = manager.groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, onCreated: (String) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch { onCreated(manager.createGroup(name.trim())) }
    }
}
