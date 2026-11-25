package com.hb.offlinechat.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.core.utils.FileUtils
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.FileTransferViewModel

/** Real local file, photo, video and document sharing over the encrypted transport. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSharingScreen(onBack: () -> Unit) {
    val vm: FileTransferViewModel = viewModel()
    val context = LocalContext.current
    val devices by vm.connectedDevices.collectAsStateWithLifecycle(emptyList())
    val transfers by vm.transfers.collectAsStateWithLifecycle(emptyList())
    val incoming by vm.incomingRequest.collectAsStateWithLifecycle(null)
    val event by vm.lastEvent.collectAsStateWithLifecycle(null)

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var selectedSize by remember { mutableStateOf(0L) }
    var recipient by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val info = FileUtils.info(context, uri)
            selectedUri = uri
            selectedName = info.name
            selectedSize = info.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File sharing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)
        ) {
            Text(
                "Send files directly to connected devices — no internet required.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Button(
                onClick = { picker.launch("*/*") },
                modifier = Modifier.padding(top = AppDimensions.spacingM)
            ) {
                Text("Select file")
            }

            selectedName?.let { name ->
                Text(
                    "Selected: $name (${FileUtils.readableSize(selectedSize)})",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = AppDimensions.spacingS)
                )
            }

            Text(
                "Connected devices:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingM)
            )
            if (devices.isEmpty()) {
                Text(
                    "No connected devices. Use Nearby Devices first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            } else {
                devices.forEach { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppDimensions.spacingS)
                            .clickable { recipient = device.deviceId },
                        colors = CardDefaults.cardColors(
                            containerColor = if (recipient == device.deviceId)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        val marker = if (recipient == device.deviceId) "  ✓" else ""
                        Text(
                            "${device.deviceName}$marker",
                            modifier = Modifier.padding(AppDimensions.spacingS),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val uri = selectedUri
                    val target = recipient
                    if (uri != null && target != null) vm.send(uri, target)
                },
                enabled = selectedUri != null && recipient != null,
                modifier = Modifier.padding(top = AppDimensions.spacingM)
            ) {
                Text("Send file")
            }

            event?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = AppDimensions.spacingS))
            }

            Text(
                "Transfers",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingL)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(transfers, key = { it.transferId }) { transfer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(AppDimensions.spacingS)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(transfer.fileName.ifBlank { transfer.transferId.take(8) }, fontSize = 13.sp)
                                Text("${transfer.direction} · ${transfer.status}", fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LinearProgressIndicator(
                                progress = { transfer.progress / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS)
                            )
                            if (transfer.status in setOf("TRANSFERRING", "PENDING")) {
                                TextButton(onClick = { vm.cancel(transfer.transferId) }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    incoming?.let { request ->
        AlertDialog(
            onDismissRequest = { vm.reject() },
            title = { Text("Incoming file") },
            text = {
                Text("${request.senderId.take(8)} wants to send ${request.name} " +
                    "(${FileUtils.readableSize(request.size)}).")
            },
            confirmButton = { Button(onClick = { vm.accept() }) { Text("Accept") } },
            dismissButton = { TextButton(onClick = { vm.reject() }) { Text("Reject") } }
        )
    }
}
