package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit) {
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
            Text("Device verification", color = MaterialTheme.colorScheme.onSurface)
            Text("QR / short-code verification is implemented in Phase 6.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Text("Blocked devices", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = AppDimensions.spacingM))
            Text("No devices blocked.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}
