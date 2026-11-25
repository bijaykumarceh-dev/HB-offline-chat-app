package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

/** Group chat UI. Host-based group coordinator is implemented in Phase 8. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGroupScreen(groupId: String, onBack: () -> Unit) {
    val messages = remember { mutableStateListOf<Pair<String, String>>() }
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group · ${groupId.take(8)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingS)) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
                Text("Members: you", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                messages.forEach { (sender, text) ->
                    Text("$sender: $text", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Message to group") }, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (draft.isNotBlank()) { messages.add("Me" to draft); draft = "" } }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
