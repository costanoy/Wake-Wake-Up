package com.wakewakeup.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WakeHistoryRepository(private val dao: WakeHistoryDao) {
    fun observeRecent(days: Int = 7): Flow<List<WakeHistoryEntry>> =
        dao.observeRecent(days).map { list -> list.map { it.toDomain() }.sortedBy { it.date } }

    suspend fun record(entry: WakeHistoryEntry) {
        dao.insert(entry.toEntity())
    }

    /**
     * First-run sample content, mirroring the design handoff's own canonical example data
     * (7.5/11.2/5.4/9.1/14.0/6.2/4.2 min) so Stats has something meaningful to show immediately.
     */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        val today = LocalDate.now()
        val samples = listOf(
            // daysAgo, alarmH, alarmM, wokeH, wokeM, waits, durationSec
            SampleDay(6, 6, 30, 6, 37, 0, 450),
            SampleDay(5, 6, 30, 6, 41, 0, 672),
            SampleDay(4, 6, 30, 6, 35, 0, 324),
            SampleDay(3, 6, 30, 6, 39, 0, 546),
            SampleDay(2, 6, 30, 6, 44, 1, 840),
            SampleDay(1, 9, 15, 9, 21, 0, 372),
            SampleDay(0, 6, 30, 6, 34, 1, 252),
        )
        samples.forEach { s ->
            dao.insert(
                WakeHistoryEntity(
                    epochDay = today.minusDays(s.daysAgo.toLong()).toEpochDay(),
                    alarmHour = s.alarmHour,
                    alarmMinute = s.alarmMinute,
                    wokeHour = s.wokeHour,
                    wokeMinute = s.wokeMinute,
                    waits = s.waits,
                    durationSec = s.durationSec,
                ),
            )
        }
    }

    private data class SampleDay(
        val daysAgo: Int,
        val alarmHour: Int,
        val alarmMinute: Int,
        val wokeHour: Int,
        val wokeMinute: Int,
        val waits: Int,
        val durationSec: Int,
    )
}
