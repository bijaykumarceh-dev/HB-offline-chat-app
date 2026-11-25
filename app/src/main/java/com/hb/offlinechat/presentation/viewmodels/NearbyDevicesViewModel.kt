package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.ConnectionState
import com.hb.offlinechat.domain.model.Device
import kotlinx.coroutines.launch

/** ViewModel for the Nearby Devices discovery screen. */
class NearbyDevicesViewModel(app: Application) : AndroidViewModel(app) {
    private val connection = AppContainer.getInstance(app).connectionManager

    val devices = connection.discoveredDevices
    val connectionState = connection.connectionState
    val error = connection.error
    val myDeviceName: String get() = connection.myDeviceName

    fun startDiscovery() {
        connection.startDiscovery()
    }

    fun connect(device: Device) {
        val endpointId = connection.endpointIdForDevice(device.deviceId)
        if (endpointId != null) connection.requestConnection(endpointId)
    }

    fun stop() {
        viewModelScope.launch { connection.stop() }
    }

    fun isConnected(state: ConnectionState) = state == ConnectionState.CONNECTED
}
