package com.wakewakeup.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WakeHistoryRepository(private val dao: WakeHistoryDao) {
    fun observeRecent(days: Int = 7): Flow<List<WakeHistoryEntry>> =
        dao.observeRecent(days).map { list -> list.map { it.toDomain() }.sortedBy { it.date } }

    suspend fun record(entry: WakeHistoryEntry) {
        dao.insert(entry.toEntity())
    }
}
