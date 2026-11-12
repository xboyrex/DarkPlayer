package dev.xboyrex.novaplayer.core.ui.dolby

import androidx.compose.runtime.Immutable

enum class NovaBandMode(val value: String, val displayName: String, val bandCount: Int) {
    TEN_BAND("10", "10 Bands", 10),
    FIFTEEN_BAND("15", "15 Bands", 15),
    TWENTY_BAND("20", "20 Bands", 20);

    companion object {
        fun fromValue(value: String): NovaBandMode =
            entries.find { it.value == value } ?: TEN_BAND
    }
}

@Immutable
data class NovaProfileOption(
    val id: Int,
    val name: String,
    val summary: String,
)

@Immutable
data class NovaBandGain(
    val frequency: Int,
    val gain: Int = 0, // scaled in 0.1 dB units (-150..150 -> -15.0 dB..+15.0 dB)
)

@Immutable
data class NovaEqualizerPreset(
    val name: String,
    val bandGains: List<NovaBandGain>,
    val isUserDefined: Boolean = false,
    val isCustom: Boolean = false,
    val bandMode: NovaBandMode = NovaBandMode.TEN_BAND,
)

@Immutable
data class NovaProfileSettings(
    val profile: Int = 0,
    val ieqPreset: Int = 0,
    val headphoneVirtualizerEnabled: Boolean = true,
    val speakerVirtualizerEnabled: Boolean = true,
    val stereoWideningAmount: Int = 32,
    val dialogueEnhancerEnabled: Boolean = true,
    val dialogueEnhancerAmount: Int = 6,
    val bassLevel: Int = 40,
    val midLevel: Int = 0,
    val trebleLevel: Int = 20,
    val bassCurve: Int = 0,
    val volumeLevelerEnabled: Boolean = false,
)

enum class NovaEqualizerViewMode {
    CURVE,
    SLIDERS,
}

object NovaDolbyPresetsData {
    val PROFILES = listOf(
        NovaProfileOption(0, "Dynamic", "Automatically adapts spatial & frequency balance to your media"),
        NovaProfileOption(1, "Movie / Video", "Cinematic surround stage with enhanced dialogue & sub-bass"),
        NovaProfileOption(2, "Music", "Rich, wide acoustic stage with natural harmonic balance"),
        NovaProfileOption(3, "Game", "Positional 3D audio cues & low-latency impact boost"),
        NovaProfileOption(4, "Work", "Vocal-focused clarity for podcasts, lectures & tutorials"),
        NovaProfileOption(5, "Casual", "Smooth, fatigue-free everyday listening profile"),
        NovaProfileOption(6, "Mood", "Warm, atmospheric soundstage with deep low-end"),
    )

    val IEQ_OPTIONS = listOf(
        0 to "Off",
        1 to "Detailed",
        2 to "Balanced",
        3 to "Warm",
    )

    val BASS_CURVES = listOf(
        0 to "Balanced",
        1 to "Sub boost",
        2 to "Punch",
    )

    val BAND_FREQUENCIES_10 = listOf(32, 64, 125, 250, 500, 1000, 2250, 5000, 10000, 19688)
    val BAND_FREQUENCIES_15 = listOf(
        32, 47, 94, 141, 234, 469, 844, 1313, 2250, 3750, 5813, 9000, 11250, 13875, 19688,
    )
    val BAND_FREQUENCIES_20 = listOf(
        32, 47, 141, 234, 328, 469, 656, 844, 1031, 1313,
        1688, 2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 19688,
    )

    val TEN_BAND_INDICES = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18)
    val FIFTEEN_BAND_INDICES = listOf(0, 1, 2, 3, 4, 5, 6, 8, 11, 12, 14, 15, 17, 18, 19)

    val BUILTIN_PRESETS_20: List<Pair<String, IntArray>> = listOf(
        "Flat (off)" to intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "Rock" to intArrayOf(90, 54, 18, -18, -54, -36, -18, -12, -6, -30, -54, -30, -6, -30, -54, -24, 6, 48, 90, 90),
        "Jazz" to intArrayOf(12, 12, 12, 12, 12, 0, -12, -12, -12, -36, -60, -30, 0, 6, 12, 12, 12, 12, 12, 12),
        "Pop" to intArrayOf(-20, -2, 17, -38, -92, -44, 5, 17, 29, 29, 29, 23, 17, -14, -44, -14, 17, 23, 29, 29),
        "Classical" to intArrayOf(-48, -48, -48, -48, -48, -48, -48, -42, -36, -6, 24, 0, -24, 36, 96, 48, 0, 48, 96, 96),
        "Hip Hop" to intArrayOf(78, 42, 6, -30, -66, -36, -6, -6, -6, -36, -66, -36, -6, 0, 6, 6, 6, 30, 54, 54),
        "Blues" to intArrayOf(42, 42, 42, -54, -150, -102, -54, 6, 66, 42, 18, 6, -6, 6, 18, 6, -6, 18, 42, 42),
        "Electronic" to intArrayOf(75, 51, 27, 3, -21, -9, 3, -3, -9, -39, -69, -39, -9, -3, 3, 3, 3, 3, 3, 3),
        "Metal" to intArrayOf(60, 36, 12, 12, 12, -6, -24, -18, -12, -48, -84, -36, 12, 12, 12, 12, 12, 12, 12, 12),
        "Acoustic" to intArrayOf(50, 45, 40, 35, 15, 10, 15, 15, 20, 30, 35, 40, 37, 30, 30, 30, 25, 20, 15, 15),
        "Bass Boost" to intArrayOf(100, 88, 85, 65, 25, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "Beats" to intArrayOf(-55, -50, -45, -42, -35, -30, -19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        "Crystal Clear" to intArrayOf(35, 55, 65, 95, 80, 65, 35, 25, 13, 50, 70, 90, 100, 110, 90, 85, 75, 65, 55, 45),
        "Deep" to intArrayOf(120, 80, 0, -67, -120, -90, -35, -35, -61, 0, -30, -50, 0, 12, 30, 25, 20, 15, 10, 5),
        "Dubstep" to intArrayOf(120, 100, 5, -10, -30, -50, -50, -48, -45, -25, -10, 0, -25, -25, 0, 0, 0, 0, 0, 0),
        "Hardstyle" to intArrayOf(61, 70, 120, 61, -50, -120, -25, 30, 65, 0, -22, -45, -61, -92, -100, -95, -90, -85, -80, -75),
        "Movie" to intArrayOf(30, 30, 61, 85, 90, 70, 61, 61, 50, 80, 35, 35, 80, 100, 80, 75, 70, 65, 60, 55),
        "R&B" to intArrayOf(30, 30, 70, 61, 45, 15, -15, -20, -15, 20, 25, 30, 35, 38, 40, 38, 35, 32, 30, 28),
        "Vocal Booster" to intArrayOf(-15, -20, -30, -30, -5, 15, 35, 35, 35, 30, 20, 15, 0, 0, -15, -12, -10, -8, -6, -4),
    )

    val BASS_CURVE_WEIGHTS = listOf(
        floatArrayOf(
            1.00f, 1.00f, 0.95f, 0.90f, 0.80f, 0.70f, 0.55f, 0.40f, 0.25f, 0.15f,
            0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
        ),
        floatArrayOf(
            1.20f, 1.15f, 1.05f, 0.90f, 0.70f, 0.55f, 0.40f, 0.25f, 0.10f, 0.05f,
            0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
        ),
        floatArrayOf(
            0.90f, 0.95f, 1.00f, 1.00f, 0.90f, 0.75f, 0.60f, 0.45f, 0.30f, 0.20f,
            0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f,
        ),
    )
}
