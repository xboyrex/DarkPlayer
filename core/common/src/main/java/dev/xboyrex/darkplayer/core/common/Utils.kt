package dev.xboyrex.darkplayer.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

val storagePermission = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.READ_MEDIA_VIDEO
    else -> Manifest.permission.READ_EXTERNAL_STORAGE
}

/**
 * Returns true only when the app holds full library access (not Android 14+ selected-videos-only).
 * Prevents destructive local playlist cleanup when the user only granted partial access (NP-02).
 */
fun Context.hasFullMediaAccess(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        this,
        storagePermission,
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Returns true when the app holds either full media access OR Android 14+ selected-videos access.
 */
fun Context.hasAnyMediaAccess(): Boolean {
    if (hasFullMediaAccess()) return true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) == PackageManager.PERMISSION_GRANTED
    }
    return false
}

object Utils {

    fun pxToDp(px: Float): Float = px / Resources.getSystem().displayMetrics.density

    fun formatDurationMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) -
            TimeUnit.MINUTES.toSeconds(minutes) -
            TimeUnit.HOURS.toSeconds(hours)
        return if (hours > 0) {
            String.format(
                format = "%02d:%02d:%02d",
                hours,
                minutes,
                seconds,
            )
        } else {
            String.format(
                format = "%02d:%02d",
                minutes,
                seconds,
            )
        }
    }

    fun formatDurationMillisSign(millis: Long): String = if (millis >= 0) {
        "+${formatDurationMillis(millis = millis)}"
    } else {
        "-${formatDurationMillis(millis = abs(n = millis))}"
    }

    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size < kb -> "$size B"
            size < mb -> "%.2f KB".format(size / kb.toDouble())
            size < gb -> "%.2f MB".format(size / mb.toDouble())
            else -> "%.2f GB".format(size / gb.toDouble())
        }
    }

    fun formatBitrate(bitrate: Long): String? {
        if (bitrate <= 0) {
            return null
        }

        val kiloBitrate = bitrate.toDouble() / 1000.0
        val megaBitrate = kiloBitrate / 1000.0
        val gigaBitrate = megaBitrate / 1000.0

        return when {
            gigaBitrate >= 1.0 -> String.format(
                format = "%.1f Gbps",
                gigaBitrate,
            )

            megaBitrate >= 1.0 -> String.format(
                format = "%.1f Mbps",
                megaBitrate,
            )

            kiloBitrate >= 1.0 -> String.format(
                format = "%.1f kbps",
                kiloBitrate,
            )

            else -> String.format(
                format = "%d bps",
                bitrate,
            )
        }
    }

    fun formatLanguage(language: String?): String? = language?.let { lang ->
        Locale.forLanguageTag(lang).displayLanguage.takeIf { it.isNotEmpty() }
    }
}
