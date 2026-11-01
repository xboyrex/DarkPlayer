@file:Suppress("DEPRECATION")

package dev.xboyrex.darkplayer.core.ui.audio

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID
import kotlin.math.roundToInt

enum class DolbyProfileType(val id: Int, val title: String, val subtitle: String) {
    DYNAMIC(0, "Dynamic", "Automatically adapts to your audio"),
    MOVIE(1, "Movie / Video", "Cinema surround stage & vocal clarity"),
    MUSIC(2, "Music", "Rich, balanced studio soundstage"),
    GAME(3, "Game", "Pinpoint 3D positional audio"),
    WORK(4, "Work", "Clear voice & low listening fatigue"),
    CASUAL(5, "Casual", "Relaxed everyday listening"),
    MOOD(6, "Mood", "Warm atmospheric harmonics"),
}

enum class DolbyIeqPreset(val id: Int, val title: String, val summary: String) {
    OFF(0, "Off", "Use current profile without intelligent EQ"),
    DETAILED(1, "Detailed", "Bring out subtle details and high-frequency clarity"),
    BALANCED(2, "Balanced", "Keep lows, mids, and highs in natural balance"),
    WARM(3, "Warm", "Give your sound a softer, warmer character"),
}

enum class DolbyBassCurve(val id: Int, val title: String) {
    BALANCED(0, "Balanced"),
    SUB_BOOST(1, "Sub boost"),
    PUNCH(2, "Punch"),
}

enum class ReverbPresetType(val id: Short, val title: String) {
    NONE(PresetReverb.PRESET_NONE, "None"),
    SMALL_ROOM(PresetReverb.PRESET_SMALLROOM, "Small Room"),
    MEDIUM_ROOM(PresetReverb.PRESET_MEDIUMROOM, "Medium Room"),
    LARGE_ROOM(PresetReverb.PRESET_LARGEROOM, "Large Room"),
    MEDIUM_HALL(PresetReverb.PRESET_MEDIUMHALL, "Medium Hall"),
    LARGE_HALL(PresetReverb.PRESET_LARGEHALL, "Large Hall"),
    PLATE(PresetReverb.PRESET_PLATE, "Plate"),
}

enum class StereoModeType(val id: Int, val title: String) {
    STEREO(0, "Stereo (Default)"),
    MONO(1, "Mono (Left + Right)"),
    LEFT_ONLY(2, "Left Channel Only"),
    RIGHT_ONLY(3, "Right Channel Only"),
}

data class EqPresetModel(
    val name: String,
    val bands5Db: List<Float>, // 60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz in dB (-15..+15)
    val bands20Scaled: IntArray, // 20-band scaled ±150 for Hardware DAP GEQ_BAND_GAINS
)

/**
 * Hardware Dolby Atmos DAP wrapper ported from hardware_dolby-16 (UUID 9d4921da-8225-4f29-aefa-39537a04bcaa).
 * Invokes AudioEffect hidden constructor and setParameter/getParameter via reflection so it compiles
 * against standard Android SDK and binds directly to vendor DAP HAL when present on the device.
 */
internal class HardwareDapEffect(priority: Int, audioSession: Int) {
    enum class DsParam(val id: Int, val length: Int = 1) {
        HEADPHONE_VIRTUALIZER(101),
        SPEAKER_VIRTUALIZER(102),
        VOLUME_LEVELER_ENABLE(103),
        IEQ_PRESET(104),
        DIALOGUE_ENHANCER_ENABLE(105),
        DIALOGUE_ENHANCER_AMOUNT(108),
        GEQ_BAND_GAINS(110, 20),
        BASS_ENHANCER_ENABLE(111),
        STEREO_WIDENING_AMOUNT(113),
    }

    private var audioEffect: AudioEffect? = null

    val isAvailable: Boolean
        get() = audioEffect != null

