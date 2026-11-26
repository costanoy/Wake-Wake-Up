package com.wakewakeup.session

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** REV-05: o arquivo velho do vigia (1.1) não pode passar por cima de uma sessão já gravada no armazenamento do aparelho. */
@RunWith(AndroidJUnit4::class)
class RingWatchdogMigrationTest {

    @get:Rule val env = AppEnv()

    private fun write(context: Context, alarmId: Long) {
        context.getSharedPreferences("ring_session", Context.MODE_PRIVATE).edit()
            .putLong("alarm_id", alarmId)
            .putLong("started_at", System.currentTimeMillis() - 1_000L)
            .commit()
    }

    @Test
    fun anOldFileDoesNotOverwriteASessionAlreadyInDeviceStorage() {
        val store = env.app.createDeviceProtectedStorageContext()
        org.junit.Assume.assumeTrue(store.dataDir != env.app.dataDir)
        write(env.app, 5L)
        write(store, 9L)

        assertEquals(9L, RingWatchdog.saved(env.app)!!.alarmId)
        assertFalse(File(env.app.dataDir, "shared_prefs/ring_session.xml").exists())
    }

    @Test
    fun anOldFileIsStillMovedWhenThereIsNothingInDeviceStorageYet() {
        val store = env.app.createDeviceProtectedStorageContext()
        if (File(store.dataDir, "shared_prefs/ring_session.xml").exists()) return // same dir in this environment: nothing to move
        write(env.app, 5L)

        assertNotNull(RingWatchdog.saved(env.app))
        assertEquals(5L, RingWatchdog.saved(env.app)!!.alarmId)
    }
}
