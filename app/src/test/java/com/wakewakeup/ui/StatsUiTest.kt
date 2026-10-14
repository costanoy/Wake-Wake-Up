package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class StatsUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    /** The way the good-morning screen opens it: straight to Stats, no onboarding in between. */
    private fun openStats() {
        val intent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_STATS, true)
        scenario = ActivityScenario.launch(intent)
        compose.waitForText(str(R.string.stats))
    }

    private fun wake(daysAgo: Long, wokeHour: Int, wokeMinute: Int, waits: Int, seconds: Int, alarmHour: Int = 6, alarmMinute: Int = 30) =
        runBlocking {
            env.history.record(
                WakeHistoryEntry(
                    date = LocalDate.now().minusDays(daysAgo), alarmHour = alarmHour, alarmMinute = alarmMinute,
                    wokeHour = wokeHour, wokeMinute = wokeMinute, waits = waits, durationSec = seconds,
                ),
            )
        }

    private fun dayLabel(daysAgo: Long): String =
        if (daysAgo == 0L) str(R.string.today).uppercase()
        else context.resources.getStringArray(R.array.day_short)[isoDayIndexOf(LocalDate.now().minusDays(daysAgo))].uppercase()

    @Test
    fun withNoWakeUpsYet() {
        openStats()
        compose.waitForText(str(R.string.stats_empty))
        assertTrue(compose.onAllNodesWithText("–").fetchSemanticsNodes().size >= 2)
        compose.onNodeWithContentDescription("${dayLabel(0)} –").assertIsDisplayed()
    }

    @Test
    fun aWeekOfWakeUps() {
        wake(daysAgo = 0, wokeHour = 6, wokeMinute = 41, waits = 1, seconds = 660)
        wake(daysAgo = 1, wokeHour = 6, wokeMinute = 35, waits = 0, seconds = 300)
        wake(daysAgo = 3, wokeHour = 6, wokeMinute = 50, waits = 2, seconds = 1_200)
        wake(daysAgo = 9, wokeHour = 9, wokeMinute = 0, waits = 5, seconds = 3_000) // older than a week: left out
        openStats()

        // Today: 11 min, woke 06:41. Average of the week: (11 + 5 + 20) / 3 = 12 min, 1 snooze a day.
        compose.waitForText(str(R.string.woke_at_time, "06:41"))
        compose.onNodeWithText("11").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.waits_per_wake, "1.0")).assertIsDisplayed()

        // The 7-day chart: one column per day, empty days dashed.
        compose.onNodeWithContentDescription("${dayLabel(0)} 11").assertIsDisplayed()
        compose.onNodeWithContentDescription("${dayLabel(1)} 5").assertIsDisplayed()
        compose.onNodeWithContentDescription("${dayLabel(2)} –").assertIsDisplayed()
        compose.onNodeWithContentDescription("${dayLabel(3)} 20").assertIsDisplayed()

        // Alarm vs. woke-up rows, newest first, minutes late in the last column.
        compose.onNodeWithText("+11").assertIsDisplayed()
        compose.onNodeWithText("+5").assertIsDisplayed()
        compose.onNodeWithText("+20").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("+150").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText(str(R.string.stats_empty)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun wakingUpAfterMidnightCountsTheRightDelay() {
        // Alarm at 23:50, up at 00:10: 20 minutes late, not -1420.
        wake(daysAgo = 0, wokeHour = 0, wokeMinute = 10, waits = 0, seconds = 1_200, alarmHour = 23, alarmMinute = 50)
        openStats()
        compose.waitForText("+20")
    }
}
