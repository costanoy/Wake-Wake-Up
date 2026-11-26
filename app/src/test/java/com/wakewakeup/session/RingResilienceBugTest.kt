package com.wakewakeup.session

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.AppDatabase
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.testing.awaitUntil
import com.wakewakeup.testing.idleMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

/** Bugs de robustez do toque (TEC-02 e TEC-03). */
@RunWith(AndroidJUnit4::class)
class RingResilienceBugTest {

    @get:Rule val env = AppEnv()

    private var controller: ServiceController<AlarmRingService>? = null
    private val players = mutableListOf<ShadowMediaPlayer>()

    @After
    fun stopService() {
        controller?.destroy()
    }

    @Test
    fun aDatabaseFailureStillStartsTheRingingService() {
        val alarm = env.save(Alarm(hour = 7, minute = 0, label = "", days = (0..6).toSet()))
        // Um erro real do Room na hora do disparo (esquema quebrado, banco corrompido).
        AppDatabase.get(env.app).openHelper.writableDatabase.execSQL("DROP TABLE alarms")
        runCatching {
            env.deliverBroadcast(Intent(env.app, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarm.id))
        }
        val started = shadowOf(env.app).allStartedServices
        assertEquals("o serviço de toque deveria ter sido iniciado mesmo sem banco", 1, started.size)
    }

    @Test
    fun aLocalPlayerThatErrorsMidRingIsReplacedByAnotherSound() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        val user = "content://com.example.provider/doc/1"
        val alarm = env.save(Alarm(hour = 6, minute = 30, label = "", days = setOf(0), soundUri = user))
        val intent = Intent(env.app, AlarmRingService::class.java).putExtra(EXTRA_ALARM_ID, alarm.id)
        controller = Robolectric.buildService(AlarmRingService::class.java, intent).create()
        controller!!.get().onStartCommand(intent, 0, 1)
        awaitUntil(message = "tocando") { players.any { it.state == ShadowMediaPlayer.State.STARTED } }
        val first = players.first { it.state == ShadowMediaPlayer.State.STARTED }
        assertEquals(DataSource.toDataSource(env.app, Uri.parse(user)), first.dataSource)

        first.invokeErrorListener(MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        idleMain()
        val other = players.lastOrNull { it !== first && it.state == ShadowMediaPlayer.State.STARTED }
        assertNotNull("deveria cair para o som padrão ou o sino", other)
    }
}
