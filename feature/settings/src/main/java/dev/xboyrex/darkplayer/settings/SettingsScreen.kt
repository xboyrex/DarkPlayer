package dev.xboyrex.darkplayer.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xboyrex.darkplayer.core.common.extensions.deleteFiles
import dev.xboyrex.darkplayer.core.common.extensions.subtitleCacheDir
import dev.xboyrex.darkplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.darkplayer.core.model.ApplicationPreferences
import dev.xboyrex.darkplayer.core.model.PlayerPreferences
import dev.xboyrex.darkplayer.core.model.ThemeConfig
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.darkplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.theme.DARK_PLAYER_PALETTES
import dev.xboyrex.darkplayer.core.ui.theme.schemeForSeed
import dev.xboyrex.darkplayer.core.ui.theme.supportsDynamicTheming
import dev.xboyrex.darkplayer.core.ui.updater.UpdateOverlayDialog
import dev.xboyrex.darkplayer.core.ui.updater.rememberUpdateState
import java.io.File
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class SettingsOutput(val navigateUp: () -> Unit, val openSetting: (Setting) -> Unit)

private sealed interface SettingsAction {
    data object NavigateUp : SettingsAction
    data class OpenSetting(val setting: Setting) : SettingsAction
}

private enum class SettingsFloatingSection {
    APPEARANCE,
    GENERAL_MODS,
    STORAGE_USAGE,
}

@Composable
fun SettingsScreen(output: SettingsOutput) {
    val preferencesRepository: PreferencesRepository = koinInject()
    val appPrefs by preferencesRepository.applicationPreferences.collectAsStateWithLifecycle()
    val playerPrefs by preferencesRepository.playerPreferences.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    SettingsScreenContent(
        appPrefs = appPrefs,
        playerPrefs = playerPrefs,
        onUpdateAppPrefs = { transform ->
            scope.launch { preferencesRepository.updateApplicationPreferences(transform) }
        },
        onUpdatePlayerPrefs = { transform ->
            scope.launch { preferencesRepository.updatePlayerPreferences(transform) }
        },
        onAction = { action ->
            when (action) {
                is SettingsAction.NavigateUp -> output.navigateUp()
                is SettingsAction.OpenSetting -> output.openSetting(action.setting)
            }
        },
    )
}

