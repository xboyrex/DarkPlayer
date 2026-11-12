package dev.xboyrex.darkplayer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationPreferences(
    val sortBy: Sort.By = Sort.By.TITLE,
    val sortOrder: Sort.Order = Sort.Order.ASCENDING,
    val themeConfig: ThemeConfig = ThemeConfig.ON,
    val useHighContrastDarkTheme: Boolean = true,
    val useDynamicColors: Boolean = false,
    val markLastPlayedMedia: Boolean = true,
    val isHistoryPaused: Boolean = false,
    val includeNetworkWatchHistory: Boolean = true,
    val excludeFolders: List<String> = emptyList(),
    val mediaViewMode: MediaViewMode = MediaViewMode.FOLDERS,
    val mediaLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,

    // Network browser
    val networkSortBy: Sort.By = Sort.By.TITLE,
    val networkSortOrder: Sort.Order = Sort.Order.ASCENDING,
    val networkMediaLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,

    // Fields
    val showDurationField: Boolean = true,
    val showFolderDurationField: Boolean = true,
    val showExtensionField: Boolean = false,
    val showPathField: Boolean = false,
    val showResolutionField: Boolean = false,
    val showSizeField: Boolean = true,
    val showThumbnailField: Boolean = true,
    val showPlayedProgress: Boolean = true,
    val showFrameRateField: Boolean = false,
    val showDateField: Boolean = false,

    val newVideoThresholdDays: Int = 7,

    // Thumbnail generation
    val thumbnailGenerationStrategy: ThumbnailGenerationStrategy =
        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE,
    val thumbnailFramePosition: Float = DEFAULT_THUMBNAIL_FRAME_POSITION,

    // Nova Player UI/UX, Liquid Glass & MX Player Pro Customization
    val novaUiStyle: NovaUiStyle = NovaUiStyle.MX_LIQUID_HYBRID,
    val navBarStyle: NovaNavBarStyle = NovaNavBarStyle.YTXTUBE_LIQUID_GLASS,
    val navBarFooterLiftDp: Int = 22,
    val navBarCollapseOnScroll: Boolean = false,
    val switchStyle: NovaSwitchStyle = NovaSwitchStyle.GLASMIO_LIQUID,
    val enableLiquidGlassEffects: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val mediaListCardStyle: MediaListCardStyle = MediaListCardStyle.MX_PRO_CLEAN,
    val mxAppThemeMode: MxAppThemeMode = MxAppThemeMode.AMOLED_BLACK,
    val mxClassicThemeHex: String = "#7C3AED",
    val showMxTopCategoryChips: Boolean = true,
    val showFloatingPlayFab: Boolean = true,

    // MX Player General Preferences
    val playMediaLinks: Boolean = true,
    val httpUserAgent: String = "",
    val showQuitButton: Boolean = false,
    val allowEditing: Boolean = true,
    val clearPlaybackHistoryOnExit: Boolean = false,
    val deleteSubtitleFilesTogether: Boolean = true,
    val cacheThumbnail: Boolean = true,
) {

    companion object {
        const val DEFAULT_THUMBNAIL_FRAME_POSITION = 0.33f
    }
}
