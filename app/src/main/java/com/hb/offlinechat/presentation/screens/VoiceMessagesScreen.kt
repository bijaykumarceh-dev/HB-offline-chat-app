package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hb.offlinechat.presentation.theme.AppDimensions

/** Voice messages UI. Recorder + playback implemented in Phase 11. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMessagesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice messages") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)) {
            Text("Record a voice message and send it to a connected device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { /* MediaRecorder wired in Phase 11 */ }, modifier = Modifier.padding(top = AppDimensions.spacingM)) {
                Text("Start recording")
            }
            Text("No voice messages yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = AppDimensions.spacingM))
        }
    }
}