    init {
        runCatching {
            val effectTypeNull = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210")
            val effectTypeDap = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa")
            val ctor = AudioEffect::class.java.getConstructor(
                UUID::class.java,
                UUID::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            audioEffect = ctor.newInstance(effectTypeNull, effectTypeDap, priority, audioSession)
        }
    }

    private fun setParameterBytes(param: Int, value: ByteArray) {
        val effect = audioEffect ?: return
        runCatching {
            val method = AudioEffect::class.java.getMethod(
                "setParameter",
                Int::class.javaPrimitiveType,
                ByteArray::class.java,
            )
            method.invoke(effect, param, value)
        }
    }

    fun setEnabled(enabled: Boolean) {
        val effect = audioEffect ?: return
        runCatching {
            val buf = ByteArray(12)
            int32ToByteArray(EFFECT_PARAM_ENABLE, buf, 0)
            int32ToByteArray(1, buf, 4)
            int32ToByteArray(if (enabled) 1 else 0, buf, 8)
            setParameterBytes(EFFECT_PARAM_CPDP_VALUES, buf)
            effect.enabled = enabled
        }
    }

    fun setProfile(profile: Int) {
        runCatching {
            val buf = ByteArray(12)
            int32ToByteArray(EFFECT_PARAM_PROFILE, buf, 0)
            int32ToByteArray(1, buf, 4)
            int32ToByteArray(profile, buf, 8)
            setParameterBytes(EFFECT_PARAM_CPDP_VALUES, buf)
        }
    }

    fun setDapParameter(param: DsParam, values: IntArray, profile: Int) {
        runCatching {
            val length = values.size
            val buf = ByteArray((length + 4) * 4)
            int32ToByteArray(EFFECT_PARAM_SET_PROFILE_PARAMETER, buf, 0)
            int32ToByteArray(length + 1, buf, 4)
            int32ToByteArray(profile, buf, 8)
            int32ToByteArray(param.id, buf, 12)
            int32ArrayToByteArray(values, buf, 16)
            setParameterBytes(EFFECT_PARAM_CPDP_VALUES, buf)
        }
    }

    fun setDapParameter(param: DsParam, enable: Boolean, profile: Int) =
        setDapParameter(param, intArrayOf(if (enable) 1 else 0), profile)

    fun setDapParameter(param: DsParam, value: Int, profile: Int) =
        setDapParameter(param, intArrayOf(value), profile)

    fun release() {
        runCatching { audioEffect?.release() }
        audioEffect = null
    }

    companion object {
        private const val EFFECT_PARAM_ENABLE = 0
        private const val EFFECT_PARAM_CPDP_VALUES = 5
        private const val EFFECT_PARAM_PROFILE = 0xA000000
        private const val EFFECT_PARAM_SET_PROFILE_PARAMETER = 0x1000000

        private fun int32ToByteArray(value: Int, dst: ByteArray, index: Int) {
            var idx = index
            dst[idx++] = (value and 0xff).toByte()
            dst[idx++] = ((value ushr 8) and 0xff).toByte()
            dst[idx++] = ((value ushr 16) and 0xff).toByte()
            dst[idx++] = ((value ushr 24) and 0xff).toByte()
        }

        private fun int32ArrayToByteArray(src: IntArray, dst: ByteArray, index: Int) {
            var idx = index
            for (x in src) {
                dst[idx++] = (x and 0xff).toByte()
                dst[idx++] = ((x ushr 8) and 0xff).toByte()
                dst[idx++] = ((x ushr 16) and 0xff).toByte()
                dst[idx++] = ((x ushr 24) and 0xff).toByte()
            }
        }
    }
}

object HardwareDolbyAndEqualizerEngine {

    private const val PREFS_NAME = "darkplayer_audio_studio"

    val BAND_FREQUENCIES_HZ = listOf(60, 230, 910, 3600, 14000)
    val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3600 Hz", "14000 Hz")

