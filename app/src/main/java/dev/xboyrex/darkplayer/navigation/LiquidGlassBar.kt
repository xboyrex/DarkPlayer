package dev.xboyrex.darkplayer.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.glass.DampedDragAnimation
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.InteractiveHighlight
import dev.xboyrex.darkplayer.core.ui.glass.navHaptic
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalLiquidBottomTabScale.current
    val view = LocalView.current
    val isTv = LocalContext.current.isTelevision
    Column(
        modifier
            .then(if (isTv) Modifier.tvFocusRing(shape = RoundedCornerShape(20.dp)) else Modifier)
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = {
                    view.navHaptic(12L, 130)
                    onClick()
                },
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * Authentic YTX Liquid Glass Bottom Navigation Bar (`GlassNavBar.kt`) with smooth
 * Glasmio scroll-to-collapse / expand menu morph and DampedDragAnimation tab drag physics.
 *
 * Strictly bounded to 64.dp height so the refractive lens capsule never stretches vertically.
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    isDarkSurface: Boolean = false,
    compactProgress: Float = 0f,
    compactIcon: ImageVector? = null,
    onExpandCompact: () -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val bgLum = MaterialTheme.colorScheme.background.luminance()
    val isLightTheme = !isDarkSurface && !isSystemInDarkTheme() && bgLum >= 0.5f
    val isAmoled = bgLum < 0.005f
    val accentColor = MaterialTheme.colorScheme.primary
    val glassSupported = GlassQuality.effectsSupported
    val solidContainer = if (isAmoled) Color(0xFF141416) else MaterialTheme.colorScheme.surfaceContainer
    val containerColor = when {
        !glassSupported -> solidContainer
        isAmoled -> Color(0xFF000000).copy(0.45f)
        isLightTheme -> Color(0xFFFAFAFA).copy(0.42f)
        else -> Color(0xFF121212).copy(0.40f)
    }
    val clampedCompact = compactProgress.coerceIn(0f, 1f)
    val isCompactMode = clampedCompact > 0.55f
    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier.height(64f.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val fullWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val compactSizePx = with(density) { 64.dp.toPx() }
        val currentSurfaceWidthDp = with(density) {
            lerp(fullWidthPx, compactSizePx, clampedCompact).toDp()
        }
        val tabWidth = with(density) {
            (fullWidthPx - 8f.dp.toPx()).coerceAtLeast(1f) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density, fullWidthPx) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / fullWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val view = LocalView.current
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {
                    view.navHaptic(18L, 170)
                },
                onDragStopped = {
                    if (clampedCompact >= 0.65f) {
                        view.navHaptic(16L, 150)
                        onExpandCompact()
                        animateToValue(selectedTabIndex().toFloat())
                        return@DampedDragAnimation
                    }
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    view.navHaptic(20L, 190)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f),
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    if (clampedCompact >= 0.65f) return@DampedDragAnimation
                    if (tabWidth > 0f) {
                        val before = targetValue.fastRoundToInt()
                        updateValue(
                            (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                                .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                        )

                        if (targetValue.fastRoundToInt() != before) {
                            view.navHaptic(16L, 170)
                        }
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                        }
                    }
                },
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                    if (dampedDragAnimation.targetValue != index.toFloat()) {
                        dampedDragAnimation.animateToValue(index.toFloat())
                    }
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    val expandedCenter = if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                    else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                    Offset(
                        x = lerp(expandedCenter, compactSizePx / 2f, clampedCompact),
                        y = size.height / 2f,
                    )
                },
            )
        }

        // 1. Main YTX Liquid Glass Capsule Bar (morphs width smoothly from 100% to 64dp on scroll-down)
        Row(
            Modifier
                .width(currentSurfaceWidthDp)
                .height(64f.dp)
                .graphicsLayer {
                    translationX = panelOffset * (1f - clampedCompact)
                }
                .then(
                    if (glassSupported) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            layerBlock = {
                                val progress = dampedDragAnimation.pressProgress
                                val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width.coerceAtLeast(1f), progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(containerColor)
                            .border(1.dp, Color.White.copy(alpha = 0.14f), Capsule())
                    },
                )
                .then(interactiveHighlight.modifier)
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (clampedCompact < 0.95f) {
                val rowAlpha = (1f - clampedCompact * 1.25f).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .alpha(rowAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }

        // 2. Hidden Refracted Accent Layer for YTX Active Lens
        if (glassSupported && clampedCompact < 0.95f) {
            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                },
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset * (1f - clampedCompact)
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(
                                    24f.dp.toPx() * progress,
                                    24f.dp.toPx() * progress,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }

        // 3. YTX Moving Active Tab Indicator Lens Pill (strictly 56.dp height x 1/tabsCount width)
        if (clampedCompact < 0.95f) {
            val indicatorAlpha = (1f - clampedCompact * 1.2f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .padding(horizontal = 4f.dp)
                    .height(56f.dp)
                    .fillMaxWidth(1f / tabsCount)
                    .graphicsLayer {
                        val expandedX = if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                        translationX = lerp(expandedX, 0f, clampedCompact)
                        alpha = indicatorAlpha
                    }
                    .then(if (!isCompactMode) interactiveHighlight.gestureModifier else Modifier)
                    .then(if (!isCompactMode) dampedDragAnimation.modifier else Modifier)
                    .then(
                        if (glassSupported) {
                            Modifier.drawBackdrop(
                                backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                                shape = { Capsule() },
                                effects = {
                                    val progress = dampedDragAnimation.pressProgress
                                    lens(
                                        10f.dp.toPx() * progress,
                                        14f.dp.toPx() * progress,
                                        chromaticAberration = true,
                                    )
                                },
                                highlight = {
                                    val progress = dampedDragAnimation.pressProgress
                                    Highlight.Default.copy(alpha = progress * indicatorAlpha)
                                },
                                shadow = {
                                    val progress = dampedDragAnimation.pressProgress
                                    Shadow(alpha = progress * indicatorAlpha)
                                },
                                innerShadow = {
                                    val progress = dampedDragAnimation.pressProgress
                                    InnerShadow(
                                        radius = 8f.dp * progress,
                                        alpha = progress * indicatorAlpha,
                                    )
                                },
                                layerBlock = {
                                    scaleX = dampedDragAnimation.scaleX
                                    scaleY = dampedDragAnimation.scaleY
                                    val velocity = dampedDragAnimation.velocity / 10f
                                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = dampedDragAnimation.pressProgress
                                    drawRect(
                                        if (isLightTheme) Color.Black.copy(0.10f)
                                        else Color.White.copy(0.12f),
                                        alpha = (1f - progress) * indicatorAlpha,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress * indicatorAlpha))
                                },
                            )
                        } else {
                            Modifier
                                .clip(Capsule())
                                .background(accentColor.copy(alpha = 0.20f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), Capsule())
                        },
                    ),
            )
        }

        // 4. Collapsed Single-Menu Pill Icon (when scrolled down; tap expands all tabs)
        if (clampedCompact > 0.05f && compactIcon != null) {
            val compactAlpha = ((clampedCompact - 0.15f) / 0.85f).coerceIn(0f, 1f)
            val contentColor = if (isLightTheme) Color.Black else accentColor
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .alpha(compactAlpha)
                    .clip(Capsule())
                    .then(
                        if (isCompactMode) {
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    view.navHaptic(14L, 150)
                                    onExpandCompact()
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = compactIcon,
                    contentDescription = "Expand Navigation Menu",
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val s = lerp(0.75f, 1f, compactAlpha)
                            scaleX = s
                            scaleY = s
                        },
                )
            }
        }
    }
}

@Composable
fun LiquidBottomBar(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tabsCount: Int = TopLevelDestination.entries.size,
    isDarkSurface: Boolean = false,
    compactProgress: Float = 0f,
    compactIcon: ImageVector? = null,
    onExpandCompact: () -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        modifier = modifier,
        isDarkSurface = isDarkSurface,
        compactProgress = compactProgress,
        compactIcon = compactIcon,
        onExpandCompact = onExpandCompact,
        content = content,
    )
}

@Composable
fun RowScope.LiquidNavTab(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    isDarkSurface: Boolean = false,
    onClick: () -> Unit,
) {
    val isLightTheme = !isDarkSurface && !isSystemInDarkTheme() && MaterialTheme.colorScheme.background.luminance() >= 0.5f
    val accentColor = MaterialTheme.colorScheme.primary
    val contentColor = when {
        selected -> accentColor
        isLightTheme -> Color(0xFF0F172A)
        else -> Color.White.copy(alpha = 0.85f)
    }

    LiquidBottomTab(onClick = onClick) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        BasicText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                color = contentColor,
                fontSize = 11.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 11.sp,
                stepSize = 0.5.sp,
            ),
        )
    }
}
