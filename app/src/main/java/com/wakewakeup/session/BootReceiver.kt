package com.wakewakeup.session

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.os.UserManagerCompat
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

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
            val locked = goAsync()
            val action = intent.action
            // Preferences and AlarmManager work: not on the main thread.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rearmFromArmedRings(context, action)
                } finally {
                    locked.finish()
                }
            }
            return
        }
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val restarted = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
                if (restarted) {
                    try {
                        switchOffMissedOneTimeAlarms(context, app)
                    } catch (e: Exception) {
                        // Whatever goes wrong here, the alarms still have to be armed below.
                        currentCoroutineContext().ensureActive()
                        Log.w(TAG, "Could not switch off missed alarms", e)
                    }
                }
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

    /**
     * A one-time alarm whose moment passed (while the phone was off, or it rang while still locked
     * after the restart, which leaves it on in the database) would otherwise ring tomorrow at that
     * time. It is switched off instead, so the list shows it off. Right after a restart AlarmManager
     * holds no ring of ours, so there is no ring in flight to spare; the ring session uses its own
     * copy of the alarm and is not affected by this.
     */
    private suspend fun switchOffMissedOneTimeAlarms(context: Context, app: WakeWakeUpApplication) {
        val now = System.currentTimeMillis()
        val repository = app.container.alarmRepository
        for (entry in runCatching { ArmedRings.all(context) }.getOrDefault(emptyList())) {
            if (entry.alarm.days.isNotEmpty() || entry.armedAt > now) continue
            try {
                val alarm = repository.getById(entry.alarm.id) ?: continue
                if (alarm.enabled && alarm.days.isEmpty()) repository.setEnabled(alarm, false)
            } catch (e: Exception) {
                // One alarm that misbehaves must not stop the others, nor the rescheduling.
                currentCoroutineContext().ensureActive()
                Log.w(TAG, "Could not switch off missed alarm ${entry.alarm.id}", e)
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
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
