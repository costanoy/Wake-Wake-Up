package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The pre-alarm check ([PreAlarmCheck]) and the receipt's "Desfazer". */
class PreAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as WakeWakeUpApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_PRE_ALARM_CHECK -> app.container.preAlarm.check()
                    ACTION_UNDO_SKIP -> {
                        val ids = intent.getLongArrayExtra(EXTRA_SKIP_IDS) ?: LongArray(0)
                        val ats = intent.getLongArrayExtra(EXTRA_SKIP_ATS) ?: LongArray(0)
                        ids.zip(ats).forEach { (id, at) -> app.container.alarmRepository.undoSkipOccurrence(id, at) }
                        NotificationManagerCompat.from(context).cancel(NotificationIds.SKIP_RECEIPT)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
