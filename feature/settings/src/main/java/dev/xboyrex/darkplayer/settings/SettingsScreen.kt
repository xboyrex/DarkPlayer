package dev.xboyrex.darkplayer.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoBlue
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoPink
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaLogoViolet

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
        containerColor = MaterialTheme.colorScheme.background,
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
            // Nova Header Card with Circular Logo & Quick Studio Pills
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            NovaLogoBlue.copy(alpha = 0.40f),
                            NovaLogoViolet.copy(alpha = 0.40f),
                            NovaLogoPink.copy(alpha = 0.35f),
                        ),
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    NovaLogoBlue.copy(alpha = 0.10f),
                                    NovaLogoViolet.copy(alpha = 0.10f),
                                    NovaLogoPink.copy(alpha = 0.08f),
                                ),
                            ),
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nova_launcher),
                            contentDescription = "Nova Player Logo",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nova Player Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "AMOLED Black & Pure White • Smart Gestures • HW+/SW Engine",
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
                            title = "UI Switcher",
                            icon = NextIcons.Style,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.APPEARANCE)) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickStudioPill(
                            title = "Player & HW+",
                            icon = NextIcons.Cpu,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.PLAYER)) },
                            modifier = Modifier.weight(1f),
                        )
                        QuickStudioPill(
                            title = "Audio & EQ",
                            icon = NextIcons.Equalizer,
                            onClick = { onAction(SettingsAction.OpenSetting(Setting.AUDIO)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            ListSectionTitle(text = "Preferences")

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
    APPEARANCE(
        titleResId = R.string.appearance_name,
        descriptionResId = R.string.appearance_description,
        icon = NextIcons.Appearance,
        setting = Setting.APPEARANCE,
        customTitle = "Theme & Interface Switcher",
        customDescription = "AMOLED Pitch Black & Pure White modes, Logo Gradient, Device Accent & Dock",
    ),
    PLAYER(
        titleResId = R.string.player_name,
        descriptionResId = R.string.player_description,
        icon = NextIcons.Player,
        setting = Setting.PLAYER,
        customTitle = "Player & Decoder (HW+ / SW)",
        customDescription = "Playback controls, orientation, fast seek, speed & HW+/SW decoder",
    ),
    GESTURES(
        titleResId = R.string.gestures_name,
        descriptionResId = R.string.gestures_description,
        icon = NextIcons.SwipeHorizontal,
        setting = Setting.GESTURES,
        customTitle = "Smart Gestures & HUD",
        customDescription = "Axis-locked horizontal seek, instant 10s skip, Volume & Brightness HUD",
    ),
    AUDIO(
        titleResId = R.string.audio,
        descriptionResId = R.string.audio_desc,
        icon = NextIcons.Audio,
        setting = Setting.AUDIO,
        customTitle = "Audio & Equalizer",
        customDescription = "Smooth play/pause fade, 5-Band EQ, bass boost, reverb & 200% boost",
    ),
    SUBTITLE(
        titleResId = R.string.subtitle,
        descriptionResId = R.string.subtitle_desc,
        icon = NextIcons.Subtitle,
        setting = Setting.SUBTITLE,
        customTitle = "Subtitles",
        customDescription = "Alignment, font size, scale, border, shadow & encoding",
    ),
    MEDIA_LIBRARY(
        titleResId = R.string.media_library,
        descriptionResId = R.string.media_library_description,
        icon = NextIcons.Folder,
        setting = Setting.MEDIA_LIBRARY,
        customTitle = "Media Library & Folders",
        customDescription = "Folder layout, displayed fields, sort order & hidden media",
    ),
    GENERAL(
        titleResId = R.string.general_name,
        descriptionResId = R.string.general_description,
        icon = NextIcons.ExtraSettings,
        setting = Setting.GENERAL,
        customTitle = "General",
        customDescription = "Storage access, watch history, backup & reset defaults",
    ),
    ABOUT(
        titleResId = R.string.about_name,
        descriptionResId = R.string.about_description,
        icon = NextIcons.Info,
        setting = Setting.ABOUT,
        customTitle = "About Nova Player",
        customDescription = "Version info, background updater & open-source licenses",
    ),
}
