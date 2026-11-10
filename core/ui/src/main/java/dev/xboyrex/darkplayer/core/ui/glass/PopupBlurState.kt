package dev.xboyrex.darkplayer.core.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

/**
 * Keeps parent containers crisp so child overlays (such as UpdateOverlayDialog inside
 * SettingsScreen or AboutPreferencesScreen) are never blurred by a parent RenderEffect.
 */
@Composable
fun Modifier.popupBackgroundBlur(
    @Suppress("UNUSED_PARAMETER") extraBlur: Boolean = false,
    @Suppress("UNUSED_PARAMETER") radius: Dp = 0.dp,
): Modifier = this
