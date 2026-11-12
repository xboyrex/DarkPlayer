package dev.xboyrex.novaplayer.core.ui.dolby

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Dual-mode Nova Dolby Atmos & Equalizer Engine:
 * - Connects to hardware/vendor Dolby DAP (`NovaDolbyEffect` UUID 9d4921da-8225-4f29-aefa-39537a04bcaa) when present.
 * - Simultaneously drives Android's native `DynamicsProcessing` / `Equalizer` / `BassBoost` / `Virtualizer` DSP
 *   on active playback sessions so every profile, 10/15/20-band EQ curve, bass/mid/treble enhancer,
 *   surround virtualizer, dialogue enhancer, and volume leveler works across all Android devices.
 */
object NovaDolbyController {
    private const val PREF_MAIN = "nova_dolby_prefs"
    private const val PREF_PRESETS = "nova_dolby_presets"

    private var appContext: Context? = null
    private var mainPrefs: SharedPreferences? = null
    private var presetsPrefs: SharedPreferences? = null

    private var hardwareDolby: NovaDolbyEffect? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var fallbackEq: Equalizer? = null
    private var fallbackBass: BassBoost? = null
    private var fallbackVirtualizer: Virtualizer? = null
    private var activeAudioSessionId: Int = 0

    var enabled by mutableStateOf(true)
        private set

    var currentProfile by mutableIntStateOf(0)
        private set

    var bandMode by mutableStateOf(NovaBandMode.TEN_BAND)
        private set

    var profileSettings by mutableStateOf(NovaProfileSettings())
        private set

    var currentPresetName by mutableStateOf("Movie")
        private set

    var bandGains by mutableStateOf<List<NovaBandGain>>(emptyList())
        private set

    var presets by mutableStateOf<List<NovaEqualizerPreset>>(emptyList())
        private set

    var isOnSpeaker by mutableStateOf(true)
        private set

    var deviceStateMemoryEnabled by mutableStateOf(true)
        private set

    var isHardwareDapActive by mutableStateOf(false)
        private set

    private var raw20BandGains = IntArray(20) { 0 }

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        mainPrefs = ctx.getSharedPreferences(PREF_MAIN, Context.MODE_PRIVATE)
        presetsPrefs = ctx.getSharedPreferences(PREF_PRESETS, Context.MODE_PRIVATE)

        enabled = mainPrefs?.getBoolean("dolby_enable", true) ?: true
        currentProfile = mainPrefs?.getInt("dolby_profile", 1) ?: 1
        bandMode = NovaBandMode.fromValue(mainPrefs?.getString("dolby_band_mode", "10") ?: "10")
        deviceStateMemoryEnabled = mainPrefs?.getBoolean("device_state_memory", true) ?: true

