package com.mayank.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.mayank.darkplayer.core.common.extensions.isTelevision
import com.mayank.darkplayer.core.model.VideoContentScale
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.extensions.copy
import com.mayank.darkplayer.core.ui.theme.DarkPlayerTheme
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.mayank.darkplayer.core.ui.theme.PlayerTimePillFill
import com.mayank.darkplayer.feature.player.buttons.LoopButton
import com.mayank.darkplayer.feature.player.buttons.PlaybackSpeedButton
import com.mayank.darkplayer.feature.player.buttons.PlayerButton
import com.mayank.darkplayer.feature.player.buttons.RotateButton
import com.mayank.darkplayer.feature.player.buttons.ShuffleButton
import com.mayank.darkplayer.feature.player.extensions.drawableRes
import com.mayank.darkplayer.feature.player.state.ChaptersState
import com.mayank.darkplayer.feature.player.state.rememberChaptersState
import com.mayank.darkplayer.feature.player.ui.preview.rememberPreviewPlayer
import com.mayank.darkplayer.feature.player.ui.titleOrDefault
import com.mayank.darkplayer.feature.player.utils.VideoScreenshotManager

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
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
    onScreenshotClick: (() -> Unit)? = null,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val timeButtonFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier = modifier) {
        val useCompactControls = maxWidth < 600.dp && !isTv
        Column(
            modifier = Modifier
                .padding(systemBarsPadding.copy(top = 0.dp))
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .padding(
                    bottom = 14.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 4.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Row 1: Floating time pill on left, Speed (1.0x) + Rotate pills on right
            Row(
                modifier = Modifier
                    .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                    .focusGroup(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.weight(weight = 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlayerButton(
                        modifier = Modifier.focusRequester(focusRequester = timeButtonFocusRequester),
                        onClick = onToggleTimeDisplay,
                        containerColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xCC121614) else androidx.compose.ui.graphics.Color(0xE8F4EFE8),
                        contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
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
                                        getStringForTime(timeTextPositionMs - progressState.durationMs)
                                    } else {
                                        getStringForTime(C.TIME_UNSET)
                                    }
                                    false -> getStringForTime(timeTextPositionMs)
                                }
                                append(positionText)
                                append(" / ")
                                append(getStringForTime(progressState.durationMs))
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }

                    if (chaptersState.chapters.isNotEmpty()) {
                        PlayerButton(
                            modifier = Modifier.weight(weight = 1f, fill = false),
                            onClick = onChaptersClick,
                            containerColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xCC121614) else androidx.compose.ui.graphics.Color(0xE8F4EFE8),
                            contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                            contentPadding = PaddingValues(vertical = 5.dp, horizontal = 10.dp).copy(end = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = chaptersState.chapters
                                        .getOrNull(chaptersState.currentChapterIndex)
                                        ?.titleOrDefault(chaptersState.currentChapterIndex)
                                        ?: stringResource(R.string.chapters),
                                    modifier = Modifier.weight(weight = 1f, fill = false),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                if (!useCompactControls) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
            }

            // Row 2: Sleek Seekbar
            PlayerSeekbar(
                position = progressState.currentPositionMs.toFloat(),
                duration = progressState.durationMs.toFloat(),
                chapters = chaptersState.chapters,
                onSeek = { onSeek(it.toLong()) },
                onSeekFinished = { onSeekEnd() },
            )

            // Row 3: Lock, PIP, Repeat, Audio/Headset, Shuffle, Resize (Crop/Fit), Screenshot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(state = rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    10.dp,
                    alignment = controlsAlignment,
                ),
            ) {
                PlayerButton(
                    onClick = onLockControlsClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_open),
                        contentDescription = null,
                    )
                }

                if (isPipSupported) {
                    PlayerButton(
                        onClick = onPictureInPictureClick,
                        containerColor = PlayerControlPillFill,
                        contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pip),
                            contentDescription = null,
                        )
                    }
                }

                LoopButton(player = player)

                PlayerButton(
                    onClick = onPlayInBackgroundClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headset),
                        contentDescription = null,
                    )
                }

                ShuffleButton(player = player)

                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                    containerColor = PlayerControlPillFill,
                    contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                ) {
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = null,
                    )
                }

                PlayerButton(
                    onClick = {
                        onScreenshotClick?.invoke()
                            ?: VideoScreenshotManager.captureCurrentFrame(context)
                    },
                    containerColor = PlayerControlPillFill,
                    contentColor = if (DarkPlayerUiPrefs.playerButtonsDark) androidx.compose.ui.graphics.Color(0xFFF4EFE8) else androidx.compose.ui.graphics.Color(0xFF161B17),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Screenshot",
                        modifier = Modifier.size(22.dp),
                    )
                }

                if (useCompactControls) {
                    PlaybackSpeedButton(
                        player = player,
                        onClick = onPlaybackSpeedClick,
                    )
                    RotateButton()
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Preview
@Composable
private fun ControlsBottomViewPreview() {
    val player = rememberPreviewPlayer()
    val progressState = rememberProgressStateWithTickInterval(player = player)
    DarkPlayerTheme(darkTheme = true) {
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
