package dev.xboyrex.darkplayer.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import dev.xboyrex.darkplayer.core.model.ControlButtonsPosition
import dev.xboyrex.darkplayer.core.model.MxQuickShortcut
import dev.xboyrex.darkplayer.core.model.PlayerControlStyle
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.VolumeBrightnessBarStyle
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoBlue
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoGradientBrush
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoPink
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoViolet
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaSegmentedControl
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.xboyrex.darkplayer.core.ui.glass.novaHaptic
import dev.xboyrex.darkplayer.feature.player.extensions.nameRes
import dev.xboyrex.darkplayer.feature.player.state.ChaptersState
import dev.xboyrex.darkplayer.feature.player.state.ControlsVisibilityState
import dev.xboyrex.darkplayer.feature.player.state.SeekGestureState
import dev.xboyrex.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.xboyrex.darkplayer.feature.player.state.seekAmountFormatted
import dev.xboyrex.darkplayer.feature.player.state.seekToPositionFormated
import dev.xboyrex.darkplayer.feature.player.ui.InfoView
import dev.xboyrex.darkplayer.feature.player.ui.OverlayView
import dev.xboyrex.darkplayer.feature.player.ui.controls.ControlsBottomView
import dev.xboyrex.darkplayer.feature.player.ui.controls.ControlsMiddleView
import dev.xboyrex.darkplayer.feature.player.ui.controls.ControlsTopView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)
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
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit = {},
    modifier: Modifier = Modifier,
    middleControlsModifier: Modifier = Modifier,
) {
    var showPlayerUiPopup by rememberSaveable { mutableStateOf(false) }
    val enabledPlayerButtons = remember(playerPreferences.enabledPlayerButtons) {
        playerPreferences.enabledPlayerButtons.toSet()
    }

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
                    showQuickShortcuts = false,
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
                    enabledPlayerButtons = enabledPlayerButtons,
                    onToggleTimeDisplay = onToggleTimeDisplay,
                    onSeek = seekGestureState::onSeek,
                    onSeekEnd = seekGestureState::onSeekEnd,
                    onPlaybackSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                    onOpenUiSwitcherPopup = { showPlayerUiPopup = true },
                    onScreenshotClick = { onShortcutClick(MxQuickShortcut.SCREENSHOT) },
                    onSleepTimerClick = { onShowOverlay(OverlayView.SLEEP_TIMER) },
                    onEqualizerClick = { onShowOverlay(OverlayView.EQUALIZER) },
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
                !controlsVisibilityState.controlsLocked -> ControlsMiddleView(
                player = player,
                modifier = middleControlsModifier,
            )
        }
    }

    // Blurred Popup for Player UI Switcher & Action Button Customization
    if (showPlayerUiPopup) {
        val view = LocalView.current
        val hapticsEnabled = LocalNovaHapticsEnabled.current
        val accent = LocalNovaAccentColor.current
        val buttonChoices = listOf(
            "SCALE" to "Aspect Ratio",
            "BG_PLAY" to "Background Play",
            "SCREENSHOT" to "Screenshot",
            "TIMER" to "Sleep Timer",
            "EQUALIZER" to "Equalizer",
            "SHUFFLE" to "Shuffle",
            "LOOP" to "Loop",
            "PIP" to "Picture-in-Picture",
        )

        Dialog(
            onDismissRequest = { showPlayerUiPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0A0B10).copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            NovaLogoBlue.copy(alpha = 0.55f),
                            NovaLogoViolet.copy(alpha = 0.55f),
                            NovaLogoPink.copy(alpha = 0.45f),
                        ),
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .padding(vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NovaLogoGradientBrush),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ViewCarousel,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Player UI & Action Buttons",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Select player buttons, HUD style & alignment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                        IconButton(
                            onClick = { showPlayerUiPopup = false },
                            modifier = Modifier.size(30.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                    Text(
                        text = "Bottom Bar Action Buttons",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        buttonChoices.forEach { (key, label) ->
                            val isSelected = key in enabledPlayerButtons
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) accent else Color.White.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        view.novaHaptic(10L, 125, hapticsEnabled)
                                        val nextSet = if (isSelected) {
                                            enabledPlayerButtons - key
                                        } else {
                                            enabledPlayerButtons + key
                                        }
                                        onUpdatePlayerPreferences {
                                            it.copy(enabledPlayerButtons = nextSet.toList())
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = Color.White,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Volume & Brightness HUD Style",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    NovaSegmentedControl(
                        items = listOf(
                            VolumeBrightnessBarStyle.MX_PRO_VERTICAL to "Studio Bar",
                            VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE to "Fluid Capsule",
                            VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL to "Minimal Pill",
                        ),
                        selected = playerPreferences.volumeBrightnessBarStyle,
                        onSelected = { newHud ->
                            onUpdatePlayerPreferences { it.copy(volumeBrightnessBarStyle = newHud) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