        updateSpeakerState()
        initAudioEffects(0)
        loadProfileState(currentProfile)
    }

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == activeAudioSessionId) return
        activeAudioSessionId = audioSessionId
        initAudioEffects(audioSessionId)
        syncAllToEffects()
    }

    private fun initAudioEffects(sessionId: Int) {
        runCatching {
            hardwareDolby?.release()
            hardwareDolby = NovaDolbyEffect(100, 0).also {
                it.dsOn = enabled
                isHardwareDapActive = true
            }
        }.onFailure {
            hardwareDolby = null
            isHardwareDapActive = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && sessionId > 0) {
            runCatching {
                dynamicsProcessing?.release()
                val cfg = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    true,
                    20,
                    true,
                    1,
                    true,
                    20,
                    true,
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, sessionId, cfg).apply {
                    this.enabled = this@NovaDolbyController.enabled
                }
            }
        }

        if (sessionId >= 0) {
            runCatching {
                fallbackEq?.release()
                fallbackEq = Equalizer(0, sessionId).apply {
                    this.enabled = this@NovaDolbyController.enabled
                }
            }
            runCatching {
                fallbackBass?.release()
                fallbackBass = BassBoost(0, sessionId).apply {
                    this.enabled = this@NovaDolbyController.enabled
                }
            }
            runCatching {
                fallbackVirtualizer?.release()
                fallbackVirtualizer = Virtualizer(0, sessionId).apply {
                    this.enabled = this@NovaDolbyController.enabled
                }
            }
        }
    }

    fun updateSpeakerState() {
        val ctx = appContext ?: return
        isOnSpeaker = runCatching {
            val am = ctx.getSystemService(AudioManager::class.java)
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
            val devices = am?.getDevicesForAttributes(attrs).orEmpty()
            devices.firstOrNull()?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }.getOrDefault(true)
    }

    private fun profilePrefs(profile: Int): SharedPreferences? =
        appContext?.getSharedPreferences("nova_dolby_profile_$profile", Context.MODE_PRIVATE)

    private fun loadProfileState(profile: Int) {
        val p = profilePrefs(profile)
        val defaultMovieGains = NovaDolbyPresetsData.BUILTIN_PRESETS_20.find { it.first == "Movie" }?.second
            ?: IntArray(20) { 0 }
        val savedGainsStr = p?.getString("geq_gains_20", null)
        raw20BandGains = if (savedGainsStr != null) {
            val parsed = savedGainsStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toIntArray()
            if (parsed.size == 20) parsed else defaultMovieGains.copyOf()
        } else {
            when (profile) {
                1 -> NovaDolbyPresetsData.BUILTIN_PRESETS_20.first { it.first == "Movie" }.second.copyOf()
                2 -> NovaDolbyPresetsData.BUILTIN_PRESETS_20.first { it.first == "Pop" }.second.copyOf()
                3 -> NovaDolbyPresetsData.BUILTIN_PRESETS_20.first { it.first == "Electronic" }.second.copyOf()
                4 -> NovaDolbyPresetsData.BUILTIN_PRESETS_20.first { it.first == "Vocal Booster" }.second.copyOf()
                else -> IntArray(20) { 0 }
            }
        }

        profileSettings = NovaProfileSettings(
            profile = profile,
            ieqPreset = p?.getInt("ieq_preset", if (profile == 1) 1 else 2) ?: 1,
            headphoneVirtualizerEnabled = p?.getBoolean("hp_virtualizer", true) ?: true,
            speakerVirtualizerEnabled = p?.getBoolean("spk_virtualizer", true) ?: true,
            stereoWideningAmount = p?.getInt("stereo_widening", 36) ?: 36,
            dialogueEnhancerEnabled = p?.getBoolean("dialogue_enabled", profile == 1 || profile == 4) ?: true,
            dialogueEnhancerAmount = p?.getInt("dialogue_amount", 6) ?: 6,
            bassLevel = p?.getInt("bass_level", 40) ?: 40,
            midLevel = p?.getInt("mid_level", 20) ?: 20,
            trebleLevel = p?.getInt("treble_level", 25) ?: 25,
            bassCurve = p?.getInt("bass_curve", 0) ?: 0,
            volumeLevelerEnabled = p?.getBoolean("volume_leveler", false) ?: false,
        )

        rebuildPresetsAndGains()
        syncAllToEffects()
    }

    private fun rebuildPresetsAndGains() {
        bandGains = deserializeGains(raw20BandGains, bandMode)
        val builtIn = NovaDolbyPresetsData.BUILTIN_PRESETS_20.map { (name, arr) ->
            NovaEqualizerPreset(
                name = name,
                bandGains = deserializeGains(arr, bandMode),
                isUserDefined = false,
                bandMode = bandMode,
            )
        }
        val userList = presetsPrefs?.all.orEmpty().mapNotNull { (name, rawVal) ->
            val str = rawVal as? String ?: return@mapNotNull null
            runCatching {
                val parts = str.split("|")
                val mode = if (parts.size > 1) NovaBandMode.fromValue(parts[1]) else NovaBandMode.TEN_BAND
                val arr = parts[0].split(",").map { it.trim().toInt() }.toIntArray()
                NovaEqualizerPreset(
                    name = name,
                    bandGains = deserializeGains(arr, mode),
                    isUserDefined = true,
                    bandMode = mode,
                )
            }.getOrNull()
        }
        val allPresets = builtIn + userList
        val matched = allPresets.find { preset ->
            preset.bandGains.size == bandGains.size &&
                preset.bandGains.zip(bandGains).all { (a, b) -> kotlin.math.abs(a.gain - b.gain) <= 2 }
        }
        currentPresetName = matched?.name ?: "Custom"
        presets = if (matched == null) {
            allPresets + NovaEqualizerPreset(
                name = "Custom",
                bandGains = bandGains,
                isCustom = true,
                bandMode = bandMode,
            )
        } else {
            allPresets
        }
    }

    fun setDolbyEnabled(value: Boolean) {
        enabled = value
        mainPrefs?.edit()?.putBoolean("dolby_enable", value)?.apply()
        syncAllToEffects()
    }

    fun setProfile(profile: Int) {
        currentProfile = profile
        mainPrefs?.edit()?.putInt("dolby_profile", profile)?.apply()
        loadProfileState(profile)
    }

    fun setBandModeSelection(mode: NovaBandMode) {
        bandMode = mode
        mainPrefs?.edit()?.putString("dolby_band_mode", mode.value)?.apply()
        rebuildPresetsAndGains()
    }

    fun setIeqPreset(preset: Int) {
        profileSettings = profileSettings.copy(ieqPreset = preset)
        profilePrefs(currentProfile)?.edit()?.putInt("ieq_preset", preset)?.apply()
        syncAllToEffects()
    }

    fun selectPreset(preset: NovaEqualizerPreset) {
        raw20BandGains = serializeGains(preset.bandGains, preset.bandMode)
        saveCurrent20BandGains()
        rebuildPresetsAndGains()
        syncAllToEffects()
    }

    fun setBandGain(index: Int, gain: Int) {
        val clamped = gain.coerceIn(-150, 150)
        val updated = bandGains.toMutableList()
        if (index in updated.indices) {
            updated[index] = updated[index].copy(gain = clamped)
            bandGains = updated
            raw20BandGains = serializeGains(updated, bandMode)
            saveCurrent20BandGains()
            rebuildPresetsAndGains()
            syncAllToEffects()
        }
    }

    fun resetGains() {
        raw20BandGains = IntArray(20) { 0 }
        saveCurrent20BandGains()
        rebuildPresetsAndGains()
        syncAllToEffects()
    }

    fun saveUserPreset(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Enter a preset name"
        if (trimmed.length > 28) return "Preset name is too long"
        if (presets.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return "Preset name already exists"
        }
        val serialized = raw20BandGains.joinToString(",") + "|${bandMode.value}"
        presetsPrefs?.edit()?.putString(trimmed, serialized)?.apply()
        rebuildPresetsAndGains()
        currentPresetName = trimmed
        return null
    }

    fun deleteUserPreset(preset: NovaEqualizerPreset) {
        if (!preset.isUserDefined) return
        presetsPrefs?.edit()?.remove(preset.name)?.apply()
        resetGains()
    }

    fun exportCurrentPresetJson(): String {
        val gainsStr = raw20BandGains.joinToString(",")
        return """{"name":"$currentPresetName","bandMode":"${bandMode.value}","gains":[$gainsStr]}"""
    }

    fun importPresetJson(raw: String): Boolean {
        return runCatching {
            val gainsMatch = Regex("""\[([0-9,\-\s]+)]""").find(raw)?.groupValues?.get(1) ?: return false
            val nums = gainsMatch.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (nums.size == 20) {
                raw20BandGains = nums.map { it.coerceIn(-150, 150) }.toIntArray()
                saveCurrent20BandGains()
                rebuildPresetsAndGains()
                syncAllToEffects()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun setBassLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prev = profileSettings.bassLevel
        val curve = profileSettings.bassCurve
        applyBassDelta(prev, clamped, curve, curve)
        profileSettings = profileSettings.copy(bassLevel = clamped)
        profilePrefs(currentProfile)?.edit()?.putInt("bass_level", clamped)?.apply()
        syncAllToEffects()
    }

    fun setBassCurve(curve: Int) {
        val prevCurve = profileSettings.bassCurve
        val level = profileSettings.bassLevel
        if (prevCurve == curve) return
        applyBassDelta(level, level, prevCurve, curve)
        profileSettings = profileSettings.copy(bassCurve = curve)
        profilePrefs(currentProfile)?.edit()?.putInt("bass_curve", curve)?.apply()
        syncAllToEffects()
    }

    private fun applyBassDelta(prevLevel: Int, newLevel: Int, prevCurve: Int, newCurve: Int) {
        val modified = raw20BandGains.copyOf()
        if (prevLevel > 0) {
            val weights = NovaDolbyPresetsData.BASS_CURVE_WEIGHTS.getOrElse(prevCurve) { NovaDolbyPresetsData.BASS_CURVE_WEIGHTS[0] }
            for (i in weights.indices) {
                modified[i] = (modified[i] - (prevLevel * 1.4f * weights[i]).toInt()).coerceIn(-150, 150)
            }
        }
        if (newLevel > 0) {
            val weights = NovaDolbyPresetsData.BASS_CURVE_WEIGHTS.getOrElse(newCurve) { NovaDolbyPresetsData.BASS_CURVE_WEIGHTS[0] }
            for (i in weights.indices) {
                modified[i] = (modified[i] + (newLevel * 1.4f * weights[i]).toInt()).coerceIn(-150, 150)
            }
        }
        raw20BandGains = modified
        saveCurrent20BandGains()
        rebuildPresetsAndGains()
    }

    fun setMidLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prev = profileSettings.midLevel
        val modified = raw20BandGains.copyOf()
        for (i in 5..13) {
            modified[i] = (modified[i] - (prev * 1.3f).toInt() + (clamped * 1.3f).toInt()).coerceIn(-150, 150)
        }
        raw20BandGains = modified
        saveCurrent20BandGains()
        profileSettings = profileSettings.copy(midLevel = clamped)
        profilePrefs(currentProfile)?.edit()?.putInt("mid_level", clamped)?.apply()
        rebuildPresetsAndGains()
        syncAllToEffects()
    }

    fun setTrebleLevel(level: Int) {
        val clamped = level.coerceIn(0, 100)
        val prev = profileSettings.trebleLevel
        val modified = raw20BandGains.copyOf()
        for (i in 14..19) {
            modified[i] = (modified[i] - (prev * 1.5f).toInt() + (clamped * 1.5f).toInt()).coerceIn(-150, 150)
        }
        raw20BandGains = modified
        saveCurrent20BandGains()
        profileSettings = profileSettings.copy(trebleLevel = clamped)
        profilePrefs(currentProfile)?.edit()?.putInt("treble_level", clamped)?.apply()
        rebuildPresetsAndGains()
        syncAllToEffects()
    }

    fun setSpeakerVirtualizer(value: Boolean) {
        profileSettings = profileSettings.copy(speakerVirtualizerEnabled = value)
        profilePrefs(currentProfile)?.edit()?.putBoolean("spk_virtualizer", value)?.apply()
        syncAllToEffects()
    }

    fun setHeadphoneVirtualizer(value: Boolean) {
        profileSettings = profileSettings.copy(headphoneVirtualizerEnabled = value)
        profilePrefs(currentProfile)?.edit()?.putBoolean("hp_virtualizer", value)?.apply()
        syncAllToEffects()
    }

    fun setStereoWidening(amount: Int) {
        val clamped = amount.coerceIn(4, 64)
        profileSettings = profileSettings.copy(stereoWideningAmount = clamped)
        profilePrefs(currentProfile)?.edit()?.putInt("stereo_widening", clamped)?.apply()
        syncAllToEffects()
    }

    fun setDialogueEnhancer(value: Boolean) {
        profileSettings = profileSettings.copy(dialogueEnhancerEnabled = value)
        profilePrefs(currentProfile)?.edit()?.putBoolean("dialogue_enabled", value)?.apply()
        syncAllToEffects()
    }

    fun setDialogueEnhancerAmount(amount: Int) {
        val clamped = amount.coerceIn(1, 12)
        profileSettings = profileSettings.copy(dialogueEnhancerAmount = clamped)
        profilePrefs(currentProfile)?.edit()?.putInt("dialogue_amount", clamped)?.apply()
        syncAllToEffects()
    }

    fun setVolumeLeveler(value: Boolean) {
        profileSettings = profileSettings.copy(volumeLevelerEnabled = value)
        profilePrefs(currentProfile)?.edit()?.putBoolean("volume_leveler", value)?.apply()
        syncAllToEffects()
    }

    fun setDeviceStateMemory(value: Boolean) {
        deviceStateMemoryEnabled = value
        mainPrefs?.edit()?.putBoolean("device_state_memory", value)?.apply()
    }

    fun resetAllProfiles() {
        for (id in 0..6) {
            appContext?.deleteSharedPreferences("nova_dolby_profile_$id")
            runCatching { hardwareDolby?.resetProfileSpecificSettings(id) }
        }
        setProfile(1)
    }

    private fun saveCurrent20BandGains() {
        profilePrefs(currentProfile)?.edit()
            ?.putString("geq_gains_20", raw20BandGains.joinToString(","))
            ?.apply()
    }

    private fun syncAllToEffects() {
        val ps = profileSettings
        // 1. Sync to Hardware/Vendor Dolby DAP if available
        runCatching {
            val hw = hardwareDolby
            if (hw != null) {
                hw.dsOn = enabled
                if (enabled) {
                    hw.profile = currentProfile
                    hw.setDapParameter(NovaDsParam.IEQ_PRESET, ps.ieqPreset, currentProfile)
                    hw.setDapParameter(NovaDsParam.HEADPHONE_VIRTUALIZER, ps.headphoneVirtualizerEnabled, currentProfile)
                    hw.setDapParameter(NovaDsParam.SPEAKER_VIRTUALIZER, ps.speakerVirtualizerEnabled, currentProfile)
                    hw.setDapParameter(NovaDsParam.STEREO_WIDENING_AMOUNT, ps.stereoWideningAmount, currentProfile)
                    hw.setDapParameter(NovaDsParam.DIALOGUE_ENHANCER_ENABLE, ps.dialogueEnhancerEnabled, currentProfile)
                    hw.setDapParameter(NovaDsParam.DIALOGUE_ENHANCER_AMOUNT, ps.dialogueEnhancerAmount, currentProfile)
                    hw.setDapParameter(NovaDsParam.BASS_ENHANCER_ENABLE, ps.bassLevel > 0, currentProfile)
                    hw.setDapParameter(NovaDsParam.VOLUME_LEVELER_ENABLE, ps.volumeLevelerEnabled, currentProfile)
                    hw.setDapParameter(NovaDsParam.GEQ_BAND_GAINS, raw20BandGains, currentProfile)
                }
            }
        }

        // 2. Sync to Android 9+ DynamicsProcessing multi-band DSP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val dp = dynamicsProcessing
                if (dp != null) {
                    dp.enabled = enabled
                    if (enabled) {
                        val freqs = NovaDolbyPresetsData.BAND_FREQUENCIES_20
                        val eq = DynamicsProcessing.Eq(true, true, 20)
                        for (i in 0 until 20) {
                            val dialogueBoostDb = if (ps.dialogueEnhancerEnabled && i in 6..12) {
                                ps.dialogueEnhancerAmount * 0.35f
                            } else {
                                0f
                            }
                            val gainDb = (raw20BandGains[i] / 10f) + dialogueBoostDb
                            val band = DynamicsProcessing.EqBand(true, freqs[i].toFloat(), gainDb)
                            eq.setBand(i, band)
                        }
                        dp.setPostEqAllChannelsTo(eq)
                    }
                }
            }
        }

        // 3. Sync to standard Android Equalizer, BassBoost, and Virtualizer
        runCatching {
            fallbackEq?.let { eq ->
                eq.enabled = enabled
                if (enabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    for (b in 0 until numBands) {
                        val mappedIdx = ((b.toFloat() / numBands.coerceAtLeast(1)) * 19).toInt().coerceIn(0, 19)
                        // raw20BandGains is in 0.1 dB; millibels is 0.01 dB -> multiply by 10
                        val targetMb = (raw20BandGains[mappedIdx] * 10).coerceIn(minMb, maxMb).toShort()
                        eq.setBandLevel(b.toShort(), targetMb)
                    }
                }
            }
        }
        runCatching {
            fallbackBass?.let { bb ->
                bb.enabled = enabled && ps.bassLevel > 0
                if (bb.strengthSupported && enabled) {
                    bb.setStrength((ps.bassLevel * 10).coerceIn(0, 1000).toShort())
                }
            }
        }
        runCatching {
            fallbackVirtualizer?.let { virt ->
                val virtActive = if (isOnSpeaker) ps.speakerVirtualizerEnabled else ps.headphoneVirtualizerEnabled
                virt.enabled = enabled && virtActive
                if (virt.strengthSupported && enabled && virtActive) {
                    val strength = ((ps.stereoWideningAmount / 64f) * 1000).toInt().coerceIn(0, 1000).toShort()
                    virt.setStrength(strength)
                }
            }
        }
    }

    private fun deserializeGains(gains: IntArray, mode: NovaBandMode): List<NovaBandGain> {
        val frequencies = when (mode) {
            NovaBandMode.TEN_BAND -> NovaDolbyPresetsData.BAND_FREQUENCIES_10
            NovaBandMode.FIFTEEN_BAND -> NovaDolbyPresetsData.BAND_FREQUENCIES_15
            NovaBandMode.TWENTY_BAND -> NovaDolbyPresetsData.BAND_FREQUENCIES_20
        }
        val indices = when (mode) {
            NovaBandMode.TEN_BAND -> NovaDolbyPresetsData.TEN_BAND_INDICES
            NovaBandMode.FIFTEEN_BAND -> NovaDolbyPresetsData.FIFTEEN_BAND_INDICES
            NovaBandMode.TWENTY_BAND -> (0..19).toList()
        }
        return frequencies.mapIndexed { index, freq ->
            val gainIndex = indices.getOrNull(index) ?: index
            NovaBandGain(frequency = freq, gain = gains.getOrElse(gainIndex) { 0 })
        }
    }

    private fun serializeGains(bandGains: List<NovaBandGain>, mode: NovaBandMode): IntArray {
        val result = IntArray(20) { 0 }
        when (mode) {
            NovaBandMode.TEN_BAND -> {
                NovaDolbyPresetsData.TEN_BAND_INDICES.forEachIndexed { index, targetIndex ->
                    if (index < bandGains.size && targetIndex < 20) {
                        result[targetIndex] = bandGains[index].gain
                    }
                }
                for (i in 0 until 19 step 2) {
                    if (i + 2 < 20) {
                        result[i + 1] = (result[i] + result[i + 2]) / 2
                    }
                }
                result[19] = result[18]
            }
            NovaBandMode.FIFTEEN_BAND -> {
                NovaDolbyPresetsData.FIFTEEN_BAND_INDICES.forEachIndexed { index, targetIndex ->
                    if (index < bandGains.size && targetIndex < 20) {
                        result[targetIndex] = bandGains[index].gain
                    }
                }
                val missing = (0..19).filter { it !in NovaDolbyPresetsData.FIFTEEN_BAND_INDICES }
                missing.forEach { idx ->
                    val prev = NovaDolbyPresetsData.FIFTEEN_BAND_INDICES.filter { it < idx }.maxOrNull() ?: 0
                    val next = NovaDolbyPresetsData.FIFTEEN_BAND_INDICES.filter { it > idx }.minOrNull() ?: 19
                    if (prev < idx && next > idx && prev < 20 && next < 20) {
                        val ratio = (idx - prev).toFloat() / (next - prev)
                        result[idx] = (result[prev] + ratio * (result[next] - result[prev])).toInt()
                    } else if (prev < 20) {
                        result[idx] = result[prev]
                    }
                }
            }
            NovaBandMode.TWENTY_BAND -> {
                bandGains.forEachIndexed { index, bandGain ->
                    if (index < 20) {
                        result[index] = bandGain.gain
                    }
                }
            }
        }
        return result
    }
}
