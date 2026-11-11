package dev.anilbeesetti.nextplayer.feature.more.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SavedSearch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.media.services.MediaOperationsService
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.MediaListCardStyle
import dev.anilbeesetti.nextplayer.core.model.MxAppThemeMode
import dev.anilbeesetti.nextplayer.core.model.NovaNavBarStyle
import dev.anilbeesetti.nextplayer.core.model.NovaSwitchStyle
import dev.anilbeesetti.nextplayer.core.model.PlayerControlStyle
import dev.anilbeesetti.nextplayer.core.model.Video
import dev.anilbeesetti.nextplayer.core.model.VolumeBrightnessBarStyle
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.base.DataState
import dev.anilbeesetti.nextplayer.core.ui.components.BindTopLevelFab
import dev.anilbeesetti.nextplayer.core.ui.components.CancelButton
import dev.anilbeesetti.nextplayer.core.ui.components.LocalNavigationBottomPadding
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.components.NextOutlinedTextField
import dev.anilbeesetti.nextplayer.core.ui.components.NextSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.TopLevelFabKey
import dev.anilbeesetti.nextplayer.core.ui.components.rememberRestorableFocusState
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusGroup
import dev.anilbeesetti.nextplayer.core.ui.components.restorableFocusItem
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusRing
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.feature.more.components.HistoryEmptyState
import dev.anilbeesetti.nextplayer.feature.videopicker.composables.VideoGridItem
import io.github.neoruaa.glasmio.ui.GlasmioSegmentedControl
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import kotlin.math.roundToInt

