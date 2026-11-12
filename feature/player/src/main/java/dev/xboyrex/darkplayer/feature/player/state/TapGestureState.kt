package dev.xboyrex.darkplayer.feature.player.state

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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.model.DoubleTapGesture
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
    val tapGestureState = remember(
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
    return tapGestureState
}

@Stable
class TapGestureState(
    private val player: Player,
    private val seekIncrementMillis: Long,
    private val useLongPressGesture: Boolean = true,
    private val coroutineScope: CoroutineScope,
    val longPressSpeed: Float = 2.0f,
    val doubleTapGesture: DoubleTapGesture,
    val interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    var seekMillis by mutableLongStateOf(value = 0L)
    var isLongPressGestureInAction by mutableStateOf(value = false)

    private var resetJob: Job? = null
    private var currentSpeed: Float = player.playbackParameters.speed

    /**
     * When a double-tap skip session is already active (`seekMillis != 0L`), any subsequent single tap
     * on the left or right zone immediately adds another `-10s` / `+10s` step without waiting for a second double-tap!
     */
    fun tryHandleContinuationTap(offset: Offset, size: IntSize): Boolean {
        if (seekMillis == 0L || !player.isCurrentMediaItemSeekable) return false
        if (doubleTapGesture == DoubleTapGesture.NONE || doubleTapGesture == DoubleTapGesture.PLAY_PAUSE) return false

        val fractionX = offset.x / size.width.toFloat().coerceAtLeast(1f)
        return when {
            fractionX < 0.42f -> {
                val target = (player.currentPosition - seekIncrementMillis).coerceAtLeast(0L)
                player.seekTo(target)
                if (seekMillis > 0L) seekMillis = 0L
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
                resetDoubleTapSeekState()
                true
            }
            fractionX > 0.58f -> {
                val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                val target = (player.currentPosition + seekIncrementMillis).coerceAtMost(dur)
                player.seekTo(target)
                if (seekMillis < 0L) seekMillis = 0L
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
                resetDoubleTapSeekState()
                true
            }
            else -> false
        }
    }

    fun handleDoubleTap(offset: Offset, size: IntSize) {
        if (!player.isCurrentMediaItemSeekable) return

        val action = when (doubleTapGesture) {
            DoubleTapGesture.FAST_FORWARD_AND_REWIND -> {
                val viewCenterX = size.width / 2
                when {
                    offset.x < viewCenterX -> DoubleTapAction.SEEK_BACKWARD
                    else -> DoubleTapAction.SEEK_FORWARD
                }
            }

            DoubleTapGesture.BOTH -> {
                val eventPositionX = offset.x / size.width.toFloat().coerceAtLeast(1f)
                when {
                    eventPositionX < 0.40f -> DoubleTapAction.SEEK_BACKWARD
                    eventPositionX > 0.60f -> DoubleTapAction.SEEK_FORWARD
                    else -> DoubleTapAction.PLAY_PAUSE
                }
            }

            DoubleTapGesture.PLAY_PAUSE -> DoubleTapAction.PLAY_PAUSE

            DoubleTapGesture.NONE -> return
        }

        when (action) {
            DoubleTapAction.SEEK_BACKWARD -> {
                val target = (player.currentPosition - seekIncrementMillis).coerceAtLeast(0L)
                player.seekTo(target)
                if (seekMillis > 0L) {
                    seekMillis = 0L
                }
                seekMillis -= seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }

            DoubleTapAction.SEEK_FORWARD -> {
                val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                val target = (player.currentPosition + seekIncrementMillis).coerceAtMost(dur)
                player.seekTo(target)
                if (seekMillis < 0L) {
                    seekMillis = 0L
                }
                seekMillis += seekIncrementMillis
                interactionSource.tryEmit(PressInteraction.Press(pressPosition = offset))
            }

            DoubleTapAction.PLAY_PAUSE -> {
                when (player.isPlaying) {
                    true -> player.pause()
                    false -> player.play()
                }
            }
        }
        resetDoubleTapSeekState()
    }

    fun handleLongPress(offset: Offset) {
        if (!useLongPressGesture) return
        if (!player.isPlaying) return

        isLongPressGestureInAction = true
        currentSpeed = player.playbackParameters.speed
        player.setPlaybackSpeed(longPressSpeed)
    }

    fun handleOnLongPressRelease() {
        if (isLongPressGestureInAction) {
            isLongPressGestureInAction = false
            player.setPlaybackSpeed(currentSpeed)
        }
    }

    private fun resetDoubleTapSeekState() {
        resetJob?.cancel()
        resetJob = coroutineScope.launch {
            delay(850.milliseconds)
            seekMillis = 0L
        }
    }
}

enum class DoubleTapAction {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    PLAY_PAUSE,
}
