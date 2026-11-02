package dev.xboyrex.darkplayer.core.ui.updater

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
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.xboyrex.darkplayer.core.ui.glass.RegisterPopupBlur
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Immutable
data class UpdateInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long,
)

internal val DEFAULT_RELEASE_NOTES = """
    • Liquid Glass Navigation Bar & Video Player UI with interactive spring physics.
    • Hardware Dolby Atmos & 5-Band Studio Equalizer with Bass Boost & Virtualizer.
    • Angle-locked horizontal seek & vertical volume/brightness gestures with zero conflict.
    • Studio Settings with Theme Mode (System/Dark/Light/AMOLED) & Color Palette Picker.
    • Automatic OTA GitHub Releases Updater by ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex).
""".trimIndent()

@Composable
fun rememberUpdateState(): UpdateState {
    val scope = rememberCoroutineScope()
    return remember(scope) { UpdateState(scope) }
}

@Stable
class UpdateState(private val scope: CoroutineScope) {

    var info by mutableStateOf<UpdateInfo?>(null)
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
    var message by mutableStateOf<String?>(null)

    val isPopupActive: Boolean
        get() = (visible && info != null) || checking || message != null

    private var downloadJob: Job? = null

    fun currentVersionName(context: Context): String = runCatching {
        val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pkgInfo.versionName ?: "2.0.0"
    }.getOrDefault("2.0.0")

    fun check(context: Context, manual: Boolean = false) {
        if (checking) return
        checking = true
        if (manual) message = null
        val installedVersion = currentVersionName(context)
        scope.launch {
            val latest = withContext(Dispatchers.IO) {
                runCatching { fetchLatest(installedVersion) }.getOrNull()
            }
            checking = false
            if (latest == null) {
                if (manual) {
                    message = "You're running the latest Dark Player ($installedVersion) by @xboyrex."
                }
                return@launch
            }
            if (isNewerVersion(latest.version, installedVersion)) {
                info = latest
                error = null
                visible = true
            } else if (manual) {
                message = "You're on the latest version ($installedVersion)."
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
                error = "Download failed. Check your internet connection and try again."
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
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            error = "Allow installs from Dark Player, then tap Install again."
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
            error = "Couldn't open the package installer. Try again."
        }
    }

    fun dismiss() {
        if (!downloading) visible = false
    }

    private fun fetchLatest(installedVersion: String): UpdateInfo? {
        val url = URL("https://api.github.com/repos/xboyrex/DarkPlayer/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DarkPlayer/$installedVersion")
        }
        try {
            if (conn.responseCode !in 200..299) return null
            val bodyText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(bodyText)
            val tag = json.optString("tag_name").ifBlank { return null }
            val version = tag.removePrefix("v").removePrefix("V").trim()
            if (version.isBlank()) return null

            val assets = json.optJSONArray("assets") ?: return null
            val preferredAbis = Build.SUPPORTED_ABIS.orEmpty()
            var bestUrl: String? = null
            var bestName = "DarkPlayer.apk"
            var bestSize = 0L
            var bestScore = -1

            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                val candidateUrl = asset.optString("browser_download_url")
                if (candidateUrl.isBlank()) continue
                val lowerName = name.lowercase(Locale.US)
                val score = when {
                    preferredAbis.firstOrNull()?.let { lowerName.contains(it.lowercase(Locale.US)) } == true -> 100
                    preferredAbis.any { lowerName.contains(it.lowercase(Locale.US)) } -> 80
                    lowerName.contains("universal") -> 60
                    else -> 40
                }
                if (score > bestScore) {
                    bestScore = score
                    bestUrl = candidateUrl
                    bestName = name
                    bestSize = asset.optLong("size")
                }
            }

            val resolvedUrl = bestUrl ?: return null
            val cleanNotes = json.optString("body")
                .replace(Regex("<[^>]*>"), "")
                .trim()
                .ifBlank { DEFAULT_RELEASE_NOTES }

            return UpdateInfo(
                version = version,
                tag = tag,
                notes = cleanNotes,
                apkUrl = resolvedUrl,
                apkName = bestName,
                sizeBytes = bestSize,
            )
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun downloadApk(
        context: Context,
        update: UpdateInfo,
        onProgress: (Float) -> Unit,
    ): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "DarkPlayer-update.apk")
        if (target.exists()) target.delete()

