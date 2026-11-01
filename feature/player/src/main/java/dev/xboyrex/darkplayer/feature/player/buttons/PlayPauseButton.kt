package dev.xboyrex.darkplayer.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.core.ui.glass.rememberReducedMotion

@OptIn(UnstableApi::class)
@Composable
fun PlayPauseButton(modifier: Modifier = Modifier, player: Player?) {
    val state = rememberPlayPauseButtonState(player = player)
    val isPlaying = !state.showPlay
    val reduced = rememberReducedMotion()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 34.dp,
        animationSpec = if (reduced) tween(0) else spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "morphPlayShape",
    )
    val icon = when (state.showPlay) {
        true -> painterResource(coreUiR.drawable.ic_play)
        false -> painterResource(coreUiR.drawable.ic_pause)
    }
    val contentDescription = stringResource(coreUiR.string.play_pause)

    PlayerButton(
        modifier = modifier.size(68.dp),
        enabled = state.isEnabled,
        shape = RoundedCornerShape(cornerRadius),
        onClick = state::onClick,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(44.dp),
        )
    }
}
