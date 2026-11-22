package com.mayank.darkplayer.feature.player.buttons

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import com.mayank.darkplayer.core.common.extensions.round
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon

/**
 * Playback speed pill (1.0x). Tap opens speed selector; long-press toggles
 * pitch-locked 2.0x speed immediately without audio distortion.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackSpeedButton(player: Player?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val state = rememberPlaybackSpeedState(player = player)
    val description = stringResource(R.string.select_playback_speed)
    PlayerButton(
        modifier = modifier.semantics { contentDescription = description },
        enabled = state.isEnabled,
        onClick = onClick,
        onLongClick = {
            player?.let { p ->
                val nextSpeed = if (p.playbackParameters.speed >= 1.95f) 1.0f else 2.0f
                p.playbackParameters = PlaybackParameters(nextSpeed, 1.0f)
            }
        },
        containerColor = PlayerControlPillFill,
        contentColor = PlayerControlPillIcon,
        contentPadding = PaddingValues(6.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = state.playbackSpeed.round(decimalPlaces = 1).toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = LocalContentColor.current,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                    minFontSize = 7.sp,
                    stepSize = 0.5.sp,
                ),
            )
        }
    }
}
