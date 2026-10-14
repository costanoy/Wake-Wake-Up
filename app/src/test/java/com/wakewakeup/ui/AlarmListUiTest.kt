package com.wakewakeup.ui

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.session.nextTriggerMillis
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermissions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class AlarmListUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    private fun launch() {
        AlarmPermissions.markOnboarded(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun alarms() = runBlocking { env.repository.observeAll().first() }

    private val dayNames get() = context.resources.getStringArray(R.array.day_full)

    /** What the list calls [date]: "today", "tomorrow, Wednesday" or "Friday". */
    private fun relativeDay(date: LocalDate): String {
        val today = LocalDate.now()
        val name = dayNames[isoDayIndexOf(date)]
        return when (date) {
            today -> str(R.string.day_today)
            today.plusDays(1) -> str(R.string.day_tomorrow, name)
            else -> name
        }
    }

    @Test
    fun anEmptyListInvitesToCreateTheFirstAlarm() {
        launch()
        compose.waitForText(str(R.string.empty_title))
        compose.onNodeWithText(str(R.string.empty_body)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.next_none).uppercase()).assertIsDisplayed()
        // No stats button until there's something to show.
        assertTrue(compose.onAllNodesWithContentDescription(str(R.string.cd_stats_icon)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theNextDisplayCountsDownToTheNearestAlarm() {
        val soon = LocalTime.now().plusHours(2).plusMinutes(30)
        val later = LocalTime.now().plusHours(5)
        env.save(Alarm(hour = later.hour, minute = later.minute, label = "Depois", days = emptySet()))
        env.save(Alarm(hour = soon.hour, minute = soon.minute, label = "Antes", days = emptySet()))
        env.save(Alarm(hour = LocalTime.now().plusHours(1).hour, minute = 0, label = "Desligado", days = emptySet(), enabled = false))
        launch()
        compose.waitForText("Antes")
        // 2 h 29 or 2 h 30 min away, depending on the seconds.
        compose.waitForText(str(R.string.next_in, str(R.string.duration_h_min, 2, 0)).uppercase().substringBefore(" 0 "), substring = true)
    }

    @Test
    fun creatingAnAlarmFromScratch() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()

        compose.waitForText(str(R.string.new_alarm))
        compose.onNodeWithContentDescription("${str(R.string.hour)} 07").assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_hour_up)).performClick()
        compose.onNodeWithContentDescription(str(R.string.cd_minute_down)).performClick()
        compose.onNodeWithContentDescription("${str(R.string.hour)} 08").assertIsDisplayed()
        compose.onNodeWithContentDescription("${str(R.string.minute)} 59").assertIsDisplayed()

        compose.onNode(hasSetTextAction()).performTextInput("Academia")
        compose.onNodeWithContentDescription(dayNames[0]).performClick()
        compose.onNodeWithContentDescription(dayNames[2]).performClick()
        compose.onNodeWithContentDescription(dayNames[2]).assertIsSelected()

        // Task: switch to the phrase.
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.task_summary_math, 3, 3)).performClick()
        compose.waitForText(str(R.string.task_setup_title))
        compose.onNodeWithContentDescription(str(R.string.mode_phrase)).performClick()
        compose.waitForText(str(R.string.phrase_today).uppercase())
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForText(str(R.string.task_summary_phrase))

        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForDescription(str(R.string.cd_alarm_switch, "08:59"))

        val saved = alarms().single()
        assertEquals(8, saved.hour)
        assertEquals(59, saved.minute)
        assertEquals("Academia", saved.label)
        assertEquals(setOf(0, 2), saved.days)
        assertEquals(TaskType.PHRASE, saved.taskType)
        assertTrue(saved.enabled)
        assertEquals(nextTriggerMillis(saved), env.scheduledFor(saved)!!.triggerAtMs)

        compose.onNodeWithContentDescription("08:59").assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "08:59"))
            .assert(hasStateDescription(str(R.string.switch_on)))
        compose.onNodeWithText(str(R.string.task_label_phrase).uppercase()).assertIsDisplayed()
    }

    @Test
    fun editingAnAlarm() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        compose.waitForText(str(R.string.edit_alarm))
        compose.waitForDescription("${str(R.string.hour)} 06")
        compose.onNodeWithContentDescription(str(R.string.cd_minute_up)).performClick()
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForDescription("06:31")

        val edited = env.reload(alarm)!!
        assertEquals(31, edited.minute)
        assertEquals("Trabalho", edited.label)
        assertEquals(1, alarms().size)
        assertEquals(nextTriggerMillis(edited), env.scheduledFor(edited)!!.triggerAtMs)
    }

    @Test
    fun leavingTheEditorWithBackDiscardsTheChanges() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        compose.waitForDescription("${str(R.string.hour)} 06")
        compose.onNodeWithContentDescription(str(R.string.cd_hour_up)).performClick()
        compose.onNodeWithContentDescription("${str(R.string.hour)} 07").assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        compose.waitForDescription("06:30")
        assertEquals(6, env.reload(alarm)!!.hour)
        // Opening it again shows what's saved, not the abandoned edit.
        compose.onNodeWithText("Trabalho").performClick()
        compose.waitForDescription("${str(R.string.hour)} 06")
    }

    @Test
    fun deletingTakesTwoTaps() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        compose.waitForDescription("${str(R.string.hour)} 06")

        compose.onNodeWithContentDescription(str(R.string.delete)).performClick()
        compose.waitForDescription(str(R.string.delete_confirm))
        assertNotNull("one tap must not delete", env.reload(alarm))

        // Left alone, it disarms after 3 seconds.
        compose.mainClock.advanceTimeBy(3_100)
        compose.waitForDescription(str(R.string.delete))

        compose.onNodeWithContentDescription(str(R.string.delete)).performClick()
        compose.onNodeWithContentDescription(str(R.string.delete_confirm)).performClick()
        compose.waitForText(str(R.string.empty_title))
        assertNull(env.reload(alarm))
        assertNull(env.scheduledFor(alarm))
    }

    @Test
    fun deletingANewAlarmJustLeaves() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        compose.waitForDescription(str(R.string.delete))
        compose.onNodeWithContentDescription(str(R.string.delete)).performClick()
        compose.onNodeWithContentDescription(str(R.string.delete_confirm)).performClick()
        compose.waitForText(str(R.string.empty_title))
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun switchingARecurringAlarmOffOffersToSkipJustOneDay() {
        // Every day, an hour from now (or early tomorrow if that would cross midnight).
        val time = LocalTime.now().plusHours(1).withSecond(0).withNano(0)
        val daily = env.save(Alarm(hour = time.hour, minute = time.minute, label = "Diario", days = (0..6).toSet()))
        val alreadyOff = env.save(Alarm(hour = 9, minute = 15, label = "Desligado", days = setOf(5, 6), enabled = false))
        val clock = "%02d:%02d".format(time.hour, time.minute)
        launch()
        compose.waitForText("Diario")

        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, clock)).performClick()
        val skipped = LocalDate.ofEpochDay(nextTriggerEpochDay(daily))
        val ringsAgain = skipped.plusDays(1)
        val prompt = str(R.string.resume_prompt, relativeDay(ringsAgain))
        compose.waitForText(prompt)
        // Only under the alarm that was just switched off.
        assertEquals(1, compose.onAllNodesWithText(str(R.string.activate).uppercase()).fetchSemanticsNodes().size)
        assertFalse(env.reload(daily)!!.enabled)

        compose.onNodeWithText(str(R.string.activate).uppercase()).performClick()
        compose.waitForText(str(R.string.resume_scheduled, relativeDay(ringsAgain)))
        val paused = env.reload(daily)!!
        assertFalse(paused.enabled)
        assertEquals(skipped.toEpochDay(), paused.resumeDate)
        // The "Next" display counts down to the ring after the skipped one, not "None".
        assertTrue(compose.onAllNodesWithText(str(R.string.next_none).uppercase()).fetchSemanticsNodes().isEmpty())

        // Tapping the drawer cancels the skip: the alarm simply stays off.
        compose.onNodeWithText(str(R.string.resume_scheduled, relativeDay(ringsAgain))).performClick()
        compose.waitForNoText(str(R.string.resume_scheduled, relativeDay(ringsAgain)))
        compose.waitFor("skip cancelled") { env.reload(daily)!!.resumeDate == null && env.scheduledFor(daily) == null }
        assertFalse(env.reload(alreadyOff)!!.enabled)
    }

    @Test
    fun switchingAOneOffAlarmOffDoesNotOfferTheSkip() {
        val oneOff = env.save(Alarm(hour = 5, minute = 0, label = "Viagem", days = emptySet()))
        launch()
        compose.waitForText("Viagem")
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "05:00")).performClick()
        compose.waitFor("alarm off") { env.reload(oneOff)?.enabled == false && env.scheduledFor(oneOff) == null }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText(str(R.string.activate).uppercase()).fetchSemanticsNodes().isEmpty())
        assertNull(env.scheduledFor(oneOff))
    }

    @Test
    fun switchingAnAlarmBackOn() {
        val alarm = env.save(Alarm(hour = 5, minute = 0, label = "Cedo", days = setOf(0, 1, 2, 3, 4), enabled = false))
        launch()
        compose.waitForText("Cedo")
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "05:00")).performClick()
        compose.waitFor("alarm on and scheduled") { env.reload(alarm)?.enabled == true && env.scheduledFor(alarm) != null }
        assertEquals(nextTriggerMillis(alarm), env.scheduledFor(alarm)!!.triggerAtMs)
        compose.waitFor("switch on") {
            compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "05:00")).stateDescription() == str(R.string.switch_on)
        }
    }

    @Test
    fun enabledAlarmsComeFirstInTimeOrder() {
        env.save(Alarm(hour = 9, minute = 0, label = "C-on", days = setOf(6)))
        env.save(Alarm(hour = 5, minute = 0, label = "A-off", days = setOf(6), enabled = false))
        env.save(Alarm(hour = 7, minute = 0, label = "B-on", days = setOf(6)))
        launch()
        compose.waitForText("C-on")
        val tops = listOf("B-on", "C-on", "A-off").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun theStatsButtonOpensTheStats() {
        env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = setOf(0)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNode(hasContentDescription(str(R.string.cd_stats_icon))).performClick()
        compose.waitForText(str(R.string.stats_empty))
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText("Trabalho")
    }
}
