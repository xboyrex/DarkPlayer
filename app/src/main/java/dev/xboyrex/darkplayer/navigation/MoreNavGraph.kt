package dev.xboyrex.darkplayer.navigation

import android.content.Context
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.feature.more.navigation.historyEntry
import dev.xboyrex.darkplayer.feature.more.navigation.moreEntry
import dev.xboyrex.darkplayer.feature.more.navigation.navigateToHistory
import dev.xboyrex.darkplayer.feature.more.navigation.navigateToTrash
import dev.xboyrex.darkplayer.feature.more.navigation.trashEntry
import dev.xboyrex.darkplayer.feature.videopicker.navigation.navigateToVault
import dev.xboyrex.darkplayer.feature.videopicker.navigation.vaultEntry
import dev.xboyrex.darkplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.moreNavGraph(
    context: Context,
    backStack: NavBackStack<NavKey>,
) {
    moreNavGraph(
        context = context,
        backStackProvider = { backStack },
    )
}

fun EntryProviderScope<NavKey>.moreNavGraph(
    context: Context,
    backStackProvider: () -> NavBackStack<NavKey>,
) {
    moreEntry(
        onHistoryClick = { backStackProvider().navigateToHistory() },
        onPlayVideo = { uriString ->
            context.startPlayback(
                uri = uriString.toUri(),
                grantReadPermission = true,
            )
        },
        onSettingsClick = { backStackProvider().navigateToSettings() },
        onTrashClick = { backStackProvider().navigateToTrash() },
        onVaultClick = { backStackProvider().navigateToVault() },
    )

    historyEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        onPlayVideo = { uriString ->
            context.startPlayback(
                uri = uriString.toUri(),
                grantReadPermission = true,
            )
        },
    )

    trashEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        onPlayVideo = { uriString ->
            context.startPlayback(
                uri = uriString.toUri(),
                grantReadPermission = true,
            )
        },
    )

    vaultEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        // Vault files are served through FileProvider, so read access must be granted at
        // playback time for both PlayerActivity and the (separate) PlayerService component.
        onPlayVideo = { uri ->
            context.startPlayback(
                uri = uri,
                grantReadPermission = true,
            )
        },
        onPlayVideos = { uris ->
            context.startPlayback(
                uris = uris,
                grantReadPermission = true,
            )
        },
    )
}
