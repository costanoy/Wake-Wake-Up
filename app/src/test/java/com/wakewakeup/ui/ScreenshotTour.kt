package com.wakewakeup.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.Difficulty
import com.wakewakeup.data.TaskType
import com.wakewakeup.data.WakeHistoryEntry
import com.wakewakeup.session.AlarmSessionState
import com.wakewakeup.session.FinishedInfo
import com.wakewakeup.session.MissionGenerator
import com.wakewakeup.session.MissionState
import com.wakewakeup.session.RingActivity
import com.wakewakeup.session.SessionScreen
import com.wakewakeup.session.nextTriggerEpochDay
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.FakeRadio
import com.wakewakeup.ui.permissions.AlarmPermissions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Renders every screen to PNG files in app/build/screenshots, for reviewing the UI without a
 * phone. Off by default: set the WWU_SCREENSHOTS environment variable to run it.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "pt-rBR-$PHONE")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
open class ScreenshotTour {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var main: ActivityScenario<MainActivity>? = null
    private var ring: ActivityScenario<RingActivity>? = null

    protected open val prefix = "pt"

    @Before
    fun onlyWhenAsked() {
        assumeTrue("set WWU_SCREENSHOTS to render the screens", System.getenv("WWU_SCREENSHOTS") != null)
        FakeRadio.install()
    }

    @After
    fun close() {
        ring?.close()
        main?.close()
    }

    private fun shot(name: String, activity: () -> Activity) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        val a = activity()
        val view = a.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun mainActivity(): Activity {
        var activity: Activity? = null
        main!!.onActivity { activity = it }
        return activity!!
    }

    private fun ringActivity(): Activity {
        var activity: Activity? = null
        ring!!.onActivity { activity = it }
        return activity!!
    }

