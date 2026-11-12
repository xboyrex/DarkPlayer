package dev.xboyrex.novaplayer.settings

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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.novaplayer.core.data.repository.PreferencesRepository
import dev.xboyrex.novaplayer.core.model.ThemeConfig
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.components.rememberRestorableFocusState
import dev.xboyrex.novaplayer.core.ui.components.restorableFocusGroup
import dev.xboyrex.novaplayer.core.ui.components.restorableFocusItem
import dev.xboyrex.novaplayer.core.ui.components.tvFocusDown
import dev.xboyrex.novaplayer.core.ui.designsystem.NovaIcons
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyAboutDialog
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyController
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyPresetsData
import dev.xboyrex.novaplayer.core.ui.dolby.NovaDolbyStudioDialog
import dev.xboyrex.novaplayer.core.ui.glass.GlassSettings
import dev.xboyrex.novaplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import dev.xboyrex.novaplayer.core.ui.update.NovaUpdateOverlay
import dev.xboyrex.novaplayer.core.ui.update.NovaUpdateState
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

data class SettingsOutput(val navigateUp: () -> Unit, val openSetting: (Setting) -> Unit)

private sealed interface SettingsAction {
    data object NavigateUp : SettingsAction
    data class OpenSetting(val setting: Setting) : SettingsAction
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

private enum class SettingsFloatingSection {
    APPEARANCE,
    STORAGE_USAGE,
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

@Composable
private fun SettingsScreenContent(onAction: (SettingsAction) -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusState = rememberRestorableFocusState()
    val preferencesRepository: PreferencesRepository = koinInject()
    val backdrop = LocalGlassBackdrop.current

    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.5.0"
        }.getOrDefault("2.5.0")
    }
    val updateState = remember { NovaUpdateState(scope, appVersion) }

