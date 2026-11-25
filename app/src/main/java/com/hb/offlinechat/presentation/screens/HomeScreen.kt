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
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.data.local.ConversationEntity
import com.hb.offlinechat.domain.model.ConnectionState
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val vm: HomeViewModel = viewModel()
    val conversations by vm.conversations.collectAsStateWithLifecycle(emptyList())
    val state by vm.connectionState.collectAsStateWithLifecycle(ConnectionState.DISCONNECTED)
    var query by remember { mutableStateOf("") }

    val filtered = remember(conversations, query) {
        val q = query.trim()
        if (q.isBlank()) conversations
        else conversations.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.lastMessage?.contains(q, ignoreCase = true) == true ||
                it.peerId.contains(q, ignoreCase = true)
        }
    }

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
                        Text(vm.myDeviceName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "$state · Discover to connect",
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
                    onClick = { onOpenGroup("") }
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
                    label = "Voice notes",
                    modifier = Modifier.weight(1f),
                    onClick = onVoiceMessages
                )
            }
            Spacer(Modifier.height(AppDimensions.spacingS))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimensions.spacingS)) {
                ActionCard(
                    icon = Icons.Filled.Call,
                    label = "Voice call",
                    modifier = Modifier.weight(1f),
                    onClick = onVoiceCall
                )
                ActionCard(
                    icon = Icons.Filled.Bluetooth,
                    label = "Devices",
                    modifier = Modifier.weight(1f),
                    onClick = onNearbyDevices
                )
            }

            Spacer(Modifier.height(AppDimensions.spacingL))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search chats") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AppDimensions.spacingS))
            Text("Recent chats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(AppDimensions.spacingS))

            if (filtered.isEmpty()) {
                Text(
                    text = if (query.isBlank())
                        "No conversations yet. Discover nearby devices to start chatting."
                    else "No chats match “$query”.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onNearbyDevices)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    filtered.forEach { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = {
                                if (conversation.isGroup) onOpenGroup(conversation.peerId)
                                else onOpenChat(conversation.peerId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(conversation: ConversationEntity, onClick: () -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conversation.lastMessageAt))
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.spacingS),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.title.ifBlank { conversation.peerId.take(8) }, fontWeight = FontWeight.Bold)
                Text(
                    conversation.lastMessage ?: "No messages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (conversation.lastMessageAt > 0L) {
                Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