    private fun launchMain(onboarded: Boolean = true) {
        if (onboarded) AlarmPermissions.markOnboarded(context)
        main = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    private fun seedList() {
        env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4), difficulty = Difficulty.MEDIUM))
        env.save(Alarm(hour = 7, minute = 15, label = "Academia", days = setOf(0, 2, 4), taskType = TaskType.PHRASE))
        env.save(Alarm(hour = 9, minute = 0, label = "Fim de semana", days = setOf(5, 6), enabled = false))
        env.save(Alarm(hour = 22, minute = 0, label = "Remédio", days = emptySet()))
    }

    @Test
    fun list() {
        launchMain()
        compose.waitForText(str(R.string.empty_title))
        shot("01-lista-vazia", ::mainActivity)
        main!!.close()

        seedList()
        launchMain()
        compose.waitForText("Trabalho")
        shot("02-lista", ::mainActivity)

        // Switching off a recurring alarm offers to skip just one day.
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "06:30")).performClick()
        compose.waitForText(str(R.string.skip_once).uppercase())
        shot("03-lista-desligou-recorrente", ::mainActivity)
        compose.onNodeWithText(str(R.string.skip_once).uppercase()).performClick()
        compose.waitForText(str(R.string.undo).uppercase())
        shot("04-lista-pulando-um-dia", ::mainActivity)
    }

    @Test
    fun permissions() {
        launchMain(onboarded = false)
        compose.waitForText(str(R.string.perm_title))
        shot("05-permissoes", ::mainActivity)
        compose.onNodeWithText(str(R.string.continue_label).uppercase()).performClick()
        compose.waitForText(str(R.string.perm_nag))
        shot("06-permissoes-aviso", ::mainActivity)
    }

    @Test
    fun editor() {
        launchMain()
        compose.waitForDescription(str(R.string.create_alarm))
        compose.onNodeWithContentDescription(str(R.string.create_alarm)).performClick()
        compose.waitForText(str(R.string.new_alarm))
        shot("07-novo-alarme", ::mainActivity)

        compose.onNode(hasSetTextAction()).performTextInput("Trabalho")
        for (day in 0..4) compose.onNodeWithContentDescription(context.resources.getStringArray(R.array.day_full)[day]).performClick()
        compose.onNodeWithContentDescription(str(R.string.delete)).performClick()
        compose.waitForDescription(str(R.string.delete_confirm))
        shot("08-editando-excluir-armado", ::mainActivity)

        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.task_summary_math, 3, 3)).performClick()
        compose.waitForText(str(R.string.task_setup_title))
        shot("09-tarefa-matematica", ::mainActivity)
        compose.onNodeWithContentDescription(str(R.string.mode_phrase)).performClick()
        compose.waitForText(MissionGenerator.phraseOfTheDay())
        shot("10-tarefa-frase", ::mainActivity)
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        compose.waitForText(str(R.string.sound_default))
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.waitForText(str(R.string.sound_bell_name))
        shot("11-som", ::mainActivity)

        compose.onNodeWithText(str(R.string.radio_row_desc)).performClick()
        compose.waitForText("Radio Um")
        shot("12-radio", ::mainActivity)
    }

    private fun startSession(alarm: Alarm) {
        AlarmSessionState.start(alarm)
        ring = ActivityScenario.launch(RingActivity::class.java)
        compose.waitForIdle()
    }

    @Test
    fun ringing() {
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4), taskCount = 3))
        startSession(alarm)
        compose.waitForDescription(str(R.string.start_mission))
        shot("13-tocando", ::ringActivity)

        AlarmSessionState.update { it.copy(waits = 2, holdUntilMillis = System.currentTimeMillis() + 4 * 60_000 + 32_000) }
        compose.waitForText(str(R.string.espere_returns_in))
        shot("14-espere", ::ringActivity)

        AlarmSessionState.update {
            it.copy(
                holdUntilMillis = null, screen = SessionScreen.MISSION,
                mission = MissionState(TaskType.MATH, index = 1, count = 3, secondsLeft = 74, question = "47 + 38", answer = "85", typed = "8"),
            )
        }
        compose.waitForText("47 + 38 =")
        shot("15-missao", ::ringActivity)

        AlarmSessionState.update { it.copy(mission = it.mission!!.copy(typed = "84", wrong = true, secondsLeft = 23)) }
        compose.waitForText(str(R.string.wrong_try_again))
        shot("16-missao-errou", ::ringActivity)

        val phrase = MissionGenerator.phraseOfTheDay()
        AlarmSessionState.update {
            it.copy(mission = MissionState(TaskType.PHRASE, index = 0, count = 1, secondsLeft = 95, phrase = phrase, typed = phrase.take(14)))
        }
        compose.waitForText(str(R.string.type_phrase_short).uppercase())
        shot("17-missao-frase", ::ringActivity)

        AlarmSessionState.update {
            it.copy(screen = SessionScreen.GOOD_MORNING, finished = FinishedInfo(waits = 2, alarmHour = 6, alarmMinute = 30, wokeHour = 6, wokeMinute = 42, durationSec = 742))
        }
        compose.waitForText(str(R.string.good_morning))
        shot("18-bom-dia", ::ringActivity)
    }

    @Test
    fun stats() {
        launchMain()
        compose.waitForText(str(R.string.empty_title))
        main!!.close()
        val today = LocalDate.now()
        runBlocking {
            listOf(6 to 450, 5 to 672, 4 to 324, 2 to 840, 1 to 372, 0 to 742).forEach { (ago, sec) ->
                env.history.record(
                    WakeHistoryEntry(date = today.minusDays(ago.toLong()), alarmHour = 6, alarmMinute = 30, wokeHour = 6, wokeMinute = 30 + sec / 60, waits = ago % 3, durationSec = sec),
                )
            }
        }
        env.save(Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4)))
        launchMain()
        compose.waitForText("Trabalho")
        compose.onNodeWithContentDescription(str(R.string.cd_stats_icon)).performClick()
        compose.waitForText(str(R.string.minutes_to_wake).uppercase())
        shot("19-estatisticas", ::mainActivity)
    }

    @Test
    fun skipDrawerDays() {
        // A daily alarm an hour from now, switched off: which day does the drawer name?
        val time = LocalTime.now().plusHours(1)
        val alarm = env.save(Alarm(hour = time.hour, minute = time.minute, label = "Diário", days = (0..6).toSet()))
        launchMain()
        compose.waitForText("Diário")
        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "%02d:%02d".format(time.hour, time.minute))).performClick()
        compose.waitForText(str(R.string.skip_once).uppercase())
        shot("20-lista-oferta-pular", ::mainActivity)
    }
}

/** The same tour in English. */
@Config(qualifiers = "en-rUS-$PHONE")
class ScreenshotTourEnglish : ScreenshotTour() {
    override val prefix = "en"
}

/** A small phone (360 × 640 dp), where space runs out first. */
@Config(qualifiers = "pt-rBR-w360dp-h640dp-xhdpi")
class ScreenshotTourSmall : ScreenshotTour() {
    override val prefix = "pequeno"
}

/** The system font made 30% bigger, as people with low vision often set it. */
@Config(qualifiers = "pt-rBR-$PHONE", fontScale = 1.3f)
class ScreenshotTourLargeFont : ScreenshotTour() {
    override val prefix = "fonte-grande"
}
