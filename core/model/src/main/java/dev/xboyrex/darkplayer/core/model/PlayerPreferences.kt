package dev.xboyrex.darkplayer.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerPreferences(
    val resume: Resume = Resume.YES,
    val rememberPlayerBrightness: Boolean = false,
    val playerBrightness: Float = 0.5f,
    val minDurationForFastSeek: Long = 120000L,
    val rememberSelections: Boolean = true,
    val playerScreenOrientation: ScreenOrientation = ScreenOrientation.VIDEO_ORIENTATION,
    val playerVideoZoom: VideoContentScale = VideoContentScale.BEST_FIT,
    val defaultPlaybackSpeed: Float = 1.0f,
    val controllerAutoHideTimeout: Int = DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
    val seekIncrement: Int = DEFAULT_SEEK_INCREMENT,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val loopMode: LoopMode = LoopMode.OFF,

    // Controls (Gestures)
    val useBrightnessGestureControls: Boolean = true,
    val useVolumeGestureControls: Boolean = true,
    val enableVolumeSwipeGesture: Boolean = true,
    val enableBrightnessSwipeGesture: Boolean = true,
    val volumeGestureSensitivity: Float = 0.5f,
    val brightnessGestureSensitivity: Float = 0.5f,
    val useSeekControls: Boolean = true,
    val useZoomControls: Boolean = true,
    val enablePanGesture: Boolean = false,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    val useLongPressControls: Boolean = true,
    val longPressControlsSpeed: Float = 2.0f,
    val seekSensitivity: Float = DEFAULT_SEEK_SENSITIVITY,

    // Player Interface
    val controlButtonsPosition: ControlButtonsPosition = ControlButtonsPosition.LEFT,
    val hidePlayerButtonsBackground: Boolean = false,
    val useMaterialYouControls: Boolean = false,
    val showRemainingTime: Boolean = false,

    // Audio Preferences
    val preferredAudioLanguage: String = "",
    val pauseOnHeadsetDisconnect: Boolean = true,
    val requireAudioFocus: Boolean = true,
    @SerialName("showHeadsetSystemVolumePanel") val showSystemVolumePanel: Boolean = true,
    val enableVolumeBoost: Boolean = false,

    // Subtitle Preferences
    val useSystemCaptionStyle: Boolean = false,
    val preferredSubtitleLanguage: String = "",
    val subtitleTextEncoding: String = "",
    val subtitleTextSize: Int = DEFAULT_SUBTITLE_TEXT_SIZE,
    val subtitleBackground: Boolean = false,
    val subtitleFont: Font = Font.DEFAULT,
    val subtitleTextBold: Boolean = true,
    val applyEmbeddedStyles: Boolean = true,
    // Vertical distance from the default subtitle spot, as a fraction of the player height.
    val subtitleOffsetY: Float = 0f,
) {

    companion object {
        const val DEFAULT_SEEK_INCREMENT = 10
        const val DEFAULT_SEEK_SENSITIVITY = 0.5f
        const val DEFAULT_SUBTITLE_TEXT_SIZE = 20
        const val DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT = 4
    }
}
