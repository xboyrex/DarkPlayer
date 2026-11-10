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

    private var startVolumePercentage = 0
    private var startBrightnessPercentage = 0
    private var currentVolumeFloat = 0f
    private var currentBrightnessFloat = 0f
    private var viewHeightPx = 1080f
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
        viewHeightPx = size.height.coerceAtLeast(300).toFloat()
        volumeState.syncWithSystem()
        startVolumePercentage = volumeState.volumePercentage
        startBrightnessPercentage = brightnessState.brightnessPercentage
        currentVolumeFloat = startVolumePercentage.toFloat()
        currentBrightnessFloat = startBrightnessPercentage.toFloat()
    }

    fun onDrag(change: PointerInputChange, dragAmount: Float) {
        val gesture = activeGesture ?: return
        if (change.isConsumed) return

        // Normalized full-swipe sensitivity so dragging across ~55% of screen height spans 0..100%
        val baseMultiplier = 180f / viewHeightPx

        when (gesture) {
            VerticalGesture.VOLUME -> {
                val maxVolumePercentage = volumeState.maxVolumePercentage
                val sens = (0.6f + volumeGestureSensitivity * 1.4f).coerceIn(0.4f, 2.5f)
                val deltaPct = -dragAmount * baseMultiplier * sens
                currentVolumeFloat = (currentVolumeFloat + deltaPct).coerceIn(0f, maxVolumePercentage.toFloat())
                val newVolume = currentVolumeFloat.roundToInt()
                volumeChangePercentage = (newVolume - startVolumePercentage).coerceIn(
                    minimumValue = -startVolumePercentage,
                    maximumValue = maxVolumePercentage - startVolumePercentage,
                )
                brightnessChangePercentage = 0
                volumeState.updateVolumePercentage(percentage = newVolume)
            }

            VerticalGesture.BRIGHTNESS -> {
                val sens = (0.6f + brightnessGestureSensitivity * 1.4f).coerceIn(0.4f, 2.5f)
                val deltaPct = -dragAmount * baseMultiplier * sens
                currentBrightnessFloat = (currentBrightnessFloat + deltaPct).coerceIn(0f, MAX_BRIGHTNESS_PERCENTAGE.toFloat())
                val newBrightness = currentBrightnessFloat.roundToInt()
                brightnessChangePercentage = (newBrightness - startBrightnessPercentage).coerceIn(
                    minimumValue = -startBrightnessPercentage,
                    maximumValue = MAX_BRIGHTNESS_PERCENTAGE - startBrightnessPercentage,
                )
                volumeChangePercentage = 0
                brightnessState.updateBrightnessPercentage(percentage = newBrightness)
            }
        }
    }

    fun onDragEnd() {
        isDragging = false
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

    companion object {
        private const val MAX_BRIGHTNESS_PERCENTAGE = 100
    }
}

enum class VerticalGesture {
    VOLUME,
    BRIGHTNESS,
}
