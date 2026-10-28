package com.wakewakeup.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    @ColumnInfo(name = "days") val daysCsv: String,
    val enabled: Boolean,
    val taskType: String,
    val difficulty: String,
    val taskCount: Int,
    val soundUri: String? = null,
    val soundName: String? = null,
    // Named after its first use (the day a paused alarm resumed); since version 6 it is the day
    // an alarm that stays on skips.
    @ColumnInfo(name = "resumeDate") val skipDate: Long? = null,
    @ColumnInfo(defaultValue = "1") val rampUp: Boolean = true,
)

fun AlarmEntity.toDomain() = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    days = daysCsv.split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
    enabled = enabled,
    taskType = runCatching { TaskType.valueOf(taskType) }.getOrDefault(TaskType.MATH),
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MEDIUM),
    taskCount = taskCount,
    soundUri = soundUri,
    soundName = soundName,
    skipDate = skipDate,
    rampUp = rampUp,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    daysCsv = days.sorted().joinToString(","),
    enabled = enabled,
    taskType = taskType.name,
    difficulty = difficulty.name,
    taskCount = taskCount,
    soundUri = soundUri,
    soundName = soundName,
    skipDate = skipDate,
    rampUp = rampUp,
)

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    /** Switching an alarm on or off by hand always drops a skip it had. */
    @Query("UPDATE alarms SET enabled = :enabled, resumeDate = NULL WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET resumeDate = :skipDate WHERE id = :id")
    suspend fun setSkipDate(id: Long, skipDate: Long?)

    /** On, but skipping [day]: one write, so the list never shows an in-between state. */
    @Query("UPDATE alarms SET enabled = 1, resumeDate = :day WHERE id = :id")
    suspend fun setSkipping(id: Long, day: Long)

    /** Skip dates are only kept until their day has passed. */
    @Query("UPDATE alarms SET resumeDate = NULL WHERE resumeDate < :today")
    suspend fun clearSkipsBefore(today: Long)
}