@Composable
fun MoreScreen(viewModel: MoreViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MoreScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

/**
 * MX Player Pro Hub / Drawer Screen + Live UI/UX & Liquid Glass Switcher + App Theme Studio
 * Implements Screenshot_20261110-155349_MX Player Pro.png (#2) and Screenshot_20261110-155414_MX Player Pro.png (#3).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoreScreenContent(state: MoreUiState, onAction: (MoreAction) -> Unit) {
    BindTopLevelFab(
        TopLevelFabKey.MORE,
        NextIcons.Settings,
    ) { onAction(MoreAction.OpenSettings) }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onAction(MoreAction.PlayVideo(it.toString())) }
        }
    val accent = LocalNovaAccentColor.current
    var showNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }
    var showAppThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showStatusSaverDialog by rememberSaveable { mutableStateOf(false) }
    var showLegalDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NextTopAppBar(
                title = "Nova Player Hub",
                fontWeight = FontWeight.Bold,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding.copy(bottom = 0.dp))
                .verticalScroll(state = rememberScrollState())
                .tvListFocus()
                .padding(horizontal = 14.dp)
                .padding(
                    top = 8.dp,
                    bottom =
                        scaffoldPadding.calculateBottomPadding() +
                            LocalNavigationBottomPadding.current + 110.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. MX Player Pro 3x3 Feature Grid Card (Screenshot #2 155349_MX Player Pro.png)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A1F2B),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MxHubGridItem(
                            icon = Icons.Rounded.SwapHoriz,
                            label = "File Transfer",
                            accent = accent,
                            onClick = { filePicker.launch("video/*") },
                            modifier = Modifier.weight(1f),
                        )
                        MxHubGridItem(
                            icon = Icons.Rounded.FolderSpecial,
                            label = "Private Folder",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenVault) },
                            modifier = Modifier.weight(1f),
                        )
                        MxHubGridItem(
                            icon = Icons.Rounded.Add,
                            label = "Video Playlists",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenHistory) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MxHubGridItem(
                            icon = Icons.Rounded.Language,
                            label = "Network\nStream",
                            accent = accent,
                            onClick = { showNetworkStreamDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                        MxHubGridItem(
                            icon = Icons.Rounded.Computer,
                            label = "Local Network",
                            accent = accent,
                            onClick = { onAction(MoreAction.OpenSettings) },
                            modifier = Modifier.weight(1f),
                        )
                        MxHubGridItem(
                            icon = Icons.Rounded.FolderShared,
                            label = "Media Manager",
                            accent = accent,
                            onClick = {
                                if (MediaOperationsService.supportsTrash()) {
                                    onAction(MoreAction.OpenTrash)
                                } else {
                                    filePicker.launch("video/*")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        MxHubGridItem(
                            icon = Icons.Rounded.CloudQueue,
                            label = "Cloud Drive",
                            accent = accent,
                            onClick = { filePicker.launch("video/*") },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.weight(2f))
                    }
                }
            }

            // 2. WhatsApp Status Saver Row (Screenshot #2 155349_MX Player Pro.png)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1F2B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showStatusSaverDialog = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SavedSearch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "WhatsApp Status Saver",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B5C))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${state.history.result.orEmpty().size.coerceAtLeast(1)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF9AA3B2),
                    )
                }
            }

            // 3. Live UI/UX & Liquid Glass Switcher Card (Prompt #4, #6, Glasmio + YTXTube + MX Pro)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A1F2B),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UI / UX & Liquid Glass Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "Choose Navigation Bar, Toggle Switch, Player & Volume Bar styles",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA3B2),
                            )
                        }
                    }

                    // Navigation Bar Style Selector
                    Text(
                        text = "Navigation Bar Engine",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            NovaNavBarStyle.YTXTUBE_LIQUID_GLASS to "YTX Liquid",
                            NovaNavBarStyle.GLASMIO_IOS27_MORPH to "Glasmio iOS",
                            NovaNavBarStyle.MX_PRO_CLASSIC to "MX Classic",
                        ),
                        selected = state.preferences.navBarStyle,
                        onSelected = { selectedStyle ->
                            onAction(
                                MoreAction.UpdateAppPreferences {
                                    it.copy(navBarStyle = selectedStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Navigation Bar Footer Lift Slider ("navigation bar or upar kro okh footer se")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Navigation Bar Lift from Footer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        Text(
                            text = "${state.preferences.navBarFooterLiftDp} dp",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                        )
                    }
                    GlasmioSlider(
                        value = state.preferences.navBarFooterLiftDp.toFloat(),
                        onValueChange = { newLift ->
                            onAction(
                                MoreAction.UpdateAppPreferences {
                                    it.copy(navBarFooterLiftDp = newLift.roundToInt().coerceIn(8, 44))
                                },
                            )
                        },
                        valueRange = 8f..44f,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Toggle Switch Style Selector
                    Text(
                        text = "Toggle Switch & Control Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            NovaSwitchStyle.GLASMIO_LIQUID to "Glasmio Glass",
                            NovaSwitchStyle.MX_PRO_CLASSIC to "MX Classic",
                            NovaSwitchStyle.MATERIAL_EXPRESSIVE to "Material 3",
                        ),
                        selected = state.preferences.switchStyle,
                        onSelected = { style ->
                            onAction(
                                MoreAction.UpdateAppPreferences {
                                    it.copy(switchStyle = style)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Volume & Brightness Bar Style Selector
                    Text(
                        text = "Volume & Brightness Bar Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            VolumeBrightnessBarStyle.MX_PRO_VERTICAL to "MX Pro Bar",
                            VolumeBrightnessBarStyle.GLASMIO_LIQUID_CAPSULE to "Glasmio Capsule",
                            VolumeBrightnessBarStyle.NEXT_VERTICAL_PILL to "Minimal Pill",
                        ),
                        selected = state.playerPreferences.volumeBrightnessBarStyle,
                        onSelected = { barStyle ->
                            onAction(
                                MoreAction.UpdatePlayerPreferences {
                                    it.copy(volumeBrightnessBarStyle = barStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Player Control Style Selector
                    Text(
                        text = "Player Interface Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    GlasmioSegmentedControl(
                        items = listOf(
                            PlayerControlStyle.MX_PRO to "MX Player Pro",
                            PlayerControlStyle.LIQUID_GLASS to "Liquid Glass",
                            PlayerControlStyle.CLASSIC_NEXT to "Classic Next",
                        ),
                        selected = state.playerPreferences.playerControlStyle,
                        onSelected = { pStyle ->
                            onAction(
                                MoreAction.UpdatePlayerPreferences {
                                    it.copy(playerControlStyle = pStyle)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liquid Glass Refraction Shaders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Real-time backdrop lens & vibrancy across navigation & player",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA3B2),
                            )
                        }
                        NextSwitch(
                            checked = state.preferences.enableLiquidGlassEffects,
                            onCheckedChange = { enabled ->
                                onAction(
                                    MoreAction.UpdateAppPreferences {
                                        it.copy(enableLiquidGlassEffects = enabled)
                                    },
                                )
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clean MX Folder & Video List",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = "Flat borderless rows like MX Player Pro",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA3B2),
                            )
                        }
                        NextSwitch(
                            checked = state.preferences.mediaListCardStyle == MediaListCardStyle.MX_PRO_CLEAN,
                            onCheckedChange = { clean ->
                                onAction(
                                    MoreAction.UpdateAppPreferences {
                                        it.copy(
                                            mediaListCardStyle = if (clean) {
                                                MediaListCardStyle.MX_PRO_CLEAN
                                            } else {
                                                MediaListCardStyle.SEGMENTED_CARD
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            }

            // 4. App Theme + Settings Card (Screenshot #2 155349_MX Player Pro.png)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1F2B),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MxHubMenuRow(
                        icon = Icons.Rounded.Palette,
                        title = "App Theme",
                        onClick = { showAppThemeDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = Color.White.copy(alpha = 0.07f),
                    )
                    MxHubMenuRow(
                        icon = Icons.Rounded.Settings,
                        title = "Settings",
                        onClick = { onAction(MoreAction.OpenSettings) },
                    )
                }
            }

            // 5. Legal + Help Card (Screenshot #2 155349_MX Player Pro.png)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1F2B),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MxHubMenuRow(
                        icon = Icons.Rounded.Gavel,
                        title = "Legal",
                        onClick = { showLegalDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = Color.White.copy(alpha = 0.07f),
                    )
                    MxHubMenuRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Help",
                        onClick = { showHelpDialog = true },
                    )
                }
            }

            // 6. Watch History Section
            HistorySection(
                history = state.history,
                preferences = state.preferences,
                onMoreClick = { onAction(MoreAction.OpenHistory) },
                onVideoClick = { onAction(MoreAction.PlayVideo(it.uriString)) },
            )
        }
    }

    if (showNetworkStreamDialog) {
        var streamUrl by rememberSaveable { mutableStateOf("") }
        NextDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            title = { Text("Network Stream") },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter network stream URL (HTTP, HTTPS, M3U8, DASH, RTSP):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NextOutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("https://example.com/live.m3u8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = streamUrl.isNotBlank(),
                    onClick = {
                        val trimmed = streamUrl.trim()
                        if (trimmed.isNotEmpty()) {
                            showNetworkStreamDialog = false
                            onAction(MoreAction.PlayVideo(trimmed))
                        }
                    },
                ) {
                    Text("Play")
                }
            },
            dismissButton = {
                CancelButton(onClick = { showNetworkStreamDialog = false })
            },
        )
    }

    // MX Player Pro App Theme Dialog (Exact match to Screenshot #3 155414_MX Player Pro.png)
    if (showAppThemeDialog) {
        MxAppThemeDialog(
            preferences = state.preferences,
            onDismiss = { showAppThemeDialog = false },
            onSelectMode = { mode ->
                onAction(
                    MoreAction.UpdateAppPreferences {
                        it.copy(
                            mxAppThemeMode = mode,
                            useHighContrastDarkTheme = mode == MxAppThemeMode.AMOLED_BLACK,
                        )
                    },
                )
            },
            onSelectAccentHex = { hex ->
                onAction(
                    MoreAction.UpdateAppPreferences {
                        it.copy(mxClassicThemeHex = hex)
                    },
                )
            },
        )
    }

    if (showStatusSaverDialog) {
        NextDialog(
            onDismissRequest = { showStatusSaverDialog = false },
            title = { Text("WhatsApp Status Videos") },
            content = {
                val recentVideos = state.history.result.orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Detected status & recent media clips on your device:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (recentVideos.isEmpty()) {
                        Text(
                            text = "No recent status videos found in WhatsApp/Media/.Statuses.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        recentVideos.take(5).forEach { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        showStatusSaverDialog = false
                                        onAction(MoreAction.PlayVideo(video.uriString))
                                    }
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = NextIcons.Video,
                                    contentDescription = null,
                                    tint = accent,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = video.displayName,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusSaverDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {},
        )
    }

    if (showLegalDialog) {
        NextDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text("Legal & Open Source Notices") },
            content = {
                Text(
                    text = "Nova Player integrates NextPlayer Media3/FFmpeg decoding, Glasmio iOS 27 Liquid Glass UI, YTXTube Backdrop Navigation, and MX Player Pro controls under Apache 2.0 / GPLv3 licenses.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) { Text("OK") }
            },
            dismissButton = {},
        )
    }

    if (showHelpDialog) {
        NextDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Nova Player Help & Gestures") },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Swipe Left Vertical: Adjust Screen Brightness (0–100%)")
                    Text("• Swipe Right Vertical: Adjust Audio Volume (0–200% Boost)")
                    Text("• Horizontal Drag: Fine-scrub video playback position")
                    Text("• Pinch Two Fingers: Video Zoom & Pan (50%–400%)")
                    Text("• Long Press Video: 2.0x Fast Forward Speed")
                    Text("• Top Quick Shortcuts: Night Mode, A-B Repeat, Sleep Timer, Equalizer, Screenshot & more")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got It") }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun MxHubGridItem(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = Color(0xFFE0E5F0),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun MxHubMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFFB8C0CC),
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF8A93A2),
        )
    }
}

/**
 * Exact implementation of Screenshot_20261110-155414_MX Player Pro.png (#3 in mx_screenshots):
 * - Adaptive, Dark Theme, Light Theme, Amoled Black cards with "Using" badge
 * - Classic Themes 31-color swatch grid
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MxAppThemeDialog(
    preferences: ApplicationPreferences,
    onDismiss: () -> Unit,
    onSelectMode: (MxAppThemeMode) -> Unit,
    onSelectAccentHex: (String) -> Unit,
) {
    val classicSwatches = remember {
        listOf(
            "#FF6E5B", "#F59331", "#F2C522", "#9D6644", "#B8DB18", "#5AD21F",
            "#3AC759", "#1EC980", "#429EED", "#1E88E5", "#5C6BC0", "#7E57C2",
            "#D542E6", "#F04886", "#EB3B78", "#E8EAED", "#FF5252", "#FF9100",
            "#FBC02D", "#8D6E63", "#AED581", "#4CAF50", "#00E676", "#00BFA5",
            "#4FC3F7", "#1976D2", "#5E35B1", "#8E24AA", "#E040FB", "#FF4081",
            "#FF4E88",
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131720),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )

                // Top 2x2 Theme Mode Cards matching MX #3 (155414)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MxThemeModeBanner(
                        title = "Adaptive",
                        selected = preferences.mxAppThemeMode == MxAppThemeMode.ADAPTIVE,
                        bgBrush = Brush.linearGradient(
                            listOf(Color(0xFFB3D8FF), Color(0xFF7FA8E0)),
                        ),
                        textColor = Color(0xFF1A2536),
                        onClick = { onSelectMode(MxAppThemeMode.ADAPTIVE) },
                        modifier = Modifier.weight(1f),
                    )
                    MxThemeModeBanner(
                        title = "Dark Theme",
                        selected = preferences.mxAppThemeMode == MxAppThemeMode.DARK_THEME,
                        bgBrush = Brush.linearGradient(
                            listOf(Color(0xFF28344A), Color(0xFF1B2333)),
                        ),
                        textColor = Color.White,
                        onClick = { onSelectMode(MxAppThemeMode.DARK_THEME) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MxThemeModeBanner(
                        title = "Light Theme",
                        selected = preferences.mxAppThemeMode == MxAppThemeMode.LIGHT_THEME,
                        bgBrush = Brush.linearGradient(
                            listOf(Color(0xFFF3F6FA), Color(0xFFDCE5F2)),
                        ),
                        textColor = Color(0xFF1A2536),
                        onClick = { onSelectMode(MxAppThemeMode.LIGHT_THEME) },
                        modifier = Modifier.weight(1f),
                    )
                    MxThemeModeBanner(
                        title = "Amoled Black",
                        selected = preferences.mxAppThemeMode == MxAppThemeMode.AMOLED_BLACK,
                        bgBrush = Brush.linearGradient(
                            listOf(Color(0xFF08090C), Color(0xFF000000)),
                        ),
                        textColor = Color.White,
                        onClick = { onSelectMode(MxAppThemeMode.AMOLED_BLACK) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "Classic Themes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 6,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    classicSwatches.forEach { hex ->
                        val color = remember(hex) {
                            runCatching { Color(hex.toColorInt()) }.getOrDefault(Color(0xFF0088FF))
                        }
                        val isSelected = preferences.mxClassicThemeHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.5.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onSelectAccentHex(hex) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MxThemeModeBanner(
    title: String,
    selected: Boolean,
    bgBrush: Brush,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalNovaAccentColor.current
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2ED573))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Using",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: DataState<List<Video>>,
    preferences: ApplicationPreferences,
    onMoreClick: () -> Unit,
    onVideoClick: (Video) -> Unit,
) {
    val videos = history.result.orEmpty().take(n = 10)
    val isTv = LocalContext.current.isTelevision
    val historyButtonFocusRequester = remember { FocusRequester() }
    val historyFocusState = rememberRestorableFocusState()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().thenIf(condition = isTv) { focusGroup() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp).weight(weight = 1f),
            )
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .focusRequester(focusRequester = historyButtonFocusRequester)
                    .focusProperties {
                        if (isTv && !preferences.isHistoryPaused && videos.isNotEmpty()) {
                            down = historyFocusState.requester
                        }
                    }
                    .tvFocusRing(),
            ) {
                Icon(
                    imageVector = NextIcons.ArrowForward,
                    contentDescription = stringResource(R.string.history),
                )
            }
        }
        when {
            preferences.isHistoryPaused -> HistoryEmptyState(
                isHistoryOff = true,
                modifier = Modifier.fillMaxWidth(),
            )

            history is DataState.Success && videos.isEmpty() -> HistoryEmptyState(
                isHistoryOff = false,
                modifier = Modifier.fillMaxWidth(),
            )

            else -> {
                LazyRow(
                    modifier = Modifier
                        .focusProperties {
                            onExit = {
                                if (isTv && requestedFocusDirection == FocusDirection.Up) {
                                    historyButtonFocusRequester.requestFocus()
                                }
                            }
                        }
                        .restorableFocusGroup(
                            state = historyFocusState,
                            ready = false,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = videos,
                        key = { it.uriString },
                    ) { video ->
                        VideoGridItem(
                            video = video,
                            isRecentlyPlayedVideo = false,
                            textStyle = MaterialTheme.typography.bodySmall,
                            preferences = preferences,
                            modifier = Modifier
                                .width(140.dp)
                                .restorableFocusItem(
                                    state = historyFocusState,
                                    video.uriString,
                                ),
                            onClick = { onVideoClick(video) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MoreScreenContentPreview() {
    MoreScreenContent(
        state = MoreUiState(
            history = DataState.Success(value = emptyList()),
        ),
        onAction = {},
    )
}
