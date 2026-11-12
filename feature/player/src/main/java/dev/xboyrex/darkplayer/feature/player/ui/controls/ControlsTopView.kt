package dev.xboyrex.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
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
import dev.xboyrex.darkplayer.core.model.MxQuickShortcut
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.model.labelRes
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

/**
 * Clean v2.5.56 Player Top Bar (matching Screenshot_20261110-153919_Nova Player.png):
 * - Left: Circular Back button + Video Title
 * - Right: [HW] Decoder badge, Playlist button, Audio track button, Subtitle track button
 */
@OptIn(UnstableApi::class)
@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    videoDecoderMode: DecoderMode?,
    showQuickShortcuts: Boolean = false,
    enabledShortcuts: List<String> = emptyList(),
    showBatteryClock: Boolean = false,
    isNightModeActive: Boolean = false,
    isMuted: Boolean = false,
    isShuffleOn: Boolean = false,
    isLoopOn: Boolean = false,
    abRepeatLabel: String? = null,
    sleepTimerActive: Boolean = false,
    playbackSpeed: Float = 1.0f,
    onBackClick: () -> Unit = {},
    onDecoderClick: () -> Unit = {},
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {},
    onMoreMenuClick: () -> Unit = {},
    onShortcutClick: (MxQuickShortcut) -> Unit = {},
) {
    val firstControlFocusRequester = remember { FocusRequester() }
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val decoderDescription = stringResource(R.string.select_decoders)
    val extraTopPadding = if (systemBarsPadding.calculateTopPadding() == 0.dp) 14.dp else 4.dp
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(systemBarsPadding.copy(bottom = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = extraTopPadding, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { firstControlFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlayerButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineBreak = LineBreak.Heading,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .semantics { contentDescription = decoderDescription }
                        .focusRequester(focusRequester = firstControlFocusRequester)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF14161E).copy(alpha = 0.80f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                        .clickable {
                            view.novaHaptic(10L, 120, hapticsEnabled)
                            onDecoderClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource((videoDecoderMode ?: DecoderMode.HARDWARE).labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                    )
                }

                PlayerButton(onClick = onPlaylistClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_playlist),
                        contentDescription = "Playlist",
                        tint = Color.White,
                    )
                }
                PlayerButton(onClick = onAudioClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_audio_track),
                        contentDescription = "Audio Track",
                        tint = Color.White,
                    )
                }
                PlayerButton(onClick = onSubtitleClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_subtitle_track),
                        contentDescription = "Subtitles",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ControlsTopViewPreview() {
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsTopView(
                title = "The.Core.2003.1080p.BluRay.Hindi.English.DD.5.1.x264.ESubs",
                videoDecoderMode = DecoderMode.HARDWARE,
                onBackClick = {},
            )
        }
    }
}
