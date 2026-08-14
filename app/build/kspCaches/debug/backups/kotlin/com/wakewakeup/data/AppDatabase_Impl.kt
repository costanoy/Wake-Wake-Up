package com.wakewakeup.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _alarmDao: Lazy<AlarmDao> = lazy {
    AlarmDao_Impl(this)
  }

  private val _wakeHistoryDao: Lazy<WakeHistoryDao> = lazy {
    WakeHistoryDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "6044a68008c1ce3eadd1486ff56964f4", "cf145a4c87db5a22efd31e274e674b99") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `alarms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, `label` TEXT NOT NULL, `days` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `taskType` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `taskCount` INTEGER NOT NULL, `soundUri` TEXT, `soundName` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `wake_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, `alarmHour` INTEGER NOT NULL, `alarmMinute` INTEGER NOT NULL, `wokeHour` INTEGER NOT NULL, `wokeMinute` INTEGER NOT NULL, `waits` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6044a68008c1ce3eadd1486ff56964f4')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `alarms`")
        connection.execSQL("DROP TABLE IF EXISTS `wake_history`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsAlarms: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAlarms.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("hour", TableInfo.Column("hour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("minute", TableInfo.Column("minute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("label", TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("days", TableInfo.Column("days", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("enabled", TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("taskType", TableInfo.Column("taskType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("difficulty", TableInfo.Column("difficulty", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("taskCount", TableInfo.Column("taskCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("soundUri", TableInfo.Column("soundUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlarms.put("soundName", TableInfo.Column("soundName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAlarms: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAlarms: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAlarms: TableInfo = TableInfo("alarms", _columnsAlarms, _foreignKeysAlarms, _indicesAlarms)
        val _existingAlarms: TableInfo = read(connection, "alarms")
        if (!_infoAlarms.equals(_existingAlarms)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |alarms(com.wakewakeup.data.AlarmEntity).
              | Expected:
              |""".trimMargin() + _infoAlarms + """
              |
              | Found:
              |""".trimMargin() + _existingAlarms)
        }
        val _columnsWakeHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWakeHistory.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("epochDay", TableInfo.Column("epochDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("alarmHour", TableInfo.Column("alarmHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("alarmMinute", TableInfo.Column("alarmMinute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("wokeHour", TableInfo.Column("wokeHour", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("wokeMinute", TableInfo.Column("wokeMinute", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("waits", TableInfo.Column("waits", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWakeHistory.put("durationSec", TableInfo.Column("durationSec", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWakeHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWakeHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWakeHistory: TableInfo = TableInfo("wake_history", _columnsWakeHistory, _foreignKeysWakeHistory, _indicesWakeHistory)
        val _existingWakeHistory: TableInfo = read(connection, "wake_history")
        if (!_infoWakeHistory.equals(_existingWakeHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |wake_history(com.wakewakeup.data.WakeHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoWakeHistory + """
              |
              | Found:
              |""".trimMargin() + _existingWakeHistory)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "alarms", "wake_history")
  }

  public override fun clearAllTables() {
    super.performClear(false, "alarms", "wake_history")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(AlarmDao::class, AlarmDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(WakeHistoryDao::class, WakeHistoryDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun alarmDao(): AlarmDao = _alarmDao.value

  public override fun wakeHistoryDao(): WakeHistoryDao = _wakeHistoryDao.value
}
