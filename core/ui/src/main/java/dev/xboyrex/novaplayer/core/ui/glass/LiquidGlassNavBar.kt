package dev.xboyrex.novaplayer.core.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import dev.xboyrex.novaplayer.core.common.extensions.isTelevision
import dev.xboyrex.novaplayer.core.ui.components.tvFocusRing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scale = LocalLiquidBottomTabScale.current
    val view = LocalView.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    Column(
        modifier
            .then(if (isTv) Modifier.tvFocusRing() else Modifier)
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = {
                    view.novaHaptic(12L, 130)
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
    backdrop: Backdrop?,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    isDarkSurface: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val isWhiteMode = !isDarkSurface && GlassSettings.themeMode == "white"
    val isAmoled = !isWhiteMode
    val glassSupported = backdrop != null && GlassQuality.effectsSupported
    val solidContainer = if (isAmoled) Color(0xFF0A0A0E) else MaterialTheme.colorScheme.surfaceContainer
    val containerColor = when {
        !glassSupported -> solidContainer
        isAmoled -> Color(0xFF000000).copy(alpha = 0.48f)
        else -> Color(0xFFFAFAFC).copy(alpha = 0.44f)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount.coerceAtLeast(1)
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
                    view.novaHaptic(18L, 170)
                },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, (tabsCount - 1).coerceAtLeast(0))
                    view.novaHaptic(20L, 190)
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
                        (targetValue + dragAmount.x / tabWidth.coerceAtLeast(1f) * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).coerceAtLeast(0).toFloat()),
                    )

                    if (targetValue.fastRoundToInt() != before) {
                        view.novaHaptic(16L, 170)
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

        // 1. Liquid Glass navigation bar capsule background
        Box(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .then(
                    if (glassSupported && backdrop != null) {
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
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                Capsule(),
                            )
                    },
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth(),
        )

        // 2. Moving active tab indicator pill behind icons/labels
        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(
                    if (glassSupported && backdrop != null) {
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
                                Highlight.Default.copy(alpha = progress)
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
                                    if (isWhiteMode) Color.Black.copy(0.11f)
                                    else Color.White.copy(0.14f),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    },
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount.coerceAtLeast(1)),
        )

        // 3. Tab icons and labels on top with drag gesture support
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
fun RowScope.LiquidNavTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    isDarkSurface: Boolean = false,
    onClick: () -> Unit,
) {
    val isWhiteMode = !isDarkSurface && GlassSettings.themeMode == "white"
    val contentColor = when {
        selected && !GlassQuality.effectsSupported -> MaterialTheme.colorScheme.onSecondaryContainer
        isWhiteMode -> if (selected) Color(0xFF0A0A0A) else Color(0xFF444650)
        else -> if (selected) Color.White else Color.White.copy(alpha = 0.72f)
    }

    LiquidBottomTab(onClick = onClick) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = icon,
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
