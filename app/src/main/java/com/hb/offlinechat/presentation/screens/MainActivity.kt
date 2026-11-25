package com.hb.offlinechat.presentation.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.presentation.navigation.AppNavGraph
import com.hb.offlinechat.presentation.theme.HbTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val preferences = remember { AppContainer.getInstance(context.applicationContext).preferences }
            val darkTheme by preferences.darkTheme.collectAsStateWithLifecycle(initialValue = true)
            val scope = rememberCoroutineScope()

            HbTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(
                        onUserReady = {},
                        darkTheme = darkTheme,
                        onToggleTheme = { scope.launch { preferences.setDarkTheme(!darkTheme) } }
                    )
                }
            }
        }
    }
}