@Composable
private fun SettingsScreenContent(
    appPrefs: ApplicationPreferences,
    playerPrefs: PlayerPreferences,
    onUpdateAppPrefs: (suspend (ApplicationPreferences) -> ApplicationPreferences) -> Unit,
    onUpdatePlayerPrefs: (suspend (PlayerPreferences) -> PlayerPreferences) -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusState = rememberRestorableFocusState()
    val scrollState = rememberScrollState()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var activeFloatingSection by remember { mutableStateOf<SettingsFloatingSection?>(null) }
    val updateState = rememberUpdateState()

    val isOverlayOpen = activeFloatingSection != null || updateState.visible || updateState.checking || updateState.message != null
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp,
        animationSpec = tween(220),
        label = "settingsBlur",
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
            SettingRow.GENERAL,
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(c.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        ) {
            // Distinct Header Bar (No Liquid Glass)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable { onAction(SettingsAction.NavigateUp) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigate_up),
                        tint = c.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = c.onSurface,
                    )
                    Text(
                        text = "Studio Configuration & Media Engine",
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .restorableFocusGroup(state = focusState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = navBottomInset + 36.dp),
            ) {
                // Developer Identity Banner Card (Distinct Layout, Solid Material Surface)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = c.outlineVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(22.dp),
                        )
                        .pressable { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.darkplayer_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.5.sp,
                                color = c.onSurface,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.primaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "PRO",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = c.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "by ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                        contentDescription = null,
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Distinct 3-Tile Quick Studio Grid (Triggers Floating Floating Blurred Sheets)
                SettingsSectionHeader("Quick Studio Panels")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickStudioTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Palette,
                        title = "Theme Studio",
                        subtitle = if (appPrefs.useLiquidGlass) "Liquid Glass" else "Solid UI",
                        accent = c.primary,
                        onClick = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                    )
                    QuickStudioTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Tune,
                        title = "Quick Mods",
                        subtitle = if (playerPrefs.enableVolumeBoost) "200% Boost" else "Standard",
                        accent = c.tertiary,
                        onClick = { activeFloatingSection = SettingsFloatingSection.GENERAL_MODS },
                    )
                    QuickStudioTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.DataUsage,
                        title = "Storage",
                        subtitle = "Cache & Space",
                        accent = c.secondary,
                        onClick = { activeFloatingSection = SettingsFloatingSection.STORAGE_USAGE },
                    )
                }

                SettingsSectionHeader("Playback, Decoders & Gestures")
                SettingsCard {
                    playbackGroup.forEachIndexed { index, row ->
                        SettingsRow(
                            modifier = Modifier.restorableFocusItem(
                                state = focusState,
                                key = row.name,
                            ),
                            icon = row.icon,
                            title = stringResource(id = row.titleResId),
                            subtitle = stringResource(id = row.descriptionResId),
                            onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                        )
                        if (index < playbackGroup.lastIndex) {
                            HorizontalDivider(
                                color = c.onSurface.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                    }
                }

                SettingsSectionHeader("Interface, Library & General")
                SettingsCard {
                    interfaceGroup.forEachIndexed { index, row ->
                        SettingsRow(
                            modifier = Modifier.restorableFocusItem(
                                state = focusState,
                                key = row.name,
                            ),
                            icon = row.icon,
                            title = stringResource(id = row.titleResId),
                            subtitle = stringResource(id = row.descriptionResId),
                            onClick = { onAction(SettingsAction.OpenSetting(row.setting)) },
                        )
                        if (index < interfaceGroup.lastIndex) {
                            HorizontalDivider(
                                color = c.onSurface.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 14.dp),
                            )
                        }
                    }
                }

                SettingsSectionHeader("About & Developer (@xboyrex)")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "About Dark Player",
                        subtitle = "Version info, licenses & developer ecosystem",
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF132818))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2ECC71)),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Stable",
                                        color = Color(0xFF2ECC71),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        },
                    )
                    HorizontalDivider(
                        color = c.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    SettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Check for updates",
                        subtitle = "Verify latest Dark Player release on GitHub",
                        onClick = {
                            updateState.check(context = context, manual = true)
                        },
                    )
                    HorizontalDivider(
                        color = c.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    val openUrl: (String) -> Unit = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = "Open Source Repository",
                        subtitle = "github.com/xboyrex/DarkPlayer",
                        onClick = { openUrl("https://github.com/xboyrex/DarkPlayer") },
                    )
                    HorizontalDivider(
                        color = c.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = "Developer Community",
                        subtitle = "@MayankDroid7 · @OnyxHub7 · @mayank.zx7",
                        onClick = { openUrl("https://t.me/MayankDroid7") },
                    )
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
            )
        }

        AnimatedContent(
            targetState = activeFloatingSection,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.90f) + fadeIn(tween(180))) togetherWith
                    (scaleOut(tween(130), targetScale = 0.92f) + fadeOut(tween(130)))
            },
            label = "settingsFloatingSection",
        ) { section ->
            when (section) {
                SettingsFloatingSection.APPEARANCE -> {
                    SettingsFloatingDialog(
                        title = "Appearance & Theme",
                        subtitle = "Theme mode, color palette & Liquid Glass",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        AppearanceThemeContent(
                            context = context,
                            appPrefs = appPrefs,
                            onUpdateAppPrefs = onUpdateAppPrefs,
                        )
                    }
                }

                SettingsFloatingSection.GENERAL_MODS -> {
                    SettingsFloatingDialog(
                        title = "Quick Playback & Gestures",
                        subtitle = "Volume boost, gestures, font & PiP",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        GeneralModificationContent(
                            appPrefs = appPrefs,
                            playerPrefs = playerPrefs,
                            onUpdateAppPrefs = onUpdateAppPrefs,
                            onUpdatePlayerPrefs = onUpdatePlayerPrefs,
                        )
                    }
                }

                SettingsFloatingSection.STORAGE_USAGE -> {
                    SettingsFloatingDialog(
                        title = "Storage Usage",
                        subtitle = "Device memory & Dark Player cache",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        StorageUsageContent(context = context)
                    }
                }

                null -> {}
            }
        }

        UpdateOverlayDialog(state = updateState)
    }
}

