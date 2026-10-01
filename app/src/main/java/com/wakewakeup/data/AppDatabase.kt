package com.wakewakeup.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the per-alarm "Aumentar aos poucos" (volume ramp) setting, on by default, and removes
 * the placeholder content earlier versions seeded on first run: the three sample alarms (only
 * while still untouched and off) and the seven fake wake-ups that padded the stats.
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN rampUp INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            """
            DELETE FROM alarms WHERE enabled = 0 AND resumeDate IS NULL AND soundUri IS NULL AND taskCount = 3 AND (
                (label = 'Trabalho' AND hour = 6 AND minute = 30 AND days = '0,1,2,3,4' AND taskType = 'MATH' AND difficulty = 'MEDIUM') OR
                (label = 'Fim de semana' AND hour = 9 AND minute = 15 AND days = '5,6' AND taskType = 'PHRASE') OR
                (label = 'Academia' AND hour = 7 AND minute = 0 AND days = '0,2,4' AND taskType = 'MATH' AND difficulty = 'EASY')
            )
            """.trimIndent(),
        )
        listOf(
            intArrayOf(6, 30, 6, 37, 0, 450), intArrayOf(6, 30, 6, 41, 0, 672), intArrayOf(6, 30, 6, 35, 0, 324),
            intArrayOf(6, 30, 6, 39, 0, 546), intArrayOf(6, 30, 6, 44, 1, 840), intArrayOf(9, 15, 9, 21, 0, 372),
            intArrayOf(6, 30, 6, 34, 1, 252),
        ).forEach { s ->
            db.execSQL(
                "DELETE FROM wake_history WHERE alarmHour = ? AND alarmMinute = ? AND wokeHour = ? AND wokeMinute = ? AND waits = ? AND durationSec = ?",
                s.toTypedArray(),
            )
        }
    }
}

@Database(entities = [AlarmEntity::class, WakeHistoryEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun wakeHistoryDao(): WakeHistoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "wake-wake-up.db")
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
