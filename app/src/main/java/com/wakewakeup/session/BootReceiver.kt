package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puts every alarm back in AlarmManager when what it holds can't be trusted anymore: a reboot
 * empties it, a new time zone moves every "07:00" to a different instant, and an app update
 * shouldn't leave anything behind.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.alarmRepository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
