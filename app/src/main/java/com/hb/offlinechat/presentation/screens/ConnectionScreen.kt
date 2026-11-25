package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    deviceId: String,
    onBack: () -> Unit,
    onChat: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(AppDimensions.spacingL)) {
                    Text(
                        "Device: ${deviceId.take(8)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(AppDimensions.spacingS))
                    Text(
                        "State: DISCONNECTED",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp
                    )
                    Text(
                        "Connection engine is implemented in Phase 5.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = AppDimensions.spacingS)
                    )
                }
            }
            Spacer(Modifier.height(AppDimensions.spacingL))
            Button(onClick = onChat) { Text("Open chat") }
        }
    }
}
