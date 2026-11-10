package dev.xboyrex.darkplayer.feature.player.ui.controls

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.xboyrex.darkplayer.core.model.VideoContentScale
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.extensions.copy
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.buttons.NextButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayPauseButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButton
import dev.xboyrex.darkplayer.feature.player.buttons.PlayerButtonBlackAlpha
import dev.xboyrex.darkplayer.feature.player.buttons.PreviousButton
import dev.xboyrex.darkplayer.feature.player.buttons.RotateButton
import dev.xboyrex.darkplayer.feature.player.extensions.drawableRes
import dev.xboyrex.darkplayer.feature.player.state.ChaptersState
import dev.xboyrex.darkplayer.feature.player.state.rememberChaptersState
import dev.xboyrex.darkplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.xboyrex.darkplayer.feature.player.ui.titleOrDefault

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
    val elapsedText = getStringForTime(timeTextPositionMs)
    val rightTimeText = if (progressState.durationMs != C.TIME_UNSET && progressState.durationMs > 0) {
        if (showRemainingTime) {
            val remainingMs = (progressState.durationMs - timeTextPositionMs).coerceAtLeast(0L)
            "-${getStringForTime(remainingMs)}"
        } else {
            "-${getStringForTime((progressState.durationMs - timeTextPositionMs).coerceAtLeast(0L))}"
        }
    } else {
        getStringForTime(C.TIME_UNSET)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
                bottom = 14.dp.takeIf { systemBarsPadding.calculateBottomPadding() == 0.dp } ?: 6.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Optional Chapter indicator pill
        if (chaptersState.chapters.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                PlayerButton(
                    onClick = onChaptersClick,
                    containerColor = PlayerButtonBlackAlpha,
                    contentPadding = PaddingValues(vertical = 3.dp, horizontal = 10.dp).copy(end = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = chaptersState.chapters.getOrNull(chaptersState.currentChapterIndex)
                                ?.titleOrDefault(chaptersState.currentChapterIndex)
                                ?: stringResource(R.string.chapters),
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
        }

        // Row 1: MX Player Pro Seekbar Row (Elapsed Time — Seekbar — Remaining Time)
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
                    .clickable(onClick = onToggleTimeDisplay),
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
                modifier = Modifier.clickable(onClick = onToggleTimeDisplay),
            )
        }

        // Row 2: MX Player Pro Bottom Transport & Action Bar (155632, 155634, 155640)
        // Left: Lock + Rotate | Center: Previous, Play/Pause, Next | Right: PiP + Aspect Ratio
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left group: Lock & Rotate
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlayerButton(onClick = onLockControlsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock_open),
                        contentDescription = "Lock controls",
                        tint = Color.White,
                    )
                }
                if (!isTv) {
                    RotateButton()
                }
            }

            // Center group: Previous |< , Play/Pause ▶ , Next >|
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (player != null) {
                    PreviousButton(player = player)
                    PlayPauseButton(player = player)
                    NextButton(player = player)
                }
            }

            // Right group: PiP & Aspect Ratio / Crop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isPipSupported) {
                    PlayerButton(onClick = onPictureInPictureClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pip),
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                        )
                    }
                }
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(
                        painter = painterResource(videoContentScale.drawableRes()),
                        contentDescription = "Aspect Ratio",
                        tint = Color.White,
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
