package com.wakewakeup.ui

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.testing.AppEnv
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
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

    /** A day as TalkBack names it: "Today", or the weekday in full. */
    private fun spokenDay(daysAgo: Long): String =
        if (daysAgo == 0L) str(R.string.today)
        else context.resources.getStringArray(R.array.day_full)[isoDayIndexOf(LocalDate.now().minusDays(daysAgo))]

    private fun spokenMinutes(n: Int): String = context.resources.getQuantityString(R.plurals.minutes_spoken, n, n)

    /** What TalkBack says for a chart column: "Monday: 5 minutes", "Today: no record". */
    private fun column(daysAgo: Long, value: Int?): String =
        "${spokenDay(daysAgo)}: ${value?.let { spokenMinutes(it) } ?: str(R.string.stats_no_record)}"

    /** Activities started since the last call. */
    private fun startedActivities(): List<Intent> = generateSequence { shadowOf(context).nextStartedActivity }.toList()

    @Test
    fun withNoWakeUpsYet() {
        openStats()
        compose.waitForText(str(R.string.stats_empty))
        assertTrue(compose.onAllNodesWithText("–").fetchSemanticsNodes().size >= 2)
        compose.onNodeWithContentDescription(column(0, null)).assertIsDisplayed()
    }

    @Test
    fun aWeekOfWakeUps() {
        wake(daysAgo = 0, wokeHour = 6, wokeMinute = 41, waits = 1, seconds = 660)
        wake(daysAgo = 1, wokeHour = 6, wokeMinute = 35, waits = 0, seconds = 300)
        wake(daysAgo = 3, wokeHour = 6, wokeMinute = 50, waits = 2, seconds = 1_200)
        wake(daysAgo = 9, wokeHour = 9, wokeMinute = 0, waits = 5, seconds = 3_000) // older than a week: left out
        openStats()

        // Today: 11 min, woke 06:41. Average of the week: (11 + 5 + 20) / 3 = 12 min, 1 snooze per alarm.
        compose.waitForText(str(R.string.woke_at_time, "06:41"))
        compose.onNodeWithText("11").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.waits_per_wake, "1.0")).assertIsDisplayed()

        // The 7-day chart: one column per day, read out with its unit; empty days dashed.
        compose.onNodeWithContentDescription(column(0, 11)).assertIsDisplayed()
        compose.onNodeWithContentDescription(column(1, 5)).assertIsDisplayed()
        compose.onNodeWithContentDescription(column(2, null)).assertIsDisplayed()
        compose.onNodeWithContentDescription(column(3, 20)).assertIsDisplayed()

        // Alarm vs. woke-up rows, newest first, minutes late in the last column.
        compose.onNodeWithText("+11").assertIsDisplayed()
        compose.onNodeWithText("+5").assertIsDisplayed()
        compose.onNodeWithText("+20").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("+150").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText(str(R.string.stats_empty)).fetchSemanticsNodes().isEmpty())
        // TalkBack reads each row as one sentence.
        compose.onNodeWithContentDescription(str(R.string.stats_row_spoken, spokenDay(0), "06:30", "06:41", spokenMinutes(11))).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.stats_row_spoken, spokenDay(3), "06:30", "06:50", spokenMinutes(20))).assertIsDisplayed()
    }

    @Test
    fun wakingUpAfterMidnightCountsTheRightDelay() {
        // Alarm at 23:50, up at 00:10: 20 minutes late, not -1420.
        wake(daysAgo = 0, wokeHour = 0, wokeMinute = 10, waits = 0, seconds = 1_200, alarmHour = 23, alarmMinute = 50)
        openStats()
        compose.waitForText("+20")
    }

    @Test
    fun thePrivacyPolicyOpensInTheBrowser() {
        openStats()
        startedActivities()
        compose.onNodeWithText(str(R.string.privacy)).performScrollTo().performClick()
        val opened = startedActivities().single()
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertEquals("https://wakewakeup.cyberhat.com.br/en/privacy.html", opened.dataString)
    }

    @Test
    fun withoutABrowserThePrivacyLinkDoesNothing() {
        // Nothing on the phone can open a web page: Android refuses the intent.
        shadowOf(context).checkActivities(true)
        openStats()
        compose.onNodeWithText(str(R.string.privacy)).performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(startedActivities().isEmpty())
        compose.onNodeWithText(str(R.string.stats)).assertIsDisplayed()
    }
}
