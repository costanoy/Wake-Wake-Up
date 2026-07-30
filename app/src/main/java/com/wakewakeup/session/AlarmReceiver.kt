package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = app.container.alarmRepository.getById(alarmId)
                if (alarm != null && alarm.enabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
                    )
                    if (alarm.days.isNotEmpty()) {
                        app.container.alarmScheduler.schedule(alarm)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
