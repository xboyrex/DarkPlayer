package com.mayank.darkplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.core.ui.components.NextTopAppBar
import com.mayank.darkplayer.core.ui.components.rememberRestorableFocusState
import com.mayank.darkplayer.core.ui.components.restorableFocusGroup
import com.mayank.darkplayer.core.ui.components.restorableFocusItem
import com.mayank.darkplayer.core.ui.components.tvFocusDown
import com.mayank.darkplayer.core.ui.designsystem.NextIcons
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateDialog
import com.mayank.darkplayer.core.ui.updater.DarkPlayerUpdateState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Storage
import com.mayank.darkplayer.core.ui.theme.DarkPlayerAppearanceDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerLanguageDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerStorageDialog
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.mayank.darkplayer.core.ui.theme.calmEntrance
import com.mayank.darkplayer.core.ui.theme.calmPressScale

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
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }
    val updateState = remember(scope, currentVersion) {
        DarkPlayerUpdateState(scope = scope, currentVersionName = currentVersion)
    }
    val focusState = rememberRestorableFocusState()
    DarkPlayerUiPrefs.ensureLoaded(context)
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }

    val primaryRows = remember {
        listOf(SettingRow.APPEARANCE, SettingRow.MEDIA_LIBRARY, SettingRow.PLAYER, SettingRow.GESTURES)
    }
    val mediaRows = remember {
        listOf(SettingRow.AUDIO, SettingRow.SUBTITLE, SettingRow.GENERAL, SettingRow.ABOUT)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NextTopAppBar(
                    title = stringResource(id = R.string.settings),
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SettingsSectionHeader(title = "Dark Player Studio")
                SettingsCardGroup {
                    DarkSettingCardRow(
                        title = "Appearance & Theme",
                        description = "${DarkPlayerUiPrefs.themeMode.label} • ${DarkPlayerUiPrefs.palette.label} • Liquid Glass ${if (DarkPlayerUiPrefs.liquidGlassEnabled) "ON" else "OFF"}",
                        icon = Icons.Rounded.Palette,
                        onClick = { showAppearanceDialog = true },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DarkSettingCardRow(
                        title = "Language",
                        description = "Choose application language & translation",
                        icon = Icons.Rounded.Translate,
                        onClick = { showLanguageDialog = true },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    DarkSettingCardRow(
                        title = "Storage Usage",
                        description = "See device storage & clear Dark Player cache",
                        icon = Icons.Rounded.Storage,
                        onClick = { showStorageDialog = true },
                    )
                }

                SettingsSectionHeader(title = "Interface & Playback")
                SettingsCardGroup {
                    primaryRows.forEachIndexed { index, row ->
                        key(row) {
                            DarkSettingCardRow(
                                modifier = Modifier.restorableFocusItem(focusState, row.name),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                            )
                            if (index < primaryRows.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }

                SettingsSectionHeader(title = "Audio, Subtitles & System")
                SettingsCardGroup {
                    mediaRows.forEachIndexed { index, row ->
                        key(row) {
                            DarkSettingCardRow(
                                modifier = Modifier.restorableFocusItem(focusState, row.name),
                                title = stringResource(id = row.titleResId),
                                description = stringResource(id = row.descriptionResId),
                                icon = row.icon,
                                onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                            )
                            if (index < mediaRows.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }

                SettingsSectionHeader(title = "Quick Controls")
                SettingsCardGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = NextIcons.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Hold for 3.0x Speed",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "Long-press player screen for pitch-locked 3x playback",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = "ON",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                SettingsSectionHeader(title = "Updates")
                SettingsCardGroup {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .calmPressScale(pressedScale = 0.97f).clickable { updateState.check(manual = true) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (updateState.checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Check for Updates",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = updateState.statusMessage ?: "Current version: Dark Player $currentVersion",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        DarkPlayerUpdateDialog(state = updateState)
        if (showAppearanceDialog) DarkPlayerAppearanceDialog(onDismiss = { showAppearanceDialog = false })
        if (showLanguageDialog) DarkPlayerLanguageDialog(onDismiss = { showLanguageDialog = false })
        if (showStorageDialog) DarkPlayerStorageDialog(onDismiss = { showStorageDialog = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        modifier = Modifier.padding(start = 6.dp),
    )
}

@Composable
private fun SettingsCardGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ,
    ) {
        content()
    }
}

@Composable
private fun DarkSettingCardRow(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    APPEARANCE(R.string.appearance_name, R.string.appearance_description, NextIcons.Appearance, Setting.APPEARANCE),
    MEDIA_LIBRARY(R.string.media_library, R.string.media_library_description, NextIcons.Movie, Setting.MEDIA_LIBRARY),
    PLAYER(R.string.player_name, R.string.player_description, NextIcons.Player, Setting.PLAYER),
    GESTURES(R.string.gestures_name, R.string.gestures_description, NextIcons.SwipeHorizontal, Setting.GESTURES),
    AUDIO(R.string.audio, R.string.audio_desc, NextIcons.Audio, Setting.AUDIO),
    SUBTITLE(R.string.subtitle, R.string.subtitle_desc, NextIcons.Subtitle, Setting.SUBTITLE),
    GENERAL(R.string.general_name, R.string.general_description, NextIcons.ExtraSettings, Setting.GENERAL),
    ABOUT(R.string.about_name, R.string.about_description, NextIcons.Info, Setting.ABOUT),
}
