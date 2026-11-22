package com.mayank.darkplayer.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.mayank.darkplayer.settings.Setting
import com.mayank.darkplayer.settings.navigation.aboutPreferencesEntry
import com.mayank.darkplayer.settings.navigation.appearancePreferencesEntry
import com.mayank.darkplayer.settings.navigation.audioPreferencesEntry
import com.mayank.darkplayer.settings.navigation.folderPreferencesEntry
import com.mayank.darkplayer.settings.navigation.generalPreferencesEntry
import com.mayank.darkplayer.settings.navigation.gesturePreferencesEntry
import com.mayank.darkplayer.settings.navigation.librariesEntry
import com.mayank.darkplayer.settings.navigation.mediaLibraryPreferencesEntry
import com.mayank.darkplayer.settings.navigation.navigateToAboutPreferences
import com.mayank.darkplayer.settings.navigation.navigateToAppearancePreferences
import com.mayank.darkplayer.settings.navigation.navigateToAudioPreferences
import com.mayank.darkplayer.settings.navigation.navigateToFolderPreferencesScreen
import com.mayank.darkplayer.settings.navigation.navigateToGeneralPreferences
import com.mayank.darkplayer.settings.navigation.navigateToGesturePreferences
import com.mayank.darkplayer.settings.navigation.navigateToLibraries
import com.mayank.darkplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import com.mayank.darkplayer.settings.navigation.navigateToPlayerPreferences
import com.mayank.darkplayer.settings.navigation.navigateToSubtitlePreferences
import com.mayank.darkplayer.settings.navigation.navigateToThumbnailPreferencesScreen
import com.mayank.darkplayer.settings.navigation.playerPreferencesEntry
import com.mayank.darkplayer.settings.navigation.settingsEntry
import com.mayank.darkplayer.settings.navigation.subtitlePreferencesEntry
import com.mayank.darkplayer.settings.navigation.thumbnailPreferencesEntry

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
