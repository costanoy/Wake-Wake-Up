package com.wakewakeup.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** The four things Android must allow for an alarm to really ring on a locked, idle phone. */
enum class AlarmPermission { NOTIFICATIONS, EXACT_ALARMS, FULL_SCREEN, BATTERY }

object AlarmPermissions {
    private const val PREFS = "wwu_prefs"
    private const val KEY_ONBOARDED = "permissions_onboarded"
    private const val KEY_ASKED_NOTIFICATIONS = "asked_notification_permission"

    fun isGranted(context: Context, permission: AlarmPermission): Boolean = when (permission) {
        AlarmPermission.NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        AlarmPermission.EXACT_ALARMS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else true
        AlarmPermission.FULL_SCREEN ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            } else true
        AlarmPermission.BATTERY ->
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    fun allGranted(context: Context) = AlarmPermission.entries.all { isGranted(context, it) }

    /** Shown once, at first launch, unless everything is already allowed. */
    fun shouldShowOnboarding(context: Context): Boolean =
        !prefs(context).getBoolean(KEY_ONBOARDED, false) && !allGranted(context)

    fun markOnboarded(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }

    /**
     * The notification permission is asked in-app the first time; after that (a "no" the user
     * already gave) only the system's settings page can change it.
     */
    fun shouldAskNotificationsInApp(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !prefs(context).getBoolean(KEY_ASKED_NOTIFICATIONS, false)

    fun markNotificationsAsked(context: Context) {
        prefs(context).edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
    }

    /** The system screen where the user can grant [permission] for this app. */
    fun openSettings(context: Context, permission: AlarmPermission) {
        val pkg = context.packageName
        val packageUri = Uri.parse("package:$pkg")
        val intent = when (permission) {
            AlarmPermission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
            AlarmPermission.EXACT_ALARMS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
            AlarmPermission.FULL_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
            AlarmPermission.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
