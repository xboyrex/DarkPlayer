package com.mayank.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.core.ui.theme.DarkPlayerTheme
import com.mayank.darkplayer.core.ui.theme.NavyTextPrimary
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon
import com.mayank.darkplayer.feature.player.buttons.PlayerButton
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.model.labelRes
import com.mayank.darkplayer.feature.player.utils.HdrSupportManager

@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    onBackClick: () -> Unit = {},
    onDecoderClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(context) { HdrSupportManager.init(context); HdrSupportManager.applyToWindow(context) }
    val hdrMode by HdrSupportManager.mode.collectAsState()
    val firstControlFocusRequester = remember { FocusRequester() }
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() == 0.dp) 14.dp else 4.dp

    Row(
        modifier = modifier
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .padding(top = extraTopPadding)
            .focusProperties { onEnter = { firstControlFocusRequester.requestFocus() } }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerButton(
            onClick = onBackClick,
            containerColor = PlayerControlPillFill,
            contentColor = PlayerControlPillIcon,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.navigate_up),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                lineBreak = LineBreak.Heading,
                fontWeight = FontWeight.SemiBold,
            ),
            color = NavyTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerButton(
                modifier = Modifier
                    .semantics { contentDescription = decoderDescription }
                    .focusRequester(focusRequester = firstControlFocusRequester),
                onClick = onDecoderClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }

            PlayerButton(
                onClick = { HdrSupportManager.toggleHdrSdr(context) },
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = hdrMode.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }

            PlayerButton(
                onClick = onPlaylistClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_playlist),
                    contentDescription = null,
                )
            }

            PlayerButton(
                onClick = onAudioClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_audio_track),
                    contentDescription = null,
                )
            }

            PlayerButton(
                onClick = onSubtitleClick,
                containerColor = PlayerControlPillFill,
                contentColor = PlayerControlPillIcon,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_subtitle_track),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ControlsTopViewPreview() {
    DarkPlayerTheme(darkTheme = true) {
        Surface {
            ControlsTopView(
                title = "Dark Player Cinema Preview 1080p HDR",
                videoDecoderMode = DecoderMode.HARDWARE,
            )
        }
    }
}
