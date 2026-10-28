package com.wakewakeup.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

const val ACTION_RING_WATCHDOG = "com.wakewakeup.action.RING_WATCHDOG"
const val EXTRA_RESUMED_SESSION = "extra_resumed_session"

private const val REQUEST_CODE = -1004
private const val INTERVAL_MS = 3 * 60_000L

/** Past this, a session the process lost is not brought back: that morning is over. */
private const val MAX_AGE_MS = 3 * 60 * 60_000L

private const val PREFS = "ring_session"
private const val KEY_ALARM = "alarm_id"
private const val KEY_STARTED = "started_at"
private const val KEY_WAITS = "waits"
private const val KEY_HOLD = "hold_until"

/**
 * Keeps a ringing alarm from dying with the app's process. While a session is open (ringing,
 * Espere or mission) it is written down here, and an exact wake every few minutes checks that the
 * ringing service is still alive. If the process was killed (out of memory, a crash), the alarm
 * starts ringing again; after a restart of the phone BootReceiver does the same.
 */
object RingWatchdog {

    data class Saved(val alarmId: Long, val startedAtMillis: Long, val waits: Int, val holdUntilMillis: Long?)

    fun save(context: Context, session: RingSession) {
        // commit, not apply: the process may die right after this.
        prefs(context).edit()
            .putLong(KEY_ALARM, session.alarmId)
            .putLong(KEY_STARTED, session.startedAtMillis)
            .putInt(KEY_WAITS, session.waits)
            .putLong(KEY_HOLD, session.holdUntilMillis ?: 0L)
            .commit()
        arm(context)
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
        alarmManager(context).cancel(pendingIntent(context))
    }

    fun saved(context: Context, now: Long = System.currentTimeMillis()): Saved? {
        val prefs = prefs(context)
        val alarmId = prefs.getLong(KEY_ALARM, -1L)
        val startedAt = prefs.getLong(KEY_STARTED, 0L)
        if (alarmId < 0 || now - startedAt !in 0..MAX_AGE_MS) return null
        return Saved(alarmId, startedAt, prefs.getInt(KEY_WAITS, 0), prefs.getLong(KEY_HOLD, 0L).takeIf { it > 0L })
    }

    /** The watchdog's wake: re-arm while the service is alive, bring the alarm back if it isn't. */
    fun onWake(context: Context) {
        if (AlarmSessionState.session.value != null) {
            arm(context)
        } else if (!resumeIfInterrupted(context)) {
            clear(context)
        }
    }

    /** Rings again for a session the process lost. False when there is none to bring back. */
    fun resumeIfInterrupted(context: Context): Boolean {
        val saved = saved(context) ?: return false
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmRingService::class.java)
                    .putExtra(EXTRA_ALARM_ID, saved.alarmId)
                    .putExtra(EXTRA_RESUMED_SESSION, true),
            )
        } catch (e: IllegalStateException) {
            // Not allowed to start from here right now: the next wake tries again.
            arm(context)
        }
        return true
    }

    private fun arm(context: Context) {
        val alarmManager = alarmManager(context)
        val at = System.currentTimeMillis() + INTERVAL_MS
        val pendingIntent = pendingIntent(context)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_RING_WATCHDOG),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmManager(context: Context) = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
