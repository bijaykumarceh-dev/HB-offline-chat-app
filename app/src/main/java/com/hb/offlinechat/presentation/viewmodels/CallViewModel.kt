package com.hb.offlinechat.presentation.viewmodels

import android.Manifest
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.Device
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for local voice calls. */
class CallViewModel(private val app: Application) : AndroidViewModel(app) {
    private val container = AppContainer.getInstance(app)
    private val call = container.callManager

    val state = call.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), call.state.value)
    val peer = call.peer
    val duration = call.duration
    val muted = call.muted
    val speaker = call.speaker
    val event = call.lastEvent

    val connectedDevices: StateFlow<List<Device>> = combine(
        container.connectionManager.discoveredDevices,
        container.connectionManager.connectedEndpoints
    ) { devices, endpoints ->
        devices.filter { container.connectionManager.endpointIdForDevice(it.deviceId) in endpoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun hasMicPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun startCall(deviceId: String) {
        if (!hasMicPermission()) {
            Toast.makeText(app, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch { call.startCall(deviceId) }
    }

    fun accept() {
        if (!hasMicPermission()) {
            Toast.makeText(app, "Microphone permission required", Toast.LENGTH_SHORT).show()
            return
        }
        call.accept()
    }

    fun reject() = call.reject()
    fun end() = call.end()
    fun toggleMute() = call.toggleMute()
    fun toggleSpeaker() = call.toggleSpeaker()

    override fun onCleared() {
        super.onCleared()
        call.shutdown()
    }
}
