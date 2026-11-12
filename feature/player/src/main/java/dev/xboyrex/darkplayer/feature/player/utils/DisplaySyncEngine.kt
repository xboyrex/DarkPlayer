package dev.xboyrex.darkplayer.feature.player.utils

import android.os.Build
import android.view.Surface
import android.view.Window
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Player-owned content frame-rate matching policy (VP-04).
 *
 * Avoids forcing maximum refresh rate globally on the Home screen; instead hints the active
 * video [Surface] on API 30+ and selects a cadence-compatible window display mode (24/25/30/50/60 fps)
 * only while video playback is active, restoring the default display mode when playback ends.
 */
object DisplaySyncEngine {

    fun applySurfaceFrameRate(surface: Surface?, frameRate: Float) {
        if (surface == null || !surface.isValid) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val validRate = if (frameRate in 10f..240f && frameRate.isFinite()) frameRate else 0f
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    surface.setFrameRate(
                        validRate,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                        Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS,
                    )
                } else {
                    surface.setFrameRate(
                        validRate,
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                    )
                }
            }
        }
    }

    fun matchWindowRefreshRate(window: Window?, contentFrameRate: Float) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (contentFrameRate !in 10f..240f || !contentFrameRate.isFinite()) {
            restoreDefaultDisplayMode(window)
            return
        }
        runCatching {
            @Suppress("DEPRECATION")
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.context.display
            } else {
                window.windowManager.defaultDisplay
            } ?: return
            val currentMode = display.mode
            val sameResModes = display.supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            if (sameResModes.isEmpty()) return

            // Find a refresh rate that is a clean integer multiple (1x, 2x, 3x, 4x, 5x) of contentFrameRate
            val compatibleMode = sameResModes
                .filter { mode -> isCadenceMultiple(mode.refreshRate, contentFrameRate) }
                .minByOrNull { mode -> mode.refreshRate }

            val targetModeId = compatibleMode?.modeId ?: 0
            val params = window.attributes
            if (params.preferredDisplayModeId != targetModeId) {
                params.preferredDisplayModeId = targetModeId
                window.attributes = params
            }
        }
    }

    fun restoreDefaultDisplayMode(window: Window?) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            val params = window.attributes
            if (params.preferredDisplayModeId != 0) {
                params.preferredDisplayModeId = 0
                window.attributes = params
            }
        }
    }

    internal fun isCadenceMultiple(refreshRate: Float, contentFrameRate: Float): Boolean {
        if (refreshRate <= 0f || contentFrameRate <= 0f) return false
        val ratio = refreshRate / contentFrameRate
        val nearestMultiple = ratio.roundToInt()
        if (nearestMultiple < 1) return false
        val expectedRefresh = contentFrameRate * nearestMultiple
        return abs(refreshRate - expectedRefresh) <= 0.35f
    }
}
