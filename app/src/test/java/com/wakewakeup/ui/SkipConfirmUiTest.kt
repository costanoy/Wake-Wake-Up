package com.wakewakeup.ui

import android.Manifest
import android.app.NotificationManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.session.NotificationIds
import com.wakewakeup.session.Occurrence
import com.wakewakeup.session.PreAlarmCheck
import com.wakewakeup.session.epochDayOf
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextRingMillis
import com.wakewakeup.testing.AppEnv
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** Where the pre-alarm notification's "Turn off" leads. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class SkipConfirmUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    /** A daily alarm two hours from now, and its next ring. */
    private fun alarmInTwoHours(): Pair<Alarm, Long> {
        val time = LocalTime.now().plusHours(2)
        val alarm = env.save(Alarm(hour = time.hour, minute = time.minute, label = "Trabalho", days = (0..6).toSet()))
        return alarm to nextRingMillis(alarm)
    }

    private fun open(alarm: Alarm, at: Long) {
        val occurrence = Occurrence(alarm.id, at, alarm.label, alarm.hour, alarm.minute)
        scenario = ActivityScenario.launch(PreAlarmCheck.confirmSkipIntent(context, listOf(occurrence)))
        compose.waitForIdle()
    }

    /** Reads the sum ("47 + 38 =") off the screen and works it out. */
    private fun answer(): Int {
        val question = compose.onAllNodes(hasText(" =", substring = true)).fetchSemanticsNodes().first()
            .config[SemanticsProperties.Text].joinToString()
        return question.removeSuffix(" =").split(" + ").sumOf { it.trim().toInt() }
    }

    private fun typeOnKeypad(number: Int) = number.toString().forEach {
        compose.onAllNodesWithText(it.toString()).filterToOne(hasClickAction()).performClick()
    }

    private fun dayName(date: LocalDate) = context.resources.getStringArray(R.array.day_full)[isoDayIndexOf(date)]

    @Test
    fun turningOffTakesASumAndCanBeUndone() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val (alarm, at) = alarmInTwoHours()
        open(alarm, at)
        compose.waitForText(str(R.string.skip_confirm_title))
        // Straight here: no permissions walkthrough in the way, even on a first launch.
        val ringsAgain = LocalDate.ofEpochDay(epochDayOf(at)).plusDays(1)
        compose.onNodeWithText(str(R.string.skip_confirm_back_on, dayName(ringsAgain)).uppercase()).fetchSemanticsNode()

        // Nothing typed, then a wrong answer: the alarm stays exactly as it was.
        compose.onNodeWithText(str(R.string.skip_confirm_turn_off).uppercase()).performClick()
        typeOnKeypad(answer() + 1)
        compose.onNodeWithContentDescription(str(R.string.confirm)).performClick()
        compose.waitForText(str(R.string.wrong_try_again))
        assertNull(env.reload(alarm)!!.skipDate)

        typeOnKeypad(answer())
        compose.onNodeWithText(str(R.string.skip_confirm_turn_off).uppercase()).performClick()
        compose.waitForText(str(R.string.skip_confirm_done))
        compose.waitFor("skipped") { env.reload(alarm)!!.skipDate == epochDayOf(at) }
        assertTrue("a skipping alarm stays on", env.reload(alarm)!!.enabled)
        val notifications = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotNull("the receipt with its undo", notifications.getNotification(NotificationIds.SKIP_RECEIPT))

        compose.onNodeWithText(str(R.string.undo).uppercase()).performClick()
        compose.waitFor("undone") { env.reload(alarm)!!.skipDate == null }
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
        assertNull(notifications.getNotification(NotificationIds.SKIP_RECEIPT))
        // Back on the list, where the alarm is on as usual.
        compose.waitForDescription(str(R.string.cd_alarm_switch, "%02d:%02d".format(alarm.hour, alarm.minute)))
    }

    @Test
    fun keepLeavesTheAlarmAlone() {
        val (alarm, at) = alarmInTwoHours()
        open(alarm, at)
        compose.waitForText(str(R.string.skip_confirm_keep).uppercase())
        compose.onNodeWithText(str(R.string.skip_confirm_keep).uppercase()).performClick()
        compose.waitForDescription(str(R.string.cd_alarm_switch, "%02d:%02d".format(alarm.hour, alarm.minute)))
        assertNull(env.reload(alarm)!!.skipDate)
        assertEquals(at, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun aRingThatAlreadyChangedCannotBeTurnedOff() {
        val (alarm, at) = alarmInTwoHours()
        // The notification was about a ring that is no longer the next one (edited, or rang).
        open(alarm, at - 24 * 3_600_000L)
        compose.waitForText(str(R.string.skip_confirm_stale))
        assertTrue(compose.onAllNodesWithText(str(R.string.skip_confirm_turn_off).uppercase()).fetchSemanticsNodes().isEmpty())
        assertNull(env.reload(alarm)!!.skipDate)
    }
}
