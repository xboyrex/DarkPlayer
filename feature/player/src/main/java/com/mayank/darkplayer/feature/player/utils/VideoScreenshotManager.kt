package com.mayank.darkplayer.feature.player.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures the active hardware-accelerated video frame from the PlayerSurface
 * using Android PixelCopy and saves a lossless PNG directly to Pictures/DarkPlayer.
 */
object VideoScreenshotManager {

    fun captureCurrentFrame(
        context: Context,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> },
    ) {
        val activity = context.findActivity()
        if (activity == null) {
            showToast(context, "Unable to access player window")
            onComplete(false, null)
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showToast(context, "Screenshot requires Android 8.0+")
            onComplete(false, null)
            return
        }

        val decorView = activity.window?.decorView
        val surfaceView = decorView?.let { findVideoSurfaceView(it) }
        val width = (surfaceView?.width ?: decorView?.width ?: 0).coerceAtLeast(1)
        val height = (surfaceView?.height ?: decorView?.height ?: 0).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())

        val copyListener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                val savedPath = saveBitmapToGallery(context, bitmap)
                bitmap.recycle()
                if (savedPath != null) {
                    showToast(context, "Screenshot saved to Gallery")
                    onComplete(true, savedPath)
                } else {
                    showToast(context, "Failed to save screenshot")
                    onComplete(false, null)
                }
            } else {
                bitmap.recycle()
                showToast(context, "Screenshot capture failed ($copyResult)")
                onComplete(false, null)
            }
        }

        try {
            if (surfaceView != null && surfaceView.holder.surface.isValid) {
                PixelCopy.request(surfaceView, bitmap, copyListener, handler)
            } else {
                PixelCopy.request(activity.window, bitmap, copyListener, handler)
            }
        } catch (_: Exception) {
            bitmap.recycle()
            showToast(context, "Unable to capture frame")
            onComplete(false, null)
        }
    }

    private fun findVideoSurfaceView(root: View): SurfaceView? {
        if (root is SurfaceView && root.width > 0 && root.height > 0) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findVideoSurfaceView(root.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "DarkPlayer_$timestamp.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DarkPlayer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri.toString()
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DarkPlayer",
            ).apply { mkdirs() }
            val target = File(dir, filename)
            try {
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
                target.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
