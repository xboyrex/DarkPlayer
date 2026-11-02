package dev.xboyrex.darkplayer.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.ClickablePreferenceItem
import dev.xboyrex.darkplayer.core.ui.components.ListSectionTitle
import dev.xboyrex.darkplayer.core.ui.components.NextTopAppBar
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.components.tvFocusDown
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.popupBackgroundBlur
import dev.xboyrex.darkplayer.core.ui.updater.UpdateOverlayDialog
import dev.xboyrex.darkplayer.core.ui.updater.rememberUpdateState

data class SettingsOutput(val navigateUp: () -> Unit, val openSetting: (Setting) -> Unit)

private sealed interface SettingsAction {
    data object NavigateUp : SettingsAction
    data class OpenSetting(val setting: Setting) : SettingsAction
}

@Composable
fun SettingsScreen(output: SettingsOutput) {
    SettingsScreenContent { action ->
        when (action) {
            is SettingsAction.NavigateUp -> output.navigateUp()
            is SettingsAction.OpenSetting -> output.openSetting(action.setting)
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun SettingsScreenContent(onAction: (SettingsAction) -> Unit) {
    val context = LocalContext.current
    val focusState = rememberRestorableFocusState()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val updateState = rememberUpdateState()
    val installedVersion = remember(context) { updateState.currentVersionName(context) }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = Motion.Emphasized),
        label = "settingsEnterAlpha",
    )
    val enterSlideY by animateFloatAsState(
        targetValue = if (entered) 0f else 18f,
        animationSpec = tween(durationMillis = 280, easing = Motion.Emphasized),
        label = "settingsEnterSlideY",
    )

    val playbackGroup = remember {
        listOf(
            SettingRow.PLAYER,
            SettingRow.GESTURES,
            SettingRow.AUDIO,
            SettingRow.SUBTITLE,
        )
    }
    val interfaceGroup = remember {
        listOf(
            SettingRow.APPEARANCE,
            SettingRow.MEDIA_LIBRARY,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.popupBackgroundBlur(extraBlur = updateState.isPopupActive),
            topBar = {
                NextTopAppBar(
                    title = stringResource(id = R.string.settings),
                    fontWeight = FontWeight.Bold,
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = { onAction(SettingsAction.NavigateUp) },
                            modifier = Modifier.tvFocusDown(focusState.requester),
                        ) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = stringResource(id = R.string.navigate_up),
                            )
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = enterAlpha
                            translationY = enterSlideY
                        }
                        .verticalScroll(state = rememberScrollState())
                        .restorableFocusGroup(state = focusState)
                        .padding(horizontal = 16.dp)
                        .padding(top = 6.dp, bottom = navBottomInset + 36.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    ListSectionTitle(text = "PLAYBACK, DECODERS & GESTURES")
                    playbackGroup.forEachIndexed { index, row ->
                        key(row) {
                            ClickablePreferenceItem(
                                modifier = Modifier.restorableFocusItem(
                                    state = focusState,
                                    row.name,
                                ),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                                isFirstItem = index == 0,
                                isLastItem = index == playbackGroup.lastIndex,
                            )
                        }
                    }

                    ListSectionTitle(text = "INTERFACE & MEDIA LIBRARY")
                    interfaceGroup.forEachIndexed { index, row ->
                        key(row) {
                            ClickablePreferenceItem(
                                modifier = Modifier.restorableFocusItem(
                                    state = focusState,
                                    row.name,
                                ),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                                isFirstItem = index == 0,
                                isLastItem = index == interfaceGroup.lastIndex,
                            )
                        }
                    }

                    ListSectionTitle(text = "GENERAL & DEVELOPER")
                    ClickablePreferenceItem(
                        modifier = Modifier.restorableFocusItem(
                            state = focusState,
                            SettingRow.GENERAL.name,
                        ),
                        title = stringResource(id = SettingRow.GENERAL.titleResId),
                        description = stringResource(id = SettingRow.GENERAL.descriptionResId),
                        icon = SettingRow.GENERAL.icon,
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.GENERAL)) },
                        isFirstItem = true,
                        isLastItem = false,
                    )
                    ClickablePreferenceItem(
                        modifier = Modifier.restorableFocusItem(
                            state = focusState,
                            "CHECK_UPDATES",
                        ),
                        title = "Check for updates",
                        description = "Dark Player v$installedVersion • Tap to check for new release",
                        icon = Icons.Rounded.SystemUpdateAlt,
                        onClick = { updateState.check(context = context, manual = true) },
                        isFirstItem = false,
                        isLastItem = false,
                    )
                    ClickablePreferenceItem(
                        modifier = Modifier.restorableFocusItem(
                            state = focusState,
                            SettingRow.ABOUT.name,
                        ),
                        title = stringResource(id = SettingRow.ABOUT.titleResId),
                        description = stringResource(id = SettingRow.ABOUT.descriptionResId),
                        icon = SettingRow.ABOUT.icon,
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                        isFirstItem = false,
                        isLastItem = true,
                    )
                }

                // Smooth top edge fade under the Settings top bar so scrolled items fade cleanly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
        }

        UpdateOverlayDialog(state = updateState)
    }
}

enum class Setting {
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    GESTURES,
    AUDIO,
    SUBTITLE,
    GENERAL,
    ABOUT,
}

private enum class SettingRow(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector,
    val setting: Setting,
) {
    PLAYER(
        titleResId = R.string.player_name,
        descriptionResId = R.string.player_description,
        icon = NextIcons.Player,
        setting = Setting.PLAYER,
    ),
    GESTURES(
        titleResId = R.string.gestures_name,
        descriptionResId = R.string.gestures_description,
        icon = NextIcons.SwipeHorizontal,
        setting = Setting.GESTURES,
    ),
    AUDIO(
        titleResId = R.string.audio,
        descriptionResId = R.string.audio_desc,
        icon = NextIcons.Audio,
        setting = Setting.AUDIO,
    ),
    SUBTITLE(
        titleResId = R.string.subtitle,
        descriptionResId = R.string.subtitle_desc,
        icon = NextIcons.Subtitle,
        setting = Setting.SUBTITLE,
    ),
    APPEARANCE(
        titleResId = R.string.appearance_name,
        descriptionResId = R.string.appearance_description,
        icon = NextIcons.Appearance,
        setting = Setting.APPEARANCE,
    ),
    MEDIA_LIBRARY(
        titleResId = R.string.media_library,
        descriptionResId = R.string.media_library_description,
        icon = NextIcons.Movie,
        setting = Setting.MEDIA_LIBRARY,
    ),
    GENERAL(
        titleResId = R.string.general_name,
        descriptionResId = R.string.general_description,
        icon = NextIcons.ExtraSettings,
        setting = Setting.GENERAL,
    ),
    ABOUT(
        titleResId = R.string.about_name,
        descriptionResId = R.string.about_description,
        icon = NextIcons.Info,
        setting = Setting.ABOUT,
    ),
}
