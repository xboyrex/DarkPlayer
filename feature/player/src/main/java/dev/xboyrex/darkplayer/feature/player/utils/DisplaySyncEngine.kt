package dev.xboyrex.darkplayer.feature.player.utils

import android.app.Activity
import android.os.Build
import android.view.Surface
import android.view.WindowManager

/**
 * Dark Player High-Refresh Display Sync & High-Refresh Frame Pacing Engine.
 * Negotiates optimal display refresh mode and frame rate hints on Android 11+ (API 30+)
 * to eliminate 24/30/60fps judder on High-Refresh panels.
 */
object DisplaySyncEngine {

    fun enableHighRefreshWindow(activity: Activity) {
        runCatching {
            val window = activity.window ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display ?: return
                val modes = display.supportedModes
                val bestMode = modes.maxWithOrNull(
                    compareBy< android.view.Display.Mode> { it.refreshRate }
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
}
