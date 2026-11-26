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
                val restarted = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
                if (restarted) switchOffMissedOneTimeAlarms(context, app)
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
     * A one-time alarm whose moment passed while the phone was off would otherwise ring tomorrow
     * at that time, as if nothing happened. It is switched off instead, so the list shows it off.
     * One that is ringing (or was, before the restart) is left to the ring session.
     */
    private suspend fun switchOffMissedOneTimeAlarms(context: Context, app: WakeWakeUpApplication) {
        val now = System.currentTimeMillis()
        val ringing = AlarmSessionState.session.value?.alarmId
        val interrupted = runCatching { RingWatchdog.saved(context)?.alarmId }.getOrNull()
        val repository = app.container.alarmRepository
        for (entry in runCatching { ArmedRings.all(context) }.getOrDefault(emptyList())) {
            val id = entry.alarm.id
            if (entry.alarm.days.isNotEmpty() || entry.armedAt >= now - MISSED_AFTER_MS) continue
            if (id == ringing || id == interrupted) continue
            val alarm = repository.getById(id) ?: continue
            if (alarm.enabled && alarm.days.isEmpty()) repository.setEnabled(alarm, false)
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
        /** Past this, a one-time ring that never came is lost rather than late. */
        const val MISSED_AFTER_MS = 2 * 60_000L

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