        var currentUrl = update.apkUrl
        var redirects = 0
        var conn: HttpURLConnection
        while (true) {
            conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "DarkPlayer/${update.version}")
            }
            val code = conn.responseCode
            if (code in 300..399 && redirects < 5) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) error("Redirect missing Location")
                currentUrl = location
                redirects++
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                error("HTTP $code")
            }
            break
        }

        try {
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

        if (target.length() <= 0L) error("Empty download")
        return target
    }
}

fun isNewerVersion(remote: String, current: String): Boolean {
    fun parts(value: String) = value.trim()
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

private fun formatUpdateSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> "${bytes / 1024L} KB"
    else -> "$bytes B"
}

@Composable
fun UpdateOverlayDialog(state: UpdateState) {
    val context = LocalContext.current
    val update = state.info
    val c = MaterialTheme.colorScheme

    RegisterPopupBlur(active = state.isPopupActive)

    AnimatedVisibility(
        visible = state.visible && update != null,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
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
                    initialScale = 0.86f,
                ) + fadeIn(tween(220)),
                exit = scaleOut(tween(140), targetScale = 0.94f) + fadeOut(tween(140)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .widthIn(max = 410.dp)
                        .shadow(28.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF101520).copy(alpha = 0.95f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(c.primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = c.primary,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Update available",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Dark Player ${update?.version.orEmpty()}" +
                                    formatUpdateSize(update?.sizeBytes ?: 0L).let {
                                        if (it.isBlank()) "" else " · $it"
                                    },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .pressable(onClick = { state.dismiss() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close update dialog",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "What's new",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        update?.notes?.trim()?.ifBlank { DEFAULT_RELEASE_NOTES } ?: DEFAULT_RELEASE_NOTES,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 9,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 172.dp)
                            .verticalScroll(rememberScrollState()),
                    )

                    state.error?.let { err ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            err,
                            color = c.error,
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
                            color = c.primary,
                            trackColor = Color.White.copy(alpha = 0.12f),
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "${(state.progress * 100).toInt()}% · downloading ${update?.apkName ?: "DarkPlayer.apk"}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
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
                                "Cancel",
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (state.downloaded != null) {
                                    state.install(context)
                                } else {
                                    state.download(context)
                                }
                            },
                            enabled = !state.downloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = c.primary,
                                contentColor = c.onPrimary,
                            ),
                        ) {
                            if (state.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = c.onPrimary,
                                )
                            } else {
                                Text(
                                    if (state.downloaded != null) "Install Now" else "Download & Install",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Official GitHub Release · github.com/xboyrex/DarkPlayer (${update?.tag.orEmpty()})",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Status modal when manually checking and already up to date
    AnimatedVisibility(
        visible = state.checking || state.message != null,
        enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
        exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!state.checking) state.message = null
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 350.dp)
                    .fillMaxWidth()
                    .shadow(28.dp, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF101520).copy(alpha = 0.94f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.08f),
                            ),
                        ),
                        shape = RoundedCornerShape(26.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.checking) {
                    CircularProgressIndicator(
                        color = c.primary,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Checking for updates…",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Connecting to github.com/xboyrex/DarkPlayer…",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                } else if (state.message != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF132A1C))
                            .border(1.dp, Color(0xFF2ECC71).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Up to date",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.message.orEmpty(),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.primary)
                            .pressable { state.message = null }
                            .padding(horizontal = 28.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "OK",
                            color = c.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        )
                    }
                }
            }
        }
    }
}
