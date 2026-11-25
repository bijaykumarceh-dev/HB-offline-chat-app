package com.hb.offlinechat.data.repository

import com.hb.offlinechat.connectivity.nearby.NearbyTransport
import com.hb.offlinechat.data.local.DeviceDao
import com.hb.offlinechat.data.local.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Persists discovered peers and applies block/trust decisions to the transport.
 * Blocked peers are hidden from discovery and their connection requests are rejected.
 */
class DeviceRepository(
    private val transport: NearbyTransport,
    private val deviceDao: DeviceDao,
    private val scope: CoroutineScope
) {
    init {
        scope.launch(Dispatchers.IO) {
            transport.discoveredDevices.collect { devices ->
                devices.forEach { device ->
                    val existing = deviceDao.getDevice(device.deviceId)
                    deviceDao.upsert(
                        DeviceEntity(
                            deviceId = device.deviceId,
                            deviceName = device.deviceName,
                            publicKey = device.devicePublicKey,
                            transportType = device.transportType.name,
                            lastSeen = device.lastSeen,
                            isBlocked = existing?.isBlocked ?: false,
                            isTrusted = existing?.isTrusted ?: false
                        )
                    )
                }
            }
        }

        deviceDao.observeDevices()
            .map { devices -> devices.filter { it.isBlocked }.map { it.deviceId }.toSet() }
            .onEach { blocked -> transport.setBlockedDevices(blocked) }
            .launchIn(scope)
    }

    fun observeDevices(): Flow<List<DeviceEntity>> = deviceDao.observeDevices()

    suspend fun setBlocked(deviceId: String, blocked: Boolean) =
        deviceDao.setBlocked(deviceId, blocked)

    suspend fun setTrusted(deviceId: String, trusted: Boolean) =
        deviceDao.setTrusted(deviceId, trusted)
}
