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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
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
import dev.xboyrex.darkplayer.core.ui.glass.NativeControlLensStyle
import dev.xboyrex.darkplayer.core.ui.glass.nativeControlLensGeometry
import dev.xboyrex.darkplayer.core.ui.glass.navHaptic
import kotlin.math.PI
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

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    isDarkSurface: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val bgLum = MaterialTheme.colorScheme.background.luminance()
    val isLightTheme = !isDarkSurface && !isSystemInDarkTheme() && bgLum >= 0.5f
    val isAmoled = bgLum < 0.005f
    val glassSupported = GlassQuality.effectsSupported
    // Glasmio default container colors (#1C1C1E @ 0.54f in dark, #F8F8FA @ 0.64f in light)
    val solidContainer = if (isAmoled) Color(0xFF1C1C1E) else MaterialTheme.colorScheme.surfaceContainer
    val containerColor = when {
        !glassSupported -> solidContainer
        isLightTheme -> Color(0xFFF8F8FA).copy(alpha = 0.64f)
        else -> Color(0xFF1C1C1E).copy(alpha = 0.56f)
    }

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val fullWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val tabWidth = with(density) {
            (fullWidthPx - 8f.dp.toPx()) / tabsCount
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
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f,
                    )
                },
            )
        }

        // Glasmio GlassBottomBar platter puff physics (from Glasmio GlassBottomBar.kt)
        val platterPuff = remember { Animatable(0f, 0.001f) }
        val flexLongDimension = fullWidthPx / density.density
        val flexShortDimension = 64f
        val flexSizeProgress = ((flexShortDimension - 44f) / 116f).coerceIn(0f, 1f)
        val flexSpringProgress = ((flexLongDimension - 44f) / 116f).coerceIn(0f, 1f)
        val puffLiftPoints = 16f - 12f * flexSizeProgress
        val puffPressSpring = remember(flexSpringProgress) {
            val response = 0.262 + (0.314 - 0.262) * flexSpringProgress
            spring<Float>(0.625f, (4.0 * PI * PI / (response * response)).toFloat(), 0.001f)
        }
        val puffReleaseSpring = remember(flexSpringProgress) {
            val response = 0.4 + (0.36 - 0.4) * flexSpringProgress
            spring<Float>(
                lerp(0.375f, 0.6f, flexSpringProgress),
                (4.0 * PI * PI / (response * response)).toFloat(),
                0.001f,
            )
        }
        LaunchedEffect(dampedDragAnimation.pressProgress > 0.05f, puffPressSpring, puffReleaseSpring) {
            val pressed = dampedDragAnimation.pressProgress > 0.05f
            platterPuff.animateTo(
                if (pressed) 1f else 0f,
                if (pressed) puffPressSpring else puffReleaseSpring,
            )
        }
        val panelPressScale by remember(platterPuff, puffLiftPoints, flexLongDimension) {
            derivedStateOf {
                1f + puffLiftPoints * platterPuff.value / flexLongDimension.coerceAtLeast(1f)
            }
        }

        // 1. Glasmio Bottom Navigation Bar Background Platter Capsule
        Box(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                    scaleX = panelPressScale
                    scaleY = panelPressScale
                }
                .then(
                    if (glassSupported) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(12f.dp.toPx())
                                lens(24f.dp.toPx(), 26f.dp.toPx(), chromaticAberration = true)
                            },
                            highlight = { Highlight.Default },
                            shadow = { Shadow.Default.copy(alpha = 0.35f) },
                            innerShadow = { InnerShadow(radius = 10.dp, alpha = 0.50f) },
                            onDrawSurface = {
                                drawRect(containerColor)
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = if (isLightTheme) 0.26f else 0.14f),
                                        0.5f to Color.White.copy(alpha = 0.04f),
                                        1f to Color.Black.copy(alpha = 0.08f),
                                    ),
                                )
                            },
                        ).border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = if (isLightTheme) 0.64f else 0.32f),
                                    Color.White.copy(alpha = if (isLightTheme) 0.22f else 0.10f),
                                ),
                            ),
                            shape = Capsule(),
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(containerColor)
                            .border(
                                0.75.dp,
                                Color(0x2BFFFFFF),
                                Capsule(),
                            )
                    },
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth(),
        )

        // 2. Glasmio Moving Active Tab Indicator Lens Pill (with NativeControlLensStyle.TabBarIndicator + velocity deformation)
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                    val velocity = dampedDragAnimation.velocity / 10f
                    scaleX = panelPressScale / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                    scaleY = panelPressScale * (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                }
                .height(54f.dp)
                .fillMaxWidth(1f / tabsCount)
                .nativeControlLensGeometry(
                    NativeControlLensStyle.TabBarIndicator,
                    horizontalProgress = { if (glassSupported) dampedDragAnimation.pressProgress else 0f },
                )
                .then(
                    if (glassSupported) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                if (progress < 0.999f) blur(8f.dp.toPx() * (1f - progress))
                                lens(
                                    (12f + 12f * progress).dp.toPx(),
                                    (16f + 12f * progress).dp.toPx(),
                                    chromaticAberration = true,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = (0.45f + 0.55f * progress).coerceIn(0f, 1f))
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress
                                Shadow(alpha = 0.25f + 0.75f * progress)
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 8f.dp * (0.4f + 0.6f * progress),
                                    alpha = 0.35f + 0.65f * progress,
                                )
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    if (isLightTheme) Color.Black.copy(0.10f)
                                    else Color.White.copy(0.14f),
                                    alpha = 1f - 0.55f * progress,
                                )
                                drawRect(Color.White.copy(alpha = 0.08f * progress))
                            },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(Color.White.copy(alpha = 0.14f))
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = 0.16f),
                                        1f to Color.Transparent,
                                    ),
                                )
                            }
                    },
                ),
        )

        // 3. Glasmio Tab Icons & Labels (1.16f selected content magnification on press)
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.16f, dampedDragAnimation.pressProgress) * panelPressScale
            },
        ) {
            Row(
                Modifier
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
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
    content: @Composable RowScope.() -> Unit,
) {
    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        modifier = modifier,
        isDarkSurface = isDarkSurface,
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
    val unselectedColor = if (isLightTheme) Color.Black.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.85f)
    val contentColor = if (selected) accentColor else unselectedColor

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
                color = if (selected) (if (isLightTheme) Color.Black else Color.White) else unselectedColor,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 8.sp,
                maxFontSize = 10.5.sp,
                stepSize = 0.5.sp,
            ),
        )
    }
}
