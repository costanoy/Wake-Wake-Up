package com.wakewakeup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.os.UserManagerCompat
import com.wakewakeup.session.PRE_ALARM_CHANNEL_ID
import kotlinx.coroutines.launch

const val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_ringing"

class WakeWakeUpApplication : Application() {

    lateinit var container: AppContainer

    /** The alarms were armed again since this process started (see MainActivity). */
    var rearmedThisProcess = false

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        // Opens the preferences files off the main thread, so the first read on the main thread
        // (the permissions onboarding, the pre-alarm check) finds them already loaded. Before
        // the first unlock after a restart they are not readable: nothing to do then.
        container.appScope.launch {
            if (UserManagerCompat.isUserUnlocked(this@WakeWakeUpApplication)) {
                getSharedPreferences("wwu_prefs", MODE_PRIVATE)
                getSharedPreferences("pre_alarm", MODE_PRIVATE)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val ringing = NotificationChannel(
            ALARM_NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            // The service plays the alarm and vibrates by itself.
            setSound(null, null)
            enableVibration(false)
        }
        // Silent, but it pops up: only someone already looking at the screen should notice it.
        val preAlarm = NotificationChannel(
            PRE_ALARM_CHANNEL_ID,
            getString(R.string.pre_alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.pre_alarm_channel_desc)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannels(listOf(ringing, preAlarm))
    }
}
