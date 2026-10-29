package dev.xboyrex.darkplayer.feature.player.ui.controls

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.model.VideoContentScale
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.buttons.LoopButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlaybackSpeedButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButtonBlackAlpha
import dev.xboyrex.darkplayer.feature.player.buttons.RotateButton
import dev.xboyrex.darkplayer.feature.player.buttons.ShuffleButton
import dev.xboyrex.darkplayer.feature.player.extensions.drawableRes
import dev.xboyrex.darkplayer.feature.player.state.ChaptersState
import dev.xboyrex.darkplayer.feature.player.state.rememberChaptersState
import dev.xboyrex.darkplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.xboyrex.darkplayer.feature.player.ui.titleOrDefault
import dev.xboyrex.darkplayer.feature.player.utils.VideoFrameCapture

private const val MILLISECONDS_PER_SECOND = 1_000L

@OptIn(UnstableApi::class)
@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player?,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    showRemainingTime: Boolean,
    onToggleTimeDisplay: () -> Unit,
    onChaptersClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onSleepTimerClick: () -> Unit = {},
    onVolumeBoosterClick: () -> Unit = {},
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val context = LocalContext.current
    val captureScope = rememberCoroutineScope()
    val isTv = remember { context.isTelevision }
    val timeButtonFocusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .padding(
                bottom =
                    16.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerButton(
                modifier = Modifier.focusRequester(focusRequester = timeButtonFocusRequester),
                onClick = onToggleTimeDisplay,
                containerColor = PlayerButtonBlackAlpha,
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 3.dp,
                ),
            ) {
                val timeTextPositionMs by remember(progressState) {
                    derivedStateOf {
                        val wholeSeconds = progressState.currentPositionMs / MILLISECONDS_PER_SECOND
                        wholeSeconds * MILLISECONDS_PER_SECOND
                    }
                }
                Text(
                    text = buildString {
                        val positionText = when (showRemainingTime) {
                            true -> if (progressState.durationMs != C.TIME_UNSET) {
                                val remainingMs = timeTextPositionMs - progressState.durationMs
                                getStringForTime(remainingMs)
                            } else {
                                getStringForTime(C.TIME_UNSET)
                            }

                            false -> getStringForTime(timeTextPositionMs)
                        }
                        append(positionText)
                        append(" / ")
                        append(getStringForTime(progressState.durationMs))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }

            if (chaptersState.chapters.isNotEmpty()) {
                PlayerButton(
                    onClick = onChaptersClick,
                    containerColor = PlayerButtonBlackAlpha,
                    contentPadding = PaddingValues(
                        vertical = 3.dp,
                        horizontal = 10.dp,
                    ).copy(end = 4.dp),
                ) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text =
                                chaptersState.chapters.getOrNull(chaptersState.currentChapterIndex)
                                    ?.titleOrDefault(chaptersState.currentChapterIndex)
                                    ?: stringResource(R.string.chapters),
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(weight = 1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackSpeedButton(
                    player = player,
                    onClick = onPlaybackSpeedClick,
                )

                if (!isTv) {
                    RotateButton()
                }
            }
        }
        PlayerSeekbar(
            position = progressState.currentPositionMs.toFloat(),
            duration = progressState.durationMs.toFloat(),
            chapters = chaptersState.chapters,
            onSeek = { onSeek(it.toLong()) },
            onSeekFinished = { onSeekEnd() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(state = rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                8.dp,
                alignment = controlsAlignment,
            ),
        ) {
            PlayerButton(onClick = onLockControlsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_open),
                    contentDescription = null,
                )
            }
            PlayerButton(
                onClick = onVideoContentScaleClick,
                onLongClick = onVideoContentScaleLongClick,
            ) {
                Icon(
                    painter = painterResource(videoContentScale.drawableRes()),
                    contentDescription = null,
                )
            }
            if (isPipSupported) {
                PlayerButton(onClick = onPictureInPictureClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pip),
                        contentDescription = null,
                    )
                }
            }
            PlayerButton(onClick = onPlayInBackgroundClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_headset),
                    contentDescription = null,
                )
            }
            PlayerButton(
                onClick = {
                    VideoFrameCapture.captureCurrentFrame(
                        context = context,
                        player = player,
                        scope = captureScope,
                    )
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_screenshot),
                    contentDescription = "Screenshot",
                )
            }
            PlayerButton(onClick = onSleepTimerClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = "Sleep timer",
                )
            }
            PlayerButton(
                onClick = onVolumeBoosterClick,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_equalizer),
                    contentDescription = "1000% Dolby Audio Booster",
                )
            }
            LoopButton(player = player)
            ShuffleButton(player = player)
        }
    }
}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsBottomViewPreview() {
    val player = rememberPreviewPlayer()
    val progressState = rememberProgressStateWithTickInterval(player = player)
    NextPlayerTheme(darkTheme = true) {
        Surface {
            ControlsBottomView(
                player = player,
                progressState = progressState,
                chaptersState = rememberChaptersState(
                    player = player,
                    progressState = progressState,
                ),
                controlsAlignment = Alignment.Start,
                videoContentScale = VideoContentScale.BEST_FIT,
                isPipSupported = true,
                showRemainingTime = false,
                onToggleTimeDisplay = {},
                onChaptersClick = {},
                onVideoContentScaleClick = {},
                onVideoContentScaleLongClick = {},
                onLockControlsClick = {},
                onPictureInPictureClick = {},
                onPlaybackSpeedClick = {},
                onPlayInBackgroundClick = {},
                onSeek = {},
                onSeekEnd = {},
            )
        }
    }
}