@Composable
private fun QuickStudioTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = c.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp),
            )
            .pressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = c.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = c.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val c = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = c.onSurfaceVariant.copy(alpha = 0.8f),
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = c.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
            ),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(c.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = c.onSurfaceVariant,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                contentDescription = null,
                tint = c.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsFloatingDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surfaceContainerHighest)
                .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .pressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = c.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            HorizontalDivider(
                color = c.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun AppearanceThemeContent(
    context: Context,
    appPrefs: ApplicationPreferences,
    onUpdateAppPrefs: (suspend (ApplicationPreferences) -> ApplicationPreferences) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appPrefs.themeConfig) {
        ThemeConfig.SYSTEM -> systemDark
        ThemeConfig.ON -> true
        ThemeConfig.OFF -> false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        SettingsSwitchRow(
            title = "Liquid Glass (Nav & Player UI)",
            subtitle = if (appPrefs.useLiquidGlass) {
                "Frosted, refractive Liquid Glass in Navigation Bar & Video Player"
            } else {
                "Using solid Material surfaces across Navigation Bar & Video Player"
            },
            checked = appPrefs.useLiquidGlass,
            onCheckedChange = { enabled ->
                GlassSettings.updateEnabled(enabled)
                onUpdateAppPrefs { it.copy(useLiquidGlass = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text("Theme mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.onSurface)
            Text("Choose how Dark Player looks", color = c.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))

            val activeMode = when {
                appPrefs.themeConfig == ThemeConfig.SYSTEM -> "system"
                appPrefs.themeConfig == ThemeConfig.OFF -> "light"
                appPrefs.useHighContrastDarkTheme -> "amoled"
                else -> "dark"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "system" to "System",
                    "dark" to "Dark",
                    "light" to "Light",
                    "amoled" to "AMOLED",
                ).forEach { (mode, label) ->
                    val on = activeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressable {
                                onUpdateAppPrefs { prefs ->
                                    when (mode) {
                                        "system" -> prefs.copy(
                                            themeConfig = ThemeConfig.SYSTEM,
                                            useHighContrastDarkTheme = false,
                                        )
                                        "light" -> prefs.copy(
                                            themeConfig = ThemeConfig.OFF,
                                            useHighContrastDarkTheme = false,
                                        )
                                        "amoled" -> prefs.copy(
                                            themeConfig = ThemeConfig.ON,
                                            useHighContrastDarkTheme = true,
                                        )
                                        else -> prefs.copy(
                                            themeConfig = ThemeConfig.ON,
                                            useHighContrastDarkTheme = false,
                                        )
                                    }
                                }
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.primary else c.surfaceContainerHigh)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (on) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        if (supportsDynamicTheming()) {
            SettingsSwitchRow(
                title = stringResource(R.string.dynamic_theme),
                subtitle = stringResource(R.string.dynamic_theme_description),
                checked = appPrefs.useDynamicColors,
                onCheckedChange = { enabled ->
                    onUpdateAppPrefs { it.copy(useDynamicColors = enabled) }
                },
            )
            HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text("Color palette", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.onSurface)
            Text("Choose an accent color for the app", color = c.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DARK_PLAYER_PALETTES.forEach { palette ->
                    val selected = !appPrefs.useDynamicColors &&
                        appPrefs.themeAccentSeed.equals(palette.hex, ignoreCase = true)
                    val scheme = schemeForSeed(
                        context = context,
                        seedHex = palette.hex,
                        dark = isDark,
                    )
                    Column(
                        modifier = Modifier
                            .width(68.dp)
                            .pressable {
                                onUpdateAppPrefs {
                                    it.copy(
                                        useDynamicColors = false,
                                        themeAccentSeed = palette.hex,
                                    )
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) scheme.primaryContainer else Color.Transparent)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(scheme.primary, scheme.primaryContainer),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = palette.name,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) c.onSurface else c.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralModificationContent(
    appPrefs: ApplicationPreferences,
    playerPrefs: PlayerPreferences,
    onUpdateAppPrefs: (suspend (ApplicationPreferences) -> ApplicationPreferences) -> Unit,
    onUpdatePlayerPrefs: (suspend (PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSwitchRow(
            title = "Use System Font",
            subtitle = "Use device default font throughout Dark Player",
            checked = appPrefs.useSystemFont,
            onCheckedChange = { enabled ->
                onUpdateAppPrefs { it.copy(useSystemFont = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = stringResource(R.string.volume_boost),
            subtitle = "Boost audio volume up to 200% using hardware LoudnessEnhancer",
            checked = playerPrefs.enableVolumeBoost,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(enableVolumeBoost = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Horizontal Seek Gesture",
            subtitle = "Swipe horizontally on the video player to scrub smoothly",
            checked = playerPrefs.useSeekControls,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(useSeekControls = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Vertical Volume & Brightness Swipes",
            subtitle = "Swipe vertically on the left for brightness, right for volume",
            checked = playerPrefs.enableVolumeSwipeGesture && playerPrefs.enableBrightnessSwipeGesture,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs {
                    it.copy(
                        enableVolumeSwipeGesture = enabled,
                        enableBrightnessSwipeGesture = enabled,
                    )
                }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "2× Hold Speed Gesture",
            subtitle = "Long-press on video to accelerate playback speed",
            checked = playerPrefs.useLongPressControls,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(useLongPressControls = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = stringResource(R.string.pip_settings),
            subtitle = stringResource(R.string.pip_settings_description),
            checked = playerPrefs.autoPip,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(autoPip = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = stringResource(R.string.background_play),
            subtitle = stringResource(R.string.background_play_description),
            checked = playerPrefs.autoBackgroundPlay,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(autoBackgroundPlay = enabled) }
            },
        )
    }
}

private data class StorageData(
    val deviceTotal: Long,
    val deviceUsed: Long,
    val deviceFree: Long,
    val appCode: Long,
    val appCache: Long,
    val appThumbnails: Long,
    val appData: Long,
    val appTotal: Long,
)

private fun getFolderSize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) getFolderSize(file) else file.length()
    }
    return size
}

private fun formatStorageBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return if (digitGroups >= 3) {
        String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    } else {
        String.format(Locale.US, "%.0f %s", value, units[digitGroups])
    }
}

private fun calculateStorageData(context: Context): StorageData = runCatching {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val used = (total - free).coerceAtLeast(0L)

    val appSize = runCatching { File(context.packageCodePath).length() }.getOrDefault(18_000_000L)
    val cacheSize = getFolderSize(context.cacheDir) +
        (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
    val thumbsDir = File(context.filesDir, "thumbnails")
    val thumbsSize = getFolderSize(thumbsDir)
    val dataSize = (getFolderSize(context.filesDir) - thumbsSize).coerceAtLeast(0L)

    StorageData(
        deviceTotal = total,
        deviceUsed = used,
        deviceFree = free,
        appCode = appSize,
        appCache = cacheSize,
        appThumbnails = thumbsSize,
        appData = dataSize,
        appTotal = appSize + cacheSize + thumbsSize + dataSize,
    )
}.getOrDefault(
    StorageData(
        deviceTotal = 128_000_000_000L,
        deviceUsed = 45_000_000_000L,
        deviceFree = 83_000_000_000L,
        appCode = 18_000_000L,
        appCache = 12_000_000L,
        appThumbnails = 8_000_000L,
        appData = 4_000_000L,
        appTotal = 42_000_000L,
    ),
)

@Composable
private fun StorageUsageContent(context: Context) {
    val c = MaterialTheme.colorScheme
    var storageData by remember { mutableStateOf(calculateStorageData(context)) }
    var clearedNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            storageData = calculateStorageData(context)
        }
    }

    val usedGb = storageData.deviceUsed / 1_000_000_000.0
    val totalGb = storageData.deviceTotal / 1_000_000_000.0
    val freeGb = storageData.deviceFree / 1_000_000_000.0
    val progress = (
        storageData.deviceUsed.toFloat() /
            storageData.deviceTotal.toFloat().coerceAtLeast(1f)
        ).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "Device storage",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        var animatedTarget by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(progress) {
            animatedTarget = progress
        }
        val animatedProgress by animateFloatAsState(
            targetValue = animatedTarget,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 260f),
            label = "storageProgress",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(c.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                c.primary,
                                c.tertiary,
                                c.secondary,
                            ),
                        ),
                    ),
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = String.format(Locale.US, "%.1f GB used of %.1f GB", usedGb, totalGb),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            Text(
                text = String.format(Locale.US, "%.1f GB free", freeGb),
                fontSize = 11.5.sp,
                color = c.onSurfaceVariant,
            )
        }

        HorizontalDivider(
            color = c.onSurface.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 12.dp),
        )

        Text(
            text = "Used by Dark Player",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Total space: ${formatStorageBytes(storageData.appTotal)}",
            fontSize = 12.sp,
            color = c.primary,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))

        StorageItemRow(label = "App & Core Media3 engine", sizeStr = formatStorageBytes(storageData.appCode))
        StorageItemRow(label = "Video thumbnail cache", sizeStr = formatStorageBytes(storageData.appThumbnails))
        StorageItemRow(label = "Subtitle & temporary cache", sizeStr = formatStorageBytes(storageData.appCache))
        StorageItemRow(label = "Saved playlists & preferences", sizeStr = formatStorageBytes(storageData.appData))

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E1517))
                .pressable {
                    runCatching {
                        context.subtitleCacheDir.deleteFiles()
                        File(context.filesDir, "thumbnails").deleteRecursively()
                        context.cacheDir.deleteRecursively()
                        context.externalCacheDir?.deleteRecursively()
                    }
                    storageData = calculateStorageData(context)
                    clearedNotice = "Thumbnails & cache cleared!"
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = clearedNotice ?: "Clear Dark Player Cache & Thumbnails",
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

@Composable
private fun StorageItemRow(label: String, sizeStr: String) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp, color = c.onSurfaceVariant)
        Text(sizeStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = c.onSurface,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                color = c.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(6.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary),
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
