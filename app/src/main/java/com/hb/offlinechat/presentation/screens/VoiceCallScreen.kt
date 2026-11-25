package com.hb.offlinechat.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.CallViewModel
import com.hb.offlinechat.voice.calling.CallState

/** Real local voice call UI driven by CallManager state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCallScreen(onBack: () -> Unit) {
    val vm: CallViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle(CallState.IDLE)
    val peer by vm.peer.collectAsStateWithLifecycle(null)
    val duration by vm.duration.collectAsStateWithLifecycle(0L)
    val muted by vm.muted.collectAsStateWithLifecycle(false)
    val speaker by vm.speaker.collectAsStateWithLifecycle(false)
    val devices by vm.connectedDevices.collectAsStateWithLifecycle(emptyList())
    val event by vm.event.collectAsStateWithLifecycle(null)

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result reflected by hasMicPermission on next action */ }

    Scaffold(topBar = { TopAppBar(title = { Text("Voice call") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                CallState.IDLE -> {
                    Text("Start a local voice call", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Audio travels directly over the local connection — no internet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = AppDimensions.spacingS)
                    )
                    event?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                            modifier = Modifier.padding(top = AppDimensions.spacingS))
                    }
                    Spacer(Modifier.height(AppDimensions.spacingL))
                    Text("Connected devices", fontWeight = FontWeight.Bold)
                    if (devices.isEmpty()) {
                        Text("No connected devices. Use Nearby Devices first.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    } else {
                        devices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS)
                                    .clickable {
                                        if (!vm.hasMicPermission()) {
                                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        vm.startCall(device.deviceId)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Call ${device.deviceName}", modifier = Modifier.padding(AppDimensions.spacingM), fontSize = 14.sp)
                            }
                        }
                    }
                }

                CallState.OUTGOING -> {
                    Text("Calling ${peer?.name ?: "peer"}…", fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(AppDimensions.spacingL))
                    FloatingActionButton(
                        onClick = { vm.end() },
                        containerColor = Color(0xFFFF3B30),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = "End call")
                    }
                    TextButtonCompat("Cancel", onClick = { vm.end() })
                }

                CallState.INCOMING -> {
                    Text("Incoming call", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("From ${peer?.name ?: "unknown"}", color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(AppDimensions.spacingL))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingL)) {
                        FloatingActionButton(
                            onClick = { vm.accept() },
                            containerColor = Color(0xFF30D158),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Accept")
                        }
                        FloatingActionButton(
                            onClick = { vm.reject() },
                            containerColor = Color(0xFFFF3B30),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "Reject")
                        }
                    }
                }

                CallState.ACTIVE -> {
                    Text("Call in progress", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Duration: %02d:%02d".format(duration / 60, duration % 60),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(AppDimensions.spacingL))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingM)) {
                        FloatingActionButton(onClick = { vm.toggleMute() }) {
                            Icon(Icons.Filled.MicOff, contentDescription = "Mute")
                        }
                        FloatingActionButton(onClick = { vm.toggleSpeaker() }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Speaker")
                        }
                        FloatingActionButton(
                            onClick = { vm.end() },
                            containerColor = Color(0xFFFF3B30)
                        ) {
                            Icon(Icons.Filled.CallEnd, contentDescription = "End call")
                        }
                    }
                    Text("Muted: $muted · Speaker: $speaker", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppDimensions.spacingS))
                }
            }
        }
    }
}

@Composable
private fun TextButtonCompat(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}
