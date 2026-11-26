package com.wakewakeup.session

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.os.UserManagerCompat
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puts every alarm back in AlarmManager when what it holds can't be trusted anymore: a reboot
 * empties it, a new time zone moves every "07:00" to a different instant, an app update
 * shouldn't leave anything behind, and exact alarms only work again once Android allows them.
 *
 * After a restart this runs twice: first while the phone is still locked (the database can't be
 * opened, so the alarms come from [ArmedRings]), then once it is unlocked (from the database).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        if (!UserManagerCompat.isUserUnlocked(context)) {
            rearmFromArmedRings(context, intent.action)
            return
        }
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.alarmRepository.rescheduleAll()
                // Restarting the phone is no way out of a ringing alarm: it comes back. The locked
                // boot may already have brought it back.
                if (intent.action == Intent.ACTION_BOOT_COMPLETED && AlarmSessionState.session.value == null) {
                    runCatching { RingWatchdog.resumeIfInterrupted(context) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Touches neither the database nor the container: only the device-protected copy. */
    private fun rearmFromArmedRings(context: Context, action: String?) {
        val now = System.currentTimeMillis()
        val scheduler = AlarmScheduler(context)
        for (entry in runCatching { ArmedRings.all(context) }.getOrDefault(emptyList())) {
            // A one-time alarm whose moment has passed rang (or was lost) already: don't bring it back.
            if (entry.alarm.days.isEmpty() && entry.armedAt <= now) continue
            runCatching { scheduler.schedule(entry.alarm) }
        }
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            runCatching { RingWatchdog.resumeIfInterrupted(context) }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
