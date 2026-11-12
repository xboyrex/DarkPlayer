package dev.xboyrex.darkplayer.feature.player.state

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.feature.player.extensions.formatted
import dev.xboyrex.darkplayer.feature.player.extensions.setIsScrubbingModeEnabled
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Composable
fun rememberSeekGestureState(
    player: Player,
    sensitivity: Float = 0.5f,
    enableSeekGesture: Boolean,
): SeekGestureState {
    val seekGestureState = remember(
        player,
        sensitivity,
        enableSeekGesture,
    ) {
        SeekGestureState(
            player = player,
            sensitivity = sensitivity,
            enableSeekGesture = enableSeekGesture,
        )
    }
    DisposableEffect(key1 = seekGestureState) { onDispose { seekGestureState.onSeekEnd() } }
    return seekGestureState
}

@Stable
class SeekGestureState(
    private val player: Player,
    private val enableSeekGesture: Boolean = true,
    private val sensitivity: Float = 0.5f,
) {
    var isSeeking: Boolean by mutableStateOf(value = false)
        private set

    var seekStartPosition: Long? by mutableStateOf(value = null)
        private set

    var seekAmount: Long? by mutableStateOf(value = null)
        private set

    private var seekStartX = 0f
    private var lastRemoteSeekDispatchMs = 0L
    private var pendingRemoteSeekPosition: Long? = null

    companion object {
        private const val REMOTE_SEEK_THROTTLE_MS = 140L
        private val REMOTE_SHARE_SCHEMES = setOf("smb", "sftp", "ftp", "webdav", "webdavs")
    }

    private fun isRemoteNetworkShare(): Boolean {
        val scheme = player.currentMediaItem?.localConfiguration?.uri?.scheme?.lowercase()
        return scheme in REMOTE_SHARE_SCHEMES
    }

    fun onSeek(value: Long) {
        val duration = player.duration
        if (duration <= 0 || player.currentPosition < 0) {
            reset()
            return
        }
        if (!isSeeking) {
            isSeeking = true
            seekStartPosition = player.currentPosition
            player.setIsScrubbingModeEnabled(enabled = true)
        }

        val position = value.coerceIn(
            minimumValue = 0L,
            maximumValue = duration,
        )
        seekAmount = position - seekStartPosition!!
        dispatchOrThrottleSeek(position)
    }

    fun onSeekEnd() {
        flushPendingRemoteSeek()
        reset()
    }

    fun onDragStart(offset: Offset) {
        if (!enableSeekGesture) return
        if (player.currentPosition < 0) return
        if (player.duration <= 0) return
        if (!player.isCurrentMediaItemSeekable) return

        isSeeking = true
        seekStartX = offset.x
        seekStartPosition = player.currentPosition
        pendingRemoteSeekPosition = null

        player.setIsScrubbingModeEnabled(enabled = true)
    }

    @OptIn(UnstableApi::class)
    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        if (seekStartPosition == null) return
        val duration = player.duration
        if (duration <= 0 || player.currentPosition < 0) {
            reset()
            return
        }
        if (!player.isCurrentMediaItemSeekable) return
        if (player.currentPosition <= 0L && dragAmount < 0) return
        if (player.currentPosition >= duration && dragAmount > 0) return
        if (change.isConsumed) return

        val newPosition =
            seekStartPosition!! + ((change.position.x - seekStartX) * (sensitivity * 100)).toInt()
        val position = newPosition.coerceIn(
            minimumValue = 0L,
            maximumValue = duration,
        )
        seekAmount = position - seekStartPosition!!
        dispatchOrThrottleSeek(position)
    }

    fun onDragEnd() {
        flushPendingRemoteSeek()
        reset()
    }

    private fun dispatchOrThrottleSeek(position: Long) {
        if (!isRemoteNetworkShare()) {
            pendingRemoteSeekPosition = null
            player.seekTo(position)
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastRemoteSeekDispatchMs >= REMOTE_SEEK_THROTTLE_MS) {
            lastRemoteSeekDispatchMs = now
            pendingRemoteSeekPosition = null
            player.seekTo(position)
        } else {
            pendingRemoteSeekPosition = position
        }
    }

    private fun flushPendingRemoteSeek() {
        val pending = pendingRemoteSeekPosition ?: return
        pendingRemoteSeekPosition = null
        val duration = player.duration
        if (duration > 0 && player.currentPosition >= 0 && player.isCurrentMediaItemSeekable) {
            player.seekTo(pending.coerceIn(0L, duration))
        }
    }

    private fun reset() {
        if (isSeeking) player.setIsScrubbingModeEnabled(enabled = false)
        isSeeking = false
        seekStartPosition = null
        seekAmount = null
        pendingRemoteSeekPosition = null
        seekStartX = 0f
    }
}

val SeekGestureState.seekAmountFormatted: String
    get() {
        val seekAmount = seekAmount ?: return ""
        val sign = if (seekAmount < 0) "-" else "+"
        return sign + abs(n = seekAmount).milliseconds.formatted()
    }

val SeekGestureState.seekToPositionFormated: String
    get() {
        val position = seekStartPosition ?: return ""
        val seekAmount = seekAmount ?: return ""
        return (position + seekAmount).milliseconds.formatted()
    }
