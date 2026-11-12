package dev.xboyrex.darkplayer.core.model

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
    val autoplay: Boolean = true,
    val enableTunneledPlayback: Boolean = false,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val loopMode: LoopMode = LoopMode.OFF,

    // Controls (Gestures)
    @Deprecated(
        message =
            "Use individual enableVolumeSwipeGesture and enableBrightnessSwipeGesture instead",
    )
    val useSwipeControls: Boolean = true,
    val enableVolumeSwipeGesture: Boolean = true,
    val enableBrightnessSwipeGesture: Boolean = true,
    val useSeekControls: Boolean = true,
    val useZoomControls: Boolean = true,
    val enablePanGesture: Boolean = false,
    val doubleTapGesture: DoubleTapGesture = DoubleTapGesture.BOTH,
    val useLongPressControls: Boolean = true,
    val longPressControlsSpeed: Float = 2.0f,
    val seekIncrement: Int = DEFAULT_SEEK_INCREMENT,
    val seekSensitivity: Float = DEFAULT_SEEK_SENSITIVITY,
    val volumeGestureSensitivity: Float = DEFAULT_VOLUME_GESTURE_SENSITIVITY,
    val brightnessGestureSensitivity: Float = DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY,

    // Player Interface
    val controllerAutoHideTimeout: Int = DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT,
    val controlButtonsPosition: ControlButtonsPosition = ControlButtonsPosition.LEFT,
    val hidePlayerButtonsBackground: Boolean = false,
    val useMaterialYouControls: Boolean = false,
    val showRemainingTime: Boolean = true,

    // Audio Preferences
    val preferredAudioLanguage: String = "",
    val pauseOnHeadsetDisconnect: Boolean = true,
    val requireAudioFocus: Boolean = true,
    val showSystemVolumePanel: Boolean = true,
    val enableVolumeBoost: Boolean = true,

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

    // Nova Player & MX Player Pro Extended Player Preferences
    val playerControlStyle: PlayerControlStyle = PlayerControlStyle.CLASSIC_NEXT,
    val volumeBrightnessBarStyle: VolumeBrightnessBarStyle = VolumeBrightnessBarStyle.MX_PRO_VERTICAL,
    val swapVolumeBrightnessSides: Boolean = false,
    val playerAccentHex: String = "#0091FF",
    val showQuickShortcuts: Boolean = false,
    val enabledShortcuts: List<String> = MxQuickShortcut.entries.map { it.name },
    val showVideoDisplayInMenu: Boolean = true,
    val showScreenRotationButton: Boolean = true,
    val showBatteryClockInTitleBar: Boolean = true,
    val showElapsedTime: Boolean = true,
    val keepScreenOn: Boolean = true,
    val pauseIfObstructed: Boolean = false,
    val showInterfaceAtStartup: Boolean = false,
    val mxTouchAction: MxTouchAction = MxTouchAction.SHOW_HIDE_INTERFACE,
    val mxLockMode: MxLockMode = MxLockMode.LOCK,
    val showInterfaceOnLockedTouch: Boolean = true,
    val enableSubtitleScrollGesture: Boolean = true,
    val enableSubtitleUpDownGesture: Boolean = true,
    val enableSubtitleZoomGesture: Boolean = true,
    val enablePlaybackSpeedGesture: Boolean = false,
    val doubleTapBackToClose: Boolean = false,
    val quickZoom: Boolean = true,
    val resumeOnlyFirstFile: Boolean = false,
    val backToListAfterPlayback: Boolean = true,
    val previewWhileSeeking: Boolean = true,
    val previewWhileSeekingNetwork: Boolean = false,
    val fastSeeking: Boolean = true,
    val showPrevNextButtons: Boolean = true,
    val showForwardBackwardButtons: Boolean = false,
    val displayPositionWhileSeeking: Boolean = true,
    val smartPreviousButton: Boolean = true,
    val suppressErrorMessage: Boolean = false,

    // MX Player Pro Audio & Equalizer / Smooth Fade Preferences (Fixes Bug #3, #4, #9, #15)
    val useAsAudioPlayer: Boolean = false,
    val syncSystemVolume: Boolean = true,
    val fadeOnStart: Boolean = true,
    val fadeOnSeek: Boolean = true,
    val smoothPlayPauseAudioFade: Boolean = true,
    val audioFadeDurationMs: Int = 320,
    val audioDelayMs: Long = 0L,
    val bluetoothAudioDelayMs: Long = 0L,
    val preferAudioPassthrough: Boolean = true,
    val enableAudioEqualizer: Boolean = true,
    val enableDolbyAtmos: Boolean = true,
    val equalizerPreset: String = "Custom",
    val equalizerBandGainsDb: List<Int> = listOf(3, 0, 0, 0, -3),
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val reverbPreset: String = "None",
    val stereoMode: Int = 0, // 0 = Stereo, 1 = Mono, 2 = Reverse Stereo

    // MX Player Pro Decoder Preferences (#13)
    val hwPlusLocal: Boolean = true,
    val hwPlusNetwork: Boolean = true,
    val tryHwDecoderIfHwPlusFails: Boolean = true,
    val tryHwPlusDecoderIfHwFails: Boolean = true,
    val hwAudioOnSwVideo: Boolean = true,
    val correctAspectRatioHw: Boolean = false,
    val hwAudioTrackSelectable: Boolean = true,
    val swDecoderLocal: Boolean = false,
    val swDecoderNetwork: Boolean = false,
    val swAudio: Boolean = false,
    val swAudioLocal: Boolean = false,
    val swAudioNetwork: Boolean = false,
    val cpuCoreLimit: Int = 0,
    val useSpeedupTricks: Boolean = false,
    val deinterlace: Boolean = false,

    // MX Player Pro Subtitle Extended Preferences (#15)
    val subtitleFolder: String = "/storage/emulated/0/Subtitles",
    val fontFolder: String = "/storage/emulated/0",
    val subtitleDefaultSyncMs: Long = 0L,
    val subtitleSyncForHw: Boolean = true,
    val subtitleItalicEffect: Boolean = false,
    val subtitleForceLtr: Boolean = true,
    val subtitleBorderWidth: Float = 2f,
    val subtitleShadowEnabled: Boolean = true,
    val subtitleTextColorHex: String = "#FFFFFF",
    val subtitleAlignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    val subtitleScale: Float = 1.0f,
    val subtitleBorderEnabled: Boolean = true,
    val subtitleFadeOut: Boolean = false,
    val matchContentFrameRate: Boolean = true,
    val enabledPlayerButtons: List<String> = DEFAULT_ENABLED_PLAYER_BUTTONS,
) {

    companion object {
        const val DEFAULT_SEEK_INCREMENT = 10
        const val DEFAULT_SEEK_SENSITIVITY = 0.50f
        const val DEFAULT_VOLUME_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_BRIGHTNESS_GESTURE_SENSITIVITY = 0.50f
        const val DEFAULT_SUBTITLE_TEXT_SIZE = 20
        const val DEFAULT_CONTROLLER_AUTO_HIDE_TIMEOUT = 4
        val DEFAULT_ENABLED_PLAYER_BUTTONS = listOf(
            "UI_SWITCHER",
            "SCALE",
            "BG_PLAY",
            "SCREENSHOT",
            "TIMER",
            "EQUALIZER",
            "SHUFFLE",
            "LOOP",
            "PIP",
        )
    }
}
