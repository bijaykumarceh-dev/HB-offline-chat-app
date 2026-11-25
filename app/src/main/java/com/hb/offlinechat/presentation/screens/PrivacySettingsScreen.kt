package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.PrivacySettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
    val vm: PrivacySettingsViewModel = viewModel()
    val devices by vm.devices.collectAsStateWithLifecycle(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & security") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(AppDimensions.spacingM)) {
                    Text("Your identity", fontWeight = FontWeight.Bold)
                    Text("${vm.myDeviceName} · ${vm.myDeviceId.take(8)}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Only this random ID and public key are broadcast. The private key stays in Android Keystore.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppDimensions.spacingS)
                    )
                }
            }

            Text(
                "Device verification",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingL)
            )
            Text(
                "Compare the 6-digit code shown for a peer on both devices. " +
                    "It is derived from the two public keys and detects an impostor.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "Known devices",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingL)
            )
            if (devices.isEmpty()) {
                Text(
                    "No devices discovered yet. Open Nearby Devices to find peers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                devices.forEach { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(AppDimensions.spacingS)) {
                            Text(device.deviceName, fontWeight = FontWeight.Bold)
                            Text("${device.deviceId.take(8)} · ${device.transportType}", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val code = vm.verificationCode(device.publicKey)
                            Text(
                                if (code != null) "Verification code: $code" else "Verification code unavailable",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Trusted", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = device.isTrusted,
                                    onCheckedChange = { vm.setTrusted(device.deviceId, it) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Blocked", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = device.isBlocked,
                                    onCheckedChange = { vm.setBlocked(device.deviceId, it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
