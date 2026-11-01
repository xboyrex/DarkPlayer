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
