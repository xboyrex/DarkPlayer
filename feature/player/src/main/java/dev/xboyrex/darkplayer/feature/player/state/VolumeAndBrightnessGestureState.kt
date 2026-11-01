package dev.xboyrex.darkplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVolumeAndBrightnessGestureState(
    volumeState: VolumeState,
    brightnessState: BrightnessState,
    enableVolumeGesture: Boolean,
    enableBrightnessGesture: Boolean,
    volumeGestureSensitivity: Float,
    brightnessGestureSensitivity: Float,
): VolumeAndBrightnessGestureState {
    val coroutineScope = rememberCoroutineScope()
    val volumeAndBrightnessGestureState = remember(
        volumeState,
        brightnessState,
        enableVolumeGesture,
        enableBrightnessGesture,
        volumeGestureSensitivity,
        brightnessGestureSensitivity,
    ) {
        VolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = enableVolumeGesture,
            enableBrightnessGesture = enableBrightnessGesture,
            volumeGestureSensitivity = volumeGestureSensitivity,
            brightnessGestureSensitivity = brightnessGestureSensitivity,
            coroutineScope = coroutineScope,
        )
    }
    return volumeAndBrightnessGestureState
}

@Stable
class VolumeAndBrightnessGestureState(
    private val volumeState: VolumeState,
    private val brightnessState: BrightnessState,
    private val enableVolumeGesture: Boolean = true,
    private val enableBrightnessGesture: Boolean = true,
    private val volumeGestureSensitivity: Float,
    private val brightnessGestureSensitivity: Float,
    private val coroutineScope: CoroutineScope,
) {
    var activeGesture: VerticalGesture? by mutableStateOf(value = null)
        private set

    var isDragging: Boolean by mutableStateOf(value = false)
        private set

    var volumeChangePercentage: Int by mutableIntStateOf(value = 0)
        private set

    var brightnessChangePercentage: Int by mutableIntStateOf(value = 0)
        private set

    private var startingY = 0f
    private var startVolumePercentage = 0
    private var startBrightnessPercentage = 0
    private var job: Job? = null

    fun onDragStart(offset: Offset, size: IntSize) {
        val viewCenterX = size.width / 2
        job?.cancel()
        val targetGesture = when {
            offset.x < viewCenterX -> VerticalGesture.BRIGHTNESS.takeIf { enableBrightnessGesture }
            else -> VerticalGesture.VOLUME.takeIf { enableVolumeGesture }
        }
        activeGesture = targetGesture
        isDragging = targetGesture != null
        startingY = offset.y
        startVolumePercentage = volumeState.volumePercentage
        startBrightnessPercentage = brightnessState.brightnessPercentage
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val activeGesture = activeGesture ?: return
        if (!isDragging) return

        when (activeGesture) {
            VerticalGesture.VOLUME -> {
                val maxVolumePercentage = volumeState.maxVolumePercentage
                val volumeChange = (startingY - change.position.y) * (volumeGestureSensitivity / 10f)
                val newVolume = (startVolumePercentage + volumeChange.roundToInt()).coerceIn(
                    minimumValue = 0,
                    maximumValue = maxVolumePercentage,
                )
                volumeChangePercentage = (newVolume - startVolumePercentage).coerceIn(
                    minimumValue = 0 - startVolumePercentage,
                    maximumValue = maxVolumePercentage - startVolumePercentage,
                )
                brightnessChangePercentage = 0
                volumeState.updateVolumePercentage(percentage = newVolume)
            }

            VerticalGesture.BRIGHTNESS -> {
                val brightnessChange =
                    (startingY - change.position.y) * (brightnessGestureSensitivity / 10f)
                val newBrightness = (startBrightnessPercentage + brightnessChange.roundToInt()).coerceIn(
                    minimumValue = 0,
                    maximumValue = MAX_BRIGHTNESS_PERCENTAGE,
                )
                brightnessChangePercentage = (newBrightness - startBrightnessPercentage).coerceIn(
                    minimumValue = 0 - startBrightnessPercentage,
                    maximumValue = MAX_BRIGHTNESS_PERCENTAGE - startBrightnessPercentage,
                )
                volumeChangePercentage = 0
                brightnessState.updateBrightnessPercentage(percentage = newBrightness)
            }
        }
    }

    fun onDragEnd() {
        isDragging = false
        startingY = 0f
        startVolumePercentage = 0
        startBrightnessPercentage = 0

        job?.cancel()
        job = coroutineScope.launch {
            delay(850.milliseconds)
            activeGesture = null
            volumeChangePercentage = 0
            brightnessChangePercentage = 0
        }
    }

    fun clearNow() {
        job?.cancel()
        isDragging = false
        activeGesture = null
        volumeChangePercentage = 0
        brightnessChangePercentage = 0
        startingY = 0f
    }

    companion object {
        private const val MAX_BRIGHTNESS_PERCENTAGE = 100
    }
}

enum class VerticalGesture {
    VOLUME,
    BRIGHTNESS,
}
