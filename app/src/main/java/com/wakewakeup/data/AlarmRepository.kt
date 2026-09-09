package com.wakewakeup.data

import com.wakewakeup.session.AlarmScheduler
import com.wakewakeup.session.nextTriggerEpochDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val dao: AlarmDao,
    private val scheduler: AlarmScheduler,
) {
    fun observeAll(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun save(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) dao.insert(alarm.toEntity()) else {
            dao.update(alarm.toEntity())
            alarm.id
        }
        val saved = alarm.copy(id = id)
        scheduler.schedule(saved)
        return id
    }

    suspend fun delete(alarm: Alarm) {
        dao.delete(alarm.toEntity())
        scheduler.cancel(alarm)
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        dao.setEnabled(alarm.id, enabled)
        if (enabled) dao.setResumeDate(alarm.id, null)
        val updated = alarm.copy(enabled = enabled, resumeDate = if (enabled) null else alarm.resumeDate)
        scheduler.schedule(updated)
    }

    /** Turns the alarm off now but arms it to silently turn itself back on at its next would-be occurrence. */
    suspend fun scheduleResume(alarm: Alarm) {
        val date = nextTriggerEpochDay(alarm)
        dao.setResumeDate(alarm.id, date)
        scheduler.schedule(alarm.copy(resumeDate = date))
    }

    /** Cancels a pending auto-resume, leaving the alarm off indefinitely. */
    suspend fun cancelResume(alarm: Alarm) {
        dao.setResumeDate(alarm.id, null)
        scheduler.schedule(alarm.copy(resumeDate = null))
    }

    /** The paused alarm's scheduled resume wake fired: turn it back on and resume normal recurrence. */
    suspend fun resumeFromPause(alarm: Alarm) {
        dao.setEnabled(alarm.id, true)
        dao.setResumeDate(alarm.id, null)
        scheduler.schedule(alarm.copy(enabled = true, resumeDate = null))
    }

    suspend fun rescheduleAll() {
        dao.getAllSchedulable().forEach { scheduler.schedule(it.toDomain()) }
    }

    /** First-run sample content: a few disabled alarms so the list isn't empty on first open. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        val weekdays = setOf(0, 1, 2, 3, 4)
        val weekend = setOf(5, 6)
        listOf(
            Alarm(hour = 6, minute = 30, label = "Trabalho", days = weekdays, enabled = false, taskType = TaskType.MATH, difficulty = Difficulty.MEDIUM),
            Alarm(hour = 9, minute = 15, label = "Fim de semana", days = weekend, enabled = false, taskType = TaskType.PHRASE),
            Alarm(hour = 7, minute = 0, label = "Academia", days = setOf(0, 2, 4), enabled = false, taskType = TaskType.MATH, difficulty = Difficulty.EASY),
        ).forEach { dao.insert(it.toEntity()) }
    }
}
