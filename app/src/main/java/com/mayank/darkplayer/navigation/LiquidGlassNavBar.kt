package com.mayank.darkplayer.navigation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
import com.mayank.darkplayer.core.ui.theme.DarkPlayerFontFamily
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.mayank.darkplayer.core.ui.theme.NavyBorder
import com.mayank.darkplayer.core.ui.theme.NavySurface
import com.mayank.darkplayer.core.ui.theme.NavySurfaceSecondary
import com.mayank.darkplayer.core.ui.theme.NavyTextMuted
import com.mayank.darkplayer.core.ui.theme.NavyTextPrimary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Shared backdrop provided ONLY to the bottom navigation bar.
 * Settings, Player controls, and all other surfaces remain clean Material 3.
 */
val LocalNavGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private fun View.navHaptic(durationMs: Long = 12L, amplitude: Int = 120) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    val done = vibrator?.hasVibrator() == true && runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }.isSuccess
    if (!done) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

/**
 * Exact refractive Liquid Glass bottom navigation bar ported for Dark Player.
 * Uses kyant backdrop vibrancy + blur + lens + chromatic aberration + damped spring drag.
 * Strictly scoped to the bottom navigation bar ONLY.
 */
@Composable
fun LiquidGlassNavigationBar(
    modifier: Modifier = Modifier,
    destinations: List<TopLevelDestination>,
    isSelected: (TopLevelDestination) -> Boolean,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    val backdrop = if (DarkPlayerUiPrefs.liquidGlassEnabled) LocalNavGlassBackdrop.current else null
    val glassSupported = NavGlassSupport.supported && backdrop != null
    val tabsCount = destinations.size.coerceAtLeast(1)
    val selectedIdx = destinations.indexOfFirst(isSelected).coerceAtLeast(0)

    val containerColor = if (glassSupported) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
    }

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth.coerceAtLeast(1))
                    .fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val view = LocalView.current
        var currentIndex by remember { mutableIntStateOf(selectedIdx) }

        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIdx.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = { view.navHaptic(18L, 170) },
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    view.navHaptic(20L, 190)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    val before = targetValue.fastRoundToInt()
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth.coerceAtLeast(1f) * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
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

        LaunchedEffect(selectedIdx) {
            currentIndex = selectedIdx
            if (dampedDragAnimation.targetValue != selectedIdx.toFloat()) {
                dampedDragAnimation.animateToValue(selectedIdx.toFloat())
            }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { idx ->
                    dampedDragAnimation.animateToValue(idx.toFloat())
                    destinations.getOrNull(idx)?.let(onDestinationClick)
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

        // 1. Frosted Liquid Glass capsule background
        Box(
            Modifier
                .graphicsLayer { translationX = panelOffset }
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
                                val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(containerColor)
                            .border(1.dp, NavyBorder.copy(alpha = 0.6f), Capsule())
                    },
                )
                .then(interactiveHighlight.modifier)
                .height(64f.dp)
                .fillMaxWidth(),
        )

        // 2. Moving active tab refractive pill (renders behind icons & labels)
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
                                Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                            },
                            shadow = {
                                Shadow(alpha = dampedDragAnimation.pressProgress)
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 8f.dp * dampedDragAnimation.pressProgress,
                                    alpha = dampedDragAnimation.pressProgress,
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
                                    Color.White.copy(alpha = 0.14f),
                                    alpha = 1f - progress,
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            },
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    },
                )
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount),
        )

        // 3. Sharp tab icons & labels on top
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.15f, dampedDragAnimation.pressProgress)
            },
        ) {
            Row(
                Modifier
                    .graphicsLayer { translationX = panelOffset }
                    .then(interactiveHighlight.gestureModifier)
                    .then(dampedDragAnimation.modifier)
                    .height(64f.dp)
                    .fillMaxWidth()
                    .padding(4f.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, destination ->
                    val selected = index == selectedIdx
                    val scale = LocalLiquidBottomTabScale.current
                    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier
                            .clip(Capsule())
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Tab,
                                onClick = {
                                    view.navHaptic(12L, 130)
                                    currentIndex = index
                                    onDestinationClick(destination)
                                },
                            )
                            .fillMaxHeight()
                            .weight(1f)
                            .graphicsLayer {
                                val s = scale()
                                scaleX = s
                                scaleY = s
                            },
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = stringResource(destination.labelRes),
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        BasicText(
                            text = stringResource(destination.labelRes),
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                color = contentColor,
                                fontFamily = DarkPlayerFontFamily,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
            }
        }
    }
}


@Composable
fun LiquidGlassCircleFab(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = if (DarkPlayerUiPrefs.liquidGlassEnabled) LocalNavGlassBackdrop.current else null
    val view = LocalView.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val iconTint = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .requiredSize(64.dp)
            .then(
                if (backdrop != null && NavGlassSupport.supported) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(22f.dp.toPx(), 22f.dp.toPx())
                        },
                        highlight = { Highlight.Ambient },
                        shadow = { Shadow(radius = 18.dp, color = Color.Black.copy(alpha = 0.40f)) },
                        innerShadow = { InnerShadow(radius = 8.dp, color = Color.White.copy(alpha = 0.08f)) },
                        onDrawSurface = {
                            drawRect(surfaceColor.copy(alpha = 0.78f))
                        },
                    )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(surfaceColor)
                        .border(1.dp, borderColor, CircleShape)
                },
            )
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClick = {
                    view.navHaptic(12L, 140)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp),
        )
    }
}
