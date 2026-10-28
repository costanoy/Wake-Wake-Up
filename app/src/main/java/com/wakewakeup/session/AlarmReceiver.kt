package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

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
                val repository = app.container.alarmRepository
                val alarm = repository.getById(alarmId) ?: return@launch
                if (!alarm.enabled) return@launch
                // A skipped day is never armed, so this is a ring armed before the skip (or a
                // time zone change that moved it): stay silent.
                val skippedToday = alarm.skipDate == LocalDate.now().toEpochDay()
                // Arm the next ring first, so nothing that goes wrong below can cost the alarm
                // its next morning. A one-time alarm is switched off by the ringing service.
                if (alarm.days.isNotEmpty()) repository.armNextRing(alarm)
                if (skippedToday) return@launch
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
                )
            } finally {
                pending.finish()
            }
        }
    }
}
