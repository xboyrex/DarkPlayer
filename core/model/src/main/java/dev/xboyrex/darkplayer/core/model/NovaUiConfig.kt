package dev.xboyrex.darkplayer.core.model

import kotlinx.serialization.Serializable

/**
 * Overall UI/UX theme & component style selectable by the user.
 */
@Serializable
enum class NovaUiStyle {
    MX_LIQUID_HYBRID,
    GLASMIO_IOS27,
    YTXTUBE_GLASS,
    CLASSIC_NEXT,
}

/**
 * Bottom Navigation Bar design & physics selectable by the user.
 */
@Serializable
enum class NovaNavBarStyle {
    YTXTUBE_LIQUID_GLASS,
    GLASMIO_IOS27_MORPH,
    MX_PRO_CLASSIC,
}

/**
 * Toggle switch & slider style selectable by the user.
 */
@Serializable
enum class NovaSwitchStyle {
    GLASMIO_LIQUID,
    MX_PRO_CLASSIC,
    MATERIAL_EXPRESSIVE,
}

/**
 * Folder & Video list card style (fixes Bug #2, #10, #11, #18).
 */
@Serializable
enum class MediaListCardStyle {
    MX_PRO_CLEAN,
    LIQUID_GLASS_CARD,
    SEGMENTED_CARD,
}

/**
 * App theme mode matching MX Player Pro (#3 Screenshot_20261110-155414_MX Player Pro.png).
 */
@Serializable
enum class MxAppThemeMode {
    ADAPTIVE,
    DARK_THEME,
    LIGHT_THEME,
    AMOLED_BLACK,
    CLASSIC_COLOR,
}

/**
 * In-player UI layout & control design selectable by the user.
 */
@Serializable
enum class PlayerControlStyle {
    MX_PRO,
    LIQUID_GLASS,
    CLASSIC_NEXT,
}

/**
 * Volume & Brightness gesture indicator bar style selectable by the user (fixes Bug #16).
 */
@Serializable
enum class VolumeBrightnessBarStyle {
    MX_PRO_VERTICAL,
    GLASMIO_LIQUID_CAPSULE,
    NEXT_VERTICAL_PILL,
}

/**
 * MX Player Pro Quick Shortcuts available in the top collapsible player row (#18, #19, #20, #24).
 */
@Serializable
enum class MxQuickShortcut(val defaultTitle: String) {
    NIGHT_MODE("Night Mode"),
    CUSTOMISE_ITEMS("Customise Items"),
    SHUFFLE("Shuffle"),
    LOOP("Loop"),
    MUTE("Mute"),
    SLEEP_TIMER("Sleep Timer"),
    AB_REPEAT("A - B Repeat"),
    EQUALIZER("Equalizer"),
    PLAYBACK_SPEED("Speed"),
    SCREENSHOT("Screenshot"),
    BACKGROUND_PLAY("Background Play"),
    SCREEN_ROTATION("Screen Rotation"),
}

/**
 * MX Player Pro Lock modes (#8, #9 Controls dialog).
 */
@Serializable
enum class MxLockMode {
    LOCK,
    KIDS_LOCK,
    KIDS_LOCK_TOUCH_EFFECTS,
}

/**
 * MX Player Pro Touch Action (#9 Controls dialog).
 */
@Serializable
enum class MxTouchAction {
    SHOW_HIDE_INTERFACE,
    PLAY_PAUSE,
    PLAY_SHOW_HIDE_INTERFACE,
}

@Serializable
enum class SubtitleAlignment {
    LEFT,
    CENTER,
    RIGHT,
}
