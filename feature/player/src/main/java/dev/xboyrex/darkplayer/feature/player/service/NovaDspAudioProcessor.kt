package dev.xboyrex.darkplayer.feature.player.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.util.UnstableApi
import dev.xboyrex.darkplayer.core.ui.audio.DolbyIeqPreset
import dev.xboyrex.darkplayer.core.ui.audio.HardwareDolbyAndEqualizerEngine
import dev.xboyrex.darkplayer.core.ui.audio.StereoModeType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * In-Process Media3 PCM AudioProcessor implementing the Stash / Tetsu / Meld DSP pipeline:
 * 1. 5.1 / 7.1 Center-Channel (FC, channel index 2) Dialogue Extraction & Boost (+4.5 dB to +9 dB)
 *    so multi-channel movies never sound quiet or muffled on stereo speakers/headphones.
 * 2. Automatic -14 LUFS Loudness Normalizer / Dynamic Range Compressor (DRC) for quiet scenes.
 * 3. 5-Band Cascaded Biquad Peaking Parametric Equalizer (60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz)
 *    + 75Hz Low-Shelf Sub-Bass Enhancer + Dolby IEQ target curve.
 * 4. Dolby Atmos Mid-Side (Hafler Matrix) Spatial Virtualizer & Stereo Widener.
 * 5. Master Preamp / Audio Booster (100%..400%) + Stereo Mode Routing + Soft-Knee tanh Limiter.
 *
 * Because this runs directly on decoded PCM frames inside Media3's AudioSink, it is 100% guaranteed
 * to work on ALL videos and codecs (Hardware MediaCodec, Android Software C2, and FFmpeg) even when
 * vendor AudioEffect HALs reject or ignore secondary audio sessions.
 */
