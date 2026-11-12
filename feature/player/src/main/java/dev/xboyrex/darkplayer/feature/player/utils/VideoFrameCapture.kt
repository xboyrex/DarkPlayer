package dev.xboyrex.darkplayer.feature.player.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import io.github.anilbeesetti.nextlib.mediainfo.MediaThumbnailRetriever
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface FrameSaveResult {
    data class Success(val fileName: String) : FrameSaveResult
    data class Failure(val reason: String) : FrameSaveResult
}

/**
 * Captures and persists the current video frame to `Pictures/NovaPlayer` (VP-13, VP-14, VP-25).
 *
 * - Preserves per-item HTTP request headers (`mediaItem.mediaMetadata.requestHeaders`) when
 *   capturing frames from authenticated HTTP/HTTPS streams (VP-13).
 * - Passes the full URI string (never `uri.path`) to FFmpeg fallback for remote URLs (VP-13).
 * - Verifies non-null output stream and `Bitmap.compress(...) == true` before clearing `IS_PENDING`,
 *   deleting any orphan MediaStore row on failure (VP-14).
 */
object VideoFrameCapture {
    private const val SCREENSHOT_FOLDER_NAME = "NovaPlayer"

    @OptIn(UnstableApi::class)
    fun captureFrame(
        context: Context,
        mediaItem: MediaItem,
        positionMs: Long,
    ): Bitmap? {
        val uri = mediaItem.localConfiguration?.uri ?: Uri.parse(mediaItem.mediaId)
        val headers = mediaItem.apiHeaders
        val positionUs = positionMs.coerceAtLeast(0L) * 1_000L
        val scheme = uri.scheme?.lowercase()
        val isHttp = scheme == "http" || scheme == "https"

        val retriever = MediaMetadataRetriever()
        try {
            if (isHttp) {
                retriever.setDataSource(uri.toString(), headers)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.getFrameAtTime(positionUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { return it }
        } catch (_: Exception) {
            // Fall through to FFmpeg MediaThumbnailRetriever
        } finally {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            }
        }

        val ffmpegRetriever = MediaThumbnailRetriever()
        return try {
            when {
                scheme == "content" -> {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        ffmpegRetriever.setDataSource(pfd)
                        ffmpegRetriever.getFrameAtTime(positionUs)
                    }
                }
                isHttp -> {
                    ffmpegRetriever.setDataSource(uri.toString())
                    ffmpegRetriever.getFrameAtTime(positionUs)
                }
                else -> {
                    val filePath = uri.path ?: uri.toString()
                    ffmpegRetriever.setDataSource(filePath)
                    ffmpegRetriever.getFrameAtTime(positionUs)
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { ffmpegRetriever.release() }
        }
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "Nova_${System.currentTimeMillis()}.png",
    ): FrameSaveResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$SCREENSHOT_FOLDER_NAME",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val itemUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            ) ?: return FrameSaveResult.Failure("Unable to create MediaStore entry")

            return try {
                val stream = resolver.openOutputStream(itemUri)
                    ?: throw IllegalStateException("MediaStore returned null OutputStream")
                val compressed = stream.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (!compressed) {
                    throw IllegalStateException("Bitmap compression returned false")
                }
                val publishValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, publishValues, null, null)
                FrameSaveResult.Success(fileName)
            } catch (e: Exception) {
                runCatching { resolver.delete(itemUri, null, null) }
                FrameSaveResult.Failure(e.message ?: "Failed to write screenshot")
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                SCREENSHOT_FOLDER_NAME,
            )
            if (!dir.exists() && !dir.mkdirs()) {
                return FrameSaveResult.Failure("Unable to create Pictures/$SCREENSHOT_FOLDER_NAME directory")
            }
            val file = File(dir, fileName)
            return try {
                val compressed = FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (!compressed || !file.exists() || file.length() == 0L) {
                    runCatching { file.delete() }
                    FrameSaveResult.Failure("Failed to write PNG file")
                } else {
                    FrameSaveResult.Success(file.name)
                }
            } catch (e: Exception) {
                runCatching { file.delete() }
                FrameSaveResult.Failure(e.message ?: "Failed to save screenshot")
            }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun captureAndSaveCurrentFrameAsync(context: Context, player: Player): FrameSaveResult {
        val mediaItem = withContext(Dispatchers.Main) { player.currentMediaItem }
            ?: return FrameSaveResult.Failure("No active media item")
        val positionMs = withContext(Dispatchers.Main) { player.currentPosition }
        return withContext(Dispatchers.IO) {
            val bitmap = captureFrame(
                context = context,
                mediaItem = mediaItem,
                positionMs = positionMs,
            ) ?: return@withContext FrameSaveResult.Failure("Unable to extract video frame")
            try {
                saveBitmapToGallery(context = context, bitmap = bitmap)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun captureAndSaveCurrentFrame(context: Context, player: Player): FrameSaveResult {
        val mediaItem = player.currentMediaItem
            ?: return FrameSaveResult.Failure("No active media item")
        val bitmap = captureFrame(
            context = context,
            mediaItem = mediaItem,
            positionMs = player.currentPosition,
        ) ?: return FrameSaveResult.Failure("Unable to extract video frame")
        return try {
            saveBitmapToGallery(context = context, bitmap = bitmap)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
