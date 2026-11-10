package dev.xboyrex.darkplayer.core.ui.glass

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class NavBarStyle(val id: String, val label: String, val subtitle: String) {
    YTX_LIQUID_GLASS("ytx_liquid", "YTX Liquid Glass", "Original YTXTube frosted glass capsule with drag lens"),
    CUTOUT_FLOATING_CIRCLE("cutout_circle", "Cutout Floating Circle", "Concave curved notch bar with floating active circle"),
    EXPANDABLE_PILL("expandable_pill", "Expandable Pill Bar", "Sleek dark capsule with expanding icon + label pill"),
    CRADLE_INTERACTIVE("cradle_interactive", "Interactive Cradle Bar", "Curved center cradle with radial quick-action bubbles"),
    GLASMIO_LIQUID("glasmio_liquid", "Glasmio iOS 27 Bar", "Glasmio specular glass bar with elastic spring pill");

    companion object {
        fun fromId(id: String): NavBarStyle = entries.firstOrNull { it.id == id } ?: YTX_LIQUID_GLASS
    }
}

enum class VolumeHudStyle(val id: String, val label: String, val subtitle: String) {
    APPLE_IOS("apple_ios", "Apple iOS", "Sleek frosted vertical pill with smooth expand-on-drag"),
    GOOGLE_M3("google_m3", "Google Pixel", "Material 3 pill with top badge, inner note thumb & mute button"),
    SAMSUNG_ONEUI("samsung_oneui", "Samsung One UI", "Slim capsule with top menu dots & animated wave icon"),
    XIAOMI_MIUI("xiaomi_miui", "Xiaomi HyperOS", "Wide vertical pill with quick boost/mute & night mode buttons"),
    ONEPLUS_OXYGEN("oneplus_oxygen", "OnePlus OxygenOS", "Tall crisp bar with bottom Equalizer & Mute oval pills");

    companion object {
        fun fromId(id: String): VolumeHudStyle = entries.firstOrNull { it.id == id } ?: APPLE_IOS
    }
}

enum class SwitchStyle(val id: String, val label: String) {
    GLASMIO_LIQUID("glasmio_liquid", "Glasmio Liquid Glass (iOS 27)"),
    MX_PRO_BLUE("mx_pro_blue", "MX Player Pro Blue"),
    YTX_EXPRESSIVE("ytx_expressive", "YTX Material Expressive");

    companion object {
        fun fromId(id: String): SwitchStyle = entries.firstOrNull { it.id == id } ?: GLASMIO_LIQUID
    }
}

enum class MxQuickShortcut(val id: String, val label: String) {
    NIGHT_MODE("night_mode", "Night Mode"),
    CUSTOMISE("customise", "Customise Items"),
    SHUFFLE("shuffle", "Shuffle"),
    LOOP("loop", "Loop"),
    MUTE("mute", "Mute"),
    SLEEP_TIMER("sleep_timer", "Sleep Timer"),
    AB_REPEAT("ab_repeat", "A - B Repeat"),
    EQUALIZER("equalizer", "Equalizer"),
    SPEED("speed", "Speed"),
    SCREENSHOT("screenshot", "Screenshot"),
    BACKGROUND_PLAY("background_play", "Background Play"),
    ROTATION("rotation", "Screen Rotation"),
}

val MxClassicThemeSwatches = listOf(
    Color(0xFF38BDF8), // MX Sky Blue (Default)
    Color(0xFFFACC15), // Nova Solar Gold
    Color(0xFFFF6B57), // Coral Red
    Color(0xFFFF9F43), // Warm Amber
    Color(0xFF4ADE80), // Emerald Green
    Color(0xFF2DD4BF), // Teal Mint
    Color(0xFF60A5FA), // Royal Blue
    Color(0xFF818CF8), // Indigo
    Color(0xFFA78BFA), // Violet
    Color(0xFFE879F9), // Magenta Pink
    Color(0xFFF43F5E), // Rose Crimson
    Color(0xFFA3E635), // Lime
    Color(0xFFFB7185), // Flamingo
    Color(0xFF34D399), // Jade
    Color(0xFF22D3EE), // Cyan
    Color(0xFFC084FC), // Lavender
    Color(0xFFFBBF24), // Honey Gold
    Color(0xFFFFFFFF), // Pure Monochrome White
)

object UiStylePreferences {
    private const val PREFS_NAME = "nova_ui_style_prefs"
    private var prefs: SharedPreferences? = null

    var navBarStyle by mutableStateOf(NavBarStyle.YTX_LIQUID_GLASS)
        private set

