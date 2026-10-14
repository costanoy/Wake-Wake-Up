package com.wakewakeup.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.FakeRadio
import com.wakewakeup.ui.permissions.AlarmPermissions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class SoundAndTaskUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun fakeNetwork() = FakeRadio.install()

    @After
    fun close() {
        scenario?.close()
    }

    private fun alarms() = runBlocking { env.repository.observeAll().first() }

    /** Opens the editor on a fresh alarm with its sound row in view. */
    private fun openEditor(alarm: Alarm = Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0, 1, 2, 3, 4))): Alarm {
        val saved = env.save(alarm)
        AlarmPermissions.markOnboarded(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForText(saved.label)
        compose.onNodeWithText(saved.label).performClick()
        compose.waitForDescription("${str(R.string.hour)} %02d".format(saved.hour))
        return saved
    }

    private fun saveAndWait(alarm: Alarm, check: (Alarm) -> Boolean) {
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()
        compose.waitFor("saved") { env.reload(alarm)?.let(check) == true }
    }

    @Test
    fun choosingOneOfTheAppsSoundsAndTurningTheRampOff() {
        val alarm = openEditor()
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.waitForText(str(R.string.sound_bell_name))
        for (name in listOf(
            R.string.sound_phone_name, R.string.sound_beep1_name, R.string.sound_beep2_name, R.string.sound_siren1_name,
            R.string.sound_siren2_name, R.string.sound_birds_name, R.string.phone_music, R.string.radio_row,
        )) {
            compose.onNodeWithText(str(name)).assertExists()
        }

        compose.onNodeWithText(str(R.string.sound_phone_name)).performClick()
        compose.waitForText(str(R.string.sound_phone_name).uppercase())
        compose.onNodeWithText(str(R.string.preview_label).uppercase()).assertIsDisplayed()
        // The preview stops by itself after a couple of seconds.
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForText(str(R.string.selected_label).uppercase())

        compose.onNodeWithContentDescription(str(R.string.ramp_title)).assert(hasStateDescription(str(R.string.switch_on)))
        compose.onNodeWithContentDescription(str(R.string.ramp_title)).performClick()
        compose.waitFor("ramp off") {
            compose.onNodeWithContentDescription(str(R.string.ramp_title)).stateDescription() == str(R.string.switch_off)
        }

        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText(str(R.string.sound_phone_name))
        saveAndWait(alarm) { it.soundName == str(R.string.sound_phone_name) }
        val saved = env.reload(alarm)!!
        assertEquals("android.resource://${context.packageName}/raw/telefone_de_disco", saved.soundUri)
        assertFalse(saved.rampUp)
    }

    @Test
    fun pickingASongFromThePhone() {
        val alarm = openEditor()
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.waitForText(str(R.string.phone_music))
        compose.onNodeWithText(str(R.string.phone_music)).performClick()
        compose.waitForIdle()

        val song = Uri.parse("content://${PhoneFiles.AUTHORITY}/document/audio%3A42")
        Robolectric.setupContentProvider(PhoneFiles::class.java, PhoneFiles.AUTHORITY)
        var request: Intent? = null
        scenario!!.onActivity { activity -> request = shadowOf(activity).nextStartedActivityForResult?.intent }
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, request?.action)
        assertEquals(listOf("audio/*"), request?.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList())

        scenario!!.onActivity { shadowOf(it).receiveResult(request, android.app.Activity.RESULT_OK, Intent().setData(song)) }
        compose.waitForText("BOM DIA")
        compose.onNodeWithText("Bom dia").assertExists()
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText("Bom dia")
        saveAndWait(alarm) { it.soundUri == song.toString() }
        assertEquals("Bom dia", env.reload(alarm)!!.soundName)
    }

    @Test
    fun goingBackToTheSystemDefault() {
        val alarm = openEditor(
            Alarm(hour = 6, minute = 30, label = "Trabalho", days = setOf(0), soundUri = "android.resource://com.wakewakeup/raw/passaros", soundName = "Birds"),
        )
        compose.onNodeWithText("Birds").performClick()
        compose.waitForText(str(R.string.sound_default))
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText(str(R.string.sound_default))
        saveAndWait(alarm) { it.soundUri == null }
        assertNull(env.reload(alarm)!!.soundName)
    }

    @Test
    fun wakingUpToARadioStation() {
        val alarm = openEditor()
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.waitForText(str(R.string.radio_row))
        compose.onNodeWithText(str(R.string.radio_row_desc)).performClick()

        compose.waitForText(str(R.string.radio_popular).uppercase())
        compose.waitForText("Radio Um")
        compose.onNodeWithText("Radio Dois").assertExists()
        assertTrue(compose.onAllNodesWithTextCount("Radio Insegura") == 0)
        assertTrue(FakeRadio.requests.last().toString().contains("countrycode=US"))

        compose.onNodeWithText("Radio Um").performClick()
        compose.waitFor("station chosen") { compose.onAllNodesWithTextCount("Radio Um") >= 1 }
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        // The sound screen names the station on the radio row.
        compose.waitForText(str(R.string.ramp_title))
        compose.onNodeWithText("Radio Um", substring = true).assertExists()
        compose.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        compose.waitForText("Radio Um")
        saveAndWait(alarm) { it.soundUri == "https://um.example/stream" }
        assertEquals("Radio Um", env.reload(alarm)!!.soundName)
    }

    @Test
    fun radioWithoutInternetCanRetryAndSearch() {
        FakeRadio.offline = true
        openEditor()
        compose.onNodeWithText(str(R.string.sound_default)).performClick()
        compose.waitForText(str(R.string.radio_row_desc))
        compose.onNodeWithText(str(R.string.radio_row_desc)).performClick()
        compose.waitForText(str(R.string.radio_offline).uppercase())
        compose.onNodeWithText(str(R.string.radio_error)).assertIsDisplayed()

        FakeRadio.offline = false
        compose.onNodeWithText(str(R.string.radio_retry).uppercase()).performClick()
        compose.waitForText("Radio Um")

        compose.onNode(hasSetTextAction()).performTextInput("jazz")
        compose.mainClock.advanceTimeBy(600)
        compose.waitForText(str(R.string.radio_results).uppercase())
        compose.waitFor("search sent") { FakeRadio.requests.last().toString().contains("name=jazz") }

        FakeRadio.body = "[]"
        compose.onNode(hasSetTextAction()).performTextInput("x")
        compose.mainClock.advanceTimeBy(600)
        compose.waitForText(str(R.string.radio_empty).uppercase())
    }

    @Test
    fun taskSetupDifficultyAndHowManyProblems() {
        val alarm = openEditor()
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.task_summary_math, 3, 3)).performClick()
        compose.waitForText(str(R.string.task_setup_title))

        repeat(5) { compose.onNodeWithContentDescription(str(R.string.cd_fewer)).performClick() }
        compose.onNodeWithContentDescription("1").assertIsDisplayed()
        repeat(12) { compose.onNodeWithContentDescription(str(R.string.cd_more)).performClick() }
        compose.onNodeWithContentDescription("10").assertIsDisplayed()

        compose.onNodeWithContentDescription(str(R.string.hard)).performClick()
        compose.onNodeWithContentDescription(str(R.string.hard)).assertIsSelected()
        compose.onNodeWithContentDescription(str(R.string.save)).performClick()

        compose.waitForText(context.resources.getQuantityString(R.plurals.task_summary_math, 10, 10))
        saveAndWait(alarm) { it.taskCount == 10 }
        assertEquals(Difficulty.HARD, env.reload(alarm)!!.difficulty)
    }

    @Test
    fun theTaskPreviewShowsTodaysPhrase() {
        openEditor()
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.task_summary_math, 3, 3)).performClick()
        compose.waitForText(str(R.string.task_setup_title))
        compose.onNodeWithContentDescription(str(R.string.mode_phrase)).performClick()
        compose.waitForText(com.wakewakeup.session.MissionGenerator.phraseOfTheDay())
        compose.onNodeWithText(str(R.string.phrase_note)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.mode_math)).performClick()
        compose.waitForText(str(R.string.how_many).uppercase())
    }

    /** The phone's file picker: knows one song, "Bom dia.mp3". */
    class PhoneFiles : ContentProvider() {
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<String>?, selection: String?, args: Array<String>?, sortOrder: String?): Cursor =
            MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf("Bom dia.mp3")) }
        override fun getType(uri: Uri) = "audio/mpeg"
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<String>?) = 0

        companion object {
            const val AUTHORITY = "com.android.providers.media.documents"
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextCount(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}
