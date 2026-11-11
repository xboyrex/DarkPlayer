package dev.xboyrex.darkplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Build.VERSION.SDK_INT
import android.util.Size as AndroidSize
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.get
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.ContentMetadata
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.toAndroidUri
import coil3.util.component1
import coil3.util.component2
import io.github.anilbeesetti.nextlib.mediainfo.MediaThumbnailRetriever
import kotlin.math.abs
import kotlin.math.roundToInt
import okio.FileSystem

class VideoThumbnailDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val strategy: ThumbnailStrategy,
    private val diskCache: Lazy<DiskCache?>,
) : Decoder {

    private val diskCacheKey: String
        get() = options.diskCacheKey ?: run {
            val metadata = source.metadata
            when {
                metadata is ContentMetadata -> metadata.uri.toAndroidUri().toString()
                source.fileSystem === FileSystem.SYSTEM -> source.file().toFile().path
                else -> error(message = "Not supported")
            }
        }

    @OptIn(ExperimentalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        readFromDiskCache()?.use { snapshot ->
            val file = snapshot.data.toFile()
            val sampledBitmap = file.inputStream().use { BitmapFactory.decodeStream(it) }
            if (sampledBitmap != null) {
                return DecodeResult(
                    image = sampledBitmap.toDrawable(options.context.resources).asImage(),
                    isSampled = true,
                )
            }
        }

        val targetW = options.size.width.pxOrElse { 480 }.coerceIn(240, 640)
        val targetH = options.size.height.pxOrElse { 300 }.coerceIn(150, 400)

        // 1. Fast path on Android 10+ (API 29+): ContentResolver.loadThumbnail (instant hardware MediaStore cache)
        val fastContentThumb = runCatching {
            val metadata = source.metadata
            if (SDK_INT >= 29 && metadata is ContentMetadata && strategy is ThumbnailStrategy.FirstFrame) {
                options.context.contentResolver.loadThumbnail(
                    metadata.uri.toAndroidUri(),
                    AndroidSize(targetW, targetH),
                    null,
                )
            } else {
                null
            }
        }.getOrNull()

        val rawBitmap = fastContentThumb ?: MediaMetadataRetriever().use { nativeRetriever ->
            runCatching { nativeRetriever.setDataSource(source = source) }

            val embeddedPicture = runCatching { nativeRetriever.embeddedPicture }.getOrNull()
            val embeddedBitmap = embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (embeddedBitmap != null) return@use embeddedBitmap

            val videoDuration = runCatching {
                nativeRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }.getOrNull() ?: 0L

            val timeUs = when (strategy) {
                is ThumbnailStrategy.FirstFrame -> 0L
                is ThumbnailStrategy.FrameAtPercentage -> (videoDuration * strategy.percentage * 1000L).toLong()
                is ThumbnailStrategy.Hybrid -> (videoDuration * strategy.percentage * 1000L).toLong()
            }

            // Use hardware-scaled getScaledFrameAtTime with OPTION_CLOSEST_SYNC (keyframe) for 10x faster decoding
            val hwFrame = if (SDK_INT >= 27) {
                runCatching {
                    nativeRetriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        targetW,
                        targetH,
                    )
                }.getOrNull()
            } else {
                null
            } ?: runCatching {
                nativeRetriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }.getOrNull()

            if (hwFrame != null) return@use hwFrame

            // Only initialize FFmpeg MediaThumbnailRetriever if hardware MediaCodec failed
            MediaThumbnailRetriever().use { ffmpegRetriever ->
                runCatching {
                    ffmpegRetriever.setDataSource(source = source)
                    ffmpegRetriever.getEmbeddedPicture()?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } ?: ffmpegRetriever.getFrameAtTime(timeUs = timeUs)
                        ?: ffmpegRetriever.getFrameAtTime(timeUs = 0L)
                }.getOrNull()
            } ?: throw IllegalStateException("Failed to decode video thumbnail.")
        }

        val srcWidth = rawBitmap.width
        val srcHeight = rawBitmap.height
        val dstSize = computeDstSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
        )
        val scaledBitmap = normalizeBitmap(
            inBitmap = rawBitmap,
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstSize = dstSize,
        )

        writeToDiskCache(inBitmap = scaledBitmap)

        return DecodeResult(
            image = scaledBitmap.toDrawable(options.context.resources).asImage(),
            isSampled = true,
        )
    }

    private fun MediaMetadataRetriever.setDataSource(source: ImageSource) {
        val metadata = source.metadata
        when {
            metadata is ContentMetadata -> {
                setDataSource(
                    options.context,
                    metadata.uri.toAndroidUri(),
                )
            }

            source.fileSystem === FileSystem.SYSTEM -> {
                setDataSource(source.file().toFile().path)
            }

            else -> error(message = "Not supported")
        }
    }

    private fun MediaThumbnailRetriever.setDataSource(source: ImageSource) {
        val metadata = source.metadata
        when {
            metadata is ContentMetadata -> {
                setDataSource(metadata.assetFileDescriptor.parcelFileDescriptor)
            }

            source.fileSystem === FileSystem.SYSTEM -> {
                setDataSource(source.file().toFile().path)
            }

            else -> error(message = "Not supported")
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? =
        if (options.diskCachePolicy.readEnabled) {
            diskCache.value?.openSnapshot(key = diskCacheKey)
        } else {
            null
        }

    private fun writeToDiskCache(inBitmap: Bitmap) {
        if (!options.diskCachePolicy.writeEnabled) return
        val editor = diskCache.value?.openEditor(key = diskCacheKey) ?: return
        try {
            editor.data.toFile().outputStream().use { output ->
                inBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    82,
                    output,
                )
            }
            editor.commit()
        } catch (_: Exception) {
            runCatching { editor.abort() }
        }
    }

    private fun normalizeBitmap(
        inBitmap: Bitmap,
        srcWidth: Int,
        srcHeight: Int,
        dstSize: Size,
    ): Bitmap {
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstSize.width.pxOrElse { inBitmap.width },
            dstHeight = dstSize.height.pxOrElse { inBitmap.height },
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        ).toFloat()

        if (scale >= 0.98f) return inBitmap

        val dstWidth = (scale * inBitmap.width).roundToInt().coerceAtLeast(1)
        val dstHeight = (scale * inBitmap.height).roundToInt().coerceAtLeast(1)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(
            width = dstWidth,
            height = dstHeight,
            config = inBitmap.config ?: Bitmap.Config.ARGB_8888,
        )
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()
        return outBitmap
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun computeDstSize(srcWidth: Int, srcHeight: Int): Size {
        if (srcWidth <= 0 || srcHeight <= 0) return Size.ORIGINAL

        val (dstWidth, dstHeight) = DecodeUtils.computeDstSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        val rawScale = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
            maxSize = options.maxBitmapSize,
        )
        val finalScale = if (options.precision == Precision.INEXACT) {
            rawScale.coerceAtMost(maximumValue = 1.0)
        } else {
            rawScale
        }
        return Size(
            (finalScale * srcWidth).roundToInt(),
            (finalScale * srcHeight).roundToInt(),
        )
    }

    class Factory(private val thumbnailStrategy: () -> ThumbnailStrategy) : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.mimeType)) return null
            return VideoThumbnailDecoder(
                source = result.source,
                options = options,
                strategy = thumbnailStrategy(),
                diskCache = lazy { imageLoader.diskCache },
            )
        }

        private fun isApplicable(mimeType: String?): Boolean =
            mimeType != null && mimeType.startsWith(prefix = "video/")
    }
}

private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try {
        return block(this)
    } finally {
        if (SDK_INT >= 29) {
            close()
        } else {
            release()
        }
    }
}

sealed class ThumbnailStrategy {
    data object FirstFrame : ThumbnailStrategy()
    data class FrameAtPercentage(val percentage: Float = 0.20f) : ThumbnailStrategy()
    data class Hybrid(val percentage: Float = 0.20f) : ThumbnailStrategy()
}