    var volumeHudStyle by mutableStateOf(VolumeHudStyle.APPLE_IOS)
        private set

    var switchStyle by mutableStateOf(SwitchStyle.GLASMIO_LIQUID)
        private set

    var navBarBottomOffsetDp by mutableIntStateOf(22)
        private set

    var mxSwatchIndex by mutableIntStateOf(0)
        private set

    var nightModeEnabled by mutableStateOf(false)
        private set

    var nightModeIntensity by mutableFloatStateOf(0.45f)
        private set

    var shortcutsBarVisible by mutableStateOf(true)
        private set

    var videoDisplayOverlay by mutableStateOf(true)
        private set

    var enabledShortcuts by mutableStateOf(MxQuickShortcut.entries.map { it.id }.toSet())
        private set

    val activeAccentColor: Color
        get() = MxClassicThemeSwatches.getOrElse(mxSwatchIndex) { MxClassicThemeSwatches[0] }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        navBarStyle = NavBarStyle.fromId(p.getString("nav_bar_style", NavBarStyle.YTX_LIQUID_GLASS.id) ?: "")
        volumeHudStyle = VolumeHudStyle.fromId(p.getString("volume_hud_style", VolumeHudStyle.APPLE_IOS.id) ?: "")
        switchStyle = SwitchStyle.fromId(p.getString("switch_style", SwitchStyle.GLASMIO_LIQUID.id) ?: "")
        navBarBottomOffsetDp = p.getInt("nav_bar_bottom_offset_dp", 22).coerceIn(6, 48)
        mxSwatchIndex = p.getInt("mx_swatch_index", 0).coerceIn(0, MxClassicThemeSwatches.lastIndex)
        nightModeEnabled = p.getBoolean("night_mode_enabled", false)
        nightModeIntensity = p.getFloat("night_mode_intensity", 0.45f).coerceIn(0.15f, 0.75f)
        shortcutsBarVisible = p.getBoolean("shortcuts_bar_visible", true)
        videoDisplayOverlay = p.getBoolean("video_display_overlay", true)
        val savedSet = p.getStringSet("enabled_shortcuts", null)
        if (!savedSet.isNullOrEmpty()) {
            enabledShortcuts = savedSet
        }
    }

    fun updateNavBarStyle(style: NavBarStyle) {
        navBarStyle = style
        prefs?.edit()?.putString("nav_bar_style", style.id)?.apply()
    }

    fun updateVolumeHudStyle(style: VolumeHudStyle) {
        volumeHudStyle = style
        prefs?.edit()?.putString("volume_hud_style", style.id)?.apply()
    }

    fun updateSwitchStyle(style: SwitchStyle) {
        switchStyle = style
        prefs?.edit()?.putString("switch_style", style.id)?.apply()
    }

    fun updateNavBarBottomOffsetDp(dpValue: Int) {
        val clamped = dpValue.coerceIn(6, 48)
        navBarBottomOffsetDp = clamped
        prefs?.edit()?.putInt("nav_bar_bottom_offset_dp", clamped)?.apply()
    }

    fun updateMxSwatchIndex(index: Int) {
        val clamped = index.coerceIn(0, MxClassicThemeSwatches.lastIndex)
        mxSwatchIndex = clamped
        prefs?.edit()?.putInt("mx_swatch_index", clamped)?.apply()
    }

    fun toggleNightMode(enabled: Boolean = !nightModeEnabled) {
        nightModeEnabled = enabled
        prefs?.edit()?.putBoolean("night_mode_enabled", enabled)?.apply()
    }

    fun updateNightModeIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.15f, 0.75f)
        nightModeIntensity = clamped
        prefs?.edit()?.putFloat("night_mode_intensity", clamped)?.apply()
    }

    fun updateShortcutsBarVisible(visible: Boolean) {
        shortcutsBarVisible = visible
        prefs?.edit()?.putBoolean("shortcuts_bar_visible", visible)?.apply()
    }

    fun updateVideoDisplayOverlay(visible: Boolean) {
        videoDisplayOverlay = visible
        prefs?.edit()?.putBoolean("video_display_overlay", visible)?.apply()
    }

    fun toggleShortcut(shortcut: MxQuickShortcut) {
        val next = enabledShortcuts.toMutableSet()
        if (next.contains(shortcut.id)) {
            if (next.size > 1) next.remove(shortcut.id)
        } else {
            next.add(shortcut.id)
        }
        enabledShortcuts = next
        prefs?.edit()?.putStringSet("enabled_shortcuts", next)?.apply()
    }
}
