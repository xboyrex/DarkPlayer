package com.mayank.darkplayer.navigation

import android.content.Context
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.mayank.darkplayer.feature.network.navigation.addConnectionEntry
import com.mayank.darkplayer.feature.network.navigation.navigateToAddConnection
import com.mayank.darkplayer.feature.network.navigation.navigateToNetworkBrowse
import com.mayank.darkplayer.feature.network.navigation.networkBrowseEntry
import com.mayank.darkplayer.feature.network.navigation.networkEntry
import com.mayank.darkplayer.settings.navigation.navigateToSettings

fun EntryProviderScope<NavKey>.networkNavGraph(context: Context, backStack: NavBackStack<NavKey>) {
    networkEntry(
        onAddConnection = { backStack.navigateToAddConnection() },
        onEditConnection = { id -> backStack.navigateToAddConnection(connectionId = id) },
        onOpenConnection = { id -> backStack.navigateToNetworkBrowse(connectionId = id) },
        onSettingsClick = backStack::navigateToSettings,
        onOpenStream = { uri -> context.startPlayback(uri = uri) },
    )

    addConnectionEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )

    networkBrowseEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onPlayVideos = { uris, startUri ->
            context.startPlayback(
                uris = uris,
                startUri = startUri,
            )
        },
        onNavigateToFolder = { id, path ->
            backStack.navigateToNetworkBrowse(
                connectionId = id,
                path = path,
            )
        },
    )
}
