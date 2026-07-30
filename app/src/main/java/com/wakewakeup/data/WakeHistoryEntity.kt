package com.wakewakeup.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "wake_history")
data class WakeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val alarmHour: Int,
    val alarmMinute: Int,
    val wokeHour: Int,
    val wokeMinute: Int,
    val waits: Int,
    val durationSec: Int,
)

fun WakeHistoryEntity.toDomain() = WakeHistoryEntry(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    alarmHour = alarmHour,
    alarmMinute = alarmMinute,
    wokeHour = wokeHour,
    wokeMinute = wokeMinute,
    waits = waits,
    durationSec = durationSec,
)

fun WakeHistoryEntry.toEntity() = WakeHistoryEntity(
    id = id,
    epochDay = date.toEpochDay(),
    alarmHour = alarmHour,
    alarmMinute = alarmMinute,
    wokeHour = wokeHour,
    wokeMinute = wokeMinute,
    waits = waits,
    durationSec = durationSec,
)

@Dao
interface WakeHistoryDao {
    @Query("SELECT * FROM wake_history ORDER BY epochDay DESC LIMIT :days")
    fun observeRecent(days: Int): Flow<List<WakeHistoryEntity>>

    @Insert
    suspend fun insert(entry: WakeHistoryEntity): Long
}
