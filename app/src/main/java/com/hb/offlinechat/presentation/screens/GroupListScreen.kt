package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.viewmodels.GroupListViewModel

/** Group list and creation screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit
) {
    val vm: GroupListViewModel = viewModel()
    val groups by vm.groups.collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(AppDimensions.spacingM)) {
            Text("Create a local group", fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.spacingS)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { vm.create(name, onOpenGroup); name = "" },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.padding(start = AppDimensions.spacingS)
                ) { Text("Create") }
            }

            Text(
                "You become the coordinator. Add connected members inside the group.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = AppDimensions.spacingS)
            )

            Text("Your groups", fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AppDimensions.spacingL))
            LazyColumn {
                items(groups, key = { it.groupId }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS)
                            .clickable { onOpenGroup(group.groupId) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(AppDimensions.spacingM)) {
                            Text(group.name, fontWeight = FontWeight.Bold)
                            Text(
                                "Coordinator ${group.coordinatorId.take(8)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
