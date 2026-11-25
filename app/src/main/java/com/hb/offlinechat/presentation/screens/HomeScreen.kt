package com.hb.offlinechat.presentation.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onNearbyDevices: () -> Unit,
    onConnection: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onFileShare: () -> Unit,
    onVoiceMessages: () -> Unit,
    onSettings: () -> Unit,
    onVoiceCall: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HB Offline Chat") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppDimensions.spacingM)
        ) {
            // Profile + connection status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(AppDimensions.cardRadius)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = AppDimensions.spacingS)) {
                        Text("You", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Disconnected · Discover to connect",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onConnection) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "Connection status")
                    }
                }
            }

            Spacer(Modifier.height(AppDimensions.spacingM))

            // Primary actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingS)) {
                ActionCard(
                    icon = Icons.Filled.QrCodeScanner,
                    label = "Scan / Nearby",
                    modifier = Modifier.weight(1f),
                    onClick = onScan
                )
                ActionCard(
                    icon = Icons.Filled.Group,
                    label = "Group chat",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenGroup("local-group") }
                )
            }
            Spacer(Modifier.height(AppDimensions.spacingS))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingS)) {
                ActionCard(
                    icon = Icons.Filled.InsertDriveFile,
                    label = "Files",
                    modifier = Modifier.weight(1f),
                    onClick = onFileShare
                )
                ActionCard(
                    icon = Icons.Filled.Mic,
                    label = "Voice",
                    modifier = Modifier.weight(1f),
                    onClick = onVoiceMessages
                )
            }

            Spacer(Modifier.height(AppDimensions.spacingL))
            Text("Recent chats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(AppDimensions.spacingS))
            Text(
                text = "No conversations yet. Scan for nearby devices to start chatting.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onNearbyDevices)
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(AppDimensions.cardRadius)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(AppDimensions.spacingS))
            Text(label, fontSize = 13.sp)
        }
    }
}
