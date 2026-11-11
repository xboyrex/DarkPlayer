package dev.xboyrex.darkplayer.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import dev.xboyrex.darkplayer.core.ui.glass.NavBarStyle
import dev.xboyrex.darkplayer.core.ui.glass.UiStylePreferences
import dev.xboyrex.darkplayer.core.ui.glass.navHaptic
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

data class NavTabItemData(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

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
 * Exact 1:1 copy-paste from YTXTube-main/app/src/main/java/com/tensei/ytxtube/GlassNavBar.kt:
 * 1. Bottom navigation bar background capsule (64.dp height)
 * 2. Moving active tab indicator pill BEHIND the tab row (56.dp height, 1f/tabsCount width)
 * 3. Tab icons and labels on top with DampedDragAnimation & InteractiveHighlight
 */
@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    isDarkSurface: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val bgLum = MaterialTheme.colorScheme.background.luminance()
    val isLightTheme = !isDarkSurface && !isSystemInDarkTheme() && bgLum > 0.5f
    val isAmoled = bgLum < 0.01f
    val glassSupported = GlassQuality.effectsSupported
    val solidContainer = if (isAmoled) Color(0xFF0E1015) else MaterialTheme.colorScheme.surfaceContainer
    val containerColor = when {
        !glassSupported -> solidContainer
        isAmoled -> Color(0xFF0A0C10).copy(0.55f)
        isLightTheme -> Color(0xFFFAFAFA).copy(0.4f)
        else -> Color(0xFF121212).copy(0.45f)
    }

    BoxWithConstraints(
        modifier = modifier.height(64f.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()).coerceAtLeast(1f) / tabsCount.coerceAtLeast(1)
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1)).fastCoerceIn(-1f, 1f)
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
        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).coerceAtLeast(0).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {
                    view.navHaptic(18L, 170)
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, (tabsCount - 1).coerceAtLeast(0))
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
                    val before = targetValue.fastRoundToInt()
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).coerceAtLeast(0).toFloat()),
                    )

                    if (targetValue.fastRoundToInt() != before) {
                        view.navHaptic(16L, 170)
                    }
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
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

        // 1. Bottom navigation bar background capsule
        Box(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
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
                            highlight = { Highlight.Default.copy(alpha = 0.72f) },
                            shadow = { Shadow.Default.copy(alpha = 0.45f) },
                            innerShadow = { InnerShadow(radius = 8.dp, alpha = 0.45f) },
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
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                Capsule(),
                            )
                    },
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.36f),
                            Color.White.copy(alpha = 0.10f),
                        ),
                    ),
                    shape = Capsule(),
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth(),
        )

        // 2. Moving active tab indicator pill BEHIND the tab row
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
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
                                    10f.dp.toPx() * progress,
                                    14f.dp.toPx() * progress,
                                    chromaticAberration = true,
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f))
                            },
                            shadow = {
                                val progress = dampedDragAnimation.pressProgress
                                Shadow(alpha = progress)
                            },
                            innerShadow = {
                                val progress = dampedDragAnimation.pressProgress
                                InnerShadow(
                                    radius = 8f.dp * progress,
                                    alpha = progress,
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
                                    if (isLightTheme) Color.Black.copy(0.12f)
                                    else Color.White.copy(0.16f),
                                    alpha = 1f - progress * 0.65f,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    },
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount.coerceAtLeast(1)),
        )

        // 3. Tab icons and labels on top
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.15f, dampedDragAnimation.pressProgress)
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
fun RowScope.LiquidNavTab(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    isDarkSurface: Boolean = true,
    onClick: () -> Unit,
) {
    val bgLum = MaterialTheme.colorScheme.background.luminance()
    val isLightTheme = !isDarkSurface && !isSystemInDarkTheme() && bgLum > 0.5f
    val contentColor = if (isLightTheme) Color.Black else Color.White

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

/**
 * Style 2: Cutout Floating Circle Bar (4306.webp & 4308.png)
 * Concave cubic-bezier notch cutout that animates to the active tab while the active icon rises into a floating circle above the bar.
 */
@Composable
fun CutoutFloatingCircleNavBar(
    tabs: List<NavTabItemData>,
    selectedIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 380f),
        label = "cutoutNavIndex",
    )

    BoxWithConstraints(
        modifier = modifier.height(78.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val density = LocalDensity.current
        val barWidthPx = constraints.maxWidth.toFloat()
        val tabCount = tabs.size.coerceAtLeast(1)
        val tabSlotWidthPx = barWidthPx / tabCount
        val notchCenterX = (animatedIndex + 0.5f) * tabSlotWidthPx
        val barHeightDp = 60.dp
        val circleSizeDp = 48.dp

        // Bar surface with animated concave cubic-bezier cutout notch
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
                .align(Alignment.BottomCenter),
        ) {
            val w = size.width
            val h = size.height
            val cornerR = 26.dp.toPx()
            val notchR = 30.dp.toPx()
            val notchDepth = 26.dp.toPx()
            val cx = notchCenterX.coerceIn(cornerR + notchR, w - cornerR - notchR)

            val path = Path().apply {
                moveTo(cornerR, 0f)
                // Left of notch
                lineTo((cx - notchR * 1.45f).coerceAtLeast(cornerR), 0f)
                // Smooth cubic bezier into concave valley
                cubicTo(
                    cx - notchR * 0.85f, 0f,
                    cx - notchR * 0.72f, notchDepth,
                    cx, notchDepth,
                )
                // Smooth cubic bezier out of concave valley
                cubicTo(
                    cx + notchR * 0.72f, notchDepth,
                    cx + notchR * 0.85f, 0f,
                    (cx + notchR * 1.45f).coerceAtMost(w - cornerR), 0f,
                )
                lineTo(w - cornerR, 0f)
                arcTo(
                    rect = Rect(w - 2 * cornerR, 0f, w, 2 * cornerR),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(w, h - cornerR)
                arcTo(
                    rect = Rect(w - 2 * cornerR, h - 2 * cornerR, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(cornerR, h)
                arcTo(
                    rect = Rect(0f, h - 2 * cornerR, 2 * cornerR, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                lineTo(0f, cornerR)
                arcTo(
                    rect = Rect(0f, 0f, 2 * cornerR, 2 * cornerR),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false,
                )
                close()
            }

            drawPath(
                path = path,
                color = Color(0xFF11131A),
            )
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        // Floating active circle seated inside the cutout curve
        val activeTab = tabs.getOrElse(selectedIndex) { tabs.first() }
        val circleOffsetXPx = notchCenterX - with(density) { (circleSizeDp / 2).toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = circleOffsetXPx.roundToInt(),
                        y = 2.dp.roundToPx(),
                    )
                }
                .size(circleSizeDp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.6f),
                    spotColor = accentColor.copy(alpha = 0.6f),
                )
                .clip(CircleShape)
                .background(accentColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = activeTab.selectedIcon,
                contentDescription = activeTab.label,
                tint = if (accentColor.luminance() > 0.55f) Color.Black else Color.White,
                modifier = Modifier.size(23.dp),
            )
        }

        // Tab items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val iconAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0f else 0.72f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "cutoutIconAlpha",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            view.navHaptic(14L, 150)
                            item.onClick()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (!isSelected) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White.copy(alpha = iconAlpha),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.58f),
                            maxLines = 1,
                        )
                    } else {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            text = item.label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Style 3: Expandable Pill Bar (4307.jpg)
 * Deep black stadium capsule bar where the selected item smoothly expands horizontally into a dark pill with [Icon + Label].
 */
@Composable
fun ExpandablePillNavBar(
    tabs: List<NavTabItemData>,
    selectedIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0A0C10))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.07f),
                    ),
                ),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val pillBg by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF252832) else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "expandPillBg",
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillBg)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 0.8.dp,
                                color = accentColor.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(999.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        view.navHaptic(14L, 150)
                        item.onClick()
                    }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.74f,
                            stiffness = 520f,
                        ),
                    )
                    .padding(
                        horizontal = if (isSelected) 16.dp else 14.dp,
                        vertical = 10.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp),
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                ) {
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Style 4: Interactive Cradle Bar (4309.png)
 * Sleek modern bar with glowing active pill and interactive bouncy elevation.
 */
@Composable
fun InteractiveCradleNavBar(
    tabs: List<NavTabItemData>,
    selectedIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF12151E).copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.10f),
                    ),
                ),
                shape = RoundedCornerShape(28.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val liftDp by animateDpAsState(
                targetValue = if (isSelected) (-3).dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "cradleLift",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .offset(y = liftDp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        view.navHaptic(14L, 150)
                        item.onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 32.dp else 26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) accentColor else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = when {
                                isSelected && accentColor.luminance() > 0.55f -> Color.Black
                                isSelected -> Color.White
                                else -> Color.White.copy(alpha = 0.68f)
                            },
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Dispatcher that renders the user-selected Navigation Bar style from UiStylePreferences.
 */
@Composable
fun NovaMultiStyleBottomBar(
    tabs: List<NavTabItemData>,
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        UiStylePreferences.init(context)
    }
    val style = UiStylePreferences.navBarStyle
    val accentColor = UiStylePreferences.activeAccentColor
    val currentIndex = selectedTabIndex()

    when (style) {
        NavBarStyle.CUTOUT_FLOATING_CIRCLE -> {
            CutoutFloatingCircleNavBar(
                tabs = tabs,
                selectedIndex = currentIndex,
                accentColor = accentColor,
                modifier = modifier,
            )
        }
        NavBarStyle.EXPANDABLE_PILL -> {
            ExpandablePillNavBar(
                tabs = tabs,
                selectedIndex = currentIndex,
                accentColor = accentColor,
                modifier = modifier,
            )
        }
        NavBarStyle.CRADLE_INTERACTIVE -> {
            InteractiveCradleNavBar(
                tabs = tabs,
                selectedIndex = currentIndex,
                accentColor = accentColor,
                modifier = modifier,
            )
        }
        NavBarStyle.YTX_LIQUID_GLASS,
        NavBarStyle.GLASMIO_LIQUID -> {
            LiquidBottomTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
                backdrop = backdrop,
                tabsCount = tabs.size,
                modifier = modifier,
            ) {
                tabs.forEach { tab ->
                    LiquidNavTab(
                        label = tab.label,
                        icon = tab.icon,
                        selectedIcon = tab.selectedIcon,
                        selected = tab.selected,
                        onClick = tab.onClick,
                    )
                }
            }
        }
    }
}
