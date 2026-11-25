package com.hb.offlinechat.presentation.screens

import android.Manifest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.core.permissions.PermissionManager
import com.hb.offlinechat.domain.model.ConnectionState
import com.hb.offlinechat.domain.model.RequiredPermissions
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.NearbyDevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDevicesScreen(
    onBack: () -> Unit,
    onConnect: (String, String) -> Unit
) {
    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    val vm: NearbyDevicesViewModel = viewModel()

    val devices by vm.devices.collectAsStateWithLifecycle(emptyList())
    val state by vm.connectionState.collectAsStateWithLifecycle(ConnectionState.DISCONNECTED)
    val error by vm.error.collectAsStateWithLifecycle(null)
    val pending by vm.pendingConnection.collectAsStateWithLifecycle(null)
    val connectedEndpoints by vm.connectedEndpoints.collectAsStateWithLifecycle(emptySet())

    var granted by remember { mutableStateOf(permissionManager.allDiscoveryGranted()) }
    var showRationale by remember { mutableStateOf(false) }

    val permissions = remember { RequiredPermissions.forDiscovery().toTypedArray() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
        if (granted) vm.startDiscovery()
        else if (results.any { !it.value }) showRationale = true
    }

    LaunchedEffect(Unit) {
        if (granted) vm.startDiscovery() else launcher.launch(permissions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby devices") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (granted) vm.startDiscovery() else launcher.launch(permissions)
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)
        ) {
            Text(
                text = "You are ${vm.myDeviceName}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "State: $state",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = AppDimensions.spacingS)
            )
            error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                    modifier = Modifier.padding(top = AppDimensions.spacingS))
            }

            if (!granted) {
                Text(
                    "Required permissions are needed to discover nearby devices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppDimensions.spacingM)
                )
                Button(onClick = { launcher.launch(permissions) }, modifier = Modifier.padding(top = AppDimensions.spacingS)) {
                    Text("Grant permissions")
                }
            }

            if (devices.isEmpty() && granted) {
                Text(
                    "Searching for nearby HB Offline Chat devices…\n" +
                        "Ensure Bluetooth and Wi-Fi are enabled.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = AppDimensions.spacingM)
                )
            }

            devices.forEach { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimensions.spacingS)
                        .clickable {
                            vm.connect(device)
                            onConnect(device.deviceId, device.deviceName)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingM), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(device.deviceName, fontWeight = FontWeight.Bold)
                            Text("${device.transportType} · ${device.deviceId.take(8)}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val isConnected = devices.firstOrNull { it.deviceId == device.deviceId }
                            ?.let { vm.endpointIdForDevice(it.deviceId) in connectedEndpoints } == true
                        Text(
                            if (isConnected) "Connected ✓" else "Connect",
                            color = if (isConnected) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    pending?.let { request ->
        AlertDialog(
            onDismissRequest = { vm.rejectRequest(request.endpointId) },
            title = { Text("Connection request") },
            text = {
                Text("${request.info.deviceName} (${request.info.deviceId.take(8)}) wants to connect.")
            },
            confirmButton = {
                Button(onClick = { vm.acceptRequest(request.endpointId) }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { vm.rejectRequest(request.endpointId) }) { Text("Reject") }
            }
        )
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions required") },
            text = { Text("Discovery needs Bluetooth / location / nearby-devices permissions. " +
                "If you selected “Don't ask again”, open Android settings to grant them manually.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    context.startActivity(permissionManager.settingsIntent())
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false; launcher.launch(permissions) }) { Text("Retry") }
            }
        )
    }
}
