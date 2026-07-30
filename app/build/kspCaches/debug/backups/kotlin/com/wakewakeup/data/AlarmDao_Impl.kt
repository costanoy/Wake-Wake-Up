package com.wakewakeup.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AlarmDao_Impl(
  __db: RoomDatabase,
) : AlarmDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAlarmEntity: EntityInsertAdapter<AlarmEntity>

  private val __deleteAdapterOfAlarmEntity: EntityDeleteOrUpdateAdapter<AlarmEntity>

  private val __updateAdapterOfAlarmEntity: EntityDeleteOrUpdateAdapter<AlarmEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAlarmEntity = object : EntityInsertAdapter<AlarmEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `alarms` (`id`,`hour`,`minute`,`label`,`days`,`enabled`,`taskType`,`difficulty`,`taskCount`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AlarmEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.hour.toLong())
        statement.bindLong(3, entity.minute.toLong())
        statement.bindText(4, entity.label)
        statement.bindText(5, entity.daysCsv)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.taskType)
        statement.bindText(8, entity.difficulty)
        statement.bindLong(9, entity.taskCount.toLong())
      }
    }
    this.__deleteAdapterOfAlarmEntity = object : EntityDeleteOrUpdateAdapter<AlarmEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `alarms` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: AlarmEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfAlarmEntity = object : EntityDeleteOrUpdateAdapter<AlarmEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `alarms` SET `id` = ?,`hour` = ?,`minute` = ?,`label` = ?,`days` = ?,`enabled` = ?,`taskType` = ?,`difficulty` = ?,`taskCount` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: AlarmEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.hour.toLong())
        statement.bindLong(3, entity.minute.toLong())
        statement.bindText(4, entity.label)
        statement.bindText(5, entity.daysCsv)
        val _tmp: Int = if (entity.enabled) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.taskType)
        statement.bindText(8, entity.difficulty)
        statement.bindLong(9, entity.taskCount.toLong())
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insert(alarm: AlarmEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfAlarmEntity.insertAndReturnId(_connection, alarm)
    _result
  }

  public override suspend fun delete(alarm: AlarmEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfAlarmEntity.handle(_connection, alarm)
  }

  public override suspend fun update(alarm: AlarmEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfAlarmEntity.handle(_connection, alarm)
  }

  public override fun observeAll(): Flow<List<AlarmEntity>> {
    val _sql: String = "SELECT * FROM alarms ORDER BY hour, minute"
    return createFlow(__db, false, arrayOf("alarms")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHour: Int = getColumnIndexOrThrow(_stmt, "hour")
        val _columnIndexOfMinute: Int = getColumnIndexOrThrow(_stmt, "minute")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfDaysCsv: Int = getColumnIndexOrThrow(_stmt, "days")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "taskType")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfTaskCount: Int = getColumnIndexOrThrow(_stmt, "taskCount")
        val _result: MutableList<AlarmEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlarmEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHour: Int
          _tmpHour = _stmt.getLong(_columnIndexOfHour).toInt()
          val _tmpMinute: Int
          _tmpMinute = _stmt.getLong(_columnIndexOfMinute).toInt()
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpDaysCsv: String
          _tmpDaysCsv = _stmt.getText(_columnIndexOfDaysCsv)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpTaskType: String
          _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
          val _tmpDifficulty: String
          _tmpDifficulty = _stmt.getText(_columnIndexOfDifficulty)
          val _tmpTaskCount: Int
          _tmpTaskCount = _stmt.getLong(_columnIndexOfTaskCount).toInt()
          _item = AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpLabel,_tmpDaysCsv,_tmpEnabled,_tmpTaskType,_tmpDifficulty,_tmpTaskCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllEnabled(): List<AlarmEntity> {
    val _sql: String = "SELECT * FROM alarms WHERE enabled = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHour: Int = getColumnIndexOrThrow(_stmt, "hour")
        val _columnIndexOfMinute: Int = getColumnIndexOrThrow(_stmt, "minute")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfDaysCsv: Int = getColumnIndexOrThrow(_stmt, "days")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "taskType")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfTaskCount: Int = getColumnIndexOrThrow(_stmt, "taskCount")
        val _result: MutableList<AlarmEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlarmEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHour: Int
          _tmpHour = _stmt.getLong(_columnIndexOfHour).toInt()
          val _tmpMinute: Int
          _tmpMinute = _stmt.getLong(_columnIndexOfMinute).toInt()
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpDaysCsv: String
          _tmpDaysCsv = _stmt.getText(_columnIndexOfDaysCsv)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpTaskType: String
          _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
          val _tmpDifficulty: String
          _tmpDifficulty = _stmt.getText(_columnIndexOfDifficulty)
          val _tmpTaskCount: Int
          _tmpTaskCount = _stmt.getLong(_columnIndexOfTaskCount).toInt()
          _item = AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpLabel,_tmpDaysCsv,_tmpEnabled,_tmpTaskType,_tmpDifficulty,_tmpTaskCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM alarms"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): AlarmEntity? {
    val _sql: String = "SELECT * FROM alarms WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfHour: Int = getColumnIndexOrThrow(_stmt, "hour")
        val _columnIndexOfMinute: Int = getColumnIndexOrThrow(_stmt, "minute")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfDaysCsv: Int = getColumnIndexOrThrow(_stmt, "days")
        val _columnIndexOfEnabled: Int = getColumnIndexOrThrow(_stmt, "enabled")
        val _columnIndexOfTaskType: Int = getColumnIndexOrThrow(_stmt, "taskType")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfTaskCount: Int = getColumnIndexOrThrow(_stmt, "taskCount")
        val _result: AlarmEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpHour: Int
          _tmpHour = _stmt.getLong(_columnIndexOfHour).toInt()
          val _tmpMinute: Int
          _tmpMinute = _stmt.getLong(_columnIndexOfMinute).toInt()
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpDaysCsv: String
          _tmpDaysCsv = _stmt.getText(_columnIndexOfDaysCsv)
          val _tmpEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfEnabled).toInt()
          _tmpEnabled = _tmp != 0
          val _tmpTaskType: String
          _tmpTaskType = _stmt.getText(_columnIndexOfTaskType)
          val _tmpDifficulty: String
          _tmpDifficulty = _stmt.getText(_columnIndexOfDifficulty)
          val _tmpTaskCount: Int
          _tmpTaskCount = _stmt.getLong(_columnIndexOfTaskCount).toInt()
          _result = AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpLabel,_tmpDaysCsv,_tmpEnabled,_tmpTaskType,_tmpDifficulty,_tmpTaskCount)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setEnabled(id: Long, enabled: Boolean) {
    val _sql: String = "UPDATE alarms SET enabled = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enabled) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
