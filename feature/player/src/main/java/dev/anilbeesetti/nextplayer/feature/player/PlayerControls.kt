package dev.anilbeesetti.nextplayer.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.model.ControlButtonsPosition
import dev.anilbeesetti.nextplayer.core.model.MxQuickShortcut
import dev.anilbeesetti.nextplayer.core.model.PlayerControlStyle
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.feature.player.extensions.nameRes
import dev.anilbeesetti.nextplayer.feature.player.state.ChaptersState
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.SeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.seekAmountFormatted
import dev.anilbeesetti.nextplayer.feature.player.state.seekToPositionFormated
import dev.anilbeesetti.nextplayer.feature.player.ui.InfoView
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsBottomView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsMiddleView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsTopView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

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
    isNightModeActive: Boolean = false,
    isMuted: Boolean = false,
    abRepeatLabel: String? = null,
    sleepTimerActive: Boolean = false,
    onShowOverlay: (OverlayView) -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onToggleTimeDisplay: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onShortcutClick: (MxQuickShortcut) -> Unit = {},
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            AnimatedVisibility(
                visible = controlsVisibilityState.controlsVisible &&
                    !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ControlsTopView(
                    title = title,
                    videoDecoderMode = videoDecoderMode,
                    showQuickShortcuts = playerPreferences.showQuickShortcuts,
                    enabledShortcuts = playerPreferences.enabledShortcuts,
                    showBatteryClock = playerPreferences.showBatteryClockInTitleBar,
                    isNightModeActive = isNightModeActive,
                    isMuted = isMuted,
                    isShuffleOn = player?.shuffleModeEnabled == true,
                    isLoopOn = (player?.repeatMode ?: Player.REPEAT_MODE_OFF) != Player.REPEAT_MODE_OFF,
                    abRepeatLabel = abRepeatLabel,
                    sleepTimerActive = sleepTimerActive,
                    playbackSpeed = player?.playbackParameters?.speed ?: 1.0f,
                    onDecoderClick = { onShowOverlay(OverlayView.DECODER_SELECTOR) },
                    onAudioClick = { onShowOverlay(OverlayView.AUDIO_SELECTOR) },
                    onSubtitleClick = { onShowOverlay(OverlayView.SUBTITLE_SELECTOR) },
                    onPlaylistClick = { onShowOverlay(OverlayView.PLAYLIST) },
                    onMoreMenuClick = { onShowOverlay(OverlayView.MORE_MENU) },
                    onShortcutClick = onShortcutClick,
                    onBackClick = onBackClick,
                )
            }
            Spacer(modifier = Modifier.weight(weight = 1f))
            AnimatedVisibility(
                visible =
                    controlsVisibilityState.controlsVisible &&
                        !controlsVisibilityState.controlsLocked,
                enter = fadeIn(),
                exit = fadeOut(),
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
                    playerControlStyle = playerPreferences.playerControlStyle,
                    showPrevNextButtons = playerPreferences.showPrevNextButtons,
                    showForwardBackwardButtons = playerPreferences.showForwardBackwardButtons,
                    showScreenRotationButton = playerPreferences.showScreenRotationButton,
                    seekIncrementSeconds = playerPreferences.seekIncrement,
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
        when {
            seekGestureState.seekAmount != null -> InfoView(
                info =
                    "${seekGestureState.seekAmountFormatted}\n" +
                        "[${seekGestureState.seekToPositionFormated}]",
            )

            videoZoomAndContentScaleState.isZooming -> InfoView(
                info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%",
            )

            videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(
                info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()),
            )

            controlsVisibilityState.controlsVisible &&
                !controlsVisibilityState.controlsLocked &&
                playerPreferences.playerControlStyle == PlayerControlStyle.CLASSIC_NEXT -> ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }
    }
}
