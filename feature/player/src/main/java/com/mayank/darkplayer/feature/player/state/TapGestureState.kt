package com.mayank.darkplayer.feature.player.state

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.model.DoubleTapGesture
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@Composable
fun rememberTapGestureState(
    player: Player,
    doubleTapGesture: DoubleTapGesture,
    seekIncrementMillis: Long,
    useLongPressGesture: Boolean,
    longPressSpeed: Float,
): TapGestureState {
    val coroutineScope = rememberCoroutineScope()
    return remember(
        player,
        doubleTapGesture,
        seekIncrementMillis,
        useLongPressGesture,
        longPressSpeed,
    ) {
        TapGestureState(
            player = player,
            doubleTapGesture = doubleTapGesture,
            seekIncrementMillis = seekIncrementMillis,
            useLongPressGesture = useLongPressGesture,
            longPressSpeed = longPressSpeed,
            coroutineScope = coroutineScope,
        )
    }
}

@Stable
class TapGestureState(
    private val player: Player,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 3.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    var seekMillis by mutableLongStateOf(value = 0L)
    var isLongPressGestureInAction by mutableStateOf(value = false)

    private var resetJob: Job? = null
    private var previousSpeed: Float = 1.0f

    fun handleDoubleTap(offset: Offset, size: IntSize) {
        if (!player.isCurrentMediaItemSeekable) return

        val action = when (doubleTapGesture) {
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                val viewCenterX = size.width / 2
                if (offset.x < viewCenterX) DoubleTapAction.SEEK_BACKWARD else DoubleTapAction.SEEK_FORWARD
            }
            DoubleTapGesture.BOTH -> {
                val eventPositionX = offset.x / size.width
                when {
                    eventPositionX < 0.35 -> DoubleTapAction.SEEK_BACKWARD
                    eventPositionX > 0.65 -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }
            }
            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE
            DoubleTapGesture.NONE -> return
        }

        when (action) {
            DoubleTapAction.SEEK_BACKWARD -> {
                player.seekTo((player.currentPosition - seekIncrementMillis).coerceAtLeast(0L))
                if (seekMillis > 0L) seekMillis = 0L
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }
            DoubleTapAction.SEEK_FORWARD -> {
                player.seekTo(player.currentPosition + seekIncrementMillis)
                if (seekMillis < 0L) seekMillis = 0L
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }
            DoubleTapAction.PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
        }
        resetDoubleTapSeekState()
    }

    /**
     * Activates pitch-locked 2x (or configured) playback speed while holding down.
     */
    fun handleLongPress(offset: Offset) {
        if (!useLongPressGesture) return
        if (!player.isPlaying && !player.playWhenReady) return

        previousSpeed = player.playbackParameters.speed.takeIf { it > 0f } ?: 1.0f
        isLongPressGestureInAction = true
        val targetSpeed = longPressSpeed.coerceIn(1.25f, 4.0f)
        player.playbackParameters = PlaybackParameters(targetSpeed, 1.0f)
    }

    /**
     * Always reverts speed back to the previous speed (pitch locked to 1.0f) on release/cancel.
     */
    fun handleOnLongPressRelease() {
        if (isLongPressGestureInAction) {
            isLongPressGestureInAction = false
            player.playbackParameters = PlaybackParameters(previousSpeed, 1.0f)
        }
    }

    private fun resetDoubleTapSeekState() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            delay(750.milliseconds)
            seekMillis = 0L
        }
    }
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}
