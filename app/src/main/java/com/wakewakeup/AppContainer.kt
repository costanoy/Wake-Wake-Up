package com.wakewakeup

import android.content.Context
import com.wakewakeup.data.AlarmRepository
import com.wakewakeup.data.AppDatabase
import com.wakewakeup.data.WakeHistoryRepository
import com.wakewakeup.session.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val alarmScheduler = AlarmScheduler(context)
    val alarmRepository = AlarmRepository(database.alarmDao(), alarmScheduler)
    val wakeHistoryRepository = WakeHistoryRepository(database.wakeHistoryDao())

    init {
        applicationScope.launch { alarmRepository.seedDefaultsIfEmpty() }
    }
}
