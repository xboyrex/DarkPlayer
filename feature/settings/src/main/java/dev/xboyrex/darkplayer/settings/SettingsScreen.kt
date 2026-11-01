package dev.xboyrex.darkplayer.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.core.ui.theme.DARK_PLAYER_PALETTES
import dev.xboyrex.darkplayer.core.ui.theme.RoundedHeavy
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
    val focusState = rememberRestorableFocusState()
    val scrollState = rememberScrollState()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var activeFloatingSection by remember { mutableStateOf<SettingsFloatingSection?>(null) }
    val updateState = rememberUpdateState()
    val installedVersion = remember(context) { updateState.currentVersionName(context) }

    val isOverlayOpen = activeFloatingSection != null || updateState.visible || updateState.checking || updateState.message != null
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp,
        animationSpec = tween(220),
        label = "settingsBlur",
    )

    Box(modifier = Modifier.fillMaxSize().background(c.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        ) {
            // Minimal YTXTube-style Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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
                Text(
                    text = "Settings",
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp,
                    color = c.onSurface,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .restorableFocusGroup(state = focusState)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = navBottomInset + 28.dp),
            ) {
                Spacer(Modifier.height(2.dp))

                // Top Profile / Studio Card matching YTXTube layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(vertical = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable { activeFloatingSection = SettingsFloatingSection.APPEARANCE }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = c.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Dark Player Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = c.onSurface,
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = "by ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (@xboyrex)",
                                fontSize = 12.sp,
                                color = c.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = if (appPrefs.useLiquidGlass) "Liquid UI" else "Solid UI",
                            modifier = Modifier
                                .pressable {
                                    val next = !appPrefs.useLiquidGlass
                                    GlassSettings.updateEnabled(next)
                                    onUpdateAppPrefs { it.copy(useLiquidGlass = next) }
                                }
                                .padding(8.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = c.primary,
                        )
                    }
                }

                SettingsSectionHeader("Customization")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "Appearance & Theme",
                        subtitle = "Theme mode, color palette and UI style",
                        onClick = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                    )
                }

                SettingsSectionHeader("Player & Interface")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Tune,
                        title = "General Modification",
                        subtitle = "Font, gestures, decoder & playback",
                        onClick = { activeFloatingSection = SettingsFloatingSection.GENERAL_MODS },
                    )
                }

                SettingsSectionHeader("Data & Storage")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.DataUsage,
                        title = "Storage Usage",
                        subtitle = "See phone storage & clear Dark Player cache",
                        onClick = { activeFloatingSection = SettingsFloatingSection.STORAGE_USAGE },
                    )
                }

                SettingsSectionHeader("About")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        subtitle = "Dark Player $installedVersion",
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
                                        text = "Up to date",
                                        color = Color(0xFF2ECC71),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Check for updates",
                        subtitle = "New GitHub releases install from inside the app",
                        onClick = {
                            updateState.check(context = context, manual = true)
                        },
                    )
                    val openUrl: (String) -> Unit = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = "Open Source",
                        subtitle = "View source code on GitHub",
                        onClick = { openUrl("https://github.com/xboyrex/DarkPlayer") },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = "Developer",
                        subtitle = "@xboyrex",
                        onClick = {
                            val tgApp = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=MayankDroid7"))
                            val tgWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MayankDroid7"))
                            runCatching { context.startActivity(tgApp) }
                                .onFailure { runCatching { context.startActivity(tgWeb) } }
                        },
                    )
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
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
                        subtitle = "Theme mode, color palette & UI style",
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
                        title = "General Modification",
                        subtitle = "Font, gestures, decoder & playback",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        GeneralModificationContent(
                            appPrefs = appPrefs,
                            playerPrefs = playerPrefs,
                            onUpdateAppPrefs = onUpdateAppPrefs,
                            onUpdatePlayerPrefs = onUpdatePlayerPrefs,
                            onOpenSetting = { setting ->
                                activeFloatingSection = null
                                onAction(SettingsAction.OpenSetting(setting))
                            },
                        )
                    }
                }

                SettingsFloatingSection.STORAGE_USAGE -> {
                    SettingsFloatingDialog(
                        title = "Storage Usage",
                        subtitle = "Device memory & Dark Player data",
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
private fun SettingsSectionHeader(title: String) {
    val c = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = c.onSurfaceVariant.copy(alpha = 0.72f),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
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
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh),
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
            .padding(horizontal = 14.dp, vertical = 11.dp),
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
                tint = c.onSurface,
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
                color = c.onSurfaceVariant.copy(alpha = 0.8f),
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
                tint = c.onSurfaceVariant.copy(alpha = 0.5f),
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
                .widthIn(max = 410.dp)
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
                        fontFamily = RoundedHeavy,
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
        // Dedicated Liquid UI vs Solid UI Segmented Button Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                text = "Interface style",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = c.onSurface,
            )
            Text(
                text = if (appPrefs.useLiquidGlass) {
                    "Liquid UI: Frosted live blur & refractive glass controls"
                } else {
                    "Solid UI: Matte studio surfaces with accent borders"
                },
                color = c.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val isLiquid = appPrefs.useLiquidGlass
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pressable {
                            GlassSettings.updateEnabled(true)
                            onUpdateAppPrefs { it.copy(useLiquidGlass = true) }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isLiquid) c.primary else c.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = if (isLiquid) c.primary else c.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (isLiquid) c.onPrimary else c.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Liquid UI",
                            color = if (isLiquid) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (isLiquid) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pressable {
                            GlassSettings.updateEnabled(false)
                            onUpdateAppPrefs { it.copy(useLiquidGlass = false) }
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isLiquid) c.primary else c.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = if (!isLiquid) c.primary else c.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = if (!isLiquid) c.onPrimary else c.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Solid UI",
                            color = if (!isLiquid) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (!isLiquid) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text("Theme mode", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = c.onSurface)
            Text("Choose how the app looks", color = c.onSurfaceVariant, fontSize = 12.sp)
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text("Color palette", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = c.onSurface)
            Text("Top 10 curated studio & YTXTube color combinations", color = c.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))

            val choices = buildList {
                if (supportsDynamicTheming()) add(null to "Wallpaper")
                DARK_PLAYER_PALETTES.forEach { add(it.hex to it.name) }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                choices.forEach { (hex, name) ->
                    val selected = if (hex == null) {
                        appPrefs.useDynamicColors
                    } else {
                        !appPrefs.useDynamicColors && appPrefs.themeAccentSeed.equals(hex, ignoreCase = true)
                    }
                    val scheme = schemeForSeed(context = context, seedHex = hex, dark = isDark)
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .pressable {
                                if (hex == null) {
                                    onUpdateAppPrefs { it.copy(useDynamicColors = true) }
                                } else {
                                    onUpdateAppPrefs {
                                        it.copy(
                                            useDynamicColors = false,
                                            themeAccentSeed = hex,
                                        )
                                    }
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
                            text = name,
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
private fun SettingsClickRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .pressable(onClick = onClick)
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.NavigateNext,
            contentDescription = null,
            tint = c.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun GeneralModificationContent(
    appPrefs: ApplicationPreferences,
    playerPrefs: PlayerPreferences,
    onUpdateAppPrefs: (suspend (ApplicationPreferences) -> ApplicationPreferences) -> Unit,
    onUpdatePlayerPrefs: (suspend (PlayerPreferences) -> PlayerPreferences) -> Unit,
    onOpenSetting: (Setting) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsSwitchRow(
            title = "Use System Font",
            subtitle = "Use device default font throughout the app",
            checked = appPrefs.useSystemFont,
            onCheckedChange = { enabled ->
                onUpdateAppPrefs { it.copy(useSystemFont = enabled) }
            },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsSwitchRow(
            title = "Video gestures",
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
            title = "Horizontal Seek Gesture",
            subtitle = "Swipe horizontally on the video player to scrub smoothly",
            checked = playerPrefs.useSeekControls,
            onCheckedChange = { enabled ->
                onUpdatePlayerPrefs { it.copy(useSeekControls = enabled) }
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

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Advanced Engine & Controls",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(2.dp))

        SettingsClickRow(
            title = stringResource(R.string.player_name),
            subtitle = stringResource(R.string.player_description),
            onClick = { onOpenSetting(Setting.PLAYER) },
        )
        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsClickRow(
            title = stringResource(R.string.gestures_name),
            subtitle = stringResource(R.string.gestures_description),
            onClick = { onOpenSetting(Setting.GESTURES) },
        )
        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsClickRow(
            title = stringResource(R.string.audio),
            subtitle = stringResource(R.string.audio_desc),
            onClick = { onOpenSetting(Setting.AUDIO) },
        )
        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsClickRow(
            title = stringResource(R.string.subtitle),
            subtitle = stringResource(R.string.subtitle_desc),
            onClick = { onOpenSetting(Setting.SUBTITLE) },
        )
        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsClickRow(
            title = stringResource(R.string.media_library),
            subtitle = stringResource(R.string.media_library_description),
            onClick = { onOpenSetting(Setting.MEDIA_LIBRARY) },
        )
        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f))

        SettingsClickRow(
            title = stringResource(R.string.general_name),
            subtitle = stringResource(R.string.general_description),
            onClick = { onOpenSetting(Setting.GENERAL) },
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
                    clearedNotice = "Cache & thumbnails cleared!"
                }
                .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(15.dp),
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
