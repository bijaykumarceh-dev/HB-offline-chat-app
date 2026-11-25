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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hb.offlinechat.presentation.theme.AppDimensions

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
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
                text = "Welcome to HB Offline Chat",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = AppDimensions.spacingS)
            )
            Text(
                text = "Chat with nearby Android devices using Bluetooth and Wi-Fi Direct.\n" +
                    "Works in airplane mode — no SIM, no mobile data, no internet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = AppDimensions.spacingL)
            )
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = AppDimensions.spacingS)
            ) {
                Text("Set up profile")
            }
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Continue without profile")
            }
        }
    }
}
