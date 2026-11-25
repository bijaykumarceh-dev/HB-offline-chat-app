package com.hb.offlinechat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hb.offlinechat.presentation.screens.AboutScreen
import com.hb.offlinechat.presentation.screens.ChatGroupScreen
import com.hb.offlinechat.presentation.screens.ChatOneToOneScreen
import com.hb.offlinechat.presentation.screens.ConnectionScreen
import com.hb.offlinechat.presentation.screens.FileSharingScreen
import com.hb.offlinechat.presentation.screens.GroupListScreen
import com.hb.offlinechat.presentation.screens.HomeScreen
import com.hb.offlinechat.presentation.screens.NearbyDevicesScreen
import com.hb.offlinechat.presentation.screens.PrivacySettingsScreen
import com.hb.offlinechat.presentation.screens.ProfileSetupScreen
import com.hb.offlinechat.presentation.screens.SettingsScreen
import com.hb.offlinechat.presentation.screens.SplashScreen
import com.hb.offlinechat.presentation.screens.VoiceCallScreen
import com.hb.offlinechat.presentation.screens.VoiceMessagesScreen
import com.hb.offlinechat.presentation.screens.WelcomeScreen

/**
 * Main navigation graph for HB Offline Chat.
 * All screens are wired for functional navigation (no non-functional controls).
 */
@Composable
fun AppNavGraph(
    onUserReady: () -> Unit = {},
    darkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(onNavigate = { navController.navigate(Routes.Welcome) })
        }

        composable(Routes.Welcome) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Routes.ProfileSetup) },
                onSkip = { navController.navigate(Routes.Home) }
            )
        }

        composable(Routes.ProfileSetup) {
            ProfileSetupScreen(
                onComplete = {
                    navController.navigate(Routes.Home)
                    onUserReady()
                },
                onSkip = { navController.navigate(Routes.Home) }
            )
        }

        composable(Routes.Home) {
            HomeScreen(
                onScan = { navController.navigate(Routes.NearbyDevices) },
                onNearbyDevices = { navController.navigate(Routes.NearbyDevices) },
                onConnection = { navController.navigate(Routes.Connection) },
                onOpenChat = { deviceId -> navController.navigate("${Routes.ChatOneToOne}/$deviceId") },
                onOpenGroup = { groupId ->
                    if (groupId.isBlank()) navController.navigate(Routes.GroupList)
                    else navController.navigate("${Routes.ChatGroup}/$groupId")
                },
                onFileShare = { navController.navigate(Routes.FileSharing) },
                onVoiceMessages = { navController.navigate(Routes.VoiceMessages) },
                onSettings = { navController.navigate(Routes.Settings) },
                onVoiceCall = { navController.navigate(Routes.VoiceCall) }
            )
        }

        composable(Routes.NearbyDevices) {
            NearbyDevicesScreen(
                onBack = { navController.popBackStack() },
                onConnect = { deviceId, _ ->
                    navController.navigate("${Routes.Connection}/$deviceId")
                }
            )
        }

        composable("${Routes.Connection}/{deviceId}") {
            val deviceId = it?.arguments?.getString(Routes.ArgDeviceId) ?: ""
            ConnectionScreen(
                deviceId = deviceId,
                onBack = { navController.popBackStack() },
                onChat = { navController.navigate("${Routes.ChatOneToOne}/$deviceId") }
            )
        }

        composable("${Routes.ChatOneToOne}/{deviceId}") {
            val deviceId = it?.arguments?.getString(Routes.ArgDeviceId) ?: ""
            ChatOneToOneScreen(
                deviceId = deviceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GroupList) {
            GroupListScreen(
                onBack = { navController.popBackStack() },
                onOpenGroup = { groupId -> navController.navigate("${Routes.ChatGroup}/$groupId") }
            )
        }

        composable("${Routes.ChatGroup}/{groupId}") {
            val groupId = it?.arguments?.getString(Routes.ArgGroupId) ?: ""
            ChatGroupScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FileSharing) {
            FileSharingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.VoiceMessages) {
            VoiceMessagesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.VoiceCall) {
            VoiceCallScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPrivacy = { navController.navigate(Routes.PrivacySettings) },
                onAbout = { navController.navigate(Routes.About) },
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        composable(Routes.PrivacySettings) {
            PrivacySettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.About) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
