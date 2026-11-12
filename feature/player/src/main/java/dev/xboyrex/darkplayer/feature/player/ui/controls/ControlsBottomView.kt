package dev.xboyrex.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewCarousel
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
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.model.PlayerControlStyle
import dev.xboyrex.darkplayer.core.model.VideoContentScale
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.buttons.LoopButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlaybackSpeedButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.buttons.RotateButton
import dev.xboyrex.darkplayer.feature.player.buttons.ShuffleButton
import dev.xboyrex.darkplayer.feature.player.extensions.drawableRes
import dev.xboyrex.darkplayer.feature.player.state.ChaptersState
import dev.xboyrex.darkplayer.feature.player.state.rememberChaptersState
import dev.xboyrex.darkplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.xboyrex.darkplayer.feature.player.ui.titleOrDefault

private const val MILLISECONDS_PER_SECOND = 1_000L

/**
 * Clean v2.5.56 Player Bottom View (exact match to Screenshot_20261110-153919_Nova Player.png):
 * - Above Seekbar:
 *   - Left: Dark rounded Time Pill (`-3:21:15 / 4:00:35` or `09:26 / 42:48`)
 *   - Right: Playback Speed pill (`1.0`) + Rotate button
 * - Seekbar: Full-width seekbar across the screen
 * - Bottom Row:
 *   - Single clean row of circular action buttons (NO duplicate Play/Pause/Prev/Next!):
 *     [Lock] [UI Switcher Popup] [Aspect Ratio] [Background Audio] [Screenshot] [Sleep Timer] [Equalizer] [Shuffle] [Loop] [PiP]
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
    playerControlStyle: PlayerControlStyle = PlayerControlStyle.CLASSIC_NEXT,
    showPrevNextButtons: Boolean = true,
    showForwardBackwardButtons: Boolean = false,
    showScreenRotationButton: Boolean = true,
    seekIncrementSeconds: Int = 10,
    enabledPlayerButtons: Set<String> = setOf("UI_SWITCHER", "SCALE", "BG_PLAY", "SCREENSHOT", "TIMER", "EQUALIZER", "SHUFFLE", "LOOP", "PIP"),
    onToggleTimeDisplay: () -> Unit,
    onChaptersClick: () -> Unit,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onOpenUiSwitcherPopup: () -> Unit = {},
    onScreenshotClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
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
    val timePillText = remember(timeTextPositionMs, progressState.durationMs, showRemainingTime) {
        val elapsed = getStringForTime(timeTextPositionMs)
        if (progressState.durationMs != C.TIME_UNSET && progressState.durationMs > 0) {
            val total = getStringForTime(progressState.durationMs)
            if (showRemainingTime) {
                val remainingMs = (progressState.durationMs - timeTextPositionMs).coerceAtLeast(0L)
                "-${getStringForTime(remainingMs)} / $total"
            } else {
                "$elapsed / $total"
            }
        } else {
            elapsed
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 1. Row above seekbar: Left = Time Pill (+ optional Chapter chip), Right = Speed [1.0] + Rotate button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { onEnter = { timeButtonFocusRequester.requestFocus() } }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .focusRequester(timeButtonFocusRequester)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14161E).copy(alpha = 0.82f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggleTimeDisplay)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = timePillText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                if (chaptersState.chapters.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF14161E).copy(alpha = 0.78f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onChaptersClick)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaybackSpeedButton(
                    player = player,
                    onClick = onPlaybackSpeedClick,
                )
                if (!isTv && showScreenRotationButton) {
                    RotateButton()
                }
            }
        }

        // 2. Full-width Seekbar
        PlayerSeekbar(
            modifier = Modifier.fillMaxWidth(),
            position = progressState.currentPositionMs.toFloat(),
            duration = progressState.durationMs.toFloat(),
            chapters = chaptersState.chapters,
            onSeek = { onSeek(it.toLong()) },
            onSeekFinished = { onSeekEnd() },
        )

        // 3. v2.5.56 Bottom Action Row (#L5: 20261110-153919)
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
            // 1. Lock Controls
            PlayerButton(onClick = onLockControlsClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_open),
                    contentDescription = "Lock Controls",
                )
            }

            // 2. UI Switcher Popup Button (Circled at bottom-left of #L5: 20261110-153919!)
            if ("UI_SWITCHER" in enabledPlayerButtons) {
                PlayerButton(onClick = onOpenUiSwitcherPopup) {
                    Icon(
                        imageVector = Icons.Rounded.ViewCarousel,
                        contentDescription = "Player UI Switcher",
                    )
                }
            }

            // 3. Aspect Ratio / Video Content Scale
            if ("SCALE" in enabledPlayerButtons) {
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = "Aspect Ratio",
                    )
                }
            }

            // 4. Background Play (Headset)
            if ("BG_PLAY" in enabledPlayerButtons) {
                PlayerButton(onClick = onPlayInBackgroundClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headset),
                        contentDescription = "Background Play",
                    )
                }
            }

            // 5. Screenshot (Camera)
            if ("SCREENSHOT" in enabledPlayerButtons) {
                PlayerButton(onClick = onScreenshotClick) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Capture Screenshot",
                    )
                }
            }

            // 6. Sleep Timer
            if ("TIMER" in enabledPlayerButtons) {
                PlayerButton(onClick = onSleepTimerClick) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Sleep Timer",
                    )
                }
            }

            // 7. Equalizer
            if ("EQUALIZER" in enabledPlayerButtons) {
                PlayerButton(onClick = onEqualizerClick) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Equalizer",
                    )
                }
            }

            // 8. Shuffle
            if ("SHUFFLE" in enabledPlayerButtons) {
                ShuffleButton(player = player)
            }

            // 9. Loop
            if ("LOOP" in enabledPlayerButtons) {
                LoopButton(player = player)
            }

            // 10. Picture in Picture (PiP)
            if (isPipSupported && "PIP" in enabledPlayerButtons) {
                Spacer(modifier = Modifier.weight(1f, fill = false))
                PlayerButton(onClick = onPictureInPictureClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pip),
                        contentDescription = "Picture in Picture",
                    )
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
