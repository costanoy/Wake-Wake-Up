package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.UserManagerCompat
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "AlarmReceiver"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RING_WATCHDOG) {
            RingWatchdog.onWake(context)
            return
        }
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Before the first unlock after a restart the database can't be opened: the
                // service rings a minimal alarm and the next ring is armed once the phone is open.
                val unlocked = UserManagerCompat.isUserUnlocked(context)
                val repository = app.container.alarmRepository
                var readFailed = false
                val alarm: Alarm? = try {
                    repository.getById(alarmId)
                } catch (e: Exception) {
                    // A closed database reports itself as a cancellation: only our own is passed on.
                    currentCoroutineContext().ensureActive()
                    // An alarm that can't be read still has to ring: the service copes with that.
                    Log.e(TAG, "Could not read alarm $alarmId; ringing anyway", e)
                    readFailed = true
                    null
                }
                if (!readFailed) {
                    if (alarm == null || !alarm.enabled) return@launch
                    // A skipped day is never armed, so this is a ring armed before the skip (or a
                    // time zone change that moved it): stay silent.
                    val skippedToday = alarm.skipDate == LocalDate.now().toEpochDay()
                    if (skippedToday) {
                        armNext(repository, alarm, unlocked)
                        return@launch
                    }
                }
                // Ring first: nothing that goes wrong with tomorrow's ring may cost today's.
                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
                    )
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Not allowed to start the ringing service", e)
                }
                // A one-time alarm is switched off by the ringing service.
                if (alarm != null) armNext(repository, alarm, unlocked)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun armNext(repository: AlarmRepository, alarm: Alarm, unlocked: Boolean) {
        if (alarm.days.isEmpty()) return
        if (!unlocked) {
            Log.i(TAG, "Phone still locked: the next ring is armed after the unlock")
            return
        }
        runCatching { repository.armNextRing(alarm) }
            .onFailure { Log.e(TAG, "Could not arm the next ring", it) }
    }
}
