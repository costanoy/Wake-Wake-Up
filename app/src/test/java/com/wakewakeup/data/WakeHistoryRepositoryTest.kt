package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WakeHistoryRepositoryTest {

    @get:Rule val env = AppEnv()

    private val today = LocalDate.of(2026, 11, 26)

    private fun entry(daysAgo: Long, waits: Int = 0, durationSec: Int = 60) = WakeHistoryEntry(
        date = today.minusDays(daysAgo), alarmHour = 6, alarmMinute = 30, wokeHour = 6, wokeMinute = 35,
        waits = waits, durationSec = durationSec,
    )

    private fun last(days: Int = 7) = runBlocking { env.history.observeLastDays(days, today).first() }

    @Test
    fun aRecordedWakeUpComesBackWithEveryField() = runBlocking {
        env.history.record(entry(0, waits = 2, durationSec = 187))
        val saved = last().single()
        assertEquals(today, saved.date)
        assertEquals(6, saved.alarmHour)
        assertEquals(30, saved.alarmMinute)
        assertEquals(35, saved.wokeMinute)
        assertEquals(2, saved.waits)
        assertEquals(187, saved.durationSec)
        assertTrue(saved.id > 0)
    }

    @Test
    fun theWeekCoversTodayAndTheSixDaysBeforeAndNothingOlder() = runBlocking {
        for (ago in 0L..9L) env.history.record(entry(ago))
        val dates = last().map { it.date }
        assertEquals(7, dates.size)
        assertEquals(today.minusDays(6), dates.min())
        assertEquals(today, dates.max())
    }

    @Test
    fun twoWakeUpsInTheSameDayAreBothKept() = runBlocking {
        env.history.record(entry(1, durationSec = 30))
        env.history.record(entry(1, durationSec = 90))
        assertEquals(setOf(30, 90), last().map { it.durationSec }.toSet())
    }

    @Test
    fun aShorterWindowDropsTheOlderDays() = runBlocking {
        for (ago in 0L..6L) env.history.record(entry(ago))
        assertEquals(3, last(3).size)
        assertEquals(1, last(1).size)
    }

    @Test
    fun noRecordsIsAnEmptyList() {
        assertTrue(last().isEmpty())
    }
}
