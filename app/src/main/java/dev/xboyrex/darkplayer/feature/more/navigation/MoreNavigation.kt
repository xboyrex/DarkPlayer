package dev.xboyrex.darkplayer.feature.more.navigation

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.feature.more.screens.history.HistoryScreen
import dev.xboyrex.darkplayer.feature.more.screens.history.HistoryViewModel
import dev.xboyrex.darkplayer.feature.more.screens.more.MoreScreen
import dev.xboyrex.darkplayer.feature.more.screens.more.MoreViewModel
import dev.xboyrex.darkplayer.feature.more.screens.trash.TrashScreen
import dev.xboyrex.darkplayer.feature.more.screens.trash.TrashViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object MoreRoute : NavKey

@Serializable
object HistoryRoute : NavKey

@Serializable
object TrashRoute : NavKey

fun EntryProviderScope<NavKey>.moreEntry(
    onHistoryClick: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit,
    onVaultClick: () -> Unit,
) {
    entry<MoreRoute> {
        val currentHistoryClick by rememberUpdatedState(onHistoryClick)
        val currentPlayVideo by rememberUpdatedState(onPlayVideo)
        val currentSettingsClick by rememberUpdatedState(onSettingsClick)
        val currentTrashClick by rememberUpdatedState(onTrashClick)
        val currentVaultClick by rememberUpdatedState(onVaultClick)

        val output = MoreViewModel.Output(
            openHistory = { currentHistoryClick() },
            playVideo = { uri -> currentPlayVideo(uri) },
            openSettings = { currentSettingsClick() },
            openTrash = { currentTrashClick() },
            openVault = { currentVaultClick() },
        )
        val viewModel = koinViewModel<MoreViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        MoreScreen(
            viewModel = viewModel,
            directOutput = output,
        )
    }
}

fun EntryProviderScope<NavKey>.historyEntry(
    onNavigateUp: () -> Unit,
    onPlayVideo: (String) -> Unit,
) {
    entry<HistoryRoute> {
        val currentNavigateUp by rememberUpdatedState(onNavigateUp)
        val currentPlayVideo by rememberUpdatedState(onPlayVideo)
        val output = HistoryViewModel.Output(
            navigateUp = { currentNavigateUp() },
            playVideo = { uri -> currentPlayVideo(uri) },
        )
        val viewModel = koinViewModel<HistoryViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        HistoryScreen(viewModel = viewModel)
    }
}

fun EntryProviderScope<NavKey>.trashEntry(
    onNavigateUp: () -> Unit,
    onPlayVideo: (String) -> Unit,
) {
    entry<TrashRoute> {
        val currentNavigateUp by rememberUpdatedState(onNavigateUp)
        val currentPlayVideo by rememberUpdatedState(onPlayVideo)
        val output = TrashViewModel.Output(
            navigateUp = { currentNavigateUp() },
            playVideo = { uri -> currentPlayVideo(uri) },
        )
        val viewModel = koinViewModel<TrashViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        TrashScreen(viewModel = viewModel)
    }
}

fun NavBackStack<NavKey>.navigateToMore() {
    add(element = MoreRoute)
}

fun NavBackStack<NavKey>.navigateToHistory() {
    add(element = HistoryRoute)
}

fun NavBackStack<NavKey>.navigateToTrash() {
    add(element = TrashRoute)
}
