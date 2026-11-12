package dev.xboyrex.darkplayer.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.xboyrex.darkplayer.core.ui.R as coreUiR

@OptIn(UnstableApi::class)
@Composable
fun PlayPauseButton(modifier: Modifier = Modifier, player: Player?) {
    val state = rememberPlayPauseButtonState(player = player)
    val contentDescription = stringResource(coreUiR.string.play_pause)

    PlayerButton(
        modifier = modifier.size(60.dp),
        enabled = state.isEnabled,
        isPrimaryAction = true,
        contentPadding = PaddingValues(0.dp),
        onClick = state::onClick,
    ) {
        AnimatedContent(
            targetState = state.showPlay,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.65f, stiffness = 600f), initialScale = 0.6f) + fadeIn(tween(120))) togetherWith
                    (scaleOut(tween(100), targetScale = 0.6f) + fadeOut(tween(100)))
            },
            label = "playPauseMorph",
        ) { showPlay ->
            Icon(
                painter = painterResource(
                    if (showPlay) coreUiR.drawable.ic_play else coreUiR.drawable.ic_pause,
                ),
                contentDescription = contentDescription,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
