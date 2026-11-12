package dev.xboyrex.novaplayer.core.ui.dolby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.Dp
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
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
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
fun NovaAnimatedEqualizerIconDynamic(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 24.dp,
    barCount: Int = 5,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_dynamic")
    val barHeights = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800 + (index * 50),
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar_height_$index",
        )
    }

    Canvas(modifier = modifier.size(size)) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height
        val barWidths = List(barCount) { index ->
            val normalizedPosition = index.toFloat() / (barCount - 1).coerceAtLeast(1)
            val centerOffset = (normalizedPosition - 0.5f) * 2f
            val widthFactor = 1.0f - (centerOffset * centerOffset).pow(0.6f)
            0.5f + (widthFactor * 0.5f)
        }
        val totalWidthFactor = barWidths.sum() + (barCount - 1) * 0.3f
        val baseBarWidth = canvasWidth / totalWidthFactor
        var currentX = 0f

        barHeights.forEachIndexed { index, heightAnimation ->
            val barWidth = baseBarWidth * barWidths[index]
            val barHeight = canvasHeight * heightAnimation.value
            val y = canvasHeight - barHeight
            drawRoundRect(
                color = color,
                topLeft = Offset(currentX, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
            currentX += barWidth + (baseBarWidth * 0.3f)
        }
    }
}

private fun gaussian(x: Float, center: Float, sigma: Float): Float =
    exp(-((x - center) * (x - center)) / (2f * sigma * sigma))

private fun lerpDolbyColor(from: Color, to: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t,
    )
}