    val PRESETS: List<EqPresetModel> = listOf(
        EqPresetModel(
            name = "Custom",
            bands5Db = listOf(3f, 0f, 0f, 0f, 3f),
            bands20Scaled = intArrayOf(30, 25, 20, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 20, 25, 30, 30),
        ),
        EqPresetModel(
            name = "Normal",
            bands5Db = listOf(3f, 0f, 0f, 0f, 3f),
            bands20Scaled = intArrayOf(30, 20, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 20, 30, 30),
        ),
        EqPresetModel(
            name = "Classical",
            bands5Db = listOf(5f, 3f, -2f, 4f, 4f),
            bands20Scaled = intArrayOf(-48, -48, -48, -48, -48, -48, -48, -42, -36, -6, 24, 0, -24, 36, 96, 48, 0, 48, 96, 96),
        ),
        EqPresetModel(
            name = "Dance",
            bands5Db = listOf(6f, 0f, 2f, 4f, 1f),
            bands20Scaled = intArrayOf(60, 36, 12, 12, 12, -6, -24, -18, -12, -48, -84, -36, 12, 12, 12, 12, 12, 12, 12, 12),
        ),
        EqPresetModel(
            name = "Flat",
            bands5Db = listOf(0f, 0f, 0f, 0f, 0f),
            bands20Scaled = IntArray(20) { 0 },
        ),
        EqPresetModel(
            name = "Folk",
            bands5Db = listOf(3f, 0f, 0f, 2f, -1f),
            bands20Scaled = intArrayOf(30, 25, 15, 5, 0, 0, 0, 5, 10, 15, 20, 20, 15, 10, 5, 0, -5, -10, -10, -10),
        ),
        EqPresetModel(
            name = "Heavy Metal",
            bands5Db = listOf(4f, 1f, 9f, 3f, 0f),
            bands20Scaled = intArrayOf(50, 45, 40, 35, 15, 10, 15, 15, 20, 30, 35, 40, 37, 30, 30, 30, 25, 20, 15, 15),
        ),
        EqPresetModel(
            name = "Hip Hop",
            bands5Db = listOf(5f, 3f, 0f, 1f, 3f),
            bands20Scaled = intArrayOf(78, 42, 6, -30, -66, -36, -6, -6, -6, -36, -66, -36, -6, 0, 6, 6, 6, 30, 54, 54),
        ),
        EqPresetModel(
            name = "Jazz",
            bands5Db = listOf(4f, 2f, -2f, 2f, 5f),
            bands20Scaled = intArrayOf(12, 12, 12, 12, 12, 0, -12, -12, -12, -36, -60, -30, 0, 6, 12, 12, 12, 12, 12, 12),
        ),
        EqPresetModel(
            name = "Pop",
            bands5Db = listOf(-1f, 2f, 5f, 1f, -2f),
            bands20Scaled = intArrayOf(-20, -2, 17, -38, -92, -44, 5, 17, 29, 29, 29, 23, 17, -14, -44, -14, 17, 23, 29, 29),
        ),
        EqPresetModel(
            name = "Rock",
            bands5Db = listOf(5f, 3f, -1f, 3f, 5f),
            bands20Scaled = intArrayOf(90, 54, 18, -18, -54, -36, -18, -12, -6, -30, -54, -30, -6, -30, -54, -24, 6, 48, 90, 90),
        ),
        EqPresetModel(
            name = "Bass Boost",
            bands5Db = listOf(8f, 6f, 1f, 0f, 0f),
            bands20Scaled = intArrayOf(100, 88, 85, 65, 25, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        ),
        EqPresetModel(
            name = "Movie",
            bands5Db = listOf(4f, 6f, 5f, 7f, 6f),
            bands20Scaled = intArrayOf(30, 30, 61, 85, 90, 70, 61, 61, 50, 80, 35, 35, 80, 100, 80, 75, 70, 65, 60, 55),
        ),
        EqPresetModel(
            name = "Vocal Booster",
            bands5Db = listOf(-2f, -1f, 4f, 4f, 0f),
            bands20Scaled = intArrayOf(-15, -20, -30, -30, -5, 15, 35, 35, 35, 30, 20, 15, 0, 0, -15, -12, -10, -8, -6, -4),
        ),
    )

    // Equalizer state
    var equalizerEnabled by mutableStateOf(false)
        private set
    var selectedPresetName by mutableStateOf("Normal")
        private set
    val bandGainsDb = mutableStateListOf(3f, 0f, 0f, 0f, 3f)
    var reverbPreset by mutableStateOf(ReverbPresetType.NONE)
        private set
    var bassBoostPercent by mutableIntStateOf(0)
        private set
    var virtualizerPercent by mutableIntStateOf(0)
        private set

    // Hardware Dolby Atmos state (from hardware_dolby-16 / LunarisDolby)
    var dolbyEnabled by mutableStateOf(true)
        private set
    var dolbyProfile by mutableStateOf(DolbyProfileType.DYNAMIC)
        private set
    var dolbyIeq by mutableStateOf(DolbyIeqPreset.BALANCED)
        private set
    var dialogueEnhancerEnabled by mutableStateOf(true)
        private set
    var dialogueEnhancerAmount by mutableIntStateOf(6) // 1..12
        private set
    var bassEnhancerEnabled by mutableStateOf(true)
        private set
    var bassCurve by mutableStateOf(DolbyBassCurve.BALANCED)
        private set
    var bassLevel by mutableIntStateOf(40) // 0..100
        private set
    var midLevel by mutableIntStateOf(20) // 0..100
        private set
    var trebleLevel by mutableIntStateOf(30) // 0..100
        private set
    var headphoneVirtualizer by mutableStateOf(true)
        private set
    var speakerVirtualizer by mutableStateOf(true)
        private set
    var stereoWidening by mutableIntStateOf(32) // 0..100
        private set
    var volumeLevelerEnabled by mutableStateOf(false)
        private set

    // Audio Track extras (24074_c798dc.png) & RetroMusic Smooth Audio Fade
    var stereoMode by mutableStateOf(StereoModeType.STEREO)
        private set
    var audioDelayMs by mutableIntStateOf(0)
        private set
    var audioFadeEnabled by mutableStateOf(true)
        private set
    var audioFadeDurationMs by mutableIntStateOf(350)
        private set

    // Player Night Mode & A-B Repeat state (24073_c798dc.jpg)
    var nightModeEnabled by mutableStateOf(false)
        private set
    var nightModeAlpha by mutableFloatStateOf(0.42f)
        private set
    var abRepeatStartMs by mutableStateOf<Long?>(null)
        private set
    var abRepeatEndMs by mutableStateOf<Long?>(null)
        private set

    private var prefs: SharedPreferences? = null
    private var currentAudioSessionId: Int = 0
    private var hardwareDap: HardwareDapEffect? = null
    private var androidEqualizer: Equalizer? = null
    private var androidBassBoost: BassBoost? = null
    private var androidVirtualizer: Virtualizer? = null
    private var androidReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null

    val isHardwareDapSupported: Boolean
        get() = hardwareDap?.isAvailable == true

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        equalizerEnabled = sp.getBoolean("eq_enabled", false)
        selectedPresetName = sp.getString("eq_preset", "Normal") ?: "Normal"
        for (i in 0 until 5) {
            bandGainsDb[i] = sp.getFloat("eq_band_$i", bandGainsDb[i]).coerceIn(-15f, 15f)
        }
        reverbPreset = ReverbPresetType.entries.find {
            it.id.toInt() == sp.getInt("reverb_id", PresetReverb.PRESET_NONE.toInt())
        } ?: ReverbPresetType.NONE
        bassBoostPercent = sp.getInt("bass_boost_pct", 0).coerceIn(0, 100)
        virtualizerPercent = sp.getInt("virt_pct", 0).coerceIn(0, 100)

        dolbyEnabled = sp.getBoolean("dolby_enabled", true)
        dolbyProfile = DolbyProfileType.entries.find {
            it.id == sp.getInt("dolby_profile", 0)
        } ?: DolbyProfileType.DYNAMIC
        dolbyIeq = DolbyIeqPreset.entries.find {
            it.id == sp.getInt("dolby_ieq", 2)
        } ?: DolbyIeqPreset.BALANCED
        dialogueEnhancerEnabled = sp.getBoolean("dolby_dialogue_en", true)
        dialogueEnhancerAmount = sp.getInt("dolby_dialogue_amt", 6).coerceIn(1, 12)
        bassEnhancerEnabled = sp.getBoolean("dolby_bass_en", true)
        bassCurve = DolbyBassCurve.entries.find {
            it.id == sp.getInt("dolby_bass_curve", 0)
        } ?: DolbyBassCurve.BALANCED
        bassLevel = sp.getInt("dolby_bass_lvl", 40).coerceIn(0, 100)
        midLevel = sp.getInt("dolby_mid_lvl", 20).coerceIn(0, 100)
        trebleLevel = sp.getInt("dolby_treble_lvl", 30).coerceIn(0, 100)
        headphoneVirtualizer = sp.getBoolean("dolby_hp_virt", true)
        speakerVirtualizer = sp.getBoolean("dolby_spk_virt", true)
        stereoWidening = sp.getInt("dolby_stereo_wide", 32).coerceIn(0, 100)
        volumeLevelerEnabled = sp.getBoolean("dolby_vol_leveler", false)
        audioFadeEnabled = sp.getBoolean("audio_fade_enabled", true)
    }

