package com.wakewakeup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import org.junit.Assert.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** TEC-06: restaurar o backup não rearma os alarmes no AlarmManager. */
@Ignore("TEC-06: sem BackupAgent, os alarmes restaurados de um backup não são armados até o app ser aberto")
@RunWith(AndroidJUnit4::class)
class RestoreRearmBugTest {

    @get:Rule val env = AppEnv()

    @Test
    fun theAppDeclaresABackupAgentThatRearmsAfterRestore() {
        assertNotNull(env.app.applicationInfo.backupAgentName)
    }
}
