package com.mayank.darkplayer.core.ui.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val RELEASES_API_URL = "https://api.github.com/repos/xboyrex/DarkPlayer/releases/latest"

@Immutable
data class DarkPlayerUpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long,
)

@Stable
class DarkPlayerUpdateState(
    private val scope: CoroutineScope,
    private val currentVersionName: String,
) {
    var info by mutableStateOf<DarkPlayerUpdateInfo?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var downloading by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var downloaded by mutableStateOf<File?>(null)
        private set
    var statusMessage by mutableStateOf<String?>(null)

    private var downloadJob: Job? = null

    fun check(manual: Boolean = false) {
        if (checking) return
        checking = true
        statusMessage = null
        scope.launch {
            val latest = withContext(Dispatchers.IO) { runCatching { fetchLatest() }.getOrNull() }
            checking = false
            if (latest == null) {
                if (manual) statusMessage = "Could not reach update server. Try again later."
                return@launch
            }
            if (isNewerVersion(latest.version, currentVersionName)) {
                info = latest
                error = null
                visible = true
            } else if (manual) {
                statusMessage = "Dark Player $currentVersionName is up to date."
            }
        }
    }

    fun download(context: Context) {
        val update = info ?: return
        if (downloading) return
        downloading = true
        error = null
        progress = 0f
        downloadJob = scope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching { downloadApk(context, update) { p -> progress = p } }.getOrNull()
            }
            downloading = false
            downloadJob = null
            if (file != null) {
                downloaded = file
                install(context)
            } else {
                error = "Download failed. Check your connection and try again."
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloading = false
        progress = 0f
    }

    fun install(context: Context) {
        val file = downloaded ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            error = "Allow installs from Dark Player in Settings, then tap Install again."
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        error = null
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            error = "Could not launch the Android package installer."
        }
    }

    fun dismiss() {
        if (!downloading) visible = false
    }

    private fun fetchLatest(): DarkPlayerUpdateInfo? {
        val conn = (URL(RELEASES_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DarkPlayer/$currentVersionName")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return null }
            val version = tag.removePrefix("v").removePrefix("V").trim()
            if (version.isBlank()) return null
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val candidate = asset.optString("browser_download_url")
                if (candidate.isBlank()) continue
                if (apkUrl == null || name.equals("DarkPlayer.apk", ignoreCase = true)) {
                    apkUrl = candidate
                    size = asset.optLong("size")
                }
                if (name.equals("DarkPlayer.apk", ignoreCase = true)) break
            }
            val resolvedUrl = apkUrl ?: return null
            DarkPlayerUpdateInfo(
                version = version,
                tag = tag,
                notes = json.optString("body").orEmpty(),
                apkUrl = resolvedUrl,
                sizeBytes = size,
            )
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun downloadApk(
        context: Context,
        update: DarkPlayerUpdateInfo,
        onProgress: (Float) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "DarkPlayer.apk")
        if (target.exists()) target.delete()

        val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "DarkPlayer/$currentVersionName")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastTick = 0L
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            val now = System.currentTimeMillis()
                            if (now - lastTick > 80 || p >= 1f) {
                                lastTick = now
                                withContext(Dispatchers.Main) { onProgress(p) }
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (target.length() <= 0L) error("Empty APK download")
        return target
    }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    fun parts(v: String) = v.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
    val r = parts(remote)
    val c = parts(current)
    for (i in 0 until maxOf(r.size, c.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = c.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private fun formatApkSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
fun DarkPlayerUpdateDialog(state: DarkPlayerUpdateState) {
    val context = LocalContext.current
    val update = state.info

    AnimatedVisibility(
        visible = state.visible && update != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { state.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = state.visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                    initialScale = 0.88f,
                ) + fadeIn(tween(220)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 410.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Update Available",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Dark Player ${update?.version.orEmpty()}" +
                                    formatApkSize(update?.sizeBytes ?: 0L).let {
                                        if (it.isBlank()) "" else " · $it"
                                    },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                                .clickable { state.dismiss() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "What's New",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = update?.notes?.trim()?.ifBlank {
                            "Performance improvements, Navy Dark UI refinements, and playback stability fixes."
                        } ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        maxLines = 9,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 172.dp)
                            .verticalScroll(rememberScrollState()),
                    )

                    state.error?.let { msg ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (state.downloading) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.onSurface,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${(state.progress * 100).toInt()}% · Downloading DarkPlayer.apk",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                if (state.downloading) state.cancelDownload() else state.dismiss()
                            },
                        ) {
                            Text(
                                text = "Later",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val targetUrl = update?.apkUrl?.takeIf { it.isNotBlank() }
                                    ?: "https://github.com/xboyrex/DarkPlayer/releases/latest"
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }
                                state.dismiss()
                            },
                            enabled = !state.downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            if (state.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.surface,
                                )
                            } else {
                                Text(
                                    text = if (state.downloaded != null) "Install" else "Update Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GitHub Releases · ${update?.tag.orEmpty()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