    @Synchronized
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAudioSessionId == audioSessionId &&
            (androidEqualizer != null || dynamicsProcessing != null || hardwareDap != null)
        ) {
            applyAllEffects()
            return
        }
        releaseEffects()
        currentAudioSessionId = audioSessionId

        runCatching {
            val dap = HardwareDapEffect(priority = 100, audioSession = audioSessionId)
            if (dap.isAvailable) {
                hardwareDap = dap
            } else {
                dap.release()
                val globalDap = HardwareDapEffect(priority = 100, audioSession = 0)
                if (globalDap.isAvailable) {
                    hardwareDap = globalDap
                } else {
                    globalDap.release()
                }
            }
        }

        runCatching {
            androidEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = equalizerEnabled
            }
        }

        runCatching {
            androidBassBoost = BassBoost(0, audioSessionId).apply {
                enabled = (equalizerEnabled && bassBoostPercent > 0) || (dolbyEnabled && bassEnhancerEnabled)
            }
        }

        runCatching {
            androidVirtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = (equalizerEnabled && virtualizerPercent > 0) ||
                    (dolbyEnabled && (headphoneVirtualizer || speakerVirtualizer))
            }
        }

        runCatching {
            androidReverb = PresetReverb(0, audioSessionId).apply {
                enabled = equalizerEnabled && reverbPreset != ReverbPresetType.NONE
            }
        }

        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val bandCount = 5
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true, bandCount,
                    false, 0,
                    true, bandCount,
                    true,
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config)
            }
        }

        applyAllEffects()
    }

    fun setEqualizerMasterEnabled(enabled: Boolean) {
        equalizerEnabled = enabled
        prefs?.edit()?.putBoolean("eq_enabled", enabled)?.apply()
        applyAllEffects()
    }

    fun selectPreset(preset: EqPresetModel) {
        selectedPresetName = preset.name
        for (i in 0 until 5) {
            bandGainsDb[i] = preset.bands5Db.getOrElse(i) { 0f }
            prefs?.edit()?.putFloat("eq_band_$i", bandGainsDb[i])?.apply()
        }
        prefs?.edit()?.putString("eq_preset", preset.name)?.apply()
        applyAllEffects()
    }

    fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until 5) return
        val clamped = (gainDb * 2f).roundToInt() / 2f // 0.5 dB precision
        bandGainsDb[bandIndex] = clamped.coerceIn(-15f, 15f)
        selectedPresetName = "Custom"
        prefs?.edit()
            ?.putFloat("eq_band_$bandIndex", bandGainsDb[bandIndex])
            ?.putString("eq_preset", "Custom")
            ?.apply()
        applyAllEffects()
    }

    fun resetEqualizerGains() {
        val flat = PRESETS.first { it.name == "Flat" }
        selectPreset(flat)
        setReverb(ReverbPresetType.NONE)
        setBassBoost(0)
        setVirtualizer(0)
    }

    fun setReverb(preset: ReverbPresetType) {
        reverbPreset = preset
        prefs?.edit()?.putInt("reverb_id", preset.id.toInt())?.apply()
        applyAllEffects()
    }

    fun setBassBoost(percent: Int) {
        bassBoostPercent = percent.coerceIn(0, 100)
        prefs?.edit()?.putInt("bass_boost_pct", bassBoostPercent)?.apply()
        applyAllEffects()
    }

    fun setVirtualizer(percent: Int) {
        virtualizerPercent = percent.coerceIn(0, 100)
        prefs?.edit()?.putInt("virt_pct", virtualizerPercent)?.apply()
        applyAllEffects()
    }

    fun setDolbyMasterEnabled(enabled: Boolean) {
        dolbyEnabled = enabled
        prefs?.edit()?.putBoolean("dolby_enabled", enabled)?.apply()
        applyAllEffects()
    }

    fun setDolbyProfileType(profile: DolbyProfileType) {
        dolbyProfile = profile
        prefs?.edit()?.putInt("dolby_profile", profile.id)?.apply()
        when (profile) {
            DolbyProfileType.DYNAMIC -> {
                dolbyIeq = DolbyIeqPreset.BALANCED
                dialogueEnhancerEnabled = true
                dialogueEnhancerAmount = 6
                bassEnhancerEnabled = true
                bassLevel = 45
                midLevel = 25
                trebleLevel = 35
            }
            DolbyProfileType.MOVIE -> {
                dolbyIeq = DolbyIeqPreset.DETAILED
                dialogueEnhancerEnabled = true
                dialogueEnhancerAmount = 9
                bassEnhancerEnabled = true
                bassCurve = DolbyBassCurve.SUB_BOOST
                bassLevel = 65
                midLevel = 35
                trebleLevel = 45
            }
            DolbyProfileType.MUSIC -> {
                dolbyIeq = DolbyIeqPreset.BALANCED
                dialogueEnhancerEnabled = false
                bassEnhancerEnabled = true
                bassCurve = DolbyBassCurve.PUNCH
                bassLevel = 55
                midLevel = 30
                trebleLevel = 50
            }
            DolbyProfileType.GAME -> {
                dolbyIeq = DolbyIeqPreset.DETAILED
                dialogueEnhancerEnabled = true
                dialogueEnhancerAmount = 7
                bassEnhancerEnabled = true
                bassLevel = 60
                stereoWidening = 65
            }
            DolbyProfileType.WORK -> {
                dolbyIeq = DolbyIeqPreset.DETAILED
                dialogueEnhancerEnabled = true
                dialogueEnhancerAmount = 10
                bassEnhancerEnabled = false
                bassLevel = 15
                midLevel = 50
                trebleLevel = 30
            }
            DolbyProfileType.CASUAL -> {
                dolbyIeq = DolbyIeqPreset.BALANCED
                dialogueEnhancerEnabled = true
                dialogueEnhancerAmount = 5
                bassEnhancerEnabled = true
                bassLevel = 35
                midLevel = 25
                trebleLevel = 25
            }
            DolbyProfileType.MOOD -> {
                dolbyIeq = DolbyIeqPreset.WARM
                dialogueEnhancerEnabled = false
                bassEnhancerEnabled = true
                bassCurve = DolbyBassCurve.SUB_BOOST
                bassLevel = 50
                midLevel = 20
                trebleLevel = 15
            }
        }
        applyAllEffects()
    }

    fun setDolbyIeqPreset(ieq: DolbyIeqPreset) {
        dolbyIeq = ieq
        prefs?.edit()?.putInt("dolby_ieq", ieq.id)?.apply()
        applyAllEffects()
    }

    fun setDialogueEnhancer(enabled: Boolean, amount: Int = dialogueEnhancerAmount) {
        dialogueEnhancerEnabled = enabled
        dialogueEnhancerAmount = amount.coerceIn(1, 12)
        prefs?.edit()
            ?.putBoolean("dolby_dialogue_en", dialogueEnhancerEnabled)
            ?.putInt("dolby_dialogue_amt", dialogueEnhancerAmount)
            ?.apply()
        applyAllEffects()
    }

    fun setDolbyBass(enabled: Boolean, level: Int = bassLevel, curve: DolbyBassCurve = bassCurve) {
        bassEnhancerEnabled = enabled
        bassLevel = level.coerceIn(0, 100)
        bassCurve = curve
        prefs?.edit()
            ?.putBoolean("dolby_bass_en", bassEnhancerEnabled)
            ?.putInt("dolby_bass_lvl", bassLevel)
            ?.putInt("dolby_bass_curve", bassCurve.id)
            ?.apply()
        applyAllEffects()
    }

    fun setDolbyMidTreble(mid: Int = midLevel, treble: Int = trebleLevel) {
        midLevel = mid.coerceIn(0, 100)
        trebleLevel = treble.coerceIn(0, 100)
        prefs?.edit()
            ?.putInt("dolby_mid_lvl", midLevel)
            ?.putInt("dolby_treble_lvl", trebleLevel)
            ?.apply()
        applyAllEffects()
    }

    fun setDolbySurround(
        hpVirt: Boolean = headphoneVirtualizer,
        spkVirt: Boolean = speakerVirtualizer,
        widening: Int = stereoWidening,
        volLeveler: Boolean = volumeLevelerEnabled,
    ) {
        headphoneVirtualizer = hpVirt
        speakerVirtualizer = spkVirt
        stereoWidening = widening.coerceIn(0, 100)
        volumeLevelerEnabled = volLeveler
        prefs?.edit()
            ?.putBoolean("dolby_hp_virt", headphoneVirtualizer)
            ?.putBoolean("dolby_spk_virt", speakerVirtualizer)
            ?.putInt("dolby_stereo_wide", stereoWidening)
            ?.putBoolean("dolby_vol_leveler", volumeLevelerEnabled)
            ?.apply()
        applyAllEffects()
    }

    fun resetDolbyDefaults() {
        setDolbyMasterEnabled(true)
        setDolbyProfileType(DolbyProfileType.DYNAMIC)
        setDolbySurround(hpVirt = true, spkVirt = true, widening = 32, volLeveler = false)
    }

    fun setStereoModeType(mode: StereoModeType) {
        stereoMode = mode
        applyAllEffects()
    }

    fun setAudioSyncDelayMs(delayMs: Int) {
        audioDelayMs = delayMs.coerceIn(-5000, 5000)
    }

    fun setAudioFade(enabled: Boolean, durationMs: Int = audioFadeDurationMs) {
        audioFadeEnabled = enabled
        audioFadeDurationMs = durationMs.coerceIn(100, 1500)
        prefs?.edit()?.putBoolean("audio_fade_enabled", audioFadeEnabled)?.apply()
    }

    fun toggleNightMode() {
        nightModeEnabled = !nightModeEnabled
    }

    fun setNightModeDim(alpha: Float) {
        nightModeAlpha = alpha.coerceIn(0.15f, 0.75f)
    }

    fun cycleAbRepeat(currentPositionMs: Long) {
        when {
            abRepeatStartMs == null -> {
                abRepeatStartMs = currentPositionMs.coerceAtLeast(0L)
                abRepeatEndMs = null
            }
            abRepeatEndMs == null -> {
                val start = abRepeatStartMs ?: 0L
                if (currentPositionMs > start + 500L) {
                    abRepeatEndMs = currentPositionMs
                } else {
                    abRepeatEndMs = start + 5000L
                }
            }
            else -> {
                abRepeatStartMs = null
                abRepeatEndMs = null
            }
        }
    }

    fun clearAbRepeat() {
        abRepeatStartMs = null
        abRepeatEndMs = null
    }

    /**
     * RetroMusic-inspired smooth audio volume fade animator for play/pause transitions.
     */
    fun animatePlayerVolumeFade(
        fadeIn: Boolean,
        onVolumeStep: (Float) -> Unit,
        onComplete: () -> Unit = {},
    ) {
        if (!audioFadeEnabled) {
            onVolumeStep(if (fadeIn) 1f else 0f)
            onComplete()
            return
        }
        val start = if (fadeIn) 0.15f else 1f
        val end = if (fadeIn) 1f else 0.15f
        ValueAnimator.ofFloat(start, end).apply {
            duration = audioFadeDurationMs.toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                onVolumeStep(anim.animatedValue as Float)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onVolumeStep(1f)
                    onComplete()
                }
            })
            start()
        }
    }

    @Synchronized
    private fun applyAllEffects() {
        val profileId = dolbyProfile.id

        // 1. Hardware Dolby Atmos DAP (when vendor effect is present on device)
        runCatching {
            hardwareDap?.let { dap ->
                val active = dolbyEnabled || equalizerEnabled
                dap.setEnabled(active)
                if (active) {
                    dap.setProfile(profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.IEQ_PRESET, dolbyIeq.id, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.HEADPHONE_VIRTUALIZER, headphoneVirtualizer, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.SPEAKER_VIRTUALIZER, speakerVirtualizer, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.STEREO_WIDENING_AMOUNT, stereoWidening, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.DIALOGUE_ENHANCER_ENABLE, dialogueEnhancerEnabled, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.DIALOGUE_ENHANCER_AMOUNT, dialogueEnhancerAmount, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.BASS_ENHANCER_ENABLE, bassEnhancerEnabled, profileId)
                    dap.setDapParameter(HardwareDapEffect.DsParam.VOLUME_LEVELER_ENABLE, volumeLevelerEnabled, profileId)

                    val gains20 = IntArray(20) { idx ->
                        val band5Idx = (idx / 4).coerceIn(0, 4)
                        val eqContribution = if (equalizerEnabled) (bandGainsDb[band5Idx] * 10f).roundToInt() else 0
                        val dolbyContribution = if (dolbyEnabled) {
                            when {
                                idx < 5 -> if (bassEnhancerEnabled) (bassLevel * 0.9f).roundToInt() else 0
                                idx in 5..13 -> ((midLevel * 0.6f) + (if (dialogueEnhancerEnabled) dialogueEnhancerAmount * 4 else 0)).roundToInt()
                                else -> (trebleLevel * 0.7f).roundToInt()
                            }
                        } else {
                            0
                        }
                        (eqContribution + dolbyContribution).coerceIn(-150, 150)
                    }
                    dap.setDapParameter(HardwareDapEffect.DsParam.GEQ_BAND_GAINS, gains20, profileId)
                }
            }
        }

        // 2. Standard Android Equalizer (5-band)
        runCatching {
            androidEqualizer?.let { eq ->
                val shouldEnable = equalizerEnabled || dolbyEnabled
                eq.enabled = shouldEnable
                if (shouldEnable) {
                    val range = eq.bandLevelRange
                    val minMb = range.getOrElse(0) { -1500 }.toInt()
                    val maxMb = range.getOrElse(1) { 1500 }.toInt()
                    val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
                    for (b in 0 until numBands) {
                        val eqDb = if (equalizerEnabled) bandGainsDb[b] else 0f
                        val dolbyDb = if (dolbyEnabled) {
                            when (b) {
                                0 -> if (bassEnhancerEnabled) (bassLevel / 100f) * 6f else 0f
                                1 -> if (bassEnhancerEnabled) (bassLevel / 100f) * 3.5f else 0f
                                2 -> (midLevel / 100f) * 4f + (if (dialogueEnhancerEnabled) dialogueEnhancerAmount * 0.35f else 0f)
                                3 -> (if (dialogueEnhancerEnabled) dialogueEnhancerAmount * 0.45f else 0f) + (trebleLevel / 100f) * 3f
                                else -> (trebleLevel / 100f) * 5f
                            }
                        } else {
                            0f
                        }
                        val targetMb = ((eqDb + dolbyDb) * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                        eq.setBandLevel(b.toShort(), targetMb)
                    }
                }
            }
        }

        // 3. BassBoost
        runCatching {
            androidBassBoost?.let { bb ->
                val effectiveStrength = maxOf(
                    if (equalizerEnabled) bassBoostPercent * 10 else 0,
                    if (dolbyEnabled && bassEnhancerEnabled) bassLevel * 10 else 0,
                ).coerceIn(0, 1000)
                bb.enabled = effectiveStrength > 0
                if (bb.strengthSupported && effectiveStrength > 0) {
                    bb.setStrength(effectiveStrength.toShort())
                }
            }
        }

        // 4. Virtualizer
        runCatching {
            androidVirtualizer?.let { virt ->
                val effectiveVirt = maxOf(
                    if (equalizerEnabled) virtualizerPercent * 10 else 0,
                    if (dolbyEnabled && (headphoneVirtualizer || speakerVirtualizer)) (stereoWidening * 10) else 0,
                ).coerceIn(0, 1000)
                virt.enabled = effectiveVirt > 0
                if (virt.strengthSupported && effectiveVirt > 0) {
                    virt.setStrength(effectiveVirt.toShort())
                }
            }
        }

        // 5. PresetReverb
        runCatching {
            androidReverb?.let { rev ->
                val active = equalizerEnabled && reverbPreset != ReverbPresetType.NONE
                rev.enabled = active
                if (active) {
                    rev.preset = reverbPreset.id
                }
            }
        }

        // 6. DynamicsProcessing (multi-band + Volume Leveler + Stereo Channel Mode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val dp = dynamicsProcessing ?: return@runCatching
                val active = equalizerEnabled || dolbyEnabled || stereoMode != StereoModeType.STEREO
                dp.enabled = active
                if (active) {
                    // Stereo mode channel attenuation
                    when (stereoMode) {
                        StereoModeType.STEREO, StereoModeType.MONO -> {
                            dp.setInputGainbyChannel(0, 0f)
                            dp.setInputGainbyChannel(1, 0f)
                        }
                        StereoModeType.LEFT_ONLY -> {
                            dp.setInputGainbyChannel(0, 3f)
                            dp.setInputGainbyChannel(1, -60f)
                        }
                        StereoModeType.RIGHT_ONLY -> {
                            dp.setInputGainbyChannel(0, -60f)
                            dp.setInputGainbyChannel(1, 3f)
                        }
                    }

                    val eq = DynamicsProcessing.Eq(true, true, 5)
                    val cutoffs = floatArrayOf(90f, 320f, 1200f, 4500f, 16000f)
                    for (i in 0 until 5) {
                        val eqDb = if (equalizerEnabled) bandGainsDb[i] else 0f
                        val dolbyDb = if (dolbyEnabled) {
                            when (i) {
                                0 -> if (bassEnhancerEnabled) (bassLevel / 100f) * 6f else 0f
                                1 -> if (bassEnhancerEnabled) (bassLevel / 100f) * 3.5f else 0f
                                2 -> (midLevel / 100f) * 4f + (if (dialogueEnhancerEnabled) dialogueEnhancerAmount * 0.4f else 0f)
                                3 -> (if (dialogueEnhancerEnabled) dialogueEnhancerAmount * 0.5f else 0f) + (trebleLevel / 100f) * 3.5f
                                else -> (trebleLevel / 100f) * 5f
                            }
                        } else {
                            0f
                        }
                        eq.setBand(i, DynamicsProcessing.EqBand(true, cutoffs[i], (eqDb + dolbyDb).coerceIn(-15f, 15f)))
                    }
                    dp.setPreEqAllChannelsTo(eq)
                    dp.setPostEqAllChannelsTo(eq)

                    val limiter = DynamicsProcessing.Limiter(
                        true,
                        volumeLevelerEnabled || dolbyEnabled,
                        0,
                        1.5f,
                        50.0f,
                        if (volumeLevelerEnabled) 12.0f else 6.0f,
                        if (volumeLevelerEnabled) -3.0f else -1.0f,
                        if (volumeLevelerEnabled) 2.5f else 0.0f,
                    )
                    dp.setLimiterAllChannelsTo(limiter)
                }
            }
        }
    }

    @Synchronized
    fun releaseEffects() {
        runCatching { hardwareDap?.release() }
        runCatching { androidEqualizer?.release() }
        runCatching { androidBassBoost?.release() }
        runCatching { androidVirtualizer?.release() }
        runCatching { androidReverb?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { dynamicsProcessing?.release() }
        hardwareDap = null
        androidEqualizer = null
        androidBassBoost = null
        androidVirtualizer = null
        androidReverb = null
        loudnessEnhancer = null
        dynamicsProcessing = null
        currentAudioSessionId = 0
    }
}
