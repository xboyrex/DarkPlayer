package dev.xboyrex.darkplayer.feature.player.service

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AudioBoostProfile(val label: String, val subtitle: String) {
    DOLBY_CINEMA("Dolby Cinema", "3D surround stage + deep sub-bass + vocal clarity"),
    EXTREME_1000("1000% Extreme", "Maximum +30dB pre-amp with brickwall anti-clip limiter"),
    BASS_PUNCH("Bass Punch", "Harmonic sub-bass exciter (60Hz-125Hz) + warm dynamics"),
    CLEAR_VOICE("Dialogue Boost", "Midrange vocal isolation (1kHz-3kHz) + night limiter"),
}

/**
 * 1000% Intelligent Dolby-Style Audio & Bass Booster Engine.
 * Combines hardware LoudnessEnhancer (up to +3000mB), multi-band DynamicsProcessing
 * (5-band psychoacoustic EQ + multi-band compressor + brickwall anti-clip limiter),
 * BassBoost harmonic exciter, and 3D Virtualizer.
 */
object DolbyVolumeBoosterEngine {

    const val MAX_VOLUME_PERCENTAGE = 1000
    const val MAX_GAIN_MILLIBELS = 3000 // +30 dB hardware gain

    var boostPercentage by mutableIntStateOf(100)
        private set

    var dolbyEnabled by mutableStateOf(true)
        private set

    var bassStrength by mutableIntStateOf(65) // 0..100
        private set

    var surroundEnabled by mutableStateOf(true)
        private set

    var intelligentLimiterEnabled by mutableStateOf(true)
        private set

    var activeProfile by mutableStateOf(AudioBoostProfile.DOLBY_CINEMA)
        private set

    private var currentAudioSessionId: Int = 0
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    val isSupported: Boolean
        get() = true

    @Synchronized
    fun attachSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (currentAudioSessionId == audioSessionId && loudnessEnhancer != null) {
            applyAllParameters()
            return
        }
        releaseEffects()
        currentAudioSessionId = audioSessionId

        runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
        }

        runCatching {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = dolbyEnabled
            }
        }

        runCatching {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = dolbyEnabled && surroundEnabled
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val bandCount = 5
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,
                    true, bandCount,
                    true, bandCount,
                    true, bandCount,
                    true,
                ).build()
                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                    enabled = dolbyEnabled
                }
            }
        }

        applyAllParameters()
    }

    @Synchronized
    fun setVolumePercentage(percentage: Int) {
        boostPercentage = percentage.coerceIn(0, MAX_VOLUME_PERCENTAGE)
        applyAllParameters()
    }

    @Synchronized
    fun setGainMillibels(gainMb: Int) {
        val clampedMb = gainMb.coerceIn(0, MAX_GAIN_MILLIBELS)
        boostPercentage = if (clampedMb == 0) {
            boostPercentage.coerceAtMost(100)
        } else {
            100 + ((clampedMb.toFloat() / MAX_GAIN_MILLIBELS) * 900f).toInt()
        }
        applyAllParameters()
    }

    fun getGainMillibels(): Int {
        if (boostPercentage <= 100) return 0
        return (((boostPercentage - 100) / 900f) * MAX_GAIN_MILLIBELS).toInt()
    }

    @Synchronized
    fun setDolbyMode(enabled: Boolean) {
        dolbyEnabled = enabled
        applyAllParameters()
    }

    @Synchronized
    fun setBassBoostStrength(strength: Int) {
        bassStrength = strength.coerceIn(0, 100)
        applyAllParameters()
    }

    @Synchronized
    fun setSurround(enabled: Boolean) {
        surroundEnabled = enabled
        applyAllParameters()
    }

    @Synchronized
    fun setIntelligentLimiter(enabled: Boolean) {
        intelligentLimiterEnabled = enabled
        applyAllParameters()
    }

    @Synchronized
    fun selectProfile(profile: AudioBoostProfile) {
        activeProfile = profile
        dolbyEnabled = true
        when (profile) {
            AudioBoostProfile.DOLBY_CINEMA -> {
                bassStrength = 70
                surroundEnabled = true
                intelligentLimiterEnabled = true
                if (boostPercentage < 200) boostPercentage = 250
            }
            AudioBoostProfile.EXTREME_1000 -> {
                bassStrength = 85
                surroundEnabled = true
                intelligentLimiterEnabled = true
                boostPercentage = 1000
            }
            AudioBoostProfile.BASS_PUNCH -> {
                bassStrength = 100
                surroundEnabled = true
                intelligentLimiterEnabled = true
                if (boostPercentage < 200) boostPercentage = 300
            }
            AudioBoostProfile.CLEAR_VOICE -> {
                bassStrength = 35
                surroundEnabled = false
                intelligentLimiterEnabled = true
                if (boostPercentage < 200) boostPercentage = 250
            }
        }
        applyAllParameters()
    }

    @Synchronized
    private fun applyAllParameters() {
        val gainMb = getGainMillibels()

        runCatching {
            loudnessEnhancer?.let { enhancer ->
                // Add +400mB psychoacoustic presence when Dolby mode is active even at 100%
                val effectiveMb = gainMb + if (dolbyEnabled && boostPercentage >= 100) 450 else 0
                enhancer.setTargetGain(effectiveMb)
                enhancer.enabled = effectiveMb > 0
            }
        }

        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = dolbyEnabled && bassStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength((bassStrength * 10).coerceIn(0, 1000).toShort())
                }
            }
        }

        runCatching {
            virtualizer?.let { virt ->
                virt.enabled = dolbyEnabled && surroundEnabled
                if (virt.strengthSupported) {
                    virt.setStrength(if (surroundEnabled) 850.toShort() else 0.toShort())
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val dp = dynamicsProcessing ?: return@runCatching
                dp.enabled = dolbyEnabled || gainMb > 0

                // Input gain in dB (up to +18dB in DynamicsProcessing in addition to LoudnessEnhancer)
                val extraDb = if (boostPercentage > 100) {
                    ((boostPercentage - 100) / 900f) * 18f
                } else {
                    0f
                }
                dp.setInputGainAllChannelsTo(extraDb)

                // 5-band psychoacoustic EQ
                val eq = DynamicsProcessing.Eq(true, true, 5)
                val bassDb = (bassStrength / 100f) * 8.5f
                val voiceDb = when (activeProfile) {
                    AudioBoostProfile.CLEAR_VOICE -> 7.5f
                    AudioBoostProfile.DOLBY_CINEMA -> 4.5f
                    else -> 3.5f
                }
                val airDb = if (surroundEnabled) 4.0f else 2.0f

                val cutoffs = floatArrayOf(90f, 250f, 1200f, 4000f, 16000f)
                val gains = floatArrayOf(
                    bassDb,
                    bassDb * 0.65f,
                    voiceDb * 0.7f,
                    voiceDb,
                    airDb,
                )
                for (i in 0 until 5) {
                    eq.setBand(i, DynamicsProcessing.EqBand(true, cutoffs[i], gains[i]))
                }
                dp.setPreEqAllChannelsTo(eq)
                dp.setPostEqAllChannelsTo(eq)

                // Intelligent Brickwall Anti-Clip Limiter
                val limiter = DynamicsProcessing.Limiter(
                    true,
                    intelligentLimiterEnabled,
                    0,
                    1.0f, // 1ms fast attack
                    45.0f, // 45ms smooth release
                    18.0f, // 18:1 brickwall ratio
                    -1.2f, // threshold dB
                    0.0f, // 0dB post-gain ceiling
                )
                dp.setLimiterAllChannelsTo(limiter)
            }
        }
    }

    @Synchronized
    fun releaseEffects() {
        runCatching { loudnessEnhancer?.release() }
        runCatching { dynamicsProcessing?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        loudnessEnhancer = null
        dynamicsProcessing = null
        bassBoost = null
        virtualizer = null
        currentAudioSessionId = 0
    }
}
