package dev.anilbeesetti.nextplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.rememberRestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusGroup
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusItem
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

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
    val settingRows = remember { SettingRow.entries }
    val focusState = rememberRestorableFocusState()

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
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .restorableFocusGroup(state = focusState)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            // Nova UI / UX & MX Pro Studio Banner (Clean without duplicate 5-Band EQ row from Bug #4)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF5C6BC0).copy(alpha = 0.16f),
                                    Color(0xFF9C27B0).copy(alpha = 0.12f),
                                ),
                            ),
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = NextIcons.Appearance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nova Player Pro & Liquid Glass Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Customize YTXTube Liquid Glass NavBar, Glasmio Switches, MX Player Pro Controls & Decoder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QuickStudioPill(
                            title = "UI / UX Switch",
                            icon = NextIcons.Style,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.APPEARANCE)) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickStudioPill(
                            title = "HW+ / Decoder",
                            icon = NextIcons.Cpu,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.PLAYER)) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickStudioPill(
                            title = "Audio & Fade",
                            icon = NextIcons.Equalizer,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.AUDIO)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            ListSectionTitle(text = "MX Player Pro & Nova Categories")

            settingRows.forEachIndexed { index, row ->
                key(row) {
                    ClickablePreferenceItem(
                        modifier = Modifier.restorableFocusItem(
                            state = focusState,
                            row.name,
                        ),
                        title = row.customTitle ?: stringResource(id = row.titleResId),
                        description = row.customDescription ?: stringResource(id = row.descriptionResId),
                        icon = row.icon,
                        onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                        isFirstItem = index == 0,
                        isLastItem = index == settingRows.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStudioPill(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
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
    val customTitle: String? = null,
    val customDescription: String? = null,
) {
    MEDIA_LIBRARY(
        titleResId = R.string.media_library,
        descriptionResId = R.string.media_library_description,
        icon = NextIcons.Folder,
        setting = Setting.MEDIA_LIBRARY,
        customTitle = "List",
        customDescription = "MX View mode, folders, displayed fields, sort & hidden media",
    ),
    PLAYER(
        titleResId = R.string.player_name,
        descriptionResId = R.string.player_description,
        icon = NextIcons.Player,
        setting = Setting.PLAYER,
        customTitle = "Player & Decoder (HW+ / SW)",
        customDescription = "MX Player Pro style, controls, navigation, double tap & HW+/SW decoder",
    ),
    AUDIO(
        titleResId = R.string.audio,
        descriptionResId = R.string.audio_desc,
        icon = NextIcons.Audio,
        setting = Setting.AUDIO,
        customTitle = "Audio & Equalizer",
        customDescription = "Smooth play/pause audio fade, 5-Band EQ, bass boost, reverb & volume boost",
    ),
    SUBTITLE(
        titleResId = R.string.subtitle,
        descriptionResId = R.string.subtitle_desc,
        icon = NextIcons.Subtitle,
        setting = Setting.SUBTITLE,
        customTitle = "Subtitle",
        customDescription = "Alignment, font size, scale, border, shadow, encoding & online subtitles",
    ),
    APPEARANCE(
        titleResId = R.string.appearance_name,
        descriptionResId = R.string.appearance_description,
        icon = NextIcons.Appearance,
        setting = Setting.APPEARANCE,
        customTitle = "App Theme & Liquid Glass UI",
        customDescription = "MX 31 classic themes, YTXTube Liquid Glass NavBar & Glasmio switches",
    ),
    GESTURES(
        titleResId = R.string.gestures_name,
        descriptionResId = R.string.gestures_description,
        icon = NextIcons.SwipeHorizontal,
        setting = Setting.GESTURES,
        customTitle = "Gestures & Bars",
        customDescription = "Volume (Right) & Brightness (Left) swipe gestures, zoom, pan & double tap",
    ),
    GENERAL(
        titleResId = R.string.general_name,
        descriptionResId = R.string.general_description,
        icon = NextIcons.ExtraSettings,
        setting = Setting.GENERAL,
        customTitle = "General",
        customDescription = "Edit access, clear history, export/import settings & reset defaults",
    ),
    ABOUT(
        titleResId = R.string.about_name,
        descriptionResId = R.string.about_description,
        icon = NextIcons.Info,
        setting = Setting.ABOUT,
        customTitle = "About Nova Player",
        customDescription = "Version info, codecs & open-source licenses",
    ),
}
