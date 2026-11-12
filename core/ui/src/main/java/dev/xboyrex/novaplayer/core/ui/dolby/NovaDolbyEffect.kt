package dev.xboyrex.novaplayer.core.ui.dolby

import android.media.audiofx.AudioEffect
import java.util.UUID

enum class NovaDsParam(val id: Int, val length: Int = 1) {
    HEADPHONE_VIRTUALIZER(101),
    SPEAKER_VIRTUALIZER(102),
    VOLUME_LEVELER_ENABLE(103),
    IEQ_PRESET(104),
    DIALOGUE_ENHANCER_ENABLE(105),
    DIALOGUE_ENHANCER_AMOUNT(108),
    GEQ_BAND_GAINS(110, 20),
    BASS_ENHANCER_ENABLE(111),
    STEREO_WIDENING_AMOUNT(113);
}

/**
 * Hardware/Vendor Dolby Audio Processing (DAP) wrapper that uses reflection to invoke hidden
 * AOSP AudioEffect constructors and parameter methods, allowing clean compilation against the
 * standard public Android SDK while maintaining full hardware/vendor DAP control at runtime.
 */
class NovaDolbyEffect(priority: Int, audioSession: Int) {

    private val audioEffect: AudioEffect
    private val setParamMethod = AudioEffect::class.java.getMethod(
        "setParameter",
        Int::class.javaPrimitiveType,
        ByteArray::class.java,
    )
    private val getParamMethod = AudioEffect::class.java.getMethod(
        "getParameter",
        Int::class.javaPrimitiveType,
        ByteArray::class.java,
    )

    init {
        val effectTypeNull = runCatching {
            AudioEffect::class.java.getField("EFFECT_TYPE_NULL").get(null) as UUID
        }.getOrElse {
            UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210")
        }
        val constructor = AudioEffect::class.java.getConstructor(
            UUID::class.java,
            UUID::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
        )
        audioEffect = constructor.newInstance(effectTypeNull, EFFECT_TYPE_DAP, priority, audioSession)
    }

    var enabled: Boolean
        get() = audioEffect.enabled
        set(value) {
            audioEffect.enabled = value
        }

    fun hasControl(): Boolean = audioEffect.hasControl()

    fun release() {
        audioEffect.release()
    }

    private fun checkStatus(status: Int) {
        if (status != AudioEffect.SUCCESS) {
            throw IllegalStateException("NovaDolbyEffect parameter operation failed with status $status")
        }
    }

    private fun rawSetParameter(param: Int, value: ByteArray): Int {
        return (setParamMethod.invoke(audioEffect, param, value) as? Int) ?: AudioEffect.ERROR
    }

    private fun rawGetParameter(param: Int, value: ByteArray): Int {
        return (getParamMethod.invoke(audioEffect, param, value) as? Int) ?: AudioEffect.ERROR
    }

    var dsOn: Boolean
        get() = getIntParam(EFFECT_PARAM_ENABLE) == 1
        set(value) {
            setIntParam(EFFECT_PARAM_ENABLE, if (value) 1 else 0)
            enabled = value
        }

    var profile: Int
        get() = getIntParam(EFFECT_PARAM_PROFILE)
        set(value) {
            setIntParam(EFFECT_PARAM_PROFILE, value)
        }

    private fun setIntParam(param: Int, value: Int) {
        val buf = ByteArray(12)
        int32ToByteArray(param, buf, 0)
        int32ToByteArray(1, buf, 4)
        int32ToByteArray(value, buf, 8)
        checkStatus(rawSetParameter(EFFECT_PARAM_CPDP_VALUES, buf))
    }

    private fun getIntParam(param: Int): Int {
        val buf = ByteArray(12)
        int32ToByteArray(param, buf, 0)
        checkStatus(rawGetParameter(EFFECT_PARAM_CPDP_VALUES + param, buf))
        return byteArrayToInt32(buf)
    }

    fun resetProfileSpecificSettings(profile: Int = this.profile) {
        setIntParam(EFFECT_PARAM_RESET_PROFILE_SETTINGS, profile)
    }

    fun setDapParameter(param: NovaDsParam, values: IntArray, profile: Int = this.profile) {
        val length = values.size
        val buf = ByteArray((length + 4) * 4)
        int32ToByteArray(EFFECT_PARAM_SET_PROFILE_PARAMETER, buf, 0)
        int32ToByteArray(length + 1, buf, 4)
        int32ToByteArray(profile, buf, 8)
        int32ToByteArray(param.id, buf, 12)
        int32ArrayToByteArray(values, buf, 16)
        checkStatus(rawSetParameter(EFFECT_PARAM_CPDP_VALUES, buf))
    }

    fun setDapParameter(param: NovaDsParam, enable: Boolean, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(if (enable) 1 else 0), profile)

    fun setDapParameter(param: NovaDsParam, value: Int, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(value), profile)

    fun getDapParameter(param: NovaDsParam, profile: Int = this.profile): IntArray {
        val length = param.length
        val buf = ByteArray((length + 2) * 4)
        val p = (param.id shl 16) + (profile shl 8) + EFFECT_PARAM_GET_PROFILE_PARAMETER
        checkStatus(rawGetParameter(p, buf))
        return byteArrayToInt32Array(buf, length)
    }

    companion object {
        private val EFFECT_TYPE_DAP = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa")

        private const val EFFECT_PARAM_ENABLE = 0
        private const val EFFECT_PARAM_CPDP_VALUES = 5
        private const val EFFECT_PARAM_PROFILE = 0xA000000
        private const val EFFECT_PARAM_SET_PROFILE_PARAMETER = 0x1000000
        private const val EFFECT_PARAM_GET_PROFILE_PARAMETER = 0x1000005
        private const val EFFECT_PARAM_RESET_PROFILE_SETTINGS = 0xC000000

        private fun int32ToByteArray(value: Int, dst: ByteArray, index: Int) {
            var idx = index
            dst[idx++] = (value and 0xff).toByte()
            dst[idx++] = ((value ushr 8) and 0xff).toByte()
            dst[idx++] = ((value ushr 16) and 0xff).toByte()
            dst[idx] = ((value ushr 24) and 0xff).toByte()
        }

        private fun byteArrayToInt32(ba: ByteArray): Int {
            return ((ba[3].toInt() and 0xff) shl 24) or
                ((ba[2].toInt() and 0xff) shl 16) or
                ((ba[1].toInt() and 0xff) shl 8) or
                (ba[0].toInt() and 0xff)
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

        private fun byteArrayToInt32Array(ba: ByteArray, dstLength: Int): IntArray {
            val srcLength = ba.size shr 2
            val dst = IntArray(dstLength.coerceAtMost(srcLength))
            for (i in dst.indices) {
                dst[i] = ((ba[i * 4 + 3].toInt() and 0xff) shl 24) or
                    ((ba[i * 4 + 2].toInt() and 0xff) shl 16) or
                    ((ba[i * 4 + 1].toInt() and 0xff) shl 8) or
                    (ba[i * 4].toInt() and 0xff)
            }
            return dst
        }
    }
}