    var activeFloatingSection by remember { mutableStateOf<SettingsFloatingSection?>(null) }
    var showDolbyStudio by rememberSaveable { mutableStateOf(false) }
    var showDolbyAbout by rememberSaveable { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateFeedback by remember { mutableStateOf<String?>(null) }

    val isOverlayOpen = activeFloatingSection != null || checkingUpdate || updateFeedback != null
    val blurRadius by animateDpAsState(
        targetValue = if (isOverlayOpen && Build.VERSION.SDK_INT >= 31) 16.dp else 0.dp,
        animationSpec = tween(220),
        label = "settingsBlur",
    )

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface)
                .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        ) {
            // Nova BackScreenHeader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 8.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .tvFocusDown(focusState.requester)
                        .novaPressable { onAction(SettingsAction.NavigateUp) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.navigate_up),
                        tint = c.onSurface,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.settings),
                    fontFamily = NovaRoundedHeavy,
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
                    .padding(bottom = navBottom + 32.dp),
            ) {
                // Nova Player Brand & Dolby Studio Quick Card
                NovaBrandHeaderCard(
                    appVersion = appVersion,
                    onOpenDolby = { showDolbyStudio = true },
                    onOpenAppearancePopup = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                )

                SettingsSectionHeader("Customization")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Liquid Glass & Theme Mode",
                        subtitle = "Pure AMOLED Black / Pure White & frosted glass surfaces",
                        onClick = { activeFloatingSection = SettingsFloatingSection.APPEARANCE },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.APPEARANCE.name),
                        icon = Icons.Rounded.Palette,
                        title = stringResource(id = R.string.appearance_name),
                        subtitle = stringResource(id = R.string.appearance_description),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.APPEARANCE)) },
                    )
                }

                SettingsSectionHeader("Dolby Atmos & Audio Engine")
                SettingsCard {
                    val prof = NovaDolbyPresetsData.PROFILES.find { it.id == NovaDolbyController.currentProfile }?.name ?: "Movie"
                    SettingsRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Dolby Atmos & 20-Band Equalizer",
                        subtitle = if (NovaDolbyController.enabled) {
                            "On ($prof · ${NovaDolbyController.currentPresetName})"
                        } else {
                            "Configure spatial audio, 10/15/20-band EQ & bass curves"
                        },
                        onClick = { showDolbyStudio = true },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.AUDIO.name),
                        icon = NovaIcons.Audio,
                        title = stringResource(id = R.string.audio),
                        subtitle = stringResource(id = R.string.audio_desc),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.AUDIO)) },
                    )
                }

                SettingsSectionHeader("Player & Interface")
                SettingsCard {
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.PLAYER.name),
                        icon = NovaIcons.Player,
                        title = stringResource(id = R.string.player_name),
                        subtitle = stringResource(id = R.string.player_description),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.PLAYER)) },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.GESTURES.name),
                        icon = NovaIcons.SwipeHorizontal,
                        title = stringResource(id = R.string.gestures_name),
                        subtitle = stringResource(id = R.string.gestures_description),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.GESTURES)) },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.SUBTITLE.name),
                        icon = NovaIcons.Subtitle,
                        title = stringResource(id = R.string.subtitle),
                        subtitle = stringResource(id = R.string.subtitle_desc),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.SUBTITLE)) },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.MEDIA_LIBRARY.name),
                        icon = NovaIcons.Movie,
                        title = stringResource(id = R.string.media_library),
                        subtitle = stringResource(id = R.string.media_library_description),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.MEDIA_LIBRARY)) },
                    )
                }

                SettingsSectionHeader("Data, Storage & Backup")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.DataUsage,
                        title = "Storage Usage & Cache",
                        subtitle = "Inspect device storage & clear Nova Player cache",
                        onClick = { activeFloatingSection = SettingsFloatingSection.STORAGE_USAGE },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.GENERAL.name),
                        icon = NovaIcons.ExtraSettings,
                        title = stringResource(id = R.string.general_name),
                        subtitle = "Export/Import JSON settings backup & reset options",
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.GENERAL)) },
                    )
                }

                SettingsSectionHeader("About & Updates")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        subtitle = "Nova Player v$appVersion",
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
                        subtitle = "New GitHub releases install directly inside the app",
                        onClick = {
                            if (checkingUpdate) return@SettingsRow
                            checkingUpdate = true
                            updateFeedback = null
                            scope.launch {
                                val start = System.currentTimeMillis()
                                updateState.check(manual = true)
                                val elapsed = System.currentTimeMillis() - start
                                if (elapsed < 1000) delay(1000 - elapsed)
                                checkingUpdate = false
                                if (!updateState.visible) {
                                    updateFeedback = "You're on the latest version (v$appVersion)"
                                }
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = "Open Source",
                        subtitle = "View source code on GitHub (@xboyrex)",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xboyrex/NovaPlayer")),
                                )
                            }
                        },
                    )
                    SettingsRow(
                        icon = Icons.Rounded.Person,
                        title = "Developer & Dolby Credits",
                        subtitle = "Mayank (@xboyrex) · Telegram, YouTube & Instagram",
                        onClick = { showDolbyAbout = true },
                    )
                    SettingsRow(
                        modifier = Modifier.restorableFocusItem(focusState, Setting.ABOUT.name),
                        icon = NovaIcons.Info,
                        title = stringResource(id = R.string.about_name),
                        subtitle = stringResource(id = R.string.about_description),
                        onClick = { onAction(SettingsAction.OpenSetting(Setting.ABOUT)) },
                    )
                }
            }
        }

        if (isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f)),
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
                        subtitle = "Liquid Glass & Pure AMOLED Black / White mode",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        AppearanceFloatingContent(
                            onSelectMode = { isAmoled ->
                                GlassSettings.updateThemeMode(if (isAmoled) "amoled" else "white")
                                scope.launch {
                                    preferencesRepository.updateApplicationPreferences {
                                        it.copy(
                                            themeConfig = if (isAmoled) ThemeConfig.ON else ThemeConfig.OFF,
                                            useHighContrastDarkTheme = true,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
                SettingsFloatingSection.STORAGE_USAGE -> {
                    SettingsFloatingDialog(
                        title = "Storage Usage",
                        subtitle = "Device memory & Nova Player cache",
                        onDismiss = { activeFloatingSection = null },
                    ) {
                        StorageUsageFloatingContent(context = context)
                    }
                }
                null -> {}
            }
        }

        AnimatedVisibility(
            visible = checkingUpdate || updateFeedback != null,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.88f) + fadeIn(tween(160)),
            exit = scaleOut(tween(120), targetScale = 0.92f) + fadeOut(tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (!checkingUpdate) updateFeedback = null },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 350.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceContainerHighest)
                        .border(1.dp, c.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { }
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (checkingUpdate) {
                        CircularProgressIndicator(
                            color = c.primary,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Checking for updates…",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Connecting to GitHub releases…",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    } else if (updateFeedback != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF132818)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF2ECC71),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Up to date",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = updateFeedback.orEmpty(),
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.primary)
                                .novaPressable { updateFeedback = null }
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("OK", color = c.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        NovaUpdateOverlay(state = updateState, backdrop = backdrop)
    }

    if (showDolbyStudio) {
        NovaDolbyStudioDialog(onDismiss = { showDolbyStudio = false })
    }
    if (showDolbyAbout) {
        NovaDolbyAboutDialog(onDismiss = { showDolbyAbout = false })
    }
}

@Composable
private fun NovaBrandHeaderCard(
    appVersion: String,
    onOpenDolby: () -> Unit,
    onOpenAppearancePopup: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_nova_logo),
                contentDescription = "Nova Player Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Nova Player",
                    fontFamily = NovaRoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = c.onSurface,
                )
                Text(
                    text = "v$appVersion · Liquid Glass & Dolby Studio",
                    fontSize = 12.sp,
                    color = c.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primaryContainer)
                    .novaPressable(onClick = onOpenDolby)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "Dolby Studio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = c.onPrimaryContainer,
                )
            }
        }
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
            .then(if (onClick != null) Modifier.novaPressable(onClick = onClick) else Modifier)
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
                color = c.onSurfaceVariant.copy(alpha = 0.82f),
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
                        .novaPressable(onClick = onDismiss),
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
                        fontFamily = NovaRoundedHeavy,
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
                        .novaPressable(onClick = onDismiss),
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
private fun AppearanceFloatingContent(
    onSelectMode: (Boolean) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val isAmoled = GlassSettings.themeMode != "white"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        SettingsSwitchRow(
            title = "Liquid Glass",
            subtitle = if (GlassSettings.enabled) {
                "Frosted, see-through surfaces with depth and blur"
            } else {
                "Using solid Material surfaces with no transparency"
            },
            checked = GlassSettings.enabled,
            onCheckedChange = { GlassSettings.updateEnabled(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            Text("Color Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.onSurface)
            Text(
                "Strictly two high-contrast modes: Pure AMOLED Black & Pure White",
                color = c.onSurfaceVariant,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    true to "Pure AMOLED Black",
                    false to "Pure White",
                ).forEach { (amoledMode, label) ->
                    val on = isAmoled == amoledMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .novaPressable { onSelectMode(amoledMode) }
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.primary else c.surfaceContainerHigh)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (on) c.onPrimary else c.onSurfaceVariant,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        SettingsSwitchRow(
            title = "Use System Font",
            subtitle = "Use device default font throughout Nova Player",
            checked = GlassSettings.useSystemFont,
            onCheckedChange = { GlassSettings.updateUseSystemFont(it) },
        )

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

        SettingsSwitchRow(
            title = "Haptic Feedback",
            subtitle = "Tactile vibration ticks on Liquid Glass tabs, sliders & buttons",
            checked = GlassSettings.hapticsEnabled,
            onCheckedChange = { GlassSettings.updateHapticsEnabled(it) },
        )
    }
}

private data class NovaStorageData(
    val deviceTotal: Long,
    val deviceUsed: Long,
    val deviceFree: Long,
    val novaApp: Long,
    val novaCache: Long,
    val novaData: Long,
    val novaTotal: Long,
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
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return if (digitGroups >= 3) {
        "%.1f %s".format(value, units[digitGroups])
    } else {
        "%.0f %s".format(value, units[digitGroups])
    }
}

private fun calculateStorageData(context: Context): NovaStorageData {
    return runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = (total - free).coerceAtLeast(0L)

        val appSize = runCatching { File(context.packageCodePath).length() }.getOrDefault(22_000_000L)
        val cacheSize = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
        val dataSize = getFolderSize(context.filesDir)

        NovaStorageData(
            deviceTotal = total,
            deviceUsed = used,
            deviceFree = free,
            novaApp = appSize,
            novaCache = cacheSize,
            novaData = dataSize,
            novaTotal = appSize + cacheSize + dataSize,
        )
    }.getOrDefault(
        NovaStorageData(128_000_000_000L, 45_000_000_000L, 83_000_000_000L, 22_000_000L, 12_000_000L, 4_000_000L, 38_000_000L),
    )
}

@Composable
private fun StorageUsageFloatingContent(context: Context) {
    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var storageData by remember { mutableStateOf(calculateStorageData(context)) }
    var isClearing by remember { mutableStateOf(false) }
    var clearedNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val fresh = calculateStorageData(context)
            withContext(Dispatchers.Main) { storageData = fresh }
        }
    }

    val usedGb = storageData.deviceUsed / 1_000_000_000.0
    val totalGb = storageData.deviceTotal / 1_000_000_000.0
    val freeGb = storageData.deviceFree / 1_000_000_000.0
    val progress = (storageData.deviceUsed.toFloat() / storageData.deviceTotal.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "Device storage",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(8.dp))

        var animatedTarget by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(progress) { animatedTarget = progress }
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
                            listOf(c.primary, c.tertiary, c.secondary),
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
                text = "${"%.1f".format(usedGb)} GB used of ${"%.1f".format(totalGb)} GB",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            Text(
                text = "${"%.1f".format(freeGb)} GB free",
                fontSize = 11.5.sp,
                color = c.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = c.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Used by Nova Player",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = c.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Total space: ${formatStorageBytes(storageData.novaTotal)}",
            fontSize = 12.sp,
            color = c.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))

        StorageItemRow("App & Core engine", formatStorageBytes(storageData.novaApp))
        StorageItemRow("Thumbnail & media cache", formatStorageBytes(storageData.novaCache))
        StorageItemRow("Database & saved config", formatStorageBytes(storageData.novaData))

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E1517))
                .novaPressable(enabled = !isClearing) {
                    isClearing = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                context.cacheDir.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                            }
                            val fresh = calculateStorageData(context)
                            withContext(Dispatchers.Main) {
                                storageData = fresh
                                isClearing = false
                                clearedNotice = "Nova Player cache cleared!"
                            }
                        }
                    }
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isClearing) {
                    CircularProgressIndicator(
                        color = Color(0xFFEF5350),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = clearedNotice ?: "Clear Nova Player Cache",
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
            .novaPressable { onCheckedChange(!checked) }
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
