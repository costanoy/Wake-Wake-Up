package com.wakewakeup.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class WakeHistoryDao_Impl(
  __db: RoomDatabase,
) : WakeHistoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfWakeHistoryEntity: EntityInsertAdapter<WakeHistoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfWakeHistoryEntity = object : EntityInsertAdapter<WakeHistoryEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `wake_history` (`id`,`epochDay`,`alarmHour`,`alarmMinute`,`wokeHour`,`wokeMinute`,`waits`,`durationSec`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WakeHistoryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.epochDay)
        statement.bindLong(3, entity.alarmHour.toLong())
        statement.bindLong(4, entity.alarmMinute.toLong())
        statement.bindLong(5, entity.wokeHour.toLong())
        statement.bindLong(6, entity.wokeMinute.toLong())
        statement.bindLong(7, entity.waits.toLong())
        statement.bindLong(8, entity.durationSec.toLong())
      }
    }
  }

  public override suspend fun insert(entry: WakeHistoryEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfWakeHistoryEntity.insertAndReturnId(_connection, entry)
    _result
  }

  public override fun observeRecent(days: Int): Flow<List<WakeHistoryEntity>> {
    val _sql: String = "SELECT * FROM wake_history ORDER BY epochDay DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("wake_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, days.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfEpochDay: Int = getColumnIndexOrThrow(_stmt, "epochDay")
        val _columnIndexOfAlarmHour: Int = getColumnIndexOrThrow(_stmt, "alarmHour")
        val _columnIndexOfAlarmMinute: Int = getColumnIndexOrThrow(_stmt, "alarmMinute")
        val _columnIndexOfWokeHour: Int = getColumnIndexOrThrow(_stmt, "wokeHour")
        val _columnIndexOfWokeMinute: Int = getColumnIndexOrThrow(_stmt, "wokeMinute")
        val _columnIndexOfWaits: Int = getColumnIndexOrThrow(_stmt, "waits")
        val _columnIndexOfDurationSec: Int = getColumnIndexOrThrow(_stmt, "durationSec")
        val _result: MutableList<WakeHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WakeHistoryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpEpochDay: Long
          _tmpEpochDay = _stmt.getLong(_columnIndexOfEpochDay)
          val _tmpAlarmHour: Int
          _tmpAlarmHour = _stmt.getLong(_columnIndexOfAlarmHour).toInt()
          val _tmpAlarmMinute: Int
          _tmpAlarmMinute = _stmt.getLong(_columnIndexOfAlarmMinute).toInt()
          val _tmpWokeHour: Int
          _tmpWokeHour = _stmt.getLong(_columnIndexOfWokeHour).toInt()
          val _tmpWokeMinute: Int
          _tmpWokeMinute = _stmt.getLong(_columnIndexOfWokeMinute).toInt()
          val _tmpWaits: Int
          _tmpWaits = _stmt.getLong(_columnIndexOfWaits).toInt()
          val _tmpDurationSec: Int
          _tmpDurationSec = _stmt.getLong(_columnIndexOfDurationSec).toInt()
          _item = WakeHistoryEntity(_tmpId,_tmpEpochDay,_tmpAlarmHour,_tmpAlarmMinute,_tmpWokeHour,_tmpWokeMinute,_tmpWaits,_tmpDurationSec)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM wake_history"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
