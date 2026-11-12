package dev.xboyrex.darkplayer.feature.player.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Choreographer
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlin.math.abs

/**
 * Dark Player High-Refresh Display Sync, HDR10/HDR10+ Capability Engine & Choreographer VSync Pacer.
 * Negotiates optimal display refresh mode, wide color gamut HDR window mode, and VSync frame pacing
 * to eliminate judder on High-Refresh and HDR panels.
 */
object DisplaySyncEngine {

    @Volatile
    var displaySupportsHdr: Boolean = false
        private set

    @Volatile
    var codecSupportsHdr: Boolean = false
        private set

    @Volatile
    var avSyncOffsetUs: Long = 0L
        private set

    fun enableHighRefreshWindow(activity: Activity) {
        runCatching {
            val window = activity.window ?: return
            detectHdrCapabilities(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displaySupportsHdr) {
                window.colorMode = ActivityInfo.COLOR_MODE_HDR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display ?: return
                val modes = display.supportedModes
                val bestMode = modes.maxWithOrNull(
                    compareBy<android.view.Display.Mode> { it.refreshRate }
                        .thenBy { it.physicalWidth * it.physicalHeight },
                )
                if (bestMode != null && bestMode.refreshRate >= 90f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = bestMode.modeId
                    window.attributes = params
                }
            }
        }
    }

    fun detectHdrCapabilities(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {
            val display = context.getSystemService(WindowManager::class.java)?.defaultDisplay
            displaySupportsHdr = display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
            if (displaySupportsHdr && !codecSupportsHdr) {
                val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                for (codec in codecs) {
                    if (codec.isEncoder) continue
                    for (type in codec.supportedTypes) {
                        val profiles = runCatching {
                            codec.getCapabilitiesForType(type).profileLevels.map { it.profile }.toSet()
                        }.getOrDefault(emptySet())
                        if (type == "video/x-vnd.on2.vp9" && profiles.any { it in VP9_HDR_PROFILES }) {
                            codecSupportsHdr = true
                        } else if (type == "video/hevc" && profiles.any { it in HEVC_HDR_PROFILES }) {
                            codecSupportsHdr = true
                        } else if (type == "video/av01" && profiles.any { it in AV1_HDR_PROFILES }) {
                            codecSupportsHdr = true
                        }
                    }
                }
            }
        }
    }

    fun applySurfaceFrameRate(surface: Surface?, frameRate: Float) {
        if (surface == null || !surface.isValid || frameRate <= 0f) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val compatibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                } else {
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                }
                surface.setFrameRate(frameRate.coerceIn(23.97f, 120f), compatibility)
            }
        }
    }

    // Choreographer VSync Pacing Tracker (API 33+)
    @Volatile
    private var releaseNs = 0L
    private val samples = LongArray(61)
    private var sampleCount = 0
    private var nextSample = 0
    private var lastSampledNs = 0L
    private var watching = false
    private var lastDrawnNs = 0L
    private var frameVsyncNs = 0L
    private var framePresentNs = 0L
    private var framePeriodNs = 16_666_667L
    private var usedVsyncNs = 0L

    @RequiresApi(33)
    private val vsyncCallback = object : Choreographer.VsyncCallback {
        override fun onVsync(data: Choreographer.FrameData) {
            val timeline = data.preferredFrameTimeline
            frameVsyncNs = data.frameTimeNanos
            framePresentNs = timeline.expectedPresentationTimeNanos
            framePeriodNs = data.frameTimelines.let {
                if (it.size >= 2) {
                    it[1].expectedPresentationTimeNanos - it[0].expectedPresentationTimeNanos
                } else {
                    0L
                }
            }.takeIf { it > 0 } ?: 16_666_667L

            if (System.nanoTime() - lastDrawnNs < 1_000_000_000L) {
                Choreographer.getInstance().postVsyncCallback(this)
            } else {
                watching = false
            }
        }
    }

    fun onFrameReleased(releaseTimeNs: Long) {
        if (releaseTimeNs - System.nanoTime() > 1_000_000L) releaseNs = releaseTimeNs
    }

    fun onFrameDrawn() {
        if (Build.VERSION.SDK_INT < 33) return
        lastDrawnNs = System.nanoTime()
        if (!watching) {
            watching = true
            Choreographer.getInstance().postVsyncCallback(vsyncCallback)
            return
        }
        if (frameVsyncNs == usedVsyncNs) return
        usedVsyncNs = frameVsyncNs
        val released = releaseNs
        if (released == 0L || released == lastSampledNs) return
        lastSampledNs = released

        val intendedNs = released + framePeriodNs * 8 / 10
        val gapUs = (framePresentNs - intendedNs) / 1000
        if (gapUs !in -150_000L..150_000L) return
        samples[nextSample] = gapUs
        nextSample = (nextSample + 1) % samples.size
        if (sampleCount < samples.size) sampleCount++
        if (sampleCount < 15) return
        val median = samples.copyOf(sampleCount).apply { sort() }[sampleCount / 2]
        val next = median.coerceIn(-100_000L, 100_000L)
        if (abs(next - avSyncOffsetUs) >= 2_000L) {
            avSyncOffsetUs = next
        }
    }

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

    private val HEVC_HDR_PROFILES = setOf(
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus,
    )
}
