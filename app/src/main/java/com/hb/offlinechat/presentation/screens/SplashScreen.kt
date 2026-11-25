package com.hb.offlinechat.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.hb.offlinechat.presentation.theme.AppDimensions
import com.hb.offlinechat.presentation.theme.HbTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: () -> Unit) {
    HbTheme {
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
                    text = "HB Offline Chat",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Local peer-to-peer messaging. No internet. No SIM. No server.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = AppDimensions.spacingS)
                )
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Text(
                        text = "Initialising local radios…",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = AppDimensions.spacingM)
                    )
                }
            }
        }
        // Auto-advance after a short splash
        LaunchedEffect(Unit) {
            delay(1500)
            onNavigate()
        }
    }
}
