package dev.xboyrex.darkplayer.feature.player.service

import androidx.compose.ui.geometry.Size
import dev.xboyrex.darkplayer.feature.player.extensions.NativePixelContentScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllersTest {

    @Test
    fun `PlaybackVolumeController preserves mute during fade and restores userVolume on unmute`() {
        val controller = PlaybackVolumeController(initialUserVolume = 0.8f, initialMuted = false)
        assertEquals(0.8f, controller.effectiveVolume, 0.001f)

        // Mute sets effectiveVolume to 0f even if fadeMultiplier changes
        controller.setMuted(true)
        assertEquals(0f, controller.effectiveVolume, 0.001f)
        controller.setFadeMultiplier(0.5f)
        assertEquals(0f, controller.effectiveVolume, 0.001f)

        // Unmute restores userVolume * fadeMultiplier
        controller.setMuted(false)
        assertEquals(0.4f, controller.effectiveVolume, 0.001f)
        controller.resetFade()
        assertEquals(0.8f, controller.effectiveVolume, 0.001f)
    }

    @Test
    fun `PlaybackSessionTimerController tracks monotonic sleep timer deadline and A-B repeat loop`() {
        var fakeNowMs = 1_000L
        val timer = PlaybackSessionTimerController(elapsedRealtimeProvider = { fakeNowMs })

        assertFalse(timer.isSleepTimerActive)
        timer.scheduleSleepTimer(minutes = 2)
        assertTrue(timer.isSleepTimerActive)
        assertEquals(120, timer.remainingSleepSeconds())

        fakeNowMs = 61_000L
        assertFalse(timer.pollSleepTimerExpired())
        assertEquals(60, timer.remainingSleepSeconds())

        fakeNowMs = 121_000L
        assertTrue(timer.pollSleepTimerExpired())
        assertFalse(timer.isSleepTimerActive)

        // A-B Repeat loop check
        timer.setAbRepeatPoints(startMs = 5_000L, endMs = 15_000L)
        assertTrue(timer.isAbLoopActive)
        assertFalse(timer.shouldLoopBackToPointA(currentPositionMs = 10_000L))
        assertTrue(timer.shouldLoopBackToPointA(currentPositionMs = 15_000L))
        timer.clearAbRepeat()
        assertFalse(timer.isAbLoopActive)
    }

    @Test
    fun `NativePixelContentScale returns 1 to 1 scale factor for valid dimensions`() {
        val factor = NativePixelContentScale.computeScaleFactor(
            srcSize = Size(1280f, 720f),
            dstSize = Size(2400f, 1080f),
        )
        assertEquals(1f, factor.scaleX, 0.001f)
        assertEquals(1f, factor.scaleY, 0.001f)
    }
}
