package com.wakewakeup.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class WakeHistoryRepository(private val dao: WakeHistoryDao) {
    /** Every wake-up recorded from [days] − 1 days ago through today, oldest first. */
    fun observeLastDays(days: Int = 7, today: LocalDate = LocalDate.now()): Flow<List<WakeHistoryEntry>> =
        dao.observeSince(today.minusDays((days - 1).toLong()).toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun record(entry: WakeHistoryEntry) {
        dao.insert(entry.toEntity())
    }
}
