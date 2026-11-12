package dev.xboyrex.novaplayer.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.novaplayer.PlaylistShortcut
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.feature.player.PlayerActivity
import dev.xboyrex.novaplayer.feature.player.utils.PlaylistPlaybackContract
import dev.xboyrex.novaplayer.feature.playlist.navigation.PlaylistDetailRoute
import dev.xboyrex.novaplayer.feature.playlist.navigation.PlaylistListRoute
import dev.xboyrex.novaplayer.feature.playlist.navigation.navigateToPlaylistDetail
import dev.xboyrex.novaplayer.feature.playlist.navigation.playlistDetailEntry
import dev.xboyrex.novaplayer.feature.playlist.navigation.playlistListEntry
import dev.xboyrex.novaplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.playlistNavGraph(
    context: Context,
    backStack: NavBackStack<NavKey>,
) {
    playlistListEntry(
        onPlaylistClick = backStack::navigateToPlaylistDetail,
        onSettingsClick = backStack::navigateToSettings,
        onAddToHomeScreen = if (PlaylistShortcut.isSupported(context = context)) {
            { playlist ->
                if (!PlaylistShortcut.requestPin(
                        context = context,
                        playlist = playlist,
                    )
                ) {
                    Toast.makeText(
                        context,
                        R.string.playlist_shortcut_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        } else {
            null
        },
    )

    playlistDetailEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayPlaylist = { playlistId, startUri ->
            context.startPlaylistPlayback(
                playlistId = playlistId,
                startUri = startUri,
            )
        },
    )
}

internal fun TopLevelNavState.openPlaylist(playlistId: Long) {
    val playlistStack = backStacks.getValue(key = PlaylistListRoute)
    playlistStack.clear()
    playlistStack.add(element = PlaylistListRoute)
    playlistStack.add(element = PlaylistDetailRoute(playlistId = playlistId))
    switchTo(route = PlaylistListRoute)
}

internal fun Context.startPlaylistPlayback(playlistId: Long, startUri: Uri) {
    startActivity(
        Intent(
            this,
            PlayerActivity::class.java,
        ).apply {
            action = Intent.ACTION_VIEW
            data = startUri
            putExtra(
                PlaylistPlaybackContract.EXTRA_PLAYLIST_ID,
                playlistId,
            )
        },
    )
}
