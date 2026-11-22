package com.mayank.darkplayer.feature.player.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HdrRenderMode(val label: String) {
    AUTO("HDR"),
    HDR("HDR"),
    SDR("SDR"),
}

/**
 * Hardware HDR capability detector and live HDR/SDR window color-mode controller.
 * Probes Display.HdrCapabilities and MediaCodecList (HEVC/VP9/AV1 10-bit profiles)
 * and switches ActivityInfo.COLOR_MODE_HDR vs COLOR_MODE_DEFAULT safely.
 */
object HdrSupportManager {

    @Volatile
    private var initialized = false

    @Volatile
    private var displaySupportsHdr = false

    @Volatile
    private var decoderSupportsHdr = false

    private val _mode = MutableStateFlow(HdrRenderMode.AUTO)
    val mode: StateFlow<HdrRenderMode> = _mode.asStateFlow()

    val isHardwareHdrSupported: Boolean
        get() = displaySupportsHdr && decoderSupportsHdr

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {
            val wm = context.getSystemService(WindowManager::class.java)
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display ?: wm?.defaultDisplay
            } else {
                wm?.defaultDisplay
            }
            displaySupportsHdr = display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
            if (displaySupportsHdr) {
                decoderSupportsHdr = detectHdrDecoders()
            }
        }
    }

    fun toggleHdrSdr(context: Context): HdrRenderMode {
        init(context)
        val next = when (_mode.value) {
            HdrRenderMode.SDR -> HdrRenderMode.HDR
            HdrRenderMode.HDR, HdrRenderMode.AUTO -> HdrRenderMode.SDR
        }
        setMode(context, next)
        return next
    }

    fun setMode(context: Context, newMode: HdrRenderMode) {
        init(context)
        _mode.value = newMode
        applyToWindow(context, newMode)
    }

    fun applyToWindow(context: Context, targetMode: HdrRenderMode = _mode.value) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val activity = context.findActivity() ?: return
        val enableHdr = when (targetMode) {
            HdrRenderMode.HDR -> true
            HdrRenderMode.SDR -> false
            HdrRenderMode.AUTO -> isHardwareHdrSupported
        }
        val desiredColorMode = if (enableHdr && isHardwareHdrSupported) {
            ActivityInfo.COLOR_MODE_HDR
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }
        runCatching {
            activity.window.colorMode = desiredColorMode
        }
    }

    private fun detectHdrDecoders(): Boolean {
        val codecs = runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        }.getOrDefault(emptyArray())

        for (codec in codecs) {
            if (codec.isEncoder) continue
            for (type in codec.supportedTypes) {
                val profiles = runCatching {
                    codec.getCapabilitiesForType(type).profileLevels.map { it.profile }.toSet()
                }.getOrDefault(emptySet())
                when (type.lowercase()) {
                    "video/hevc" -> if (profiles.any { it in HEVC_HDR_PROFILES }) return true
                    "video/x-vnd.on2.vp9" -> if (profiles.any { it in VP9_HDR_PROFILES }) return true
                    "video/av01" -> if (profiles.any { it in AV1_HDR_PROFILES }) return true
                }
            }
        }
        return false
    }

    private val HEVC_HDR_PROFILES = setOf(
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus,
    )

    private val VP9_HDR_PROFILES = setOf(
        MediaCodecInfo.CodecProfileLevel.VP9Profile2,
        MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR,
        MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus,
    )

    private val AV1_HDR_PROFILES = buildSet {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10)
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10)
            add(MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus)
        } else {
            add(2)
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
