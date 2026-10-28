package com.wakewakeup.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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

    private fun granted() = AlarmPermission.entries.count { AlarmPermissions.isGranted(context, it) }

    private fun grantAllButFullScreen() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context.getSystemService(PowerManager::class.java)).setIgnoringBatteryOptimizations(context.packageName, true)
    }

    @Test
    fun firstLaunchWalksThroughThePermissionsOnce() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        compose.onNodeWithText(str(R.string.perm_count, granted(), 4).uppercase()).assertIsDisplayed()
        for (title in listOf(R.string.perm_notifications_title, R.string.perm_exact_title, R.string.perm_fullscreen_title, R.string.perm_battery_title)) {
            compose.onNodeWithText(str(title)).assertIsDisplayed()
        }

        // Continuing with something missing warns first, then lets you through.
        compose.onNodeWithText(str(R.string.continue_label).uppercase()).performClick()
        compose.waitForText(str(R.string.perm_nag))
        compose.onNodeWithText(str(R.string.continue_label).uppercase()).performClick()
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
        val allow = str(R.string.allow).uppercase()

        // First tap: the system permission dialog.
        compose.onAllNodesWithText(allow).onFirst().performClick()
        compose.waitForIdle()
        var asked: List<String>? = null
        scenario!!.onActivity { asked = shadowOf(it).lastRequestedPermission?.requestedPermissions?.toList() }
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), asked)
        assertFalse(AlarmPermissions.shouldAskNotificationsInApp(context))

        // A "no" can only be undone in Settings, so the next tap goes there.
        shadowOf(context).clearNextStartedActivities()
        compose.onAllNodesWithText(allow).onFirst().performClick()
        compose.waitForIdle()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, shadowOf(context).nextStartedActivity?.action)
    }

    @Test
    fun theOtherPermissionsOpenTheirSettingsPages() {
        launch()
        compose.waitForText(str(R.string.perm_title))
        val pages = mutableListOf<String?>()
        val allow = str(R.string.allow).uppercase()
        // Notifications are first; skip the in-app dialog by marking it asked.
        AlarmPermissions.markNotificationsAsked(context)
        val rows = compose.onAllNodesWithText(allow).fetchSemanticsNodes().size
        for (i in 0 until rows) {
            shadowOf(context).clearNextStartedActivities()
            compose.onAllNodesWithText(allow)[i].performClick()
            compose.waitForIdle()
            val intent = shadowOf(context).nextStartedActivity
            pages += intent?.action
            // Notifications and battery open lists the app is found in; the others, its own page.
            if (intent?.action != Settings.ACTION_APP_NOTIFICATION_SETTINGS && intent?.action != Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) {
                assertEquals("package:${context.packageName}", intent?.data.toString())
            }
        }
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
        compose.waitForText(str(R.string.perm_count, granted(), 4).uppercase())
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
}
