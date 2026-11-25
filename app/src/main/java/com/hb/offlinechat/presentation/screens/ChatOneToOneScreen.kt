package com.hb.offlinechat.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.domain.model.DeliveryStatus
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.ChatViewModel

/** One-to-one chat UI backed by Room and the encrypted Nearby transport. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatOneToOneScreen(deviceId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: ChatViewModel = viewModel(factory = ChatViewModel.factory(deviceId, app))
    val messages by vm.messages.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat · ${deviceId.take(8)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { vm.retryQueued(); vm.retryFailed() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry queued messages")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingS)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom
            ) {
                messages.forEach { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = if (msg.senderId == deviceId) Arrangement.Start else Arrangement.End
                    ) {
                        Column {
                            Text(
                                text = msg.plainText ?: "[encrypted]",
                                color = if (msg.senderId == deviceId) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${msg.deliveryStatus} · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (msg.deliveryStatus == DeliveryStatus.FAILED.name) {
                            IconButton(onClick = { vm.delete(msg.messageId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete failed message")
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Message") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (draft.isNotBlank()) {
                        vm.send(draft)
                        draft = ""
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
