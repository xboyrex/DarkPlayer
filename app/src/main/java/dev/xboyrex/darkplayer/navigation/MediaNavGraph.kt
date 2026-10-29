package dev.xboyrex.darkplayer.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.feature.player.PlayerActivity
import dev.xboyrex.darkplayer.feature.player.utils.PlayerApi
import dev.xboyrex.darkplayer.feature.videopicker.navigation.mediaPickerEntry
import dev.xboyrex.darkplayer.feature.videopicker.navigation.navigateToMediaPickerScreen
import dev.xboyrex.darkplayer.feature.videopicker.navigation.navigateToSearch
import dev.xboyrex.darkplayer.feature.videopicker.navigation.navigateToVault
import dev.xboyrex.darkplayer.feature.videopicker.navigation.searchEntry
import dev.xboyrex.darkplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.mediaNavGraph(context: Context, backStack: NavBackStack<NavKey>) {
    mediaPickerEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayVideo = { uri -> context.startPlayback(uri = uri) },
        onPlayVideos = { uris -> context.startPlayback(uris = uris) },
        onFolderClick = backStack::navigateToMediaPickerScreen,
        onSettingsClick = backStack::navigateToSettings,
        onSearchClick = backStack::navigateToSearch,
        onVaultClick = backStack::navigateToVault,
    )

    searchEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayVideo = { uri -> context.startPlayback(uri = uri) },
        onFolderClick = backStack::navigateToMediaPickerScreen,
    )
}

internal fun Context.startPlayback(uri: Uri, grantReadPermission: Boolean = false) {
    startPlayback(
        uri = uri,
        playlist = null,
        grantReadPermission = grantReadPermission,
    )
}

internal fun Context.startPlayback(
    uris: List<Uri>,
    startUri: Uri? = null,
    grantReadPermission: Boolean = false,
) {
    val uri = startUri?.takeIf(predicate = uris::contains) ?: uris.firstOrNull() ?: return
    startPlayback(
        uri = uri,
        playlist = uris,
        grantReadPermission = grantReadPermission,
    )
}

private fun Context.startPlayback(uri: Uri, playlist: List<Uri>?, grantReadPermission: Boolean) {
    if (grantReadPermission) {
        (playlist ?: listOf(element = uri)).forEach {
            grantUriPermission(
                packageName,
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
    val intent = Intent(
        this,
        PlayerActivity::class.java,
    ).apply {
        action = Intent.ACTION_VIEW
        data = uri
        playlist?.let {
            putParcelableArrayListExtra(
                PlayerApi.API_PLAYLIST,
                ArrayList(it),
            )
        }
        if (grantReadPermission) addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(intent)
}
