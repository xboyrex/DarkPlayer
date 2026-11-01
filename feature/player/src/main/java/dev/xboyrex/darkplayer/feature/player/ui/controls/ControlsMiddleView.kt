package dev.xboyrex.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.LocalControlsVisibilityState
import dev.xboyrex.darkplayer.feature.player.buttons.NextButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayPauseButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.buttons.PreviousButton
import dev.xboyrex.darkplayer.feature.player.ui.preview.rememberPreviewPlayer
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun ControlsMiddleView(
    modifier: Modifier = Modifier,
    player: Player?,
    onSeekBack: (() -> Unit)? = null,
    onSeekForward: (() -> Unit)? = null,
) {
    val playPauseFocusRequester = remember { FocusRequester() }
    val controlsVisibilityState = LocalControlsVisibilityState.current
    val hasMultipleItems = (player?.mediaItemCount ?: 0) > 1

    val defaultSeek: (Long) -> Unit = { deltaMs ->
        player?.let { p ->
            val maxDur = p.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: Long.MAX_VALUE
            val target = (p.currentPosition.coerceAtLeast(0L) + deltaMs).coerceIn(0L, maxDur)
            p.seekTo(target)
            controlsVisibilityState?.showControls()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { onEnter = { playPauseFocusRequester.requestFocus() } }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(
            if (hasMultipleItems) 22.dp else 36.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasMultipleItems) {
            PreviousButton(player = player)
        }
        SeekSkipButton(
            isForward = false,
            enabled = player != null,
            onClick = {
                if (onSeekBack != null) {
                    onSeekBack()
                    controlsVisibilityState?.showControls()
                } else {
                    defaultSeek(-10_000L)
                }
            },
        )
        PlayPauseButton(
            player = player,
            modifier = Modifier.focusRequester(focusRequester = playPauseFocusRequester),
        )
        SeekSkipButton(
            isForward = true,
            enabled = player != null,
            onClick = {
                if (onSeekForward != null) {
                    onSeekForward()
                    controlsVisibilityState?.showControls()
                } else {
                    defaultSeek(10_000L)
                }
            },
        )
        if (hasMultipleItems) {
            NextButton(player = player)
        }
    }
}

@Composable
private fun SeekSkipButton(
    isForward: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }

    PlayerButton(
        modifier = Modifier.size(50.dp),
        enabled = enabled,
        onClick = {
            scope.launch {
                rotationAnim.snapTo(if (isForward) 42f else -42f)
                rotationAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
                )
            }
            onClick()
        },
    ) {
        Icon(
            imageVector = if (isForward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10,
            contentDescription = if (isForward) "Forward 10 seconds" else "Back 10 seconds",
            modifier = Modifier
                .size(29.dp)
                .graphicsLayer {
                    rotationZ = rotationAnim.value
                },
        )
    }
}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsMiddleViewPreview() {
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsMiddleView(
                player = rememberPreviewPlayer(),
            )
        }
    }
}
