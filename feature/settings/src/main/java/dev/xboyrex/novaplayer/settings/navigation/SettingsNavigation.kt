package dev.xboyrex.novaplayer.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.novaplayer.settings.Setting
import dev.xboyrex.novaplayer.settings.SettingsOutput
import dev.xboyrex.novaplayer.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object SettingsRoute : NavKey

fun NavBackStack<NavKey>.navigateToSettings() {
    add(element = SettingsRoute)
}

fun EntryProviderScope<NavKey>.settingsEntry(
    onNavigateUp: () -> Unit,
    onItemClick: (Setting) -> Unit,
) {
    entry<SettingsRoute> {
        SettingsScreen(
            output = SettingsOutput(
                navigateUp = onNavigateUp,
                openSetting = onItemClick,
            ),
        )
    }
}
