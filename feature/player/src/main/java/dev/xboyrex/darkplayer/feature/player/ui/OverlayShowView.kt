package dev.xboyrex.darkplayer.feature.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.Chapter
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.VideoContentScale
import dev.xboyrex.darkplayer.feature.player.extensions.noRippleClickable
import dev.xboyrex.darkplayer.feature.player.state.SubtitleOptionsEvent
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.DecoderMode

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayView?,
    videoDecoderMode: DecoderMode?,
    audioDecoderMode: DecoderMode?,
    videoContentScale: VideoContentScale,
    playerPreferences: PlayerPreferences = PlayerPreferences(),
    sleepTimerMinutes: Int = 0,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Chapter) -> Unit,
    onDismiss: () -> Unit = {},
    onVideoDecoderModeSelected: (DecoderMode) -> Unit = {},
    onAudioDecoderModeSelected: (DecoderMode) -> Unit = {},
    onSelectSubtitleClick: () -> Unit = {},
    onSelectAudioClick: () -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit = {},
    onShowOverlay: (OverlayView) -> Unit = {},
    onCutAbRepeatClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onSelectSleepTimerMinutes: (Int) -> Unit = {},
    onShareVideo: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                other = if (overlayView != null) {
                    Modifier.noRippleClickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    )

    AudioTrackSelectorView(
        show = overlayView == OverlayView.AUDIO_SELECTOR,
        player = player,
        audioDecoderMode = audioDecoderMode,
        onAudioDecoderModeSelected = onAudioDecoderModeSelected,
        onSelectAudioClick = onSelectAudioClick,
        onDismiss = onDismiss,
    )

    DecoderSelectorView(
        show = overlayView == OverlayView.DECODER_SELECTOR,
        videoMode = videoDecoderMode,
        audioMode = audioDecoderMode,
        onVideoModeSelected = onVideoDecoderModeSelected,
        onAudioModeSelected = onAudioDecoderModeSelected,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayView.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onEvent = onSubtitleOptionEvent,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayView.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayView.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == OverlayView.PLAYLIST,
        player = player,
    )

    ChaptersView(
        show = overlayView == OverlayView.CHAPTERS,
        chapters = chapters,
        currentChapterIndex = currentChapterIndex,
        onChapterSelected = onChapterSelected,
    )

    MxPlayerMorePanel(
        show = overlayView == OverlayView.MORE_MENU,
        playerPreferences = playerPreferences,
        onUpdatePreferences = onUpdatePlayerPreferences,
        onAspectRatioClick = { onShowOverlay(OverlayView.VIDEO_CONTENT_SCALE) },
        onLongPressSpeedClick = { onShowOverlay(OverlayView.PLAYBACK_SPEED) },
        onDisplaySettingsClick = { onShowOverlay(OverlayView.DISPLAY_SETTINGS) },
        onPlaylistsClick = { onShowOverlay(OverlayView.PLAYLIST) },
        onInformationClick = { onShowOverlay(OverlayView.MEDIA_INFORMATION) },
        onShareClick = {
            onDismiss()
            onShareVideo()
        },
        onCutAbRepeatClick = {
            onDismiss()
            onCutAbRepeatClick()
        },
        onBookmarkClick = {
            onDismiss()
            onBookmarkClick()
        },
        onChaptersClick = { onShowOverlay(OverlayView.CHAPTERS) },
        onEqualizerClick = { onShowOverlay(OverlayView.EQUALIZER) },
    )

    MxEqualizerOverlayView(
        show = overlayView == OverlayView.EQUALIZER,
        playerPreferences = playerPreferences,
        onUpdatePreferences = onUpdatePlayerPreferences,
    )

    MxDisplaySettingsOverlayView(
        show = overlayView == OverlayView.DISPLAY_SETTINGS,
        playerPreferences = playerPreferences,
        onUpdatePreferences = onUpdatePlayerPreferences,
    )

    MxSleepTimerOverlayView(
        show = overlayView == OverlayView.SLEEP_TIMER,
        selectedMinutes = sleepTimerMinutes,
        onSelectMinutes = onSelectSleepTimerMinutes,
        onDismiss = onDismiss,
    )

    MxMediaInformationOverlayView(
        show = overlayView == OverlayView.MEDIA_INFORMATION,
        player = player,
    )
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class OverlayView {
    DECODER_SELECTOR,
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    CHAPTERS,
    MORE_MENU,
    EQUALIZER,
    DISPLAY_SETTINGS,
    SLEEP_TIMER,
    MEDIA_INFORMATION,
}
