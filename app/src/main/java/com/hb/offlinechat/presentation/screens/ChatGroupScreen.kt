package com.hb.offlinechat.presentation.screens

import android.app.Application
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.GroupChatViewModel

/** Host-coordinated group chat backed by Room and the encrypted transport. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupScreen(groupId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: GroupChatViewModel = viewModel(factory = GroupChatViewModel.factory(app, groupId))
    val messages by vm.messages.collectAsStateWithLifecycle(emptyList())
    val members by vm.members.collectAsStateWithLifecycle(emptyList())
    val devices by vm.connectedDevices.collectAsStateWithLifecycle(emptyList())
    var draft by remember { mutableStateOf("") }
    var showMembers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group · ${groupId.take(8)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { showMembers = !showMembers }) {
                        Text(if (showMembers) "Chat" else "Members")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingS)) {
            if (showMembers) {
                Text("Group members", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(members, key = { it.deviceId }) { member ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${member.displayName} (${member.role})", fontSize = 13.sp)
                            if (member.deviceId != vm.myDeviceId) {
                                TextButton(onClick = { vm.removeMember(member.deviceId) }) { Text("Remove") }
                            }
                        }
                    }
                }
                Text("Add connected device", fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = AppDimensions.spacingS))
                devices.forEach { device ->
                    val alreadyMember = members.any { it.deviceId == device.deviceId }
                    if (!alreadyMember) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                .clickable { vm.addMember(device) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("+ ${device.deviceName}", modifier = Modifier.padding(AppDimensions.spacingS), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    reverseLayout = false
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        val mine = msg.senderId == vm.myDeviceId
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                        ) {
                            Column {
                                Text(
                                    "${msg.senderId.take(6)}: ${msg.plainText ?: "[encrypted]"}",
                                    color = if (mine) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Message to group") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.send(draft); draft = "" }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
