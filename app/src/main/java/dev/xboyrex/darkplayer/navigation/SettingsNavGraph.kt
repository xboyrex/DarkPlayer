package dev.xboyrex.darkplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.darkplayer.settings.Setting
import dev.xboyrex.darkplayer.settings.navigation.aboutPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.appearancePreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.audioPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.folderPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.generalPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.gesturePreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.librariesEntry
import dev.xboyrex.darkplayer.settings.navigation.mediaLibraryPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.navigateToAboutPreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToAppearancePreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToAudioPreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToFolderPreferencesScreen
import dev.xboyrex.darkplayer.settings.navigation.navigateToGeneralPreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToGesturePreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToLibraries
import dev.xboyrex.darkplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import dev.xboyrex.darkplayer.settings.navigation.navigateToPlayerPreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToSubtitlePreferences
import dev.xboyrex.darkplayer.settings.navigation.navigateToThumbnailPreferencesScreen
import dev.xboyrex.darkplayer.settings.navigation.playerPreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.settingsEntry
import dev.xboyrex.darkplayer.settings.navigation.subtitlePreferencesEntry
import dev.xboyrex.darkplayer.settings.navigation.thumbnailPreferencesEntry

fun EntryProviderScope<NavKey>.settingsNavGraph(backStack: NavBackStack<NavKey>) {
    settingsNavGraph(backStackProvider = { backStack })
}

fun EntryProviderScope<NavKey>.settingsNavGraph(backStackProvider: () -> NavBackStack<NavKey>) {
    settingsEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        onItemClick = { setting ->
            val backStack = backStackProvider()
            when (setting) {
                Setting.APPEARANCE -> backStack.navigateToAppearancePreferences()
                Setting.MEDIA_LIBRARY -> backStack.navigateToMediaLibraryPreferencesScreen()
                Setting.PLAYER -> backStack.navigateToPlayerPreferences()
                Setting.GESTURES -> backStack.navigateToGesturePreferences()
                Setting.AUDIO -> backStack.navigateToAudioPreferences()
                Setting.SUBTITLE -> backStack.navigateToSubtitlePreferences()
                Setting.GENERAL -> backStack.navigateToGeneralPreferences()
                Setting.ABOUT -> backStack.navigateToAboutPreferences()
            }
        },
    )
    appearancePreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    mediaLibraryPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
        onFolderSettingClick = { backStackProvider().navigateToFolderPreferencesScreen() },
        onThumbnailSettingClick = { backStackProvider().navigateToThumbnailPreferencesScreen() },
    )
    thumbnailPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    folderPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    playerPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    gesturePreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    audioPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    subtitlePreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    generalPreferencesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    aboutPreferencesEntry(
        onLibrariesClick = { backStackProvider().navigateToLibraries() },
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
    librariesEntry(
        onNavigateUp = { backStackProvider().removeLastIfNotRoot() },
    )
}
