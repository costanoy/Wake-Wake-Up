package com.wakewakeup

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.wakewakeup.ui.navigation.WwuNavGraph
import com.wakewakeup.ui.theme.WakeWakeUpTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark-themed regardless of the system setting, so force
        // light system-bar icons rather than letting them follow system light/dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        requestNotificationPermissionIfNeeded()
        val openStats = intent?.getBooleanExtra(EXTRA_OPEN_STATS, false) ?: false
        setContent {
            WakeWakeUpTheme {
                WwuNavGraph(openStatsOnStart = openStats)
            }
        }
    }

    // Without this, the alarm's foreground-service notification (and its full-screen
    // launch trigger) may not show on Android 13+ until the user grants it manually.
    // Only ever asked once — repeatedly re-prompting on every launch after a decline
    // (a "yes/no" the user already answered) is exactly what we don't want to do.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        const val EXTRA_OPEN_STATS = "extra_open_stats"
        private const val PREFS_NAME = "wwu_prefs"
        private const val KEY_ASKED_NOTIFICATIONS = "asked_notification_permission"
    }
}
