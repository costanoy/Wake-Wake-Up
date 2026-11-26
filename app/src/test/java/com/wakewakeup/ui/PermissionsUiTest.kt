package com.wakewakeup.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.MainActivity
import com.wakewakeup.R
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
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE)
class PermissionsUiTest {

    @get:Rule(order = 0) val env = AppEnv()
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
    }

    /** What "x de 3" counts: battery is only recommended. */
    private val required = AlarmPermission.entries.filter { it.required }

    private fun granted() = required.count { AlarmPermissions.isGranted(context, it) }

    private val plateTitles = listOf(R.string.perm_notifications_title, R.string.perm_exact_title, R.string.perm_fullscreen_title, R.string.perm_battery_title)

    /** What TalkBack calls the PERMITIR key of the plate titled [title]. */
    private fun allowKey(title: Int) = str(R.string.cd_allow, str(title))

    private fun grantAllButFullScreen() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(PowerManager::class.java)).setIgnoringBatteryOptimizations(context.packageName, true)
    }

    @Test
    fun firstLaunchWalksThroughThePermissionsOnce() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        compose.onNodeWithText(str(R.string.perm_count, granted(), 3).uppercase()).assertIsDisplayed()
        for (title in plateTitles) {
            compose.onNodeWithText(str(title)).assertIsDisplayed()
        }
        // Battery is there too, marked as only recommended.
        compose.onNodeWithText(str(R.string.perm_recommended).uppercase()).assertIsDisplayed()

        // Continuing with something missing warns first, then lets you through.
        compose.onNodeWithText(str(R.string.continue_label).uppercase()).performClick()
        compose.waitForText(str(R.string.perm_nag))
        compose.onNodeWithText(str(R.string.continue_anyway).uppercase()).performClick()
        compose.waitForText(str(R.string.empty_title))

        // Never again after that.
        scenario!!.close()
        launch()
        compose.waitForText(str(R.string.empty_title))
        assertTrue(compose.onAllNodesWithText(str(R.string.perm_title)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun notificationsAreAskedInTheAppOnlyTheFirstTime() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        val allow = allowKey(R.string.perm_notifications_title)

        // First tap: the system permission dialog.
        compose.onNodeWithContentDescription(allow).performClick()
        compose.waitForIdle()
        var asked: List<String>? = null
        scenario!!.onActivity { asked = shadowOf(it).lastRequestedPermission?.requestedPermissions?.toList() }
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), asked)
        assertFalse(AlarmPermissions.shouldAskNotificationsInApp(context))

        // A "no" can only be undone in Settings, so the next tap goes there.
        shadowOf(context).clearNextStartedActivities()
        compose.onNodeWithContentDescription(allow).performClick()
        compose.waitForIdle()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(context).nextStartedActivity?.action)
    }

    @Test
    fun theOtherPermissionsOpenTheirSettingsPages() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        val pages = mutableListOf<String?>()
        // Notifications are first; skip the in-app dialog by marking it asked.
        AlarmPermissions.markNotificationsAsked(context)
        for (title in plateTitles) {
            // Each key says which permission it allows, so TalkBack doesn't read four alike.
            val key = allowKey(title)
            // Exact alarms are already allowed (AppEnv): that plate shows LIBERADO instead.
            if (compose.onAllNodesWithContentDescription(key).fetchSemanticsNodes().isEmpty()) continue
            shadowOf(context).clearNextStartedActivities()
            compose.onNodeWithContentDescription(key).performClick()
            compose.waitForIdle()
            val intent = shadowOf(context).nextStartedActivity
            pages += intent?.action
            // Notifications and battery open lists the app is found in; the others, its own page.
            if (intent?.action != Settings.ACTION_APP_NOTIFICATION_SETTINGS && intent?.action != Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) {
                assertEquals("package:${context.packageName}", intent?.data.toString())
            }
        }
        assertEquals(pages.toString(), 3, pages.size)
        assertTrue(pages.toString(), pages.contains(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        assertTrue(pages.toString(), pages.all { it != null })
    }

    @Test
    fun aPermissionGrantedInSettingsLightsUpOnReturn() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        val before = granted()
        grantAllButFullScreen()
        scenario!!.moveToState(Lifecycle.State.STARTED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        compose.waitForText(str(R.string.perm_count, granted(), 3).uppercase())
        assertTrue(granted() > before)
        assertTrue(compose.onAllNodesWithText(str(R.string.allowed).uppercase()).fetchSemanticsNodes().size >= 3)
    }

    @Test
    fun withEverythingAllowedTheAppOpensStraightOnTheList() {
        grantAllButFullScreen()
        if (!AlarmPermissions.allGranted(context)) {
            // Robolectric can't grant the full-screen permission; the screen still lets you through.
            launch()
            compose.waitForText(str(R.string.perm_title))
            return
        }
        launch()
        compose.waitForText(str(R.string.empty_title))
    }

    @Test
    fun alarmPermissionsAreReadFromTheSystem() {
        assertFalse(AlarmPermissions.isGranted(context, AlarmPermission.NOTIFICATIONS))
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(AlarmPermissions.isGranted(context, AlarmPermission.NOTIFICATIONS))

        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(AlarmPermissions.isGranted(context, AlarmPermission.EXACT_ALARMS))
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(AlarmPermissions.isGranted(context, AlarmPermission.EXACT_ALARMS))
        assertTrue((context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms())
    }

    @Test
    fun batteryIsOnlyRecommended() {
        assertEquals(3, required.size)
        assertFalse(AlarmPermission.BATTERY.required)
        // Exact alarms come allowed (AppEnv) and Robolectric can't allow the full screen.
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(AlarmPermissions.isGranted(context, AlarmPermission.BATTERY))
        // Battery saving still on, yet only the full screen counts as missing.
        assertEquals(listOf(AlarmPermission.FULL_SCREEN), AlarmPermissions.missingRequired(context))
    }
}
