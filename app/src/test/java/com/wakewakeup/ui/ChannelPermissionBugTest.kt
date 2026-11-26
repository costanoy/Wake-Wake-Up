package com.wakewakeup.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.ALARM_NOTIFICATION_CHANNEL_ID
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.permissions.AlarmPermission
import com.wakewakeup.ui.permissions.AlarmPermissions
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** TEC-07: canal "tocando" desligado não é detectado. */
@Ignore("TEC-07: AlarmPermissions não olha a importância do canal alarm_ringing")
@RunWith(AndroidJUnit4::class)
class ChannelPermissionBugTest {

    @get:Rule val env = AppEnv()

    @Test
    fun aMutedRingingChannelCountsAsMissingNotifications() {
        shadowOf(env.app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = env.app.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID)
        manager.createNotificationChannel(NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, "x", NotificationManager.IMPORTANCE_NONE))
        assertFalse(AlarmPermissions.isGranted(env.app, AlarmPermission.NOTIFICATIONS))
    }
}
