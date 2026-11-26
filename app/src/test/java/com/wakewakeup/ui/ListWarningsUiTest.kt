package com.wakewakeup.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioManager
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.ALARM_NOTIFICATION_CHANNEL_ID
import com.wakewakeup.MainActivity
import com.wakewakeup.R
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermission
import com.wakewakeup.ui.permissions.AlarmPermissions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/** The warnings under PRÓXIMO: what can keep an alarm from being heard, and the way to fix it. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class ListWarningsUiTest {

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

    /** Back from Settings (or anywhere else): the list reads everything again. */
    private fun comeBack() {
        scenario!!.moveToState(Lifecycle.State.STARTED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
    }

    /** The detail line of the permission warning (its title is the same for any count). */
    private fun missing(count: Int) = context.resources.getQuantityString(R.plurals.perm_missing_detail, count, count)

    /** The app reads the volume through the application's AudioManager, the same one as here. */
    private val audio: AudioManager get() = context.getSystemService(AudioManager::class.java)!!

    private fun setAlarmVolume(level: Int) = audio.setStreamVolume(AudioManager.STREAM_ALARM, level, 0)

    private fun alarmVolume() = audio.getStreamVolume(AudioManager.STREAM_ALARM)

    private val maxVolume get() = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    @Test
    fun missingPermissionsShowUnderTheNextDisplay() {
        // Robolectric starts with notifications off and can't allow the full screen; exact alarms
        // come allowed (AppEnv). It shows even before there's an alarm.
        launch()
        compose.waitForText(str(R.string.empty_title))
        compose.waitForText(missing(2))
        // Two lines: what is wrong, then what to do.
        compose.onNodeWithText(str(R.string.perm_missing_title)).assertIsDisplayed()

        // Allowed in Settings meanwhile: the count follows when the list comes back.
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        comeBack()
        compose.waitForText(missing(1))
        // Battery saving is still on, but battery is only recommended and never counts.
        assertFalse(AlarmPermissions.isGranted(context, AlarmPermission.BATTERY))
    }

    private fun setRingingChannel(importance: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID)
        manager.createNotificationChannel(NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, "x", importance))
    }

    @Test
    fun aLowRingingChannelShowsTheWarning() {
        // Notifications are allowed for the app; only the channel decides. The full screen (not
        // allowed under Robolectric) is the one other missing permission.
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        setRingingChannel(NotificationManager.IMPORTANCE_LOW)
        launch()
        compose.waitForText(missing(2))
    }

    @Test
    fun aHighRingingChannelShowsNoWarningOfItsOwn() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        setRingingChannel(NotificationManager.IMPORTANCE_HIGH)
        launch()
        compose.waitForText(missing(1))
    }

    @Test
    fun theWarningOpensThePermissionsAndOneTapComesBack() {
        env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = setOf(0)))
        launch()
        compose.waitForText(missing(2))
        compose.onNodeWithText(missing(2)).performClick()
        compose.waitForText(str(R.string.perm_short_title))

        // A detour, not the first-launch walkthrough: a ‹ in the header, no risk text first, and
        // one tap on the foot key goes straight back.
        compose.onNodeWithContentDescription(str(R.string.cd_back)).assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText(str(R.string.perm_nag)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText(str(R.string.back_to_list).uppercase()).performClick()
        compose.waitForNoText(str(R.string.perm_short_title))
        compose.waitForText("Trabalho")

        // Back to the list it came from, not a second one on top: Back now leaves the app.
        var leaving = false
        scenario!!.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
            leaving = it.isFinishing
        }
        assertTrue(leaving)
    }

    @Test
    fun aLowAlarmVolumeIsFlaggedAndTurnedUp() {
        env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = setOf(0)))
        setAlarmVolume(2)
        launch()
        val warning = str(R.string.alarm_volume_low, 2, maxVolume)
        compose.waitForText(warning)

        compose.onNodeWithText(str(R.string.alarm_volume_up).uppercase()).performClick()
        compose.waitForNoText(warning)
        assertEquals((maxVolume * 0.8f).roundToInt(), alarmVolume())
    }

    @Test
    fun theVolumeOnlyMattersWhileAnAlarmIsOn() {
        env.save(Alarm(hour = 7, minute = 0, label = "Cedo", days = setOf(0), enabled = false))
        setAlarmVolume(1)
        launch()
        compose.waitForText("Cedo")
        val warning = str(R.string.alarm_volume_low, 1, maxVolume)
        assertTrue(compose.onAllNodesWithText(warning).fetchSemanticsNodes().isEmpty())

        compose.onNodeWithContentDescription(str(R.string.cd_alarm_switch, "07:00")).performClick()
        compose.waitForText(warning)
    }

    @Test
    fun aLoudEnoughPhoneGetsNoVolumeWarning() {
        env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = setOf(0)))
        setAlarmVolume(maxVolume / 2 + 1)
        launch()
        compose.waitForText("Trabalho")
        // The permission warning is there (Robolectric), the volume one isn't.
        compose.waitForText(missing(2))
        assertTrue(compose.onAllNodesWithText(str(R.string.alarm_volume_up).uppercase()).fetchSemanticsNodes().isEmpty())
    }
}
