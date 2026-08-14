package com.wakewakeup.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.nextTriggerMillis
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.alarmRepository

    val alarms: StateFlow<List<Alarm>> = repository.observeAll()
        .map { list -> list.sortedWith(compareByDescending<Alarm> { it.enabled }.thenBy { it.hour * 60 + it.minute }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleEnabled(alarm: Alarm) {
        viewModelScope.launch {
            repository.setEnabled(alarm, !alarm.enabled)
        }
    }

    fun nextAlarmMillis(alarms: List<Alarm>): Long? =
        alarms.filter { it.enabled }.minOfOrNull { nextTriggerMillis(it) }
}
