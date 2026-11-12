package dev.xboyrex.darkplayer.feature.player.service

import android.os.SystemClock

/**
 * Service-owned controller for Sleep Timer and A–B Repeat loop points (F06).
 *
 * Lives in [PlayerService] so both Sleep Timer and A–B Repeat remain active when
 * [dev.xboyrex.darkplayer.feature.player.PlayerActivity] closes for background playback.
 */
class PlaybackSessionTimerController(
    private val elapsedRealtimeProvider: () -> Long = SystemClock::elapsedRealtime,
) {
    var sleepTimerDeadlineElapsedMs: Long = 0L
        private set

    var pointAMs: Long = -1L
        private set

    var pointBMs: Long = -1L
        private set

    val isSleepTimerActive: Boolean
        get() = sleepTimerDeadlineElapsedMs > elapsedRealtimeProvider()

    val isAbLoopActive: Boolean
        get() = pointAMs >= 0L && pointBMs > pointAMs

    fun scheduleSleepTimer(minutes: Int) {
        sleepTimerDeadlineElapsedMs = if (minutes > 0) {
            elapsedRealtimeProvider() + minutes * 60_000L
        } else {
            0L
        }
    }

    fun remainingSleepSeconds(nowElapsedMs: Long = elapsedRealtimeProvider()): Int {
        if (sleepTimerDeadlineElapsedMs <= 0L) return 0
        val diffMs = (sleepTimerDeadlineElapsedMs - nowElapsedMs).coerceAtLeast(0L)
        return ((diffMs + 999L) / 1_000L).toInt()
    }

    fun pollSleepTimerExpired(nowElapsedMs: Long = elapsedRealtimeProvider()): Boolean {
        if (sleepTimerDeadlineElapsedMs <= 0L) return false
        if (nowElapsedMs >= sleepTimerDeadlineElapsedMs) {
            sleepTimerDeadlineElapsedMs = 0L
            return true
        }
        return false
    }

    fun setAbRepeatPoints(startMs: Long, endMs: Long) {
        pointAMs = startMs
        pointBMs = endMs
    }

    fun clearAbRepeat() {
        pointAMs = -1L
        pointBMs = -1L
    }

    fun shouldLoopBackToPointA(currentPositionMs: Long): Boolean =
        pointAMs >= 0L && pointBMs > pointAMs && currentPositionMs >= pointBMs
}