@Composable
fun NovaAnimatedWaveformBanner(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    barCount: Int = 56,
    animated: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_banner")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveform_phase",
    )

    val swell by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveform_swell",
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val centerY = canvasHeight / 2f
        val slotWidth = canvasWidth / barCount
        val barWidth = slotWidth * 0.42f
        val radius = barWidth / 2f
        val maxHalfHeight = centerY * 0.86f

        for (index in 0 until barCount) {
            val position = (index + 0.5f) / barCount
            val centerX = position * canvasWidth

            val envelope = (
                gaussian(position, 0.34f, 0.13f) * 0.95f +
                    gaussian(position, 0.62f, 0.11f) * 0.78f +
                    gaussian(position, 0.48f, 0.05f) * 0.45f +
                    gaussian(position, 0.82f, 0.09f) * 0.30f +
                    gaussian(position, 0.14f, 0.10f) * 0.26f
                ).coerceIn(0f, 1f)

            val ripple = if (animated) {
                0.72f + 0.28f * sin(phase + position * 14f)
            } else {
                0.85f
            }
            val level = envelope * ripple * if (animated) swell else 0.9f
            val halfHeight = maxHalfHeight * level

            val alpha = (0.28f + envelope * 0.72f).coerceIn(0f, 1f)
            val color = if (envelope > 0.55f) {
                lerpDolbyColor(barColor, accentColor, (envelope - 0.55f) / 0.45f)
            } else {
                barColor
            }

            if (halfHeight <= radius * 1.15f) {
                drawCircle(
                    color = color.copy(alpha = alpha * 0.65f),
                    radius = radius * 0.72f,
                    center = Offset(centerX, centerY),
                )
            } else {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(centerX - radius, centerY - halfHeight),
                    size = Size(barWidth, halfHeight * 2f),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
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
    var showAutoEqDialog by remember { mutableStateOf(false) }
    var showPresetManagerDialog by remember { mutableStateOf(false) }

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
                        onOpenAutoEq = { showAutoEqDialog = true },
                        onOpenPresetManager = { showPresetManagerDialog = true },
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

    if (showAutoEqDialog) {
        NovaAutoEqDialog(onDismiss = { showAutoEqDialog = false })
    }

    if (showPresetManagerDialog) {
        NovaPresetImportExportDialog(onDismiss = { showPresetManagerDialog = false })
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
    bottomPadding: Dp,
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
        // Master Power Card with Dolby Logo & Animated 56-Bar Gaussian Waveform Banner
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
                                .height(64.dp),
                            barColor = if (enabled) c.primary else c.outline,
                            accentColor = if (enabled) c.onPrimaryContainer else c.onSurfaceVariant,
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
                        NovaAnimatedEqualizerIconDynamic(
                            color = c.primary,
                            size = 24.dp,
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
    bottomPadding: Dp,
    onRequestSavePreset: () -> Unit,
    onOpenAutoEq: () -> Unit,
    onOpenPresetManager: () -> Unit,
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
                            text = { Text("AutoEQ Headphone Database") },
                            leadingIcon = { Icon(Icons.Rounded.Headphones, null) },
                            onClick = {
                                menuExpanded = false
                                onOpenAutoEq()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Import / Export Presets (.ldp)") },
                            leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                            onClick = {
                                menuExpanded = false
                                onOpenPresetManager()
                            },
                        )
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

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenAutoEq,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AutoEQ Database", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onOpenPresetManager,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import / Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            isActive = NovaDolbyController.enabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(275.dp),
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

/**
 * Exact InteractiveFrequencyResponseCurve implementation with:
 * - Vertical & horizontal grid lines
 * - Cubic Bézier curve + vertical gradient fill
 * - Glowing dragged node + floating dB badge
 * - Frequency axis labels and +15 / 0 / -15 dB axis labels
 */
@Composable
fun NovaInteractiveFrequencyCurve(
    bandGains: List<NovaBandGain>,
    onBandGainChange: (index: Int, newGain: Int) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isEditable: Boolean = true,
) {
    val c = MaterialTheme.colorScheme
    val view = LocalView.current
    val primaryColor = c.primary
    val onSurfaceVariantColor = c.onSurfaceVariant
    val secondaryColor = c.secondary
    val primaryContainerColor = c.primaryContainer
    val onPrimaryContainerColor = c.onPrimaryContainer
    val surfaceContainerHighest = c.surfaceContainerHighest

    val plotInkColor = if (isActive) onPrimaryContainerColor else onSurfaceVariantColor
    val curveColor = if (isActive) primaryColor else secondaryColor
    val backgroundColor = if (isActive) primaryContainerColor.copy(alpha = 0.35f) else surfaceContainerHighest
    val borderColor = if (isActive) primaryColor.copy(alpha = 0.65f) else c.outlineVariant
    val borderWidth = if (isActive) 1.5.dp else 1.dp

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var controlPoints by remember { mutableStateOf(bandGains.map { it.gain }) }

    LaunchedEffect(bandGains) {
        if (draggedIndex == null) {
            controlPoints = bandGains.map { it.gain }
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(backgroundColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp),
                )
                .pointerInput(bandGains.size, isEditable) {
                    if (!isEditable || bandGains.isEmpty()) return@pointerInput
                    detectTapGestures { offset ->
                        val stepX = size.width / (bandGains.size - 1).coerceAtLeast(1).toFloat()
                        val idx = (offset.x / stepX).roundToInt().coerceIn(0, bandGains.lastIndex)
                        val centerY = size.height / 2f
                        val normalizedGain = ((centerY - offset.y) / (centerY * 0.85f)).coerceIn(-1f, 1f)
                        val newGain = (normalizedGain * 150f).roundToInt().coerceIn(-150, 150)
                        view.novaHaptic(10L, 110)
                        controlPoints = controlPoints.toMutableList().also {
                            if (idx in it.indices) it[idx] = newGain
                        }
                        onBandGainChange(idx, newGain)
                    }
                }
                .pointerInput(bandGains.size, isEditable) {
                    if (!isEditable || bandGains.isEmpty()) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width
                            val height = size.height
                            val stepX = width / (bandGains.size - 1).coerceAtLeast(1).toFloat()

                            var closestIndex = -1
                            var closestDistance = Float.MAX_VALUE

                            bandGains.forEachIndexed { index, _ ->
                                val x = index * stepX
                                val gainVal = controlPoints.getOrElse(index) { 0 }
                                val normalizedGain = (gainVal / 150f).coerceIn(-1f, 1f)
                                val y = height / 2f - (normalizedGain * height / 2f * 0.85f)
                                val distance = sqrt(
                                    (offset.x - x) * (offset.x - x) +
                                        (offset.y - y) * (offset.y - y),
                                )
                                if (distance < closestDistance && distance < 140f) {
                                    closestDistance = distance
                                    closestIndex = index
                                }
                            }
                            if (closestIndex == -1) {
                                closestIndex = (offset.x / stepX).roundToInt().coerceIn(0, bandGains.lastIndex)
                            }
                            draggedIndex = closestIndex
                            view.novaHaptic(12L, 125)
                        },
                        onDrag = { change, _ ->
                            draggedIndex?.let { index ->
                                val height = size.height
                                val centerY = height / 2f
                                val y = change.position.y
                                val normalizedGain = ((centerY - y) / (height / 2f * 0.85f)).coerceIn(-1f, 1f)
                                val newGain = (normalizedGain * 150f).toInt().coerceIn(-150, 150)
                                if (index in controlPoints.indices && controlPoints[index] != newGain) {
                                    controlPoints = controlPoints.toMutableList().apply {
                                        this[index] = newGain
                                    }
                                    onBandGainChange(index, newGain)
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            draggedIndex?.let { index ->
                                if (index in controlPoints.indices) {
                                    onBandGainChange(index, controlPoints[index])
                                }
                            }
                            draggedIndex = null
                        },
                        onDragCancel = {
                            draggedIndex = null
                        },
                    )
                },
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val gridColor = plotInkColor.copy(alpha = 0.18f)
            val gridVerticalColor = plotInkColor.copy(alpha = 0.12f)

            drawLine(
                color = plotInkColor.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 3f,
            )

            for (i in 1..4) {
                val y = (height / 5f) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                )
            }

            if (bandGains.size >= 2 && controlPoints.size == bandGains.size) {
                val stepX = width / (bandGains.size - 1)
                bandGains.forEachIndexed { index, _ ->
                    val x = index * stepX
                    drawLine(
                        color = gridVerticalColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f,
                    )
                }

                val path = Path()
                controlPoints.forEachIndexed { index, gain ->
                    val x = index * stepX
                    val normalizedGain = (gain / 150f).coerceIn(-1f, 1f)
                    val y = centerY - (normalizedGain * centerY * 0.85f)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevGain = controlPoints[index - 1]
                        val prevNormalizedGain = (prevGain / 150f).coerceIn(-1f, 1f)
                        val prevY = centerY - (prevNormalizedGain * centerY * 0.85f)
                        val cpX1 = prevX + stepX * 0.4f
                        val cpX2 = x - stepX * 0.4f
                        path.cubicTo(cpX1, prevY, cpX2, y, x, y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            curveColor.copy(alpha = if (isActive) 0.4f else 0.28f),
                            curveColor.copy(alpha = 0.04f),
                        ),
                    ),
                )

                drawPath(
                    path = path,
                    color = curveColor,
                    style = Stroke(width = if (isActive) 5f else 4f),
                )

                controlPoints.forEachIndexed { index, gain ->
                    val x = index * stepX
                    val normalizedGain = (gain / 150f).coerceIn(-1f, 1f)
                    val y = centerY - (normalizedGain * centerY * 0.85f)
                    val isBeingDragged = draggedIndex == index
                    val pointRadius = if (isBeingDragged) 14f else 10f

                    if (isBeingDragged) {
                        drawCircle(
                            color = curveColor.copy(alpha = 0.3f),
                            radius = 24f,
                            center = Offset(x, y),
                        )
                    }
                    drawCircle(
                        color = c.surface,
                        radius = pointRadius,
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = curveColor,
                        radius = pointRadius - 2.5f,
                        center = Offset(x, y),
                    )
                }
            }
        }

        // Bottom Frequency Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val step = if (bandGains.size > 12) 2 else 1
            bandGains.filterIndexed { i, _ -> i % step == 0 || i == bandGains.lastIndex }.forEach { bandGain ->
                Text(
                    text = if (bandGain.frequency >= 1000) {
                        "${bandGain.frequency / 1000}k"
                    } else {
                        "${bandGain.frequency}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = plotInkColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Left dB Axis Labels (+15, 0, -15)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .padding(start = 6.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "+15",
                style = MaterialTheme.typography.labelSmall,
                color = plotInkColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = plotInkColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "-15",
                style = MaterialTheme.typography.labelSmall,
                color = plotInkColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Floating dB Tooltip Badge while dragging
        draggedIndex?.let { index ->
            val gain = controlPoints.getOrElse(index) { 0 }
            val gainDb = gain / 10f
            val freq = bandGains.getOrNull(index)?.frequency ?: 0
            val freqLabel = if (freq >= 1000) "${freq / 1000f}kHz" else "${freq}Hz"
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = primaryColor,
                shadowElevation = 4.dp,
            ) {
                Text(
                    text = "$freqLabel · ${if (gainDb >= 0) "+" else ""}%.1f dB".format(gainDb),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = c.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun NovaAutoEqDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val results = remember(searchQuery, NovaDolbyController.autoEqCatalog) {
        NovaDolbyController.searchAutoEq(searchQuery)
    }

    LaunchedEffect(Unit) {
        NovaDolbyController.refreshRemoteAutoEqIndex()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AutoEQ Headphone Database",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = c.onSurface,
                        )
                        Text(
                            text = "Harman target frequency response profiles (${NovaDolbyController.autoEqCatalog.size} models)",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Search headphones or IEMs (e.g. Sony, AirPods, Moondrop)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (NovaDolbyController.isAutoEqLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing online AutoEQ repository...", fontSize = 12.sp, color = c.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id }) { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    scope.launch {
                                        val ok = NovaDolbyController.applyAutoEqEntry(entry)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Applied AutoEQ profile for ${entry.name}" else "Failed to apply ${entry.name}",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        if (ok) onDismiss()
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = c.surfaceContainerHigh,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = c.onSurface,
                                    )
                                    Text(
                                        text = "${entry.source} · Rig: ${entry.measurementRig}",
                                        fontSize = 11.5.sp,
                                        color = c.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = null,
                                    tint = c.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaPresetImportExportDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = MaterialTheme.colorScheme
    val customPresets = NovaDolbyController.presets.filter { it.isUserDefined }
    var selectedForExport by remember { mutableStateOf<NovaEqualizerPreset?>(null) }

    val singleExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val targetPreset = selectedForExport
        if (uri != null && targetPreset != null) {
            scope.launch {
                NovaDolbyController.exportPresetToUri(context, targetPreset, uri).fold(
                    onSuccess = { Toast.makeText(context, "Preset exported successfully!", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }

    val batchExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                NovaDolbyController.exportAllCustomPresetsToUri(context, uri).fold(
                    onSuccess = { count -> Toast.makeText(context, "Exported $count custom presets!", Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, "Batch export failed: ${it.message}", Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                NovaDolbyController.importPresetsFromUri(context, uri).fold(
                    onSuccess = { count ->
                        Toast.makeText(context, "Imported $count preset(s)!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show() },
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = c.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preset Import & Export",
                            fontFamily = NovaRoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = c.onSurface,
                        )
                        Text(
                            text = "Import or back up .ldp / JSON multi-band equalizer presets",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { importLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import File")
                    }
                    Button(
                        onClick = { batchExportLauncher.launch("nova_dolby_presets_backup.ldp") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = customPresets.isNotEmpty(),
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup All (${customPresets.size})")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = cm?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                        val ok = NovaDolbyController.importPresetJson(text)
                        Toast.makeText(
                            context,
                            if (ok) "Preset imported from clipboard!" else "No valid preset in clipboard",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.outlineVariant),
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Preset from Clipboard")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Custom Presets (${customPresets.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = c.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (customPresets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No custom presets saved yet. Save a custom curve or apply an AutoEQ profile first.",
                            fontSize = 13.sp,
                            color = c.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(customPresets, key = { it.name }) { preset ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = c.surfaceContainerHigh,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = c.onSurface,
                                        )
                                        Text(
                                            text = "${preset.bandMode.displayName} · ${preset.bandGains.size} bands",
                                            fontSize = 11.5.sp,
                                            color = c.onSurfaceVariant,
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                selectedForExport = preset
                                                singleExportLauncher.launch("${preset.name.replace(" ", "_")}_${preset.bandMode.value}band.ldp")
                                            },
                                        ) {
                                            Icon(Icons.Rounded.FileDownload, contentDescription = "Export file", tint = c.primary)
                                        }
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val json = NovaDolbyController.exportPresetToJsonObject(preset).toString(2)
                                                cm?.setPrimaryClip(ClipData.newPlainText("DolbyPreset", json))
                                                Toast.makeText(context, "Copied '${preset.name}' to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                        ) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = c.secondary)
                                        }
                                        IconButton(
                                            onClick = {
                                                NovaDolbyController.deleteUserPreset(preset)
                                            },
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = c.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
    bottomPadding: Dp,
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
