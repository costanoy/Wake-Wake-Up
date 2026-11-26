package com.wakewakeup.session

import android.app.AlarmManager
import android.content.Intent
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.resetDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** LOG-01 / LOG-12: o que o BootReceiver faz com o celular ainda bloqueado, e depois dele. */
@RunWith(AndroidJUnit4::class)
class BootRearmTest {

    @get:Rule val env = AppEnv()

    private val everyDay = (0..6).toSet()

    private fun lockUser() {
        shadowOf(env.app.getSystemService(UserManager::class.java)).setUserUnlocked(false)
    }

    @Test
    fun aLockedBootArmsTheAlarmsFromTheProtectedCopyWithoutTheDatabase() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "Trabalho", days = everyDay))
        val expected = nextRingMillis(alarm)
        env.app.getSystemService(AlarmManager::class.java).cancelAll()
        assertNull(env.scheduledFor(alarm))
        resetDatabase() // closed, as it is before the first unlock
        lockUser()

        env.deliverBroadcast(Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))

        assertEquals(expected, env.scheduledFor(alarm)!!.triggerAtMs)
    }

    @Test
    fun aLockedBootKeepsASkipAndDropsAOneTimeAlarmThatAlreadyPassed() {
        val skipping = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        runBlocking { env.repository.skipNext(skipping) }
        val expected = nextRingMillis(env.reload(skipping)!!)
        val once = env.save(Alarm(hour = 8, minute = 0, label = "", days = emptySet()))
        // The one-time ring was armed for a moment that is gone by now.
        ArmedRings.put(env.app, once, System.currentTimeMillis() - 60_000L)
        env.app.getSystemService(AlarmManager::class.java).cancelAll()
        lockUser()

        env.deliverBroadcast(Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))

        assertEquals(expected, env.scheduledFor(skipping)!!.triggerAtMs)
        assertNull(env.scheduledFor(once))
    }

    @Test
    fun turningAnAlarmOffOrDeletingItRemovesItsProtectedCopy() {
        val off = env.save(Alarm(hour = 7, minute = 0, label = "", days = everyDay))
        val gone = env.save(Alarm(hour = 8, minute = 0, label = "", days = everyDay))
        assertEquals(setOf(off.id, gone.id), ArmedRings.all(env.app).map { it.alarm.id }.toSet())

        runBlocking {
            env.repository.setEnabled(off, false)
            env.repository.delete(gone)
        }

        assertTrue(ArmedRings.all(env.app).isEmpty())
    }

    @Test
    fun aBadProtectedEntryIsIgnoredNotThrown() {
        val prefs = env.app.createDeviceProtectedStorageContext().getSharedPreferences("armed_rings", 0)
        prefs.edit().putString("a_5", "{not json").putString("a_x", "{}").commit()
        assertTrue(ArmedRings.all(env.app).isEmpty())
        assertNotNull(prefs)
    }
}
