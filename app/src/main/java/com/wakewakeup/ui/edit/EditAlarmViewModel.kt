package com.wakewakeup.ui.edit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.ui.navigation.Destinations
import kotlinx.coroutines.launch

private fun defaultDraft() = Alarm(hour = 7, minute = 0, label = "", days = emptySet())

/**
 * Activity-scoped so the same instance backs both the Edit-alarm and
 * Task-setup screens (task setup edits fields on the same draft alarm).
 */
class EditAlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WakeWakeUpApplication).container.alarmRepository

    var draft by mutableStateOf(defaultDraft())
        private set
    var isNew by mutableStateOf(true)
        private set

    private var loadedForId: Long? = null
    private var busy = false

    fun startNew() {
        loadedForId = Destinations.NEW_ALARM_ID
        isNew = true
        draft = defaultDraft()
    }

    /** Opening an alarm from the list: its card already has it, so the editor never shows a stale draft. */
    fun startEdit(alarm: Alarm) {
        loadedForId = alarm.id
        isNew = false
        draft = alarm
    }

    fun load(alarmId: Long) {
        if (alarmId == Destinations.NEW_ALARM_ID || loadedForId == alarmId) return
        loadedForId = alarmId
        isNew = false
        viewModelScope.launch {
            repository.getById(alarmId)?.let { draft = it }
        }
    }

    /** Forces the next [load] call to re-fetch from the database, discarding unsaved edits. */
    fun discardUnsaved() {
        loadedForId = null
    }

    fun setLabel(value: String) {
        draft = draft.copy(label = value)
    }

    fun setHour(hour: Int) {
        draft = draft.copy(hour = hour)
    }

    fun setMinute(minute: Int) {
        draft = draft.copy(minute = minute)
    }

    fun toggleDay(dayIndex: Int) {
        val days = draft.days.toMutableSet()
        if (!days.remove(dayIndex)) days.add(dayIndex)
        draft = draft.copy(days = days)
    }

    fun setSound(uri: String?, name: String?) {
        draft = draft.copy(soundUri = uri, soundName = name)
    }

    fun setTaskType(type: TaskType) {
        draft = draft.copy(taskType = type)
    }

    fun setDifficulty(difficulty: Difficulty) {
        draft = draft.copy(difficulty = difficulty)
    }

    fun changeTaskCount(delta: Int) {
        draft = draft.copy(taskCount = (draft.taskCount + delta).coerceIn(1, 10))
    }

    fun setRampUp(rampUp: Boolean) {
        draft = draft.copy(rampUp = rampUp)
    }

    /**
     * There's no on/off switch inside this screen — only the alarm-list card
     * has one — so confirming a configuration here always turns the alarm on.
     */
    fun save(onSaved: () -> Unit) {
        // A second tap while the first save is still writing must not insert the alarm twice.
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val id = repository.save(draft.copy(enabled = true))
                draft = draft.copy(id = id, enabled = true)
                isNew = false
                loadedForId = null
                onSaved()
            } finally {
                busy = false
            }
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                if (!isNew) repository.delete(draft)
                loadedForId = null
                onDeleted()
            } finally {
                busy = false
            }
        }
    }
}
