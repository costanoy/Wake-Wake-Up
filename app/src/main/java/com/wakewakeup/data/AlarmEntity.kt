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
)

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun count(): Int

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