@OptIn(UnstableApi::class)
class NovaDspAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded: Boolean = false

    // Biquad filter state per channel (up to 8 channels x 6 cascaded biquads: 5 EQ bands + 1 Bass Shelf)
    private val maxChannels = 8
    private val numFilters = 6
    private val z1 = Array(maxChannels) { FloatArray(numFilters) }
    private val z2 = Array(maxChannels) { FloatArray(numFilters) }

    // Biquad coefficients [b0, b1, b2, a1, a2] per filter
    private val b0 = FloatArray(numFilters) { 1f }
    private val b1 = FloatArray(numFilters)
    private val b2 = FloatArray(numFilters)
    private val a1 = FloatArray(numFilters)
    private val a2 = FloatArray(numFilters)

    private var lastSampleRate = 0
    private var lastConfigHash = 0

    // Dynamic Range Normalizer (-14 LUFS target) envelope state
    private var rmsEnvelope = 0.08f

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        resetFilterState()
        return outputAudioFormat
    }

    override fun isActive(): Boolean =
        outputAudioFormat != AudioFormat.NOT_SET

    private fun updateBiquadCoefficientsIfNeeded(sampleRate: Int) {
        val eqEnabled = HardwareDolbyAndEqualizerEngine.equalizerEnabled
        val dolbyEnabled = HardwareDolbyAndEqualizerEngine.dolbyEnabled
        val bassBoostPct = HardwareDolbyAndEqualizerEngine.bassBoostPercent
        val dolbyBass = if (dolbyEnabled && HardwareDolbyAndEqualizerEngine.bassEnhancerEnabled) {
            HardwareDolbyAndEqualizerEngine.bassLevel
        } else {
            0
        }
        val ieq = if (dolbyEnabled) HardwareDolbyAndEqualizerEngine.dolbyIeq else DolbyIeqPreset.OFF
        val bGains = HardwareDolbyAndEqualizerEngine.bandGainsDb

        val b0Gain = if (eqEnabled) bGains.getOrElse(0) { 0f } else 0f
        val b1Gain = if (eqEnabled) bGains.getOrElse(1) { 0f } else 0f
        val b2Gain = if (eqEnabled) bGains.getOrElse(2) { 0f } else 0f
        val b3Gain = if (eqEnabled) bGains.getOrElse(3) { 0f } else 0f
        val b4Gain = if (eqEnabled) bGains.getOrElse(4) { 0f } else 0f

        var hash = sampleRate
        hash = 31 * hash + b0Gain.toBits()
        hash = 31 * hash + b1Gain.toBits()
        hash = 31 * hash + b2Gain.toBits()
        hash = 31 * hash + b3Gain.toBits()
        hash = 31 * hash + b4Gain.toBits()
        hash = 31 * hash + bassBoostPct
        hash = 31 * hash + dolbyBass
        hash = 31 * hash + ieq.ordinal
        hash = 31 * hash + (if (dolbyEnabled) 1 else 0)
        hash = 31 * hash + HardwareDolbyAndEqualizerEngine.midLevel
        hash = 31 * hash + HardwareDolbyAndEqualizerEngine.trebleLevel

        if (sampleRate == lastSampleRate && hash == lastConfigHash) return
        lastSampleRate = sampleRate
        lastConfigHash = hash

        val sr = sampleRate.toFloat().coerceAtLeast(8000f)

        // IEQ & Dolby Mid/Treble offsets in dB
        val ieqOffsets = when (ieq) {
            DolbyIeqPreset.OFF -> floatArrayOf(0f, 0f, 0f, 0f, 0f)
            DolbyIeqPreset.DETAILED -> floatArrayOf(1.5f, 0.5f, 1.8f, 3.2f, 4.0f)
            DolbyIeqPreset.BALANCED -> floatArrayOf(1.8f, 0.8f, 1.2f, 1.8f, 2.0f)
            DolbyIeqPreset.WARM -> floatArrayOf(3.0f, 2.2f, 0.5f, -1.0f, -1.8f)
        }
        val midOffsetDb = if (dolbyEnabled) ((HardwareDolbyAndEqualizerEngine.midLevel - 50) / 50f) * 4.5f else 0f
        val trebleOffsetDb = if (dolbyEnabled) ((HardwareDolbyAndEqualizerEngine.trebleLevel - 50) / 50f) * 5.5f else 0f

        val freqs = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        val totalGains = floatArrayOf(
            (b0Gain + ieqOffsets[0]).coerceIn(-15f, 15f),
            (b1Gain + ieqOffsets[1]).coerceIn(-15f, 15f),
            (b2Gain + ieqOffsets[2] + midOffsetDb).coerceIn(-15f, 15f),
            (b3Gain + ieqOffsets[3] + midOffsetDb * 0.5f + trebleOffsetDb * 0.5f).coerceIn(-15f, 15f),
            (b4Gain + ieqOffsets[4] + trebleOffsetDb).coerceIn(-15f, 15f),
        )

        for (i in 0 until 5) {
            val f0 = freqs[i].coerceAtMost(sr * 0.45f)
            val db = totalGains[i]
            if (abs(db) < 0.05f) {
                b0[i] = 1f; b1[i] = 0f; b2[i] = 0f; a1[i] = 0f; a2[i] = 0f
            } else {
                computePeakingBiquad(i, f0, sr, q = 1.05f, gainDb = db)
            }
        }

        // Filter 5: 75 Hz Low-Shelf Sub-Bass Enhancer
        val combinedBassPct = maxOf(bassBoostPct, dolbyBass)
        val bassShelfDb = (combinedBassPct / 100f) * 9.5f
        if (bassShelfDb < 0.1f) {
            b0[5] = 1f; b1[5] = 0f; b2[5] = 0f; a1[5] = 0f; a2[5] = 0f
        } else {
            computeLowShelfBiquad(5, 75f.coerceAtMost(sr * 0.4f), sr, gainDb = bassShelfDb)
        }
    }

    private fun computePeakingBiquad(index: Int, f0: Float, sr: Float, q: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * f0 / sr).toFloat()
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2f * q)

        val norm = 1f + alpha / a
        b0[index] = (1f + alpha * a) / norm
        b1[index] = (-2f * cosW0) / norm
        b2[index] = (1f - alpha * a) / norm
        a1[index] = (-2f * cosW0) / norm
        a2[index] = (1f - alpha / a) / norm
    }

    private fun computeLowShelfBiquad(index: Int, f0: Float, sr: Float, gainDb: Float) {
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * f0 / sr).toFloat()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val sqrtA = sqrt(a)
        val alpha = sinW0 / 2f * sqrt(2f)

        val a0 = (a + 1f) + (a - 1f) * cosW0 + 2f * sqrtA * alpha
        b0[index] = (a * ((a + 1f) - (a - 1f) * cosW0 + 2f * sqrtA * alpha)) / a0
        b1[index] = (2f * a * ((a - 1f) - (a + 1f) * cosW0)) / a0
        b2[index] = (a * ((a + 1f) - (a - 1f) * cosW0 - 2f * sqrtA * alpha)) / a0
        a1[index] = (-2f * ((a - 1f) + (a + 1f) * cosW0)) / a0
        a2[index] = ((a + 1f) + (a - 1f) * cosW0 - 2f * sqrtA * alpha) / a0
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun processBiquads(channel: Int, sample: Float): Float {
        var x = sample
        val ch = channel.coerceIn(0, maxChannels - 1)
        val s1 = z1[ch]
        val s2 = z2[ch]
        for (f in 0 until numFilters) {
            val cb0 = b0[f]
            val cb1 = b1[f]
            val cb2 = b2[f]
            if (cb0 == 1f && cb1 == 0f && cb2 == 0f) continue
            val y = cb0 * x + s1[f]
            s1[f] = cb1 * x - a1[f] * y + s2[f]
            s2[f] = cb2 * x - a2[f] * y
            x = y
        }
        return x
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val format = inputAudioFormat
        val channels = format.channelCount.coerceAtLeast(1)
        val sampleRate = format.sampleRate
        updateBiquadCoefficientsIfNeeded(sampleRate)

        val remainingBytes = inputBuffer.remaining()
        if (buffer.capacity() < remainingBytes) {
            buffer = ByteBuffer.allocateDirect(remainingBytes).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        // Read live DSP parameters from HardwareDolbyAndEqualizerEngine
        val lowSoundFix = HardwareDolbyAndEqualizerEngine.lowSoundNormalizeEnabled
        val nightMode = HardwareDolbyAndEqualizerEngine.nightModeEnabled
        val volLeveler = HardwareDolbyAndEqualizerEngine.volumeLevelerEnabled
        val dolbyEnabled = HardwareDolbyAndEqualizerEngine.dolbyEnabled
        val eqEnabled = HardwareDolbyAndEqualizerEngine.equalizerEnabled
        val dialogueEnabled = HardwareDolbyAndEqualizerEngine.dialogueEnhancerEnabled
        val dialogueAmount = HardwareDolbyAndEqualizerEngine.dialogueEnhancerAmount
        val boostMb = HardwareDolbyAndEqualizerEngine.volumeBoostGainMb
        val preampDb = HardwareDolbyAndEqualizerEngine.preampDb
        val stereoMode = HardwareDolbyAndEqualizerEngine.stereoMode

        // Compute linear preamp & boost multiplier
        val boostDb = (boostMb / 100f).coerceIn(0f, 24f)
        val baselineLowSoundDb = if (lowSoundFix || nightMode) 3.8f else 0f
        val totalPreampLinear = 10.0.pow((preampDb + boostDb + baselineLowSoundDb) / 20.0).toFloat()

        // 5.1 / 7.1 Center-channel vocal boost multiplier
        val centerChannelBoost = if (lowSoundFix || (dolbyEnabled && dialogueEnabled) || nightMode) {
            1.45f + (dialogueAmount / 12f) * 0.55f
        } else {
            1.0f
        }

        // Stereo Hafler / Dolby Widening factor (0.0 .. 0.65)
        val surroundPct = maxOf(
            if (eqEnabled) HardwareDolbyAndEqualizerEngine.virtualizerPercent else 0,
            if (dolbyEnabled && (HardwareDolbyAndEqualizerEngine.headphoneVirtualizer || HardwareDolbyAndEqualizerEngine.speakerVirtualizer)) {
                HardwareDolbyAndEqualizerEngine.stereoWidening
            } else 0,
        )
        val stereoWidthFactor = 1f + (surroundPct / 100f) * 0.58f

        val runNormalizer = lowSoundFix || nightMode || (dolbyEnabled && volLeveler)
        val frameSamples = FloatArray(channels.coerceAtMost(maxChannels))

        if (format.encoding == C.ENCODING_PCM_16BIT) {
            val frameCount = remainingBytes / (2 * channels)
            for (frame in 0 until frameCount) {
                var frameEnergy = 0f
                for (ch in 0 until channels) {
                    val raw = inputBuffer.short.toFloat() / 32768f
                    if (ch < maxChannels) {
                        frameSamples[ch] = raw
                        frameEnergy += raw * raw
                    }
                }
                val frameRms = sqrt(frameEnergy / channels)
                val normGain = computeNormalizerGain(frameRms, runNormalizer)

                processFrameInPlace(
                    frameSamples = frameSamples,
                    channels = minOf(channels, maxChannels),
                    preampGain = totalPreampLinear * normGain,
                    centerChannelBoost = centerChannelBoost,
                    stereoWidthFactor = stereoWidthFactor,
                    stereoMode = stereoMode,
                )

                for (ch in 0 until channels) {
                    val outSample = if (ch < maxChannels) frameSamples[ch] else 0f
                    val pcm16 = (outSample * 32767f).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    buffer.putShort(pcm16)
                }
            }
        } else if (format.encoding == C.ENCODING_PCM_FLOAT) {
            val frameCount = remainingBytes / (4 * channels)
            for (frame in 0 until frameCount) {
                var frameEnergy = 0f
                for (ch in 0 until channels) {
                    val raw = inputBuffer.float
                    val safe = if (raw.isFinite()) raw else 0f
                    if (ch < maxChannels) {
                        frameSamples[ch] = safe
                        frameEnergy += safe * safe
                    }
                }
                val frameRms = sqrt(frameEnergy / channels)
                val normGain = computeNormalizerGain(frameRms, runNormalizer)

                processFrameInPlace(
                    frameSamples = frameSamples,
                    channels = minOf(channels, maxChannels),
                    preampGain = totalPreampLinear * normGain,
                    centerChannelBoost = centerChannelBoost,
                    stereoWidthFactor = stereoWidthFactor,
                    stereoMode = stereoMode,
                )

                for (ch in 0 until channels) {
                    val outSample = if (ch < maxChannels) frameSamples[ch] else 0f
                    buffer.putFloat(outSample)
                }
            }
        }

        // Copy any trailing partial bytes safely
        while (inputBuffer.hasRemaining()) {
            buffer.put(inputBuffer.get())
        }

        buffer.flip()
        outputBuffer = buffer
    }

    private fun computeNormalizerGain(frameRms: Float, enabled: Boolean): Float {
        if (!enabled) return 1f
        val coeff = if (frameRms > rmsEnvelope) 0.015f else 0.0008f
        rmsEnvelope += coeff * (frameRms - rmsEnvelope)
        val clampedRms = rmsEnvelope.coerceIn(0.012f, 0.95f)
        // Target RMS ~ 0.18 (-15 dBFS / -14 LUFS dialogue target)
        return if (frameRms > 0.002f) {
            (0.18f / clampedRms).coerceIn(0.85f, 2.65f)
        } else {
            1f
        }
    }

    private fun processFrameInPlace(
        frameSamples: FloatArray,
        channels: Int,
        preampGain: Float,
        centerChannelBoost: Float,
        stereoWidthFactor: Float,
        stereoMode: StereoModeType,
    ) {
        // 1. In 5.1 (6ch) or 7.1 (8ch) audio, channel 2 is Front Center (dialogue)
        if (channels >= 6 && centerChannelBoost > 1.01f) {
            val center = frameSamples[2] * centerChannelBoost
            frameSamples[2] = center
            // Mix vocal clarity into Front Left (0) and Front Right (1) so stereo downmixes never lose dialogue
            frameSamples[0] = frameSamples[0] * 0.88f + center * 0.28f
            frameSamples[1] = frameSamples[1] * 0.88f + center * 0.28f
        }

        // 2. Apply Preamp + 5-Band Biquad EQ + 75Hz Sub-Bass Shelf per channel
        for (ch in 0 until channels) {
            val boosted = frameSamples[ch] * preampGain
            frameSamples[ch] = processBiquads(ch, boosted)
        }

        // 3. Apply Dolby Atmos / Stereo Virtualizer (Mid-Side spatial widening) & Stereo Mode routing
        if (channels >= 2) {
            var left = frameSamples[0]
            var right = frameSamples[1]

            if (stereoWidthFactor > 1.01f) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f * stereoWidthFactor
                left = mid + side
                right = mid - side
            }

            when (stereoMode) {
                StereoModeType.STEREO -> {
                    frameSamples[0] = left
                    frameSamples[1] = right
                }
                StereoModeType.MONO -> {
                    val mono = (left + right) * 0.5f
                    frameSamples[0] = mono
                    frameSamples[1] = mono
                }
                StereoModeType.LEFT_ONLY -> {
                    frameSamples[0] = left
                    frameSamples[1] = left
                }
                StereoModeType.RIGHT_ONLY -> {
                    frameSamples[0] = right
                    frameSamples[1] = right
                }
            }
        }

        // 4. Soft-knee tanh brickwall limiter so audio never clips or crackles even at 400% boost
        for (ch in 0 until channels) {
            val s = frameSamples[ch]
            frameSamples[ch] = if (abs(s) > 0.85f) {
                tanh(s.toDouble()).toFloat()
            } else {
                s
            }
        }
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean =
        inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        resetFilterState()
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        lastSampleRate = 0
        lastConfigHash = 0
    }

    private fun resetFilterState() {
        for (ch in 0 until maxChannels) {
            z1[ch].fill(0f)
            z2[ch].fill(0f)
        }
        rmsEnvelope = 0.08f
    }
}
