package com.wakewakeup.ui

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wakewakeup.testing.AppEnv
import com.wakewakeup.ui.sound.SoundPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.io.IOException

/** The short preview behind the play keys of the sound and radio screens. */
@RunWith(AndroidJUnit4::class)
class SoundPreviewTest {

    @get:Rule val env = AppEnv()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preview: SoundPreview
    private val players = mutableListOf<ShadowMediaPlayer>()
    private val station = "https://um.example/stream"

    @Before
    fun setUp() {
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        preview = SoundPreview(context, scope)
    }

    @After
    fun tearDown() {
        preview.stop()
        scope.cancel()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun loadsUntilThePlayerIsPrepared() {
        preview.play(station, 10_000)
        assertTrue(preview.loading)
        assertTrue(preview.isPreviewing(station))
        idle()
        assertFalse("ready: no longer connecting", preview.loading)
        assertTrue(preview.isPreviewing(station))
        assertEquals(ShadowMediaPlayer.State.STARTED, players.last().state)
    }

    @Test
    fun aPlayerErrorLeavesTheSoundMarkedAsFailed() {
        preview.play(station, 10_000)
        idle()
        players.last().invokeErrorListener(1, 0)
        assertNull(preview.previewing)
        assertFalse(preview.loading)
        assertTrue(preview.isFailed(station))
        assertFalse(preview.isFailed("https://dois.example/stream"))
    }

    @Test
    fun aSourceThatCannotBeOpenedFailsAndTheNextTryClearsIt() {
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, Uri.parse(station)), IOException("gone"))
        preview.play(station, 10_000)
        assertNull(preview.previewing)
        assertTrue(preview.isFailed(station))

        preview.play(null, 2_500)
        assertFalse("a new preview forgets the old failure", preview.isFailed(station))
        assertTrue(preview.isPreviewing(null))
    }

    @Test
    fun togglingTheSoundThatIsPlayingStopsIt() {
        preview.toggle(station, 10_000)
        idle()
        assertTrue(preview.isPreviewing(station))
        preview.toggle(station, 10_000)
        assertNull(preview.previewing)
        assertFalse(preview.loading)
        assertFalse("stopping is not failing", preview.isFailed(station))

        preview.toggle(station, 10_000)
        assertTrue(preview.isPreviewing(station))
    }
}
