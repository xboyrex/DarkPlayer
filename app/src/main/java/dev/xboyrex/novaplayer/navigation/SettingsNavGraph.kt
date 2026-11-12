package dev.xboyrex.novaplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.xboyrex.novaplayer.settings.Setting
import dev.xboyrex.novaplayer.settings.navigation.aboutPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.appearancePreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.audioPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.folderPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.generalPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.gesturePreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.librariesEntry
import dev.xboyrex.novaplayer.settings.navigation.mediaLibraryPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.navigateToAboutPreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToAppearancePreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToAudioPreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToFolderPreferencesScreen
import dev.xboyrex.novaplayer.settings.navigation.navigateToGeneralPreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToGesturePreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToLibraries
import dev.xboyrex.novaplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import dev.xboyrex.novaplayer.settings.navigation.navigateToPlayerPreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToSubtitlePreferences
import dev.xboyrex.novaplayer.settings.navigation.navigateToThumbnailPreferencesScreen
import dev.xboyrex.novaplayer.settings.navigation.playerPreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.settingsEntry
import dev.xboyrex.novaplayer.settings.navigation.subtitlePreferencesEntry
import dev.xboyrex.novaplayer.settings.navigation.thumbnailPreferencesEntry

fun EntryProviderScope<NavKey>.settingsNavGraph(backStack: NavBackStack<NavKey>) {
    settingsEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onItemClick = { setting ->
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
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    mediaLibraryPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
        onFolderSettingClick = backStack::navigateToFolderPreferencesScreen,
        onThumbnailSettingClick = backStack::navigateToThumbnailPreferencesScreen,
    )
    thumbnailPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    folderPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    playerPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    gesturePreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    audioPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    subtitlePreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    generalPreferencesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    aboutPreferencesEntry(
        onLibrariesClick = backStack::navigateToLibraries,
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
    librariesEntry(
        onNavigateUp = { backStack.removeLastIfNotRoot() },
    )
}
