package com.mayank.darkplayer.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.mayank.darkplayer.settings.Setting
import com.mayank.darkplayer.settings.SettingsOutput
import com.mayank.darkplayer.settings.SettingsScreen
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
