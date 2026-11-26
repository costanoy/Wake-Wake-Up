package com.wakewakeup.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.TaskType
import com.wakewakeup.session.isoDayIndexOf
import com.wakewakeup.session.nextRingMillis
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
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
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

    /** What the list calls [date], a day less than a week away: "Wednesday". */
    private fun dayName(date: LocalDate): String = dayNames[isoDayIndexOf(date)]

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
        // The alarm's own time first, then 2 h 29 or 2 h 30 min away, depending on the seconds.
        val clock = "%02d:%02d".format(soon.hour, soon.minute)
        val countdown = str(R.string.next_in, str(R.string.duration_h_min, 2, 0)).uppercase().substringBefore(" 0 ")
        compose.waitForText("$clock · $countdown", substring = true)
    }

    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    @Test
    fun theNextStripScrollsAwayWithTheListOnASmallPhone() {
        for (i in 1..4) env.save(Alarm(hour = 5 + i, minute = 0, label = "Alarme $i", days = setOf(0)))
        launch()
        compose.waitForText("Alarme 1")
        val next = str(R.string.next_label).uppercase()
        assertTrue(compose.onAllNodesWithText(next).fetchSemanticsNodes().isNotEmpty())

        // To the last card (index 0 is PRÓXIMO and the warnings): the strip leaves the screen.
        compose.onNode(hasScrollAction()).performScrollToIndex(4)
        compose.waitForText("Alarme 4")
        compose.onNodeWithText("Alarme 4").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText(next).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aRingADayOrMoreAwayShowsItsDayAndTime() {
        // Weekly, two days from now: a countdown in hours would leave you working out which morning.
        val day = LocalDate.now().plusDays(2)
        env.save(Alarm(hour = 9, minute = 0, label = "Feira", days = setOf(isoDayIndexOf(day))))
        launch()
        compose.waitForText("Feira")
        val short = context.resources.getStringArray(R.array.day_short)[isoDayIndexOf(day)]
        compose.waitForText("$short, 09:00".uppercase())
    }

    @Test
    fun aRingAWeekOrMoreAwaySaysItIsTheNextOne() {
        // Weekly and skipping the day after tomorrow: it rings that weekday of the week after.
        val day = LocalDate.now().plusDays(2)
        env.save(Alarm(hour = 9, minute = 0, label = "Feira", days = setOf(isoDayIndexOf(day)), skipDate = day.toEpochDay()))
        launch()
        compose.waitForText("Feira")
        val short = context.resources.getStringArray(R.array.day_short)[isoDayIndexOf(day)]
        compose.waitForText("${str(R.string.day_next_week, short)}, 09:00".uppercase())
    }

    @Test
    fun creatingAnAlarmFromScratch() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()

        compose.waitForText(str(R.string.new_alarm))
        compose.onNode(drum(R.string.hour)).assert(hasStateDescription("07")).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_hour_up)).performClick()
        compose.onNodeWithContentDescription(str(R.string.cd_minute_down)).performClick()
        compose.onNode(drum(R.string.hour)).assert(hasStateDescription("08"))
        compose.onNode(drum(R.string.minute)).assert(hasStateDescription("59"))

        compose.onNode(hasSetTextAction()).performTextInput("Academia")
        // The day keys print MON TUE WED, like the cards. No day picked yet: it rings once.
        compose.onNodeWithText(context.resources.getStringArray(R.array.day_short)[0].uppercase()).assertIsDisplayed()
        compose.onNodeWithText(onceHint(8, 59)).assertIsDisplayed()
        compose.onNodeWithContentDescription(dayNames[0]).performClick()
        compose.onNodeWithContentDescription(dayNames[2]).performClick()
        compose.onNodeWithContentDescription(dayNames[2]).assertIsOn()
        compose.waitForNoText(onceHint(8, 59))

        // Task: switch to the phrase. DONE only goes back; nothing is saved yet.
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.task_summary_math, 3, 3)).performClick()
        compose.waitForText(str(R.string.task_setup_title))
        compose.onNodeWithContentDescription(str(R.string.mode_phrase)).performClick()
        compose.waitForText(str(R.string.phrase_today).uppercase())
        compose.onNodeWithContentDescription(str(R.string.ready)).performClick()
        compose.waitForText(str(R.string.task_summary_phrase))
        assertTrue(alarms().isEmpty())

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
        waitForDrum(R.string.hour, 6)
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
        waitForDrum(R.string.hour, 6)
        compose.onNodeWithContentDescription(str(R.string.cd_hour_up)).performClick()
        compose.onNode(drum(R.string.hour)).assert(hasStateDescription("07"))
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        // Something changed, so ‹ asks first; DISCARD leaves the alarm as it was.
        compose.waitForText(str(R.string.leave_edit_title))
        compose.onNodeWithText(str(R.string.leave_edit_body)).assertExists()
        compose.onNodeWithText(str(R.string.discard).uppercase()).performClick()
        compose.waitForDescription("06:30")
        assertEquals(6, env.reload(alarm)!!.hour)
        // Opening it again shows what's saved, not the abandoned edit.
        compose.onNodeWithText("Trabalho").performClick()
        waitForDrum(R.string.hour, 6)
    }

    @Test
    fun theSystemBackAsksBeforeLosingAChange() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        waitForDrum(R.string.hour, 6)
        compose.onNodeWithContentDescription(str(R.string.cd_minute_up)).performClick()

        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForText(str(R.string.leave_edit_title))
        // SAVE in the dialog saves, like the round key.
        compose.onNode(hasText(str(R.string.save).uppercase()) and hasAnyAncestor(isDialog())).performClick()
        compose.waitForDescription("06:31")
        assertEquals(31, env.reload(alarm)!!.minute)
        assertTrue(compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theSystemBackLeavesAnUntouchedAlarmAtOnce() {
        env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        waitForDrum(R.string.hour, 6)
        // Changed and changed back is unchanged too.
        compose.onNodeWithContentDescription(dayNames[5]).performClick()
        compose.onNodeWithContentDescription(dayNames[5]).performClick()

        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForDescription("06:30")
        assertTrue(compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty())
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theTimeDrumsWorkWithTalkBack() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.hour, 7)

        // Each drum is a slider TalkBack can set, and the minutes also go ten at a time.
        compose.onNode(drum(R.string.hour)).performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
        compose.onNode(drum(R.string.minute)).performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        compose.onNode(drum(R.string.minute)).performCustomAccessibilityActionWithLabel(str(R.string.cd_minutes_plus_10))
        waitForDrum(R.string.minute, 35)
        compose.onNode(drum(R.string.minute)).performCustomAccessibilityActionWithLabel(str(R.string.cd_minutes_minus_10))
        waitForDrum(R.string.minute, 25)
        compose.onNode(drum(R.string.hour)).assert(hasStateDescription("06"))

        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForDescription(str(R.string.cd_alarm_switch, "06:25"))
        val saved = alarms().single()
        assertEquals(6, saved.hour)
        assertEquals(25, saved.minute)
    }

    /** What the editor prints under the day keys when none is picked: the day it will ring, and the time. */
    private fun onceHint(hour: Int, minute: Int): String {
        val day = LocalDate.ofEpochDay(nextTriggerEpochDay(Alarm(hour = hour, minute = minute, label = "", days = emptySet()), LocalDateTime.now()))
        return str(R.string.days_none_hint_at, dayName(day), "%02d:%02d".format(hour, minute))
    }

    @Test
    fun theOnceHintNamesTheDayAndFollowsTheTime() {
        // A time already past today rings tomorrow; one still to come rings today. Needs room on
        // both sides of "now" inside the same day.
        val now = LocalTime.now()
        assumeTrue(now.isAfter(LocalTime.of(1, 30)) && now.isBefore(LocalTime.of(22, 30)))
        val passed = now.minusHours(1)
        val coming = now.plusHours(1)
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.hour, 7)

        compose.onNode(drum(R.string.hour)).performSemanticsAction(SemanticsActions.SetProgress) { it(passed.hour.toFloat()) }
        compose.onNode(drum(R.string.minute)).performSemanticsAction(SemanticsActions.SetProgress) { it(passed.minute.toFloat()) }
        val tomorrow = dayName(LocalDate.now().plusDays(1))
        compose.waitForText(str(R.string.days_none_hint_at, tomorrow, "%02d:%02d".format(passed.hour, passed.minute)))

        compose.onNode(drum(R.string.hour)).performSemanticsAction(SemanticsActions.SetProgress) { it(coming.hour.toFloat()) }
        compose.onNode(drum(R.string.minute)).performSemanticsAction(SemanticsActions.SetProgress) { it(coming.minute.toFloat()) }
        val today = dayName(LocalDate.now())
        compose.waitForText(str(R.string.days_none_hint_at, today, "%02d:%02d".format(coming.hour, coming.minute)))
    }

    private fun typeInTheTimeDialog(digits: String) {
        compose.waitForText(str(R.string.type_time_title))
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput(digits)
    }

    @Test
    fun typingFourDigitsSetsTheTime() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.hour, 7)

        compose.onNode(drum(R.string.hour)).performClick()
        typeInTheTimeDialog("0645")
        compose.onNodeWithText(str(R.string.confirm).uppercase()).performClick()
        compose.waitForNoText(str(R.string.type_time_title))
        waitForDrum(R.string.hour, 6)
        waitForDrum(R.string.minute, 45)

        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitForDescription(str(R.string.cd_alarm_switch, "06:45"))
        val saved = alarms().single()
        assertEquals(6, saved.hour)
        assertEquals(45, saved.minute)
    }

    @Test
    fun aTimeThatDoesNotExistShowsTheErrorAndKeepsTheDialog() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.minute, 0)

        compose.onNode(drum(R.string.minute)).performClick()
        typeInTheTimeDialog("2575")
        compose.waitForText(str(R.string.type_time_invalid))
        compose.onNodeWithText(str(R.string.confirm).uppercase()).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(str(R.string.type_time_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.type_time_invalid)).assertIsDisplayed()
        // Nothing moved behind the dialog.
        compose.onNode(drum(R.string.hour)).assert(hasStateDescription("07"))
    }

    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi", fontScale = 1.3f)
    @Test
    fun theMissionLabelFitsItsColumnWithALargeFont() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        compose.waitForText(str(R.string.new_alarm))

        val label = compose.onNodeWithText(str(R.string.task).uppercase())
        label.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        label.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        assertFalse("MISSION is cut off", layouts.single().hasVisualOverflow)
        assertEquals(1, layouts.single().lineCount)
    }

    @Test
    fun deletingTakesTwoTaps() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launch()
        compose.waitForText("Trabalho")
        compose.onNodeWithText("Trabalho").performClick()
        waitForDrum(R.string.hour, 6)

        val key = compose.onNodeWithContentDescription(str(R.string.delete)).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription(str(R.string.delete)).performClick()
        compose.waitForDescription(str(R.string.delete_confirm))
        assertNotNull("one tap must not delete", env.reload(alarm))
        // The longer caption doesn't push the key aside, so the second tap lands on it.
        assertEquals(key, compose.onNodeWithContentDescription(str(R.string.delete_confirm)).fetchSemanticsNode().boundsInRoot)

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
    fun leavingANewAlarmSavesNothing() {
        launch()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.hour, 7)
        // Nothing to delete yet: ‹ is the way out, straight away while nothing was touched.
        assertTrue(compose.onAllNodesWithContentDescription(str(R.string.delete)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText(str(R.string.empty_title))

        // Once something is set, it asks, and DISCARD creates nothing.
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        waitForDrum(R.string.hour, 7)
        compose.onNodeWithContentDescription(dayNames[0]).performClick()
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText(str(R.string.leave_new_title))
        compose.onNodeWithText(str(R.string.discard).uppercase()).performClick()
        compose.waitForText(str(R.string.empty_title))
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun aCardReadsItsDaysAsOnePhrase() {
        env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        env.save(Alarm(hour = 9, minute = 0, label = "Fim de semana", days = setOf(5, 6), enabled = false))
        env.save(Alarm(hour = 7, minute = 15, label = "Academia", days = setOf(0, 2, 4)))
        env.save(Alarm(hour = 8, minute = 0, label = "Diario", days = (0..6).toSet()))
        launch()
        compose.waitForText("Academia")
        // Not seven abbreviations per card: what TalkBack says is the days that ring.
        compose.onNodeWithContentDescription(str(R.string.days_weekdays)).assertExists()
        compose.onNodeWithContentDescription(str(R.string.days_weekend)).assertExists()
        compose.onNodeWithContentDescription(str(R.string.days_every_day)).assertExists()
        compose.onNodeWithContentDescription(str(R.string.days_and, "${dayNames[0]}, ${dayNames[2]}", dayNames[4])).assertExists()
    }

    /** The editor's hour or minute drum, which TalkBack adjusts like a slider ("Hour", "07"). */
    private fun drum(labelRes: Int) =
        hasContentDescription(str(labelRes)) and SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    private fun waitForDrum(labelRes: Int, value: Int) {
        val shown = "%02d".format(value)
        compose.waitFor("${str(labelRes)} drum at $shown") {
            compose.onAllNodes(drum(labelRes) and hasStateDescription(shown)).fetchSemanticsNodes().isNotEmpty()
        }
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
        compose.waitForText(str(R.string.skip_prompt, dayName(skipped), dayName(ringsAgain)))
        // Only under the alarm that was just switched off.
        assertEquals(1, compose.onAllNodesWithText(str(R.string.skip_once).uppercase()).fetchSemanticsNodes().size)
        assertFalse(env.reload(daily)!!.enabled)

        compose.onNodeWithText(str(R.string.skip_once).uppercase()).performClick()
        val skippingText = str(R.string.skipping_info, dayName(skipped), dayName(ringsAgain))
        compose.waitForText(skippingText)
        val skipping = env.reload(daily)!!
        assertTrue("a skipping alarm stays on", skipping.enabled)
        assertEquals(skipped.toEpochDay(), skipping.skipDate)
        // Armed for the day after, so the phone's own "next alarm" is right too.
        assertEquals(nextRingMillis(skipping), env.scheduledFor(daily)!!.triggerAtMs)
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, clock)).assert(hasStateDescription(str(R.string.switch_on)))

        // DESFAZER puts the skipped day back.
        compose.onNodeWithText(str(R.string.undo).uppercase()).performClick()
        compose.waitForNoText(skippingText)
        compose.waitFor("skip undone") {
            env.reload(daily)!!.skipDate == null && env.scheduledFor(daily)?.triggerAtMs == nextTriggerMillis(daily)
        }
        assertTrue(env.reload(daily)!!.enabled)
        assertFalse(env.reload(alreadyOff)!!.enabled)
    }

    @Test
    fun switchingAOneOffAlarmOffDoesNotOfferTheSkip() {
        val oneOff = env.save(Alarm(hour = 5, minute = 0, label = "Viagem", days = emptySet()))
        launch()
        compose.waitForText("Viagem")
        // One line with the day it rings, not seven unlit days that read as "never".
        val ringsOn = LocalDate.ofEpochDay(nextTriggerEpochDay(oneOff))
        compose.onNodeWithContentDescription(str(R.string.once_on_day, dayName(ringsOn))).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "05:00")).performClick()
        compose.waitFor("alarm off") { env.reload(oneOff)?.enabled == false && env.scheduledFor(oneOff) == null }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText(str(R.string.skip_once).uppercase()).fetchSemanticsNodes().isEmpty())
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
    fun alarmsStayInTimeOrderWhetherOnOrOff() {
        env.save(Alarm(hour = 9, minute = 0, label = "C-on", days = setOf(6)))
        val off = env.save(Alarm(hour = 5, minute = 0, label = "A-off", days = setOf(6), enabled = false))
        env.save(Alarm(hour = 7, minute = 0, label = "B-on", days = setOf(6)))
        launch()
        compose.waitForText("C-on")
        fun tops() = listOf("A-off", "B-on", "C-on").map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops().sorted(), tops())

        // Switching one on doesn't move its card from under the finger.
        val before = tops()
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "05:00")).performClick()
        compose.waitFor("alarm on") { env.reload(off)?.enabled == true }
        compose.waitForIdle()
        assertEquals(before, tops())
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
