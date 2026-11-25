package com.hb.offlinechat.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimensions.spacingL)
                .wrapContentSize(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Set up your profile",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = AppDimensions.spacingS)
            )
            Text(
                text = "Choose a display name that nearby devices will see.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = AppDimensions.spacingL)
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                placeholder = { Text("e.g. Alex") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingM)
            )
            Button(
                onClick = onComplete,
                enabled = displayName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS)
            ) {
                Text("Continue")
            }
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Skip")
            }
        }
    }
}
