package dev.xboyrex.darkplayer.core.ui.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val expectedSizeBytes: Long,
    val expectedSha256: String?,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class NetworkError(val message: String) : UpdateCheckResult
    data class InvalidReleaseMetadata(val message: String) : UpdateCheckResult
}

/**
 * Silent background updater for Nova Player (NP-09 & NP-10 fixes):
 * - Runs silently in the background on app launch without any blocking front modal ("Checking for updates...").
 * - Distinguishes `UpToDate` from `NetworkError` and `InvalidReleaseMetadata` on manual checks.
 * - Verifies both exact byte size (`expectedSizeBytes`) and SHA-256 digest (`expectedSha256`) before installation.
 */
object AppUpdater {
    private const val RELEASES_API_URL = "https://api.github.com/repos/xboyrex/DarkPlayer/releases/latest"

    var availableRelease by mutableStateOf<ReleaseInfo?>(null)
        private set

    var isDownloading by mutableStateOf(false)
        private set

    var downloadProgress by mutableStateOf(0f)
        private set

    suspend fun checkInBackgroundSilently(currentVersionName: String) {
        when (val res = checkForUpdates(currentVersionName)) {
            is UpdateCheckResult.UpdateAvailable -> {
                availableRelease = res.release
            }
            else -> {
                // Silent in background: never interrupt the user on startup
            }
        }
    }

    fun dismissAvailableRelease() {
        availableRelease = null
    }

    suspend fun checkForUpdates(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val conn = runCatching {
            (URL(RELEASES_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "NovaPlayer-Updater")
            }
        }.getOrElse { e ->
            return@withContext UpdateCheckResult.NetworkError(e.localizedMessage ?: "Unable to connect to GitHub")
        }

        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.NetworkError("GitHub API returned HTTP $code")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            if (tagName.isEmpty()) {
                return@withContext UpdateCheckResult.InvalidReleaseMetadata("Missing tag_name in release metadata")
            }
            val remoteVersion = tagName.removePrefix("v").trim()
            val assets = json.optJSONArray("assets")
                ?: return@withContext UpdateCheckResult.InvalidReleaseMetadata("No APK assets in release")

            var selectedUrl = ""
            var selectedName = ""
            var selectedSize = -1L
            var sha256SumsUrl: String? = null
            val preferredAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", -1L)
                if (name.equals("SHA256SUMS.txt", ignoreCase = true)) {
                    sha256SumsUrl = url
                } else if (name.endsWith(".apk", ignoreCase = true)) {
                    if (selectedUrl.isEmpty() || (preferredAbi.isNotEmpty() && name.contains(preferredAbi, ignoreCase = true)) || name.contains("universal", ignoreCase = true)) {
                        selectedUrl = url
                        selectedName = name
                        selectedSize = size
                    }
                }
            }

            if (selectedUrl.isEmpty() || selectedSize <= 0L) {
                return@withContext UpdateCheckResult.InvalidReleaseMetadata("No valid APK asset found")
            }

            val expectedSha = sha256SumsUrl?.let { fetchSha256ForAsset(it, selectedName) }

            if (isNewerVersion(remoteVersion, currentVersionName.removePrefix("v").trim())) {
                val info = ReleaseInfo(
                    tagName = tagName,
                    versionName = remoteVersion,
                    changelog = json.optString("body", ""),
                    apkDownloadUrl = selectedUrl,
                    expectedSizeBytes = selectedSize,
                    expectedSha256 = expectedSha,
                )
                UpdateCheckResult.UpdateAvailable(info)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.NetworkError(e.localizedMessage ?: "Failed to check for updates")
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchSha256ForAsset(sumsUrl: String, assetName: String): String? {
        return runCatching {
            val conn = (URL(sumsUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
            }
            conn.inputStream.bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .firstOrNull { it.endsWith(assetName) }
                    ?.split(Regex("\\s+"))
                    ?.firstOrNull()
                    ?.lowercase()
            }
        }.getOrNull()
    }

    suspend fun downloadAndVerifyApk(
        context: Context,
        release: ReleaseInfo,
    ): Result<File> = withContext(Dispatchers.IO) {
        isDownloading = true
        downloadProgress = 0f
        val targetFile = File(context.cacheDir, "nova_update_${release.versionName}.apk")
        if (targetFile.exists()) targetFile.delete()

        try {
            val conn = (URL(release.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("Download failed with HTTP $code"))
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            conn.inputStream.buffered().use { input ->
                targetFile.outputStream().buffered().use { output ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        digest.update(buf, 0, read)
                        totalBytes += read
                        if (release.expectedSizeBytes > 0L) {
                            downloadProgress = (totalBytes.toFloat() / release.expectedSizeBytes.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
            }

            // NP-10: Strict byte-size verification
            if (release.expectedSizeBytes > 0L && totalBytes != release.expectedSizeBytes) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("Downloaded APK size ($totalBytes B) does not match expected size (${release.expectedSizeBytes} B)"),
                )
            }

            // NP-10: Strict SHA-256 checksum verification when SHA256SUMS.txt is published
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }.lowercase()
            if (!release.expectedSha256.isNullOrBlank() && actualSha256 != release.expectedSha256.lowercase()) {
                targetFile.delete()
                return@withContext Result.failure(
                    SecurityException("SHA-256 integrity check failed for downloaded APK"),
                )
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            targetFile.delete()
            Result.failure(e)
        } finally {
            isDownloading = false
        }
    }

    fun promptInstall(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val lParts = local.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
