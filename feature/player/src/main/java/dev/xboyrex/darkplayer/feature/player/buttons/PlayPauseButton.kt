package dev.xboyrex.darkplayer.feature.player.buttons

import androidx.annotation.OptIn
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
    val icon = when (state.showPlay) {
        true -> painterResource(coreUiR.drawable.ic_play)
        false -> painterResource(coreUiR.drawable.ic_pause)
    }
    val contentDescription = stringResource(coreUiR.string.play_pause)

    PlayerButton(
        modifier = modifier.size(64.dp),
        enabled = state.isEnabled,
        contentPadding = PaddingValues(0.dp),
        onClick = state::onClick,
    ) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
        )
    }
}
