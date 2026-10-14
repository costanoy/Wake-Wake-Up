package com.wakewakeup.testing

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.wakewakeup.WakeWakeUpApplication
import com.wakewakeup.data.Alarm
import com.wakewakeup.data.AppDatabase
import com.wakewakeup.session.AlarmSessionState
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowBroadcastPendingResult
import org.robolectric.shadows.ShadowBroadcastReceiver
import org.robolectric.shadows.ShadowMediaPlayer
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * The app's real process-wide pieces (Room database, AlarmManager, session state) wired up
 * inside Robolectric, plus the bits of glue the tests need to drive them.
 *
 * The app keeps two singletons that outlive a single test in the same JVM: the Room database
 * and [AlarmSessionState]. Both are reset after every test, so the next test's Application
 * builds a fresh database in its own fresh data directory.
 */
class AppEnv : ExternalResource() {

    val app: WakeWakeUpApplication get() = ApplicationProvider.getApplicationContext()
    val repository get() = app.container.alarmRepository
    val history get() = app.container.wakeHistoryRepository

    override fun before() {
        // Robolectric's MediaPlayer only "plays" data sources it was told about: let every
        // sound (bundled, system default, radio stream) be playable unless a test says otherwise.
        ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(FAKE_SOUND_MS, 0) }
    }

    override fun after() {
        AlarmSessionState.clear()
        resetDatabase()
        // AndroidX caches the first Application it sees in a static ViewModel factory. A phone
        // has one Application per process; Robolectric makes a new one per test, so without this
        // the next test's ViewModels would read the previous test's (closed) database.
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance")
            .apply { isAccessible = true }
            .set(null, null)
    }

    fun save(alarm: Alarm): Alarm = runBlocking {
        val id = repository.save(alarm)
        repository.getById(id)!!
    }

    fun reload(alarm: Alarm): Alarm? = runBlocking { repository.getById(alarm.id) }

    val alarmManager: ShadowAlarmManager
        get() = shadowOf(app.getSystemService(Context.ALARM_SERVICE) as AlarmManager)

    fun scheduledAlarms(): List<ShadowAlarmManager.ScheduledAlarm> = alarmManager.scheduledAlarms

    /** The AlarmManager entry for [alarm], matched by the broadcast's request code. */
    fun scheduledFor(alarm: Alarm): ShadowAlarmManager.ScheduledAlarm? =
        scheduledAlarms().singleOrNull { shadowOf(it.broadcast()).requestCode == alarm.id.toInt() }

    /** The intent an AlarmManager entry broadcasts when it fires. */
    fun broadcastIntentOf(scheduled: ShadowAlarmManager.ScheduledAlarm): Intent = shadowOf(scheduled.broadcast()).savedIntent

    // Robolectric 4.17 only exposes the entry's PendingIntent through this deprecated field.
    @Suppress("DEPRECATION")
    private fun ShadowAlarmManager.ScheduledAlarm.broadcast(): PendingIntent = operation!!

    /** Sends [intent] the way the system would, then waits for the receiver's async work to end. */
    fun deliverBroadcast(intent: Intent) {
        app.sendBroadcast(intent)
        awaitReceivers(intent.component?.className)
    }

    private fun awaitReceivers(className: String?) {
        idleMain()
        shadowOf(app).registeredReceivers
            .map { it.broadcastReceiver }
            .filter { className == null || it.javaClass.name == className }
            .forEach { awaitPendingResult(it) }
        idleMain()
    }

    private fun awaitPendingResult(receiver: BroadcastReceiver) {
        val shadowReceiver = Shadow.extract<ShadowBroadcastReceiver>(receiver)
        if (!shadowReceiver.wentAsync()) return
        val pending = shadowReceiver.originalPendingResult ?: return
        Shadow.extract<ShadowBroadcastPendingResult>(pending).future.get(10, TimeUnit.SECONDS)
    }

    /** Fires an AlarmManager entry, delivering its broadcast to AlarmReceiver and waiting for it. */
    fun fire(scheduled: ShadowAlarmManager.ScheduledAlarm) {
        val target = broadcastIntentOf(scheduled).component?.className
        alarmManager.fireAlarm(scheduled)
        awaitReceivers(target)
    }
}

/**
 * Fake sounds last a day: Robolectric's MediaPlayer re-queues its playback events every time a
 * looping sound wraps around, and the queue doubles with each lap — minutes of a ringing alarm
 * would flood memory. (A real phone loops the sound natively, without any of this.)
 */
const val FAKE_SOUND_MS = 24 * 60 * 60 * 1000

fun resetDatabase() {
    val field = AppDatabase::class.java.getDeclaredField("instance").apply { isAccessible = true }
    (field.get(null) as AppDatabase?)?.close()
    field.set(null, null)
}

fun idleMain() {
    shadowOf(Looper.getMainLooper()).idle()
}

/** Moves the main looper's clock forward, running everything that comes due (tickers, delays). */
fun advance(duration: Duration) {
    shadowOf(Looper.getMainLooper()).idleFor(duration)
}

/** Pumps the main looper until [condition] holds — for work that hops to Room's threads and back. */
fun awaitUntil(timeoutMs: Long = 10_000, message: String = "condition", condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (true) {
        idleMain()
        if (condition()) return
        if (System.currentTimeMillis() > deadline) throw AssertionError("Timed out waiting for $message")
        Thread.sleep(5)
    }
}
