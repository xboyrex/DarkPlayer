package dev.xboyrex.darkplayer.settings.navigation

import androidx.compose.runtime.SideEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.settings.screens.about.AboutPreferencesScreen
import dev.xboyrex.darkplayer.settings.screens.about.AboutPreferencesViewModel
import dev.xboyrex.darkplayer.settings.screens.about.LibrariesScreen
import dev.xboyrex.darkplayer.settings.screens.about.LibrariesViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object AboutPreferencesRoute : NavKey

@Serializable
object LibrariesRoute : NavKey

fun NavBackStack<NavKey>.navigateToAboutPreferences() {
    add(element = AboutPreferencesRoute)
}

fun NavBackStack<NavKey>.navigateToLibraries() {
    add(element = LibrariesRoute)
}

fun EntryProviderScope<NavKey>.aboutPreferencesEntry(
    onLibrariesClick: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    entry<AboutPreferencesRoute> {
        val output = AboutPreferencesViewModel.Output(
            navigateUp = onNavigateUp,
            openLibraries = onLibrariesClick,
        )
        val viewModel = koinViewModel<AboutPreferencesViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        AboutPreferencesScreen(viewModel = viewModel)
    }
}

fun EntryProviderScope<NavKey>.librariesEntry(onNavigateUp: () -> Unit) {
    entry<LibrariesRoute> {
        val output = LibrariesViewModel.Output(navigateUp = onNavigateUp)
        val viewModel = koinViewModel<LibrariesViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        LibrariesScreen(viewModel = viewModel)
    }
}
