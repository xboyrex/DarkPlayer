package dev.anilbeesetti.nextplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.model.PlayerControlStyle
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.LoopButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.NextButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayPauseButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlaybackSpeedButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PreviousButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.RotateButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.ShuffleButton
import dev.anilbeesetti.nextplayer.feature.player.extensions.drawableRes
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberChaptersState
import dev.anilbeesetti.nextplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.anilbeesetti.nextplayer.feature.player.ui.titleOrDefault

private const val MILLISECONDS_PER_SECOND = 1_000L

/**
 * Redesigned Player Bottom Controls & Seekbar Row
 * Fixes Bug #16 (`153919_Nova Player.png`) and implements MX Player Pro (#18, #19, #20):
 * - Seekbar Row: Elapsed time on the LEFT (`08:11`), horizontal seekbar in the center, and Remaining/Total time on the RIGHT (`-2:06:47`)
 * - Bottom Action Row (`MX_PRO` & `LIQUID_GLASS`):
 *   - Left: Lock button + Screen Rotation button
 *   - Center: Previous (`|<`), optional `-10s`, Play/Pause (`▶ / ||`), optional `+10s`, Next (`>|`)
 *   - Right: Picture-in-Picture (PiP) button + Aspect Ratio / Fit / Crop button
 */
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
    playerControlStyle: PlayerControlStyle = PlayerControlStyle.MX_PRO,
    showPrevNextButtons: Boolean = true,
    showForwardBackwardButtons: Boolean = false,
    showScreenRotationButton: Boolean = true,
    seekIncrementSeconds: Int = 10,
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
) {
    val systemBarsPadding = WindowInsets.systemBars.union(
        WindowInsets.displayCutout,
    ).asPaddingValues()
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val timeButtonFocusRequester = remember { FocusRequester() }

    val timeTextPositionMs by remember(progressState) {
        derivedStateOf {
            val wholeSeconds = progressState.currentPositionMs / MILLISECONDS_PER_SECOND
            wholeSeconds * MILLISECONDS_PER_SECOND
        }
    }
    val elapsedText = remember(timeTextPositionMs) {
        getStringForTime(timeTextPositionMs)
    }
    val rightTimeText = remember(timeTextPositionMs, progressState.durationMs, showRemainingTime) {
        if (progressState.durationMs != C.TIME_UNSET && progressState.durationMs > 0) {
            if (showRemainingTime) {
                val remainingMs = (progressState.durationMs - timeTextPositionMs).coerceAtLeast(0L)
                "-${getStringForTime(remainingMs)}"
            } else {
                getStringForTime(progressState.durationMs)
            }
        } else {
            getStringForTime(C.TIME_UNSET)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.80f),
                    ),
                ),
            )
            .padding(systemBarsPadding.copy(top = 0.dp))
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .padding(
                bottom = 12.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 4.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Optional Chapter Chip above seekbar
        if (chaptersState.chapters.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onChaptersClick)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = chaptersState.chapters.getOrNull(chaptersState.currentChapterIndex)
                                ?.titleOrDefault(chaptersState.currentChapterIndex)
                                ?: stringResource(R.string.chapters),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        // 1. MX Player Pro Seekbar Row: [08:11]  =======●----------------  [-2:06:47]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = elapsedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .focusRequester(timeButtonFocusRequester)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleTimeDisplay)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )

            PlayerSeekbar(
                modifier = Modifier.weight(1f),
                position = progressState.currentPositionMs.toFloat(),
                duration = progressState.durationMs.toFloat(),
                chapters = chaptersState.chapters,
                onSeek = { onSeek(it.toLong()) },
                onSeekFinished = { onSeekEnd() },
            )

            Text(
                text = rightTimeText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleTimeDisplay)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }

        // 2. Bottom Controls Bar
        if (playerControlStyle == PlayerControlStyle.MX_PRO ||
            playerControlStyle == PlayerControlStyle.LIQUID_GLASS
        ) {
            // Exact MX Player Pro Bottom Bar Layout (#18, #19, #20):
            // Left: Lock (+ Rotate) | Center: Prev, (-10s), Play/Pause, (+10s), Next | Right: PiP, Aspect Ratio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left Group: Lock & Screen Rotation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    IconButton(onClick = onLockControlsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock_open),
                            contentDescription = "Lock Controls",
                            tint = Color.White,
                        )
                    }
                    if (!isTv && showScreenRotationButton) {
                        RotateButton()
                    }
                }

                // Center Group: Playback Transport Controls (Prev / -10s / Play-Pause / +10s / Next)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (showPrevNextButtons) {
                        PreviousButton(player = player)
                    }
                    if (showForwardBackwardButtons && player != null) {
                        IconButton(
                            onClick = {
                                val target = (player.currentPosition - seekIncrementSeconds * 1000L).coerceAtLeast(0L)
                                player.seekTo(target)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay10,
                                contentDescription = "Rewind",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    PlayPauseButton(player = player)

                    if (showForwardBackwardButtons && player != null) {
                        IconButton(
                            onClick = {
                                val dur = player.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
                                val target = (player.currentPosition + seekIncrementSeconds * 1000L).coerceAtMost(dur)
                                player.seekTo(target)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Fast Forward",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    if (showPrevNextButtons) {
                        NextButton(player = player)
                    }
                }

                // Right Group: PiP & Aspect Ratio / Screen Fit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isPipSupported) {
                        IconButton(onClick = onPictureInPictureClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_pip),
                                contentDescription = "Picture in Picture",
                                tint = Color.White,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onVideoContentScaleClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(videoContentScale.drawableRes()),
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                        )
                    }
                }
            }
        } else {
            // Classic NextPlayer Bottom Row
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
                LoopButton(player = player)
                ShuffleButton(player = player)
                Spacer(modifier = Modifier.width(8.dp))
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
                showRemainingTime = true,
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
