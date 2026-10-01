package com.wakewakeup

import android.content.Context
import com.wakewakeup.data.AlarmRepository
import com.wakewakeup.data.AppDatabase
import com.wakewakeup.data.WakeHistoryRepository
import com.wakewakeup.session.AlarmScheduler

class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    val alarmScheduler = AlarmScheduler(context)
    val alarmRepository = AlarmRepository(database.alarmDao(), alarmScheduler)
    val wakeHistoryRepository = WakeHistoryRepository(database.wakeHistoryDao())
}
