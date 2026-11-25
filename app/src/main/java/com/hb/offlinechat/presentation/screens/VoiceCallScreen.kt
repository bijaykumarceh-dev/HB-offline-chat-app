package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

/** Voice call UI. Audio transport implemented in Phase 12. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCallScreen(
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onEnd: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice call") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Calling…", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(AppDimensions.spacingS))
            Text("Duration: 00:00", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(AppDimensions.spacingL))
            FloatingActionButton(
                onClick = onEnd,
                containerColor = Color(0xFFFF3B30),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Filled.CallEnd, contentDescription = "End call")
            }
            Spacer(Modifier.height(AppDimensions.spacingM))
            FloatingActionButton(onClick = onAccept) {
                Icon(Icons.Filled.MicOff, contentDescription = "Mute")
            }
        }
    }
}
