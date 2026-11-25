package com.hb.offlinechat.presentation.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.hb.offlinechat.presentation.navigation.AppNavGraph
import com.hb.offlinechat.presentation.theme.HbTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HbTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        onUserReady = {},
                        onConnectionRequested = { deviceId, deviceName ->
                            // Wired in Phase 4-5 (real connection engine)
                        },
                        onChatOpened = { deviceId -> },
                        onGroupOpened = { groupId -> }
                    )
                }
            }
        }
    }
}
