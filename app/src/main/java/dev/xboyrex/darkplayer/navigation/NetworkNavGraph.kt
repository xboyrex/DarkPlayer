package dev.xboyrex.darkplayer.navigation

import android.content.Context
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.feature.network.navigation.addConnectionEntry
import dev.xboyrex.darkplayer.feature.network.navigation.navigateToAddConnection
import dev.xboyrex.darkplayer.feature.network.navigation.navigateToNetworkBrowse
import dev.xboyrex.darkplayer.feature.network.navigation.networkBrowseEntry
import dev.xboyrex.darkplayer.feature.network.navigation.networkEntry
import dev.xboyrex.darkplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.networkNavGraph(
    context: Context,
    backStack: NavBackStack<NavKey>,
) {
    networkNavGraph(
        context = context,
        backStackProvider = { backStack },
    )
}

fun EntryProviderScope<NavKey>.networkNavGraph(
    context: Context,
    backStackProvider: () -> NavBackStack<NavKey>,
) {
    networkEntry(
        onAddConnection = { backStackProvider().navigateToAddConnection() },
        onEditConnection = { id -> backStackProvider().navigateToAddConnection(connectionId = id) },
        onOpenConnection = { id -> backStackProvider().navigateToNetworkBrowse(connectionId = id) },
        onSettingsClick = { backStackProvider().navigateToSettings() },
        onOpenStream = { uri -> context.startPlayback(uri = uri) },
    )

    addConnectionEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )

    networkBrowseEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        onPlayVideos = { uris, startUri ->
            context.startPlayback(
                uris = uris,
                startUri = startUri,
            )
        },
        onNavigateToFolder = { id, path ->
            backStackProvider().navigateToNetworkBrowse(
                connectionId = id,
                path = path,
            )
        },
    )
}
