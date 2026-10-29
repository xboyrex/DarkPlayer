package dev.xboyrex.darkplayer.feature.player.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.media3.common.Player
import io.github.anilbeesetti.nextlib.mediainfo.MediaThumbnailRetriever
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Captures the current playback video frame and saves it to Pictures/DarkPlayer.
 * Works across both local (content:// / file://) and network streams.
 */
object VideoFrameCapture {

    fun captureCurrentFrame(
        context: Context,
        player: Player?,
        scope: CoroutineScope,
    ) {
        val currentItem = player?.currentMediaItem ?: return
        val uri = currentItem.localConfiguration?.uri ?: return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val timeUs = positionMs * 1000L
        val title = (currentItem.mediaMetadata.title?.toString() ?: "DarkPlayerFrame")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(40)

        scope.launch {
            val savedName = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = extractFrameBitmap(context, uri, timeUs) ?: return@runCatching null
                    saveBitmapToGallery(context, bitmap, title)
                }.getOrNull()
            }

            if (savedName != null) {
                Toast.makeText(
                    context,
                    "Screenshot saved: Pictures/DarkPlayer/$savedName",
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Could not capture video frame",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun extractFrameBitmap(
        context: Context,
        uri: android.net.Uri,
        timeUs: Long,
    ): Bitmap? {
        val nativeBitmap = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                when (uri.scheme?.lowercase()) {
                    "content", "file" -> retriever.setDataSource(context, uri)
                    else -> retriever.setDataSource(uri.toString(), emptyMap())
                }
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                if (Build.VERSION.SDK_INT >= 29) retriever.close() else retriever.release()
            }
        }.getOrNull()

        if (nativeBitmap != null) return nativeBitmap

        return runCatching {
            val ffRetriever = MediaThumbnailRetriever()
            try {
                if (uri.scheme.equals("content", ignoreCase = true)) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        ffRetriever.setDataSource(pfd)
                        ffRetriever.getFrameAtTime(timeUs)
                    }
                } else {
                    ffRetriever.setDataSource(uri.path ?: uri.toString())
                    ffRetriever.getFrameAtTime(timeUs)
                }
            } finally {
                ffRetriever.release()
            }
        }.getOrNull()
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        baseTitle: String,
    ): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${baseTitle}_$timestamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/DarkPlayer",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(itemUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            fileName
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DarkPlayer",
            )
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            fileName
        }
    }
}
