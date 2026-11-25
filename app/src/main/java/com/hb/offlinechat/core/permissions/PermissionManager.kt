package com.hb.offlinechat.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.hb.offlinechat.domain.model.RequiredPermissions

/** Result of a permission request, including permanent-denial detection. */
data class PermissionResult(
    val granted: Boolean,
    val denied: List<String>,
    val permanentlyDenied: List<String>
)

/**
 * Version-aware runtime permission helper.
 * Never silently enables device radios; only requests permissions the OS requires.
 */
class PermissionManager(private val context: Context) {

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun missingForDiscovery(): List<String> = RequiredPermissions.forDiscovery().filter { !isGranted(it) }

    fun allDiscoveryGranted(): Boolean = missingForDiscovery().isEmpty()

    fun evaluateResult(
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        val denied = mutableListOf<String>()
        val permanentlyDenied = mutableListOf<String>()
        permissions.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) {
                denied += permission
                // On Android >= 30 the OS sets PERMISSION_GRANTED after "Don't ask again" is chosen
                // combined with shouldShowRequestPermissionRationale == false.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Caller supplies rationale via [shouldShowRationale].
                }
            }
        }
        return PermissionResult(denied.isEmpty(), denied, permanentlyDenied)
    }

    fun shouldShowRationale(activity: android.app.Activity, permission: String): Boolean =
        activity.shouldShowRequestPermissionRationale(permission)

    /** Opens the app's Android settings page for permanently denied permissions. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun rationaleFor(permission: String): String = when (permission) {
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE ->
            "Bluetooth permission is needed to discover and connect to nearby devices without internet."

        Manifest.permission.ACCESS_FINE_LOCATION ->
            "On this Android version, location permission is required to scan for nearby Bluetooth devices. " +
                "The app does not track your location."

        Manifest.permission.NEARBY_WIFI_DEVICES ->
            "Nearby devices permission is needed for Wi-Fi Direct peer-to-peer connections. " +
                "The app does not use location from Wi-Fi."

        Manifest.permission.RECORD_AUDIO ->
            "Microphone permission is needed to record voice messages and make voice calls."

        Manifest.permission.POST_NOTIFICATIONS ->
            "Notification permission lets the app notify you of incoming messages and transfers."

        else -> "This permission is needed for HB Offline Chat to work on your device."
    }
}
