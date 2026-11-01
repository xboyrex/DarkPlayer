package dev.xboyrex.darkplayer.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .restorableFocusGroup(state = focusState)
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = navBottomInset + 36.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                Surface(
                    onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.06f),
                                ),
                            ),
                            shape = RoundedCornerShape(22.dp),
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        Color.Transparent,
                                    ),
                                ),
                            )
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.darkplayer_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "by ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = NextIcons.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

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
