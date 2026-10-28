package com.wakewakeup

import android.content.Context
import android.util.Log
import com.wakewakeup.data.AlarmRepository
import com.wakewakeup.data.AppDatabase
import com.wakewakeup.data.WakeHistoryRepository
import com.wakewakeup.session.AlarmScheduler
import com.wakewakeup.session.PreAlarmCheck
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    /**
     * For writes that must finish even if the screen or service that started them goes away.
     * One failing (a full disk) is logged rather than taking the whole app down with it.
     */
    val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e -> Log.w("WakeWakeUp", "Background work failed", e) },
    )
    private val database = AppDatabase.get(context)
    val alarmScheduler = AlarmScheduler(context)
    val preAlarm = PreAlarmCheck(context, database.alarmDao())
    val alarmRepository = AlarmRepository(database.alarmDao(), alarmScheduler, onChanged = { preAlarm.reschedule() })
    val wakeHistoryRepository = WakeHistoryRepository(database.wakeHistoryDao())
}
