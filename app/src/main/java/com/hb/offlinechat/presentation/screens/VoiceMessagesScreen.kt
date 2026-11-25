package com.hb.offlinechat.presentation.screens

import android.Manifest
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.VoiceMessagesViewModel

/** Real voice messages: record with MediaRecorder, send over the encrypted transfer pipeline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMessagesScreen(onBack: () -> Unit) {
    val vm: VoiceMessagesViewModel = viewModel()
    val recording by vm.isRecording.collectAsStateWithLifecycle(false)
    val elapsed by vm.elapsed.collectAsStateWithLifecycle(0L)
    val voices by vm.receivedVoices.collectAsStateWithLifecycle(emptyList())
    val devices by vm.connectedDevices.collectAsStateWithLifecycle(emptyList())
    val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle(null)
    val event by vm.lastEvent.collectAsStateWithLifecycle(null)

    var selectedRecipient by remember { mutableStateOf<String?>(null) }
    var showRecorder by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showRecorder = true
            vm.startRecording()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)) {
            Text(
                "Record a voice note and send it to a connected device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Button(
                onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                enabled = !recording,
                modifier = Modifier.padding(top = AppDimensions.spacingM)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Text("  Record voice message")
            }

            if (recording) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingM),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recording ${elapsed / 1000}s", color = MaterialTheme.colorScheme.onErrorContainer)
                        Row {
                            TextButton(onClick = { vm.cancelRecording() }) { Text("Cancel") }
                            Button(onClick = { vm.stopRecording() }) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                            }
                        }
                    }
                }
            }

            event?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp,
                    modifier = Modifier.padding(top = AppDimensions.spacingS))
            }

            Text("Connected devices", fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingM))
            if (devices.isEmpty()) {
                Text("No connected devices. Use Nearby Devices first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            } else {
                devices.forEach { device ->
                    val marker = if (selectedRecipient == device.deviceId) "  ✓" else ""
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS)
                            .clickable { selectedRecipient = device.deviceId },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRecipient == device.deviceId)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("${device.deviceName}$marker", modifier = Modifier.padding(AppDimensions.spacingS), fontSize = 13.sp)
                    }
                }
            }

            Button(
                onClick = { selectedRecipient?.let { vm.sendRecording(it) } },
                enabled = selectedRecipient != null && !recording,
                modifier = Modifier.padding(top = AppDimensions.spacingM)
            ) {
                Text("Send recording")
            }

            Text("Received voice messages", fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingL))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(voices, key = { it.path }) { voice ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingS),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(voice.name, fontSize = 13.sp)
                                Text("From ${voice.peerId.take(8)}", fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (nowPlaying == voice.path) {
                                Button(onClick = { vm.stopPlayback() }) { Text("Stop") }
                            } else {
                                IconButton(onClick = { vm.play(voice) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecorder && recording) {
        // Dialog kept for accessibility/focus of the recording state.
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Recording in progress") },
            text = { Text("Tap Stop when finished.") },
            confirmButton = { Button(onClick = { vm.stopRecording(); showRecorder = false }) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { vm.cancelRecording(); showRecorder = false }) { Text("Cancel") } }
        )
    }
}
