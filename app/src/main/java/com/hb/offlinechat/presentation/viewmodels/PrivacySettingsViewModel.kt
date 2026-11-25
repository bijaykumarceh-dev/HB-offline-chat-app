package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.core.security.CryptoManager
import com.hb.offlinechat.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Privacy screen state: local identity, known peers and block/trust controls. */
class PrivacySettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = AppContainer.getInstance(app)
    private val deviceRepository = container.deviceRepository

    val devices = deviceRepository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myDeviceId: String get() = container.connectionManager.myDeviceId
    val myDeviceName: String get() = container.connectionManager.myDeviceName
    val myPublicKey: String get() = container.deviceIdentity.publicKeyBase64

    fun verificationCode(peerPublicKey: String): String? =
        if (peerPublicKey.isBlank()) null
        else CryptoManager.shortAuthenticationCode(myPublicKey, peerPublicKey)

    fun setBlocked(deviceId: String, blocked: Boolean) {
        viewModelScope.launch { deviceRepository.setBlocked(deviceId, blocked) }
    }

    fun setTrusted(deviceId: String, trusted: Boolean) {
        viewModelScope.launch { deviceRepository.setTrusted(deviceId, trusted) }
    }
}
