package dev.xboyrex.novaplayer.core.ui.dolby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.xboyrex.novaplayer.core.ui.R
import dev.xboyrex.novaplayer.core.ui.glass.LiquidBottomTabs
import dev.xboyrex.novaplayer.core.ui.glass.LiquidNavTabItem
import dev.xboyrex.novaplayer.core.ui.glass.NovaRoundedHeavy
import dev.xboyrex.novaplayer.core.ui.glass.novaHaptic
import dev.xboyrex.novaplayer.core.ui.glass.novaPressable
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun NovaDolbyVectorLogo(
    modifier: Modifier = Modifier,
    leftColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    rightColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_left),
            contentDescription = "Dolby Atmos",
            tint = leftColor,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_dolby_logo_right),
            contentDescription = null,
            tint = rightColor,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun NovaAnimatedWaveformBanner(
    modifier: Modifier = Modifier,
    barColor: Color,
    barCount: Int = 26,
    animated: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dolbyWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Canvas(modifier = modifier) {
        val spacing = size.width / (barCount * 1.6f)
        val barWidth = spacing * 0.85f
        val centerY = size.height / 2f
        for (i in 0 until barCount) {
            val x = i * (size.width / barCount) + barWidth / 2f
            val normalized = if (animated) {
                val wave1 = sin(phase + i * 0.42f)
                val wave2 = sin(phase * 1.5f - i * 0.25f)
                ((wave1 + wave2) * 0.25f + 0.55f).coerceIn(0.18f, 0.95f)
            } else {
                0.22f
            }
            val barH = size.height * normalized
            drawLine(
                color = barColor.copy(alpha = if (animated) 0.9f else 0.4f),
                start = Offset(x, centerY - barH / 2f),
                end = Offset(x, centerY + barH / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun NovaDolbyStudioDialog(
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        NovaDolbyScreen(onBack = onDismiss)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaDolbyScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        NovaDolbyController.init(context)
        NovaDolbyController.updateSpeakerState()
    }

    val c = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val pageBackdrop = rememberLayerBackdrop()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                .background(c.background),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh)
                        .novaPressable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = c.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (pagerState.targetPage) {
                            1 -> "Dolby Graphic Equalizer"
                            2 -> "Dolby Advanced Tuning"
                            else -> "Dolby Atmos Studio"
                        },
                        fontFamily = NovaRoundedHeavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = c.onSurface,
                    )
                    Text(
                        text = if (NovaDolbyController.enabled) {
                            val profName = NovaDolbyPresetsData.PROFILES.find { it.id == NovaDolbyController.currentProfile }?.name ?: "Dynamic"
                            "Active · $profName · ${NovaDolbyController.currentPresetName}"
                        } else {
                            "Bypassed · Tap power card to activate"
                        },
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant,
                    )
                }

                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Dolby Credits & Info",
                        tint = c.primary,
                    )
                }
                IconButton(onClick = { showResetConfirm = true }) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Profiles",
                        tint = c.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> NovaDolbyMainPage(
                        bottomPadding = navBottom + 88.dp,
                        onOpenEqualizer = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onOpenAbout = { showAboutDialog = true },
                    )
                    1 -> NovaDolbyEqualizerPage(
                        bottomPadding = navBottom + 88.dp,
                        onRequestSavePreset = { showSavePresetDialog = true },
                    )
                    2 -> NovaDolbyAdvancedPage(
                        bottomPadding = navBottom + 88.dp,
                    )
                }
            }
        }

        // Liquid Glass Bottom Navigation Bar for the 3 Dolby Studio Tabs
        val dolbyTabs = listOf(
            Triple("Atmos", Icons.Rounded.Home, 0),
            Triple("Equalizer", Icons.Rounded.GraphicEq, 1),
            Triple("Advanced", Icons.Rounded.Tune, 2),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = navBottom.coerceAtLeast(10.dp) + 8.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
        ) {
            LiquidBottomTabs(
                selectedTabIndex = { pagerState.targetPage },
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                backdrop = pageBackdrop,
                tabsCount = dolbyTabs.size,
                modifier = Modifier.fillMaxWidth(),
            ) {
                dolbyTabs.forEach { (label, icon, idx) ->
                    LiquidNavTabItem(
                        label = label,
                        icon = icon,
                        selected = pagerState.targetPage == idx,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(idx) }
                        },
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        NovaDolbyAboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all Dolby profiles?", fontWeight = FontWeight.Bold) },
            text = { Text("This will restore all profiles, equalizer bands, and enhancers to factory defaults.") },
            confirmButton = {
                Button(
                    onClick = {
                        NovaDolbyController.resetAllProfiles()
                        showResetConfirm = false
                        Toast.makeText(context, "Successfully reset all Dolby profiles", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Preset", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = {
                            presetName = it
                            errorMsg = null
                        },
                        label = { Text("Preset name") },
                        singleLine = true,
                        isError = errorMsg != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    errorMsg?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val err = NovaDolbyController.saveUserPreset(presetName)
                        if (err == null) {
                            showSavePresetDialog = false
                            Toast.makeText(context, "Preset saved", Toast.LENGTH_SHORT).show()
                        } else {
                            errorMsg = err
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun NovaDolbyMainPage(
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenEqualizer: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val enabled = NovaDolbyController.enabled
    val currentProfile = NovaDolbyController.currentProfile
    val ps = NovaDolbyController.profileSettings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Master Power Card with Dolby Logo & Animated Waveform
        item {
            val containerColor by animateColorAsState(
                targetValue = if (enabled) c.primaryContainer else c.surfaceContainerHigh,
                label = "dolbyPowerContainer",
            )
            val contentColor = if (enabled) c.onPrimaryContainer else c.onSurface

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .novaPressable { NovaDolbyController.setDolbyEnabled(!enabled) },
                shape = RoundedCornerShape(26.dp),
                color = containerColor,
                contentColor = contentColor,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (enabled) c.primary else c.surfaceContainerHighest,
                            modifier = Modifier.size(68.dp),
                        ) {
                            NovaDolbyVectorLogo(
                                modifier = Modifier.padding(15.dp),
                                leftColor = if (enabled) c.onPrimary else contentColor,
                                rightColor = if (enabled) c.onPrimary else contentColor,
                            )
                        }
                        NovaAnimatedWaveformBanner(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            barColor = if (enabled) c.primary else c.outline,
                            animated = enabled,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Dolby Atmos",
                                fontFamily = NovaRoundedHeavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                            )
                            Text(
                                text = if (enabled) {
                                    "On · Spatial virtualizer & 20-band DSP active"
                                } else {
                                    "Off · Audio effects bypassed"
                                },
                                fontSize = 12.5.sp,
                                color = contentColor.copy(alpha = 0.8f),
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { NovaDolbyController.setDolbyEnabled(it) },
                            thumbContent = {
                                Icon(
                                    imageVector = if (enabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        // Profile Selector
        item {
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NovaDolbySectionCard(
                    title = "Choose a Profile",
                    icon = Icons.Rounded.Headphones,
                ) {
                    val dynamicProfile = NovaDolbyPresetsData.PROFILES.first()
                    NovaProfileTile(
                        option = dynamicProfile,
                        icon = Icons.Rounded.AutoAwesome,
                        selected = currentProfile == dynamicProfile.id,
                        onClick = { NovaDolbyController.setProfile(dynamicProfile.id) },
                        showSubtitle = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val rest = NovaDolbyPresetsData.PROFILES.drop(1).chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rest.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { option ->
                                    NovaProfileTile(
                                        option = option,
                                        icon = when (option.id) {
                                            1 -> Icons.Rounded.Movie
                                            2 -> Icons.Rounded.MusicNote
                                            3 -> Icons.Rounded.SportsEsports
                                            4 -> Icons.Rounded.Work
                                            5 -> Icons.Rounded.Coffee
                                            else -> Icons.Rounded.Favorite
                                        },
                                        selected = currentProfile == option.id,
                                        onClick = { NovaDolbyController.setProfile(option.id) },
                                        showSubtitle = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Intelligent Equalizer
        item {
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NovaDolbySectionCard(
                    title = "Intelligent Equalizer",
                    icon = Icons.Rounded.Equalizer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NovaDolbyPresetsData.IEQ_OPTIONS.forEach { (id, label) ->
                            val selected = ps.ieqPreset == id
                            val bg by animateColorAsState(
                                if (selected) c.primary else c.surfaceContainerHighest,
                                label = "ieqBg",
                            )
                            val fg = if (selected) c.onPrimary else c.onSurface
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(bg)
                                    .novaPressable { NovaDolbyController.setIeqPreset(id) }
                                    .padding(vertical = 14.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = when (id) {
                                        0 -> Icons.Rounded.PowerOff
                                        1 -> Icons.Rounded.GraphicEq
                                        2 -> Icons.Rounded.Balance
                                        else -> Icons.Rounded.Whatshot
                                    },
                                    contentDescription = label,
                                    tint = fg,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    color = fg,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (ps.ieqPreset) {
                            1 -> "Detailed: Brings out subtle high-frequency details and vocal presence."
                            2 -> "Balanced: Keeps lows, mids, and highs in natural studio balance."
                            3 -> "Warm: Gives your sound a softer, richer low-mid character."
                            else -> "Off: Uses your active profile and custom graphic equalizer curve."
                        },
                        fontSize = 12.sp,
                        color = c.onSurfaceVariant,
                    )
                }
            }
        }

        // Quick Equalizer & About Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .novaPressable(onClick = onOpenEqualizer),
                    shape = RoundedCornerShape(22.dp),
                    color = c.surfaceContainerHigh,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Graphic Equalizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = c.onSurface,
                        )
                        Text(
                            text = "${NovaDolbyController.currentPresetName} · ${NovaDolbyController.bandMode.displayName}",
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .novaPressable(onClick = onOpenAbout),
                    shape = RoundedCornerShape(22.dp),
                    color = c.surfaceContainerHigh,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "About Dolby Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = c.onSurface,
                        )
                        Text(
                            text = "Mayank (@xboyrex)",
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NovaDolbyEqualizerPage(
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRequestSavePreset: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var viewMode by remember { mutableStateOf(NovaEqualizerViewMode.CURVE) }
    var presetDropdownExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val presets = NovaDolbyController.presets
    val currentPresetName = NovaDolbyController.currentPresetName
    val currentPreset = presets.find { it.name == currentPresetName } ?: presets.firstOrNull()
    val bandGains = NovaDolbyController.bandGains
    val bandMode = NovaDolbyController.bandMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Preset Selector + Actions Card
        NovaDolbySectionCard(
            title = "Equalizer Preset",
            icon = Icons.Rounded.LibraryMusic,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = presetDropdownExpanded,
                    onExpandedChange = { presetDropdownExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        color = c.surfaceContainerHighest,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = currentPresetName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                color = c.onSurface,
                            )
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetDropdownExpanded)
                        }
                    }
                    ExposedDropdownMenu(
                        expanded = presetDropdownExpanded,
                        onDismissRequest = { presetDropdownExpanded = false },
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = preset.name,
                                        fontWeight = if (preset.name == currentPresetName) FontWeight.Bold else FontWeight.Normal,
                                        color = if (preset.name == currentPresetName) c.primary else c.onSurface,
                                    )
                                },
                                onClick = {
                                    NovaDolbyController.selectPreset(preset)
                                    presetDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                IconButton(onClick = onRequestSavePreset) {
                    Icon(Icons.Rounded.Save, contentDescription = "Save Preset", tint = c.primary)
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More Options", tint = c.onSurface)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reset Gains (0 dB)") },
                            leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) },
                            onClick = {
                                menuExpanded = false
                                NovaDolbyController.resetGains()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Preset to Clipboard") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                            onClick = {
                                menuExpanded = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("NovaDolbyPreset", NovaDolbyController.exportCurrentPresetJson()))
                                Toast.makeText(context, "Preset copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Paste Preset from Clipboard") },
                            leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) },
                            onClick = {
                                menuExpanded = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val text = cm?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                val ok = NovaDolbyController.importPresetJson(text)
                                Toast.makeText(
                                    context,
                                    if (ok) "Preset imported successfully" else "Clipboard does not contain a valid preset",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                        if (currentPreset?.isUserDefined == true) {
                            DropdownMenuItem(
                                text = { Text("Delete Preset") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = {
                                    menuExpanded = false
                                    NovaDolbyController.deleteUserPreset(currentPreset)
                                },
                            )
                        }
                    }
                }
            }
        }

        // Band Configuration (10 / 15 / 20 Bands) & View Mode (Curve / Sliders)
        NovaDolbySectionCard(
            title = "Band Precision & View Mode",
            icon = Icons.Rounded.Tune,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NovaBandMode.entries.forEach { mode ->
                    val selected = bandMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) c.primary else c.surfaceContainerHighest)
                            .novaPressable { NovaDolbyController.setBandModeSelection(mode) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = mode.displayName,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (selected) c.onPrimary else c.onSurface,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    NovaEqualizerViewMode.CURVE to ("Interactive Curve" to Icons.Rounded.ShowChart),
                    NovaEqualizerViewMode.SLIDERS to ("Band Sliders" to Icons.Rounded.Tune),
                ).forEach { (mode, pair) ->
                    val selected = viewMode == mode
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) c.primaryContainer else c.surfaceContainerHighest)
                            .novaPressable { viewMode = mode }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = pair.second,
                            contentDescription = null,
                            tint = if (selected) c.onPrimaryContainer else c.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pair.first,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (selected) c.onPrimaryContainer else c.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Interactive Curve or Sliders View
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "eqViewMode",
        ) { mode ->
            when (mode) {
                NovaEqualizerViewMode.CURVE -> {
                    NovaDolbySectionCard(
                        title = "Interactive Frequency Response (±15 dB)",
                        icon = Icons.Rounded.ShowChart,
                    ) {
                        Text(
                            text = "Drag any control point vertically to shape the ${bandMode.bandCount}-band response curve in real time.",
                            fontSize = 11.5.sp,
                            color = c.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        NovaInteractiveFrequencyCurve(
                            bandGains = bandGains,
                            onBandGainChange = { index, gain ->
                                NovaDolbyController.setBandGain(index, gain)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                        )
                    }
                }
                NovaEqualizerViewMode.SLIDERS -> {
                    NovaDolbySectionCard(
                        title = "Vertical Band Sliders (±15 dB)",
                        icon = Icons.Rounded.Tune,
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                        ) {
                            itemsIndexed(bandGains) { index, bg ->
                                NovaVerticalBandSlider(
                                    frequency = bg.frequency,
                                    gain = bg.gain,
                                    onGainChange = { newGain ->
                                        NovaDolbyController.setBandGain(index, newGain)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 0.1 dB Precision Band Tuner Card
        NovaBandTunerCard(
            bandGains = bandGains,
            onGainChange = { idx, gain -> NovaDolbyController.setBandGain(idx, gain) },
        )
    }
}

@Composable
private fun NovaInteractiveFrequencyCurve(
    bandGains: List<NovaBandGain>,
    onBandGainChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val view = LocalView.current
    var activeBandIndex by remember { mutableIntStateOf(-1) }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHighest)
            .pointerInput(bandGains.size) {
                if (bandGains.isEmpty()) return@pointerInput
                detectTapGestures { offset ->
                    val stepX = size.width.toFloat() / (bandGains.size - 1).coerceAtLeast(1)
                    val idx = ((offset.x / stepX).roundToInt()).coerceIn(0, bandGains.lastIndex)
                    val centerY = size.height / 2f
                    val norm = ((centerY - offset.y) / (centerY * 0.82f)).coerceIn(-1f, 1f)
                    val newGain = (norm * 150f).roundToInt().coerceIn(-150, 150)
                    view.novaHaptic(10L, 110)
                    onBandGainChange(idx, newGain)
                }
            }
            .pointerInput(bandGains.size) {
                if (bandGains.isEmpty()) return@pointerInput
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val stepX = size.width.toFloat() / (bandGains.size - 1).coerceAtLeast(1)
                        activeBandIndex = ((startOffset.x / stepX).roundToInt()).coerceIn(0, bandGains.lastIndex)
                        view.novaHaptic(12L, 130)
                    },
                    onDragEnd = { activeBandIndex = -1 },
                    onDragCancel = { activeBandIndex = -1 },
                    onDrag = { change, _ ->
                        change.consume()
                        val stepX = size.width.toFloat() / (bandGains.size - 1).coerceAtLeast(1)
                        val idx = ((change.position.x / stepX).roundToInt()).coerceIn(0, bandGains.lastIndex)
                        activeBandIndex = idx
                        val centerY = size.height / 2f
                        val norm = ((centerY - change.position.y) / (centerY * 0.82f)).coerceIn(-1f, 1f)
                        val newGain = (norm * 150f).roundToInt().coerceIn(-150, 150)
                        onBandGainChange(idx, newGain)
                    },
                )
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Horizontal dB grid lines
        for (i in 0..4) {
            val y = (h / 4f) * i
            drawLine(
                color = c.onSurfaceVariant.copy(alpha = if (i == 2) 0.45f else 0.16f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (i == 2) 2f else 1f,
            )
        }

        if (bandGains.size >= 2) {
            val stepX = w / (bandGains.size - 1)
            val path = Path()
            val points = bandGains.mapIndexed { index, bg ->
                val x = index * stepX
                val norm = (bg.gain / 150f).coerceIn(-1f, 1f)
                val y = centerY - (norm * centerY * 0.82f)
                Offset(x, y)
            }

            points.forEachIndexed { index, pt ->
                if (index == 0) {
                    path.moveTo(pt.x, pt.y)
                } else {
                    val prev = points[index - 1]
                    val cpX1 = prev.x + stepX * 0.4f
                    val cpX2 = pt.x - stepX * 0.4f
                    path.cubicTo(cpX1, prev.y, cpX2, pt.y, pt.x, pt.y)
                }
            }

            val fillPath = Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        c.primary.copy(alpha = 0.34f),
                        c.primary.copy(alpha = 0.03f),
                    ),
                ),
            )

            drawPath(
                path = path,
                color = c.primary,
                style = Stroke(width = 5f),
            )

            points.forEachIndexed { index, pt ->
                val isSelected = index == activeBandIndex
                drawCircle(
                    color = if (isSelected) c.tertiary else c.primary,
                    radius = if (isSelected) 15f else 10f,
                    center = pt,
                )
                drawCircle(
                    color = c.surface,
                    radius = if (isSelected) 7f else 4.5f,
                    center = pt,
                )
            }
        }
    }
}

@Composable
private fun NovaVerticalBandSlider(
    frequency: Int,
    gain: Int,
    onGainChange: (Int) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var sliderVal by remember(gain) { mutableFloatStateOf(gain / 10f) }

    Column(
        modifier = Modifier
            .width(58.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = c.primaryContainer,
        ) {
            Text(
                text = if (sliderVal > 0f) "+%.1f".format(sliderVal) else "%.1f".format(sliderVal),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = c.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Slider(
            value = sliderVal,
            onValueChange = {
                sliderVal = it
                onGainChange((it * 10f).roundToInt())
            },
            valueRange = -15f..15f,
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        ),
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
                .weight(1f)
                .width(44.dp),
        )
        Text(
            text = if (frequency >= 1000) "${frequency / 1000}k" else "$frequency",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
private fun NovaBandTunerCard(
    bandGains: List<NovaBandGain>,
    onGainChange: (Int, Int) -> Unit,
) {
    if (bandGains.isEmpty()) return
    val c = MaterialTheme.colorScheme
    var selectedIdx by remember(bandGains.size) { mutableIntStateOf(0) }
    val idx = selectedIdx.coerceIn(0, bandGains.lastIndex)
    val band = bandGains[idx]
    val freqLabel = if (band.frequency >= 1000) "%.1f kHz".format(band.frequency / 1000f) else "${band.frequency} Hz"
    val dbVal = band.gain / 10f

    NovaDolbySectionCard(
        title = "0.1 dB Precision Band Tuner",
        icon = Icons.Rounded.GraphicEq,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (idx > 0) selectedIdx = idx - 1 },
                enabled = idx > 0,
            ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Band")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Band ${idx + 1} of ${bandGains.size} · $freqLabel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = c.onSurface,
                )
                Text(
                    text = if (dbVal > 0f) "+%.1f dB".format(dbVal) else "%.1f dB".format(dbVal),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = c.primary,
                )
            }
            IconButton(
                onClick = { if (idx < bandGains.lastIndex) selectedIdx = idx + 1 },
                enabled = idx < bandGains.lastIndex,
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Band")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "−0.1 dB" to (band.gain - 1).coerceAtLeast(-150),
                "0 dB" to 0,
                "+0.1 dB" to (band.gain + 1).coerceAtMost(150),
            ).forEach { (label, targetGain) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceContainerHighest)
                        .novaPressable { onGainChange(idx, targetGain) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = c.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun NovaDolbyAdvancedPage(
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val c = MaterialTheme.colorScheme
    val ps = NovaDolbyController.profileSettings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Bass, Mid & Treble Enhancers
        item {
            NovaDolbySectionCard(
                title = "Bass, Mid & Treble Enhancers",
                icon = Icons.Rounded.Tune,
            ) {
                // Bass Enhancer
                NovaDolbySwitchRow(
                    title = "Bass Enhancer",
                    subtitle = "Enhances low-frequency sub-bass and punch",
                    checked = ps.bassLevel > 0,
                    onCheckedChange = { on -> NovaDolbyController.setBassLevel(if (on) 50 else 0) },
                )
                AnimatedVisibility(visible = ps.bassLevel > 0) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "Bass Curve",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = c.onSurface,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NovaDolbyPresetsData.BASS_CURVES.forEach { (curveId, label) ->
                                val selected = ps.bassCurve == curveId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) c.primary else c.surfaceContainerHighest)
                                        .novaPressable { NovaDolbyController.setBassCurve(curveId) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (selected) c.onPrimary else c.onSurface,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        NovaDolbySliderRow(
                            title = "Bass Level",
                            value = ps.bassLevel,
                            valueRange = 0f..100f,
                            valueLabel = "${ps.bassLevel}%",
                            onValueChange = { NovaDolbyController.setBassLevel(it) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Mid Enhancer
                NovaDolbySwitchRow(
                    title = "Mid Enhancer",
                    subtitle = "Enhances mid-frequency vocals and instruments",
                    checked = ps.midLevel > 0,
                    onCheckedChange = { on -> NovaDolbyController.setMidLevel(if (on) 40 else 0) },
                )
                AnimatedVisibility(visible = ps.midLevel > 0) {
                    NovaDolbySliderRow(
                        title = "Mid Level",
                        value = ps.midLevel,
                        valueRange = 0f..100f,
                        valueLabel = "${ps.midLevel}%",
                        onValueChange = { NovaDolbyController.setMidLevel(it) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Treble Enhancer
                NovaDolbySwitchRow(
                    title = "Treble Enhancer",
                    subtitle = "Enhances high-frequency air and clarity",
                    checked = ps.trebleLevel > 0,
                    onCheckedChange = { on -> NovaDolbyController.setTrebleLevel(if (on) 35 else 0) },
                )
                AnimatedVisibility(visible = ps.trebleLevel > 0) {
                    NovaDolbySliderRow(
                        title = "Treble Level",
                        value = ps.trebleLevel,
                        valueRange = 0f..100f,
                        valueLabel = "${ps.trebleLevel}%",
                        onValueChange = { NovaDolbyController.setTrebleLevel(it) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        // Surround Virtualizer & Stereo Widening
        item {
            NovaDolbySectionCard(
                title = "Surround Virtualizer & Spatial Stage",
                icon = Icons.Rounded.Speaker,
            ) {
                NovaDolbySwitchRow(
                    title = "Headphone Surround Virtualizer",
                    subtitle = "Creates 3D virtual surround soundstage on headphones & IEMs",
                    checked = ps.headphoneVirtualizerEnabled,
                    onCheckedChange = { NovaDolbyController.setHeadphoneVirtualizer(it) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                NovaDolbySwitchRow(
                    title = "Speaker Surround Virtualizer",
                    subtitle = "Expands stereo soundstage on built-in device speakers",
                    checked = ps.speakerVirtualizerEnabled,
                    onCheckedChange = { NovaDolbyController.setSpeakerVirtualizer(it) },
                )
                Spacer(modifier = Modifier.height(10.dp))
                NovaDolbySliderRow(
                    title = "Stereo Widening Strength",
                    value = ps.stereoWideningAmount,
                    valueRange = 4f..64f,
                    valueLabel = "${ps.stereoWideningAmount}",
                    onValueChange = { NovaDolbyController.setStereoWidening(it) },
                )
            }
        }

        // Dialogue Enhancer & Volume Leveler
        item {
            NovaDolbySectionCard(
                title = "Dialogue Enhancer & Volume Leveler",
                icon = Icons.Rounded.RecordVoiceOver,
            ) {
                NovaDolbySwitchRow(
                    title = "Dialogue Enhancer",
                    subtitle = "Isolates and boosts spoken voices in movies and videos",
                    checked = ps.dialogueEnhancerEnabled,
                    onCheckedChange = { NovaDolbyController.setDialogueEnhancer(it) },
                )
                AnimatedVisibility(visible = ps.dialogueEnhancerEnabled) {
                    NovaDolbySliderRow(
                        title = "Dialogue Clarity Strength",
                        value = ps.dialogueEnhancerAmount,
                        valueRange = 1f..12f,
                        valueLabel = "${ps.dialogueEnhancerAmount}",
                        onValueChange = { NovaDolbyController.setDialogueEnhancerAmount(it) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NovaDolbySwitchRow(
                    title = "Volume Leveler",
                    subtitle = "Balances sudden loud explosions and quiet whispers",
                    checked = ps.volumeLevelerEnabled,
                    onCheckedChange = { NovaDolbyController.setVolumeLeveler(it) },
                )
                Spacer(modifier = Modifier.height(12.dp))
                NovaDolbySwitchRow(
                    title = "Per-Device Audio Memory",
                    subtitle = "Remembers Dolby settings per output (Speakers, Headphones, Bluetooth)",
                    checked = NovaDolbyController.deviceStateMemoryEnabled,
                    onCheckedChange = { NovaDolbyController.setDeviceStateMemory(it) },
                )
            }
        }
    }
}

@Composable
private fun NovaProfileTile(
    option: NovaProfileOption,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    showSubtitle: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        if (selected) c.primary else c.surfaceContainerHighest,
        label = "profileTileBg",
    )
    val fg = if (selected) c.onPrimary else c.onSurface

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .novaPressable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = fg,
    ) {
        if (showSubtitle) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        option.summary,
                        fontSize = 11.5.sp,
                        color = fg.copy(alpha = 0.8f),
                    )
                }
                if (selected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .heightIn(min = 90.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    text = option.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun NovaDolbySectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = c.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = c.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontFamily = NovaRoundedHeavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = c.onSurface,
                )
            }
            content()
        }
    }
}

@Composable
private fun NovaDolbySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) c.secondaryContainer.copy(alpha = 0.55f) else c.surfaceContainerHigh)
            .novaPressable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = c.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = c.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.primary),
        )
    }
}

@Composable
private fun NovaDolbySliderRow(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val view = LocalView.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                color = c.onSurface,
            )
            Surface(
                shape = CircleShape,
                color = c.primaryContainer,
            ) {
                Text(
                    text = valueLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = c.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                val next = it.roundToInt()
                if (next != value) {
                    view.novaHaptic(8L, 95)
                    onValueChange(next)
                }
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = c.primary,
                activeTrackColor = c.primary,
            ),
        )
    }
}
