package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** TEC-06: restaurar o backup não rearma os alarmes no AlarmManager. */
@RunWith(AndroidJUnit4::class)
class RestoreRearmBugTest {

    @get:Rule val env = AppEnv()

    @Test
    fun theAppDeclaresABackupAgentThatRearmsAfterRestore() {
        assertNotNull(env.app.applicationInfo.backupAgentName)
    }

    @Test
    fun finishingARestoreArmsTheAlarmsThatAreOn() {
        val alarm = env.save(com.wakewakeup.data.Alarm(hour = 7, minute = 0, label = "", days = (0..6).toSet()))
        env.app.getSystemService(android.app.AlarmManager::class.java).cancelAll()
        org.junit.Assert.assertNull(env.scheduledFor(alarm))

        // The system builds the agent and hands it the app's context; there is no Service to set up.
        val agent = RestoreRearmAgent()
        android.content.ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", android.content.Context::class.java)
            .apply { isAccessible = true }.invoke(agent, env.app)
        agent.onRestoreFinished()

        assertNotNull(env.scheduledFor(alarm))
    }
}
