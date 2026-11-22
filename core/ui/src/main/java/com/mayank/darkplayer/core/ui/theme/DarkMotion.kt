package com.mayank.darkplayer.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DarkSpringFast = spring<Float>(
    dampingRatio = 0.74f,
    stiffness = 560f,
)

val DarkSpringGentle = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = 420f,
)

/**
 * Non-consuming tactile press-scale spring animation for buttons, cards, and tiles.
 */
fun Modifier.calmPressScale(
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = DarkSpringFast,
        label = "calm_press_scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

/**
 * Subtle staggered fade + rise + micro-scale entrance for screen sections and cards.
 */
fun Modifier.calmEntrance(
    delayMillis: Int = 0,
    offsetY: Float = 26f,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = DarkSpringGentle,
        )
    }
    this.graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * offsetY
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
    }
}
