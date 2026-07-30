package com.wakewakeup.data

import com.wakewakeup.session.AlarmScheduler
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
        if (saved.enabled) scheduler.schedule(saved) else scheduler.cancel(saved)
        return id
    }

    suspend fun delete(alarm: Alarm) {
        dao.delete(alarm.toEntity())
        scheduler.cancel(alarm)
    }

    suspend fun setEnabled(alarm: Alarm, enabled: Boolean) {
        dao.setEnabled(alarm.id, enabled)
        val updated = alarm.copy(enabled = enabled)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(updated)
    }

    suspend fun rescheduleAll() {
        dao.getAllEnabled().forEach { scheduler.schedule(it.toDomain()) }
    }

    /** First-run sample content: three disabled alarms so the list isn't empty on first open. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        listOf(
            Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4), enabled = false, taskType = TaskType.MATH, difficulty = Difficulty.MEDIUM),
            Alarm(hour = 9, minute = 15, label = "Fim de semana", days = setOf(5, 6), enabled = false, taskType = TaskType.PHRASE),
            Alarm(hour = 7, minute = 0, label = "Academia", days = setOf(0, 2, 4), enabled = false, taskType = TaskType.MATH, difficulty = Difficulty.EASY),
        ).forEach { dao.insert(it.toEntity()) }
    }
}
