package com.mayank.darkplayer.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import com.mayank.darkplayer.core.model.ControlButtonsPosition
import com.mayank.darkplayer.core.model.PlayerPreferences
import com.mayank.darkplayer.feature.player.extensions.nameRes
import com.mayank.darkplayer.feature.player.model.DecoderMode
import com.mayank.darkplayer.feature.player.state.ChaptersState
import com.mayank.darkplayer.feature.player.state.ControlsVisibilityState
import com.mayank.darkplayer.feature.player.state.SeekGestureState
import com.mayank.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import com.mayank.darkplayer.feature.player.state.seekAmountFormatted
import com.mayank.darkplayer.feature.player.state.seekToPositionFormated
import com.mayank.darkplayer.feature.player.ui.DarkPlayerProEffects
import com.mayank.darkplayer.feature.player.ui.DarkPlayerSleepTimerDialog
import com.mayank.darkplayer.feature.player.ui.InfoView
import com.mayank.darkplayer.feature.player.ui.OverlayView
import com.mayank.darkplayer.feature.player.ui.ProQuickToolsStrip
import com.mayank.darkplayer.feature.player.ui.rememberDarkPlayerProState
import com.mayank.darkplayer.feature.player.ui.controls.ControlsBottomView
import com.mayank.darkplayer.feature.player.ui.controls.ControlsMiddleView
import com.mayank.darkplayer.feature.player.ui.controls.ControlsTopView

@OptIn(UnstableApi::class)
@Composable
fun PlayerControls(
    player: Player?,
    title: String,
    playerPreferences: PlayerPreferences,
    videoDecoderMode: DecoderMode?,
    controlsVisibilityState: ControlsVisibilityState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    progressState: ProgressStateWithTickInterval,
    chaptersState: ChaptersState,
    isPipSupported: Boolean,
    onShowOverlay: (OverlayView) -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onToggleTimeDisplay: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {
    val proState = rememberDarkPlayerProState()
    DarkPlayerProEffects(player = player, proState = proState)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (proState.nightModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5A120A04)),
            )
        }

        Column {
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 110)),
                exit = fadeOut(animationSpec = tween(durationMillis = 75)),
            ) {
                Column {
                    ControlsTopView(
                        title = title,
                        videoDecoderMode = videoDecoderMode,
                        onDecoderClick = { onShowOverlay(OverlayView.DECODER_SELECTOR) },
                        onAudioClick = { onShowOverlay(OverlayView.AUDIO_SELECTOR) },
                        onSubtitleClick = { onShowOverlay(OverlayView.SUBTITLE_SELECTOR) },
                        onPlaylistClick = { onShowOverlay(OverlayView.PLAYLIST) },
                        onBackClick = onBackClick,
                    )
                    if (!controlsVisibilityState.controlsLocked) {
                        ProQuickToolsStrip(player = player, proState = proState)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible &&
                    !controlsVisibilityState.controlsLocked,
                enter = fadeIn(animationSpec = tween(durationMillis = 110)),
                exit = fadeOut(animationSpec = tween(durationMillis = 75)),
            ) {
                ControlsBottomView(
                    player = player,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    onChaptersClick = { onShowOverlay(OverlayView.CHAPTERS) },
                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                        ControlButtonsPosition.LEFT -> Alignment.Start
                        ControlButtonsPosition.RIGHT -> Alignment.End
                    },
                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                    isPipSupported = isPipSupported,
                    showRemainingTime = playerPreferences.showRemainingTime,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onLockControlsClick = {
                        controlsVisibilityState.showControls()
                        controlsVisibilityState.lockControls()
                    },
                    onVideoContentScaleClick = {
                        controlsVisibilityState.showControls()
                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                    },
                    onVideoContentScaleLongClick = {
                        onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE)
                    },
                    onPictureInPictureClick = onPictureInPictureClick,
                )
            }
        }

        // Hide center Play/Pause briefly while Crop/Best Fit/Stretch or Seek/Zoom text is shown
        if (controlsVisibilityState.controlsVisible &&
            seekGestureState.seekAmount == null &&
            !videoZoomAndContentScaleState.isZooming &&
            !videoZoomAndContentScaleState.showContentScaleIndicator
        ) {
            ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }

        // Center status overlays for Seek, Zoom, and Crop / Best Fit / Stretch / 100%
        when {
            seekGestureState.seekAmount != null -> InfoView(
                info = "${seekGestureState.seekAmountFormatted}\n[${seekGestureState.seekToPositionFormated}]",
            )

            videoZoomAndContentScaleState.isZooming -> InfoView(
                info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%",
            )

            videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(
                modifier = Modifier.align(Alignment.Center),
                info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()),
            )
        }

        DarkPlayerSleepTimerDialog(proState = proState)
    }
}
