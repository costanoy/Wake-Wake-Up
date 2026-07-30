package com.wakewakeup.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wakewakeup.WakeWakeUpApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
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
}
