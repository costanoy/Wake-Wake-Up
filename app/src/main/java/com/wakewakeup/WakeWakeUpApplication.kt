package com.wakewakeup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

const val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_ringing"

class WakeWakeUpApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ALARM_NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }
}
