package com.wakewakeup.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.resetDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Phones that ran a build before the redesign have a version 4 database: three sample alarms
 * and seven fake wake-ups seeded on first launch, next to whatever the user created. Updating
 * the app must keep the user's data, remove the untouched samples, and open without crashing.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule val env = AppEnv()

    // Room's version 4 schema (the current one, minus the rampUp column).
    private val alarmsV4 = "CREATE TABLE IF NOT EXISTS `alarms` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `hour` INTEGER NOT NULL, " +
        "`minute` INTEGER NOT NULL, `label` TEXT NOT NULL, `days` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `taskType` TEXT NOT NULL, " +
        "`difficulty` TEXT NOT NULL, `taskCount` INTEGER NOT NULL, `soundUri` TEXT, `soundName` TEXT, `resumeDate` INTEGER)"
    private val historyV4 = "CREATE TABLE IF NOT EXISTS `wake_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `epochDay` INTEGER NOT NULL, " +
        "`alarmHour` INTEGER NOT NULL, `alarmMinute` INTEGER NOT NULL, `wokeHour` INTEGER NOT NULL, `wokeMinute` INTEGER NOT NULL, " +
        "`waits` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL)"

    private fun SQLiteDatabase.alarm(
        label: String, hour: Int, minute: Int, days: String, taskType: String, difficulty: String,
        enabled: Boolean = false, taskCount: Int = 3, soundUri: String? = null, resumeDate: Long? = null,
    ) = insert(
        "alarms", null,
        ContentValues().apply {
            put("hour", hour); put("minute", minute); put("label", label); put("days", days)
            put("enabled", if (enabled) 1 else 0); put("taskType", taskType); put("difficulty", difficulty)
            put("taskCount", taskCount); put("soundUri", soundUri); put("soundName", soundUri?.let { "Som" }); put("resumeDate", resumeDate)
        },
    )

    private fun SQLiteDatabase.wake(daysAgo: Long, s: IntArray) = insert(
        "wake_history", null,
        ContentValues().apply {
            put("epochDay", LocalDate.now().minusDays(daysAgo).toEpochDay())
            put("alarmHour", s[0]); put("alarmMinute", s[1]); put("wokeHour", s[2]); put("wokeMinute", s[3]); put("waits", s[4]); put("durationSec", s[5])
        },
    )

    @Test
    fun upgradingFromVersion4KeepsTheUsersDataAndDropsTheSamples() {
        // Replace the fresh database the app set up with one written by the old version.
        resetDatabase()
        val file = env.app.getDatabasePath("wake-wake-up.db")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(alarmsV4)
            db.execSQL(historyV4)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'version-4-hash')")
            // The three samples, exactly as version 4 seeded them.
            db.alarm("Trabalho", 6, 30, "0,1,2,3,4", "MATH", "MEDIUM")
            db.alarm("Fim de semana", 9, 15, "5,6", "PHRASE", "MEDIUM")
            db.alarm("Academia", 7, 0, "0,2,4", "MATH", "EASY")
            // A sample the user adopted (turned on) and one they edited: both are theirs now.
            db.alarm("Trabalho", 6, 30, "0,1,2,3,4", "MATH", "MEDIUM", enabled = true)
            db.alarm("Academia", 7, 0, "0,2,4", "MATH", "HARD")
            // The user's own alarms, one of them paused for a day.
            db.alarm("Remedio", 22, 0, "", "PHRASE", "EASY", enabled = true, taskCount = 1, soundUri = "content://media/1")
            db.alarm("Faculdade", 7, 15, "0,1,2,3,4", "MATH", "HARD", resumeDate = LocalDate.now().plusDays(1).toEpochDay())
            // The seven fake wake-ups, and two real ones.
            listOf(
                intArrayOf(6, 30, 6, 37, 0, 450), intArrayOf(6, 30, 6, 41, 0, 672), intArrayOf(6, 30, 6, 35, 0, 324),
                intArrayOf(6, 30, 6, 39, 0, 546), intArrayOf(6, 30, 6, 44, 1, 840), intArrayOf(9, 15, 9, 21, 0, 372),
                intArrayOf(6, 30, 6, 34, 1, 252),
            ).forEachIndexed { i, s -> db.wake(6L - i, s) }
            db.wake(1, intArrayOf(6, 30, 6, 52, 2, 1_310))
            db.wake(0, intArrayOf(7, 15, 7, 20, 0, 300))
            db.version = 4
        }

        val database = AppDatabase.get(env.app)
        val alarms = runBlocking { database.alarmDao().observeAll().first() }.map { it.toDomain() }
        val history = runBlocking { database.wakeHistoryDao().observeSince(LocalDate.now().minusDays(30).toEpochDay()).first() }

        // The paused alarm is on again and skips the day it was paused for.
        assertEquals(
            listOf("Trabalho" to true, "Academia" to false, "Faculdade" to true, "Remedio" to true),
            alarms.map { it.label to it.enabled },
        )
        assertEquals(Difficulty.HARD, alarms.first { it.label == "Academia" }.difficulty)
        assertTrue("existing alarms get the volume ramp", alarms.all { it.rampUp })
        val skipping = alarms.first { it.label == "Faculdade" }
        assertEquals(LocalDate.now().plusDays(1).toEpochDay(), skipping.skipDate)
        val remedio = alarms.first { it.label == "Remedio" }
        assertEquals(emptySet<Int>(), remedio.days)
        assertEquals("content://media/1", remedio.soundUri)
        assertEquals(TaskType.PHRASE, remedio.taskType)

        assertEquals(listOf(1_310, 300), history.map { it.durationSec })

        // New alarms keep working on the migrated database.
        val id = runBlocking { database.alarmDao().insert(Alarm(hour = 5, minute = 0, label = "Novo", days = setOf(6)).toEntity()) }
        val inserted = runBlocking { database.alarmDao().getById(id)!!.toDomain() }
        assertEquals("Novo", inserted.label)
        assertTrue(inserted.rampUp)
    }

    @Test
    fun upgradingFromVersion5TurnsAPausedDayIntoASkip() {
        resetDatabase()
        val file = env.app.getDatabasePath("wake-wake-up.db")
        file.parentFile?.mkdirs()
        val tomorrow = LocalDate.now().plusDays(1).toEpochDay()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(alarmsV4)
            db.execSQL("ALTER TABLE alarms ADD COLUMN rampUp INTEGER NOT NULL DEFAULT 1")
            db.execSQL(historyV4)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'version-5-hash')")
            // Paused for a day the old way: off, with the day it was meant to come back on.
            db.alarm("Pausado", 6, 30, "0,1,2,3,4", "MATH", "MEDIUM", resumeDate = tomorrow)
            // Leftovers the old code could write: a date on an alarm that was on, or on a one-time one.
            db.alarm("Ligado", 7, 0, "0,1,2,3,4", "MATH", "MEDIUM", enabled = true, resumeDate = tomorrow)
            db.alarm("Uma vez", 8, 0, "", "MATH", "MEDIUM", resumeDate = tomorrow)
            db.alarm("Desligado", 9, 0, "5,6", "MATH", "MEDIUM")
            db.version = 5
        }

        val alarms = runBlocking { AppDatabase.get(env.app).alarmDao().getAll() }.map { it.toDomain() }.associateBy { it.label }
        assertEquals(true to tomorrow, alarms.getValue("Pausado").let { it.enabled to it.skipDate })
        assertEquals(true to null, alarms.getValue("Ligado").let { it.enabled to it.skipDate })
        assertEquals(false to null, alarms.getValue("Uma vez").let { it.enabled to it.skipDate })
        assertEquals(false to null, alarms.getValue("Desligado").let { it.enabled to it.skipDate })
    }
}
