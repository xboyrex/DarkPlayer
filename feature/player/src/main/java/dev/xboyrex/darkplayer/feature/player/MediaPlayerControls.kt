package dev.xboyrex.darkplayer.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.model.MxQuickShortcut
import dev.xboyrex.darkplayer.core.ui.components.requestFocusUntilLanded
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaAccentColor
import dev.xboyrex.darkplayer.feature.player.state.ControlsVisibilityState
import dev.xboyrex.darkplayer.feature.player.state.SeekGestureState
import dev.xboyrex.darkplayer.feature.player.state.TapGestureState
import dev.xboyrex.darkplayer.feature.player.state.VideoZoomAndContentScaleState
import dev.xboyrex.darkplayer.feature.player.state.rememberChaptersState
import dev.xboyrex.darkplayer.feature.player.state.rememberMediaPresentationState
import dev.xboyrex.darkplayer.feature.player.state.rememberMetadataState
import dev.xboyrex.darkplayer.feature.player.state.rememberPlayerKeyInputState
import dev.xboyrex.darkplayer.feature.player.ui.DpadSeekIndicator
import dev.xboyrex.darkplayer.feature.player.ui.OverlayShowView
import dev.xboyrex.darkplayer.feature.player.ui.OverlayView
import dev.xboyrex.darkplayer.feature.player.ui.PlayerGestureIndicators
import dev.xboyrex.darkplayer.feature.player.ui.captureVideoScreenshot
import dev.xboyrex.darkplayer.feature.player.ui.shareCurrentVideo
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
internal fun MediaPlayerControls(
    player: Player,
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    isPipSupported: Boolean,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
    dpadUpDown: Int? = null,
) {
    val playerPreferences = state.playerPreferences
    val metadataState = rememberMetadataState(player = player)
    val mediaPresentationState = rememberMediaPresentationState(player = player)
    val progressState = rememberProgressStateWithTickInterval(player = player)
    val chaptersState = rememberChaptersState(
        player = player,
        progressState = progressState,
    )
    val keyInputState = rememberPlayerKeyInputState(
        player = player,
        controls = controlsVisibilityState,
        seekIncrementMs = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        dpadUpDown = dpadUpDown,
    )
    var overlayView by remember { mutableStateOf<OverlayView?>(value = null) }

    // MX Player Pro Live Interactive Feature States:
    var isNightModeActive by rememberSaveable { mutableStateOf(false) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var abRepeatPointA by rememberSaveable { mutableLongStateOf(-1L) }
    var abRepeatPointB by rememberSaveable { mutableLongStateOf(-1L) }
    var sleepTimerMinutes by rememberSaveable { mutableIntStateOf(0) }
    var sleepTimerRemainingSec by rememberSaveable { mutableIntStateOf(0) }
    var bookmarkedPositionMs by rememberSaveable { mutableLongStateOf(-1L) }

    // A-B Repeat active loop watcher
    LaunchedEffect(abRepeatPointA, abRepeatPointB, progressState.currentPositionMs) {
        if (abRepeatPointA >= 0L && abRepeatPointB > abRepeatPointA) {
            if (progressState.currentPositionMs >= abRepeatPointB) {
                player.seekTo(abRepeatPointA)
            }
        }
    }

    // Sleep Timer countdown watcher
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            sleepTimerRemainingSec = sleepTimerMinutes * 60
            while (sleepTimerRemainingSec > 0) {
                delay(1_000L)
                sleepTimerRemainingSec--
            }
            player.pause()
            sleepTimerMinutes = 0
        } else {
            sleepTimerRemainingSec = 0
        }
    }

    // Reset A-B repeat when media changes
    LaunchedEffect(metadataState.mediaId) {
        if (overlayView == OverlayView.CHAPTERS) overlayView = null
        abRepeatPointA = -1L
        abRepeatPointB = -1L
    }

    fun showOverlay(overlay: OverlayView) {
        controlsVisibilityState.hideControls()
        overlayView = overlay
    }

    val context = LocalContext.current
    val accent = LocalNovaAccentColor.current
    val isTv = remember { context.isTelevision }
    val handleDpadKeys = isTv || dpadUpDown != null
    val rootFocusRequester = remember { FocusRequester() }
    val middleControlsFocusRequester = remember { FocusRequester() }
    val unlockFocusRequester = remember { FocusRequester() }
    var isMiddleControlsFocused by remember { mutableStateOf(value = false) }
    var isUnlockFocused by remember { mutableStateOf(value = false) }

    val handleAbRepeatToggle = {
        when {
            abRepeatPointA < 0L -> {
                abRepeatPointA = player.currentPosition
                Toast.makeText(
                    context,
                    "A-B Repeat: Point A set at ${getStringForTime(abRepeatPointA)}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
            abRepeatPointB < 0L -> {
                val now = player.currentPosition
                if (now > abRepeatPointA + 500L) {
                    abRepeatPointB = now
                    Toast.makeText(
                        context,
                        "A-B Repeat looping: ${getStringForTime(abRepeatPointA)} ⇄ ${getStringForTime(abRepeatPointB)}",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    abRepeatPointA = -1L
                    Toast.makeText(context, "A-B Repeat cleared", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                abRepeatPointA = -1L
                abRepeatPointB = -1L
                Toast.makeText(context, "A-B Repeat turned off", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val abRepeatBadge = when {
        abRepeatPointA >= 0L && abRepeatPointB > abRepeatPointA -> "A⇄B"
        abRepeatPointA >= 0L -> "A..."
        else -> null
    }

    if (handleDpadKeys) {
        LaunchedEffect(
            controlsVisibilityState.controlsVisible,
            controlsVisibilityState.controlsLocked,
            key3 = overlayView,
        ) {
            if (overlayView != null) return@LaunchedEffect
            if (!isTv || !controlsVisibilityState.controlsVisible) {
                runCatching { rootFocusRequester.requestFocus() }
                return@LaunchedEffect
            }
            val locked = controlsVisibilityState.controlsLocked
            val target = if (locked) unlockFocusRequester else middleControlsFocusRequester
            target.requestFocusUntilLanded(attempts = 20) {
                if (locked) isUnlockFocused else isMiddleControlsFocused
            }
        }
    }

    CompositionLocalProvider(
        LocalControlsVisibilityState provides controlsVisibilityState,
        LocalUseMaterialYouControls provides playerPreferences.useMaterialYouControls,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .thenIf(condition = handleDpadKeys) {
                    focusRequester(focusRequester = rootFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (overlayView != null ||
                                (
                                    !isTv && keyEvent.key != Key.DirectionUp &&
                                        keyEvent.key != Key.DirectionDown
                                    )
                            ) {
                                false
                            } else {
                                keyInputState.onKeyEvent(keyEvent = keyEvent)
                            }
                        }
                },
        ) {
            // Night Mode Warm Dim Overlay (MX Player Pro Night Mode feature)
            if (isNightModeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF140D05).copy(alpha = 0.48f)),
                )
            }

            if (mediaPresentationState.isBuffering) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                )
            }
            PlayerGestureIndicators(tapGestureState = tapGestureState)
            DpadSeekIndicator(
                visible = keyInputState.isSeeking && keyInputState.seekOffsetMs != 0L,
                offsetMs = keyInputState.seekOffsetMs,
                positionMs = keyInputState.seekPositionMs,
            )

            // Active A-B Repeat or Sleep Timer floating status badge
            if (abRepeatBadge != null || sleepTimerRemainingSec > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 20.dp),
                ) {
                    val statusText = buildList {
                        if (abRepeatBadge != null) add("Loop $abRepeatBadge")
                        if (sleepTimerRemainingSec > 0) {
                            add("Sleep ${sleepTimerRemainingSec / 60}:${"%02d".format(sleepTimerRemainingSec % 60)}")
                        }
                    }.joinToString("  •  ")
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }

            if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                PlayerLockedControls(
                    onUnlockControls = controlsVisibilityState::unlockControls,
                    unlockButtonModifier = Modifier.thenIf(condition = isTv) {
                        focusRequester(focusRequester = unlockFocusRequester)
                            .onFocusChanged { isUnlockFocused = it.hasFocus }
                    },
                )
            } else {
                PlayerControls(
                    player = player,
                    title = metadataState.title.orEmpty(),
                    playerPreferences = playerPreferences,
                    videoDecoderMode = state.decoderServiceState.videoMode,
                    controlsVisibilityState = controlsVisibilityState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    progressState = progressState,
                    chaptersState = chaptersState,
                    isPipSupported = isPipSupported,
                    isNightModeActive = isNightModeActive,
                    isMuted = isMuted,
                    abRepeatLabel = abRepeatBadge,
                    sleepTimerActive = sleepTimerMinutes > 0,
                    onShowOverlay = ::showOverlay,
                    onBackClick = { onAction(PlayerAction.NavigateUp) },
                    onPlayInBackgroundClick = { onAction(PlayerAction.PlayInBackground) },
                    onToggleTimeDisplay = { onAction(PlayerAction.ToggleTimeDisplay) },
                    onPictureInPictureClick = onPictureInPictureClick,
                    onShortcutClick = { shortcut ->
                        when (shortcut) {
                            MxQuickShortcut.NIGHT_MODE -> {
                                isNightModeActive = !isNightModeActive
                                Toast.makeText(
                                    context,
                                    if (isNightModeActive) "Night Mode ON" else "Night Mode OFF",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            MxQuickShortcut.CUSTOMISE_ITEMS -> showOverlay(OverlayView.MORE_MENU)
                            MxQuickShortcut.SHUFFLE -> {
                                player.shuffleModeEnabled = !player.shuffleModeEnabled
                            }
                            MxQuickShortcut.LOOP -> {
                                player.repeatMode = when (player.repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                    else -> Player.REPEAT_MODE_OFF
                                }
                            }
                            MxQuickShortcut.MUTE -> {
                                isMuted = !isMuted
                                player.volume = if (isMuted) 0f else 1f
                                Toast.makeText(
                                    context,
                                    if (isMuted) "Audio Muted" else "Audio Unmuted",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            MxQuickShortcut.SLEEP_TIMER -> showOverlay(OverlayView.SLEEP_TIMER)
                            MxQuickShortcut.AB_REPEAT -> handleAbRepeatToggle()
                            MxQuickShortcut.EQUALIZER -> showOverlay(OverlayView.EQUALIZER)
                            MxQuickShortcut.PLAYBACK_SPEED -> showOverlay(OverlayView.PLAYBACK_SPEED)
                            MxQuickShortcut.SCREENSHOT -> captureVideoScreenshot(context, player)
                            MxQuickShortcut.BACKGROUND_PLAY -> onAction(PlayerAction.PlayInBackground)
                            MxQuickShortcut.SCREEN_ROTATION -> {
                                val activity = context as? Activity
                                if (activity != null) {
                                    val isLandscape = activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                                        activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                    activity.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }
                            }
                        }
                    },
                    middleControlsModifier = Modifier.thenIf(condition = isTv) {
                        focusRequester(focusRequester = middleControlsFocusRequester)
                            .onFocusChanged { isMiddleControlsFocused = it.hasFocus }
                    },
                )
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                videoDecoderMode = state.decoderServiceState.videoMode,
                audioDecoderMode = state.decoderServiceState.audioMode,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                playerPreferences = playerPreferences,
                sleepTimerMinutes = sleepTimerMinutes,
                chapters = chaptersState.chapters,
                currentChapterIndex = chaptersState.currentChapterIndex,
                onChapterSelected = { chapter ->
                    if (player.isCurrentMediaItemSeekable) {
                        player.seekTo(chapter.startTimeMs)
                        overlayView = null
                        controlsVisibilityState.showControls()
                    }
                },
                onDismiss = { overlayView = null },
                onVideoDecoderModeSelected = {
                    onAction(PlayerAction.SetVideoDecoderMode(mode = it))
                },
                onAudioDecoderModeSelected = {
                    onAction(PlayerAction.SetAudioDecoderMode(mode = it))
                },
                onSelectSubtitleClick = { onAction(PlayerAction.SelectSubtitle) },
                onSelectAudioClick = { onAction(PlayerAction.SelectAudio) },
                onSubtitleOptionEvent = {
                    onAction(PlayerAction.OnSubtitleOptionEvent(event = it))
                },
                onVideoContentScaleChanged =
                    videoZoomAndContentScaleState::onVideoContentScaleChanged,
                onUpdatePlayerPreferences = { transform ->
                    onAction(PlayerAction.UpdatePreferences(transform))
                },
                onShowOverlay = { overlayView = it },
                onCutAbRepeatClick = handleAbRepeatToggle,
                onBookmarkClick = {
                    if (bookmarkedPositionMs < 0L) {
                        bookmarkedPositionMs = player.currentPosition
                        Toast.makeText(
                            context,
                            "Bookmarked at ${getStringForTime(bookmarkedPositionMs)}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        player.seekTo(bookmarkedPositionMs)
                        Toast.makeText(
                            context,
                            "Jumped to bookmark (${getStringForTime(bookmarkedPositionMs)})",
                            Toast.LENGTH_SHORT,
                        ).show()
                        bookmarkedPositionMs = -1L
                    }
                },
                onSelectSleepTimerMinutes = { sleepTimerMinutes = it },
                onShareVideo = { shareCurrentVideo(context, player) },
            )
        }
    }

    BackHandler {
        when {
            overlayView != null -> overlayView = null

            isTv && controlsVisibilityState.controlsVisible ->
                controlsVisibilityState.hideControls()

            else -> onAction(PlayerAction.NavigateUp)
        }
    }
}
