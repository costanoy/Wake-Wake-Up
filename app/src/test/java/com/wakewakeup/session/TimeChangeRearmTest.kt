package com.wakewakeup.session

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.awaitUntil
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** LOG-08: mudar a hora do celular à mão rearma os alarmes. */
@RunWith(AndroidJUnit4::class)
class TimeChangeRearmTest {

    @get:Rule val env = AppEnv()

    @Test
    fun changingTheClockByHandRearmsTheAlarms() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = (0..6).toSet()))
        AlarmScheduler(env.app).cancel(alarm)
        assertNull(env.scheduledFor(alarm))
        env.deliverBroadcast(Intent(Intent.ACTION_TIME_CHANGED).setClass(env.app, BootReceiver::class.java))
        awaitUntil(message = "alarm armed again") { env.scheduledFor(alarm) != null }
        assertNotNull(env.scheduledFor(alarm))
    }
}
