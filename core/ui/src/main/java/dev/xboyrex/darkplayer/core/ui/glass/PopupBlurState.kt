package dev.xboyrex.darkplayer.core.ui.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object PopupBlurState {
    var activePopupCount by mutableIntStateOf(0)
        private set

    val isBlurred: Boolean
        get() = activePopupCount > 0

    fun acquire() {
        activePopupCount = (activePopupCount + 1).coerceAtLeast(1)
    }

    fun release() {
        activePopupCount = (activePopupCount - 1).coerceAtLeast(0)
    }
}

@Composable
fun RegisterPopupBlur(active: Boolean = true) {
    DisposableEffect(active) {
        if (active) {
            PopupBlurState.acquire()
        }
        onDispose {
            if (active) {
                PopupBlurState.release()
            }
        }
    }
}

@Composable
fun Modifier.popupBackgroundBlur(
    extraBlur: Boolean = false,
    radius: Dp = 18.dp,
): Modifier {
    val shouldBlur = PopupBlurState.isBlurred || extraBlur
    val animatedRadius by animateDpAsState(
        targetValue = if (shouldBlur) radius else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "popupBlurRadius",
    )
    return if (animatedRadius > 0.1.dp) {
        this.blur(radius = animatedRadius)
    } else {
        this
    }
}
