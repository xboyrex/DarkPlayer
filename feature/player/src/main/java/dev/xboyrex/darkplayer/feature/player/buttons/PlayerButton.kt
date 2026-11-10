package dev.xboyrex.darkplayer.feature.player.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.glass.InteractiveHighlight
import dev.xboyrex.darkplayer.core.ui.glass.applyGlassDragScale
import dev.xboyrex.darkplayer.feature.player.LocalHidePlayerButtonsBackground
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = Color(0xFF1C1C1E).copy(alpha = 0.72f)
private val PlayerSolidSurfaceColor = Color(0xFF1C1C1E)
private val PlayerSolidPrimaryWhite = Color(0xFFFFFFFF)
private val PlayerSolidOnWhite = Color(0xFF0A0E17)
private val PlayerActiveAccent = Color(0xFFFACC15)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimaryAction: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = CircleShape,
    containerColor: Color = PlayerButtonBlackAlpha,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val useSolidMode = LocalUseMaterialYouControls.current
    val hideBackground = LocalHidePlayerButtonsBackground.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 620f),
        label = "playerButtonScale",
    )

    LaunchedEffect(key1 = interactionSource) {
        var isLongPressClicked = false
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongPressClicked = false
                    delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                    onLongClick?.let {
                        isLongPressClicked = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it.invoke()
                    }
                }

                is PressInteraction.Release -> {
                    if (!isLongPressClicked) {
                        onClick()
                    }
                }
            }
        }
    }

    val targetContainerColor = when {
        hideBackground && !isPrimaryAction && !isActive -> Color.Transparent
        !enabled -> if (useSolidMode) PlayerSolidSurfaceColor.copy(alpha = 0.38f) else containerColor.copy(alpha = 0.28f)
        isPrimaryAction -> PlayerSolidPrimaryWhite
        isActive && useSolidMode -> PlayerSolidPrimaryWhite
        isActive -> PlayerActiveAccent.copy(alpha = 0.26f)
        useSolidMode -> PlayerSolidSurfaceColor.copy(alpha = 0.88f)
        else -> containerColor
    }

    val targetContentColor = when {
        !enabled -> Color.White.copy(alpha = 0.38f)
        isPrimaryAction -> PlayerSolidOnWhite
        isActive && useSolidMode -> PlayerSolidOnWhite
        isActive -> PlayerActiveAccent
        else -> Color.White
    }

    val animatedBg by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 150),
        label = "playerBtnBg",
    )
    val animatedFg by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 150),
        label = "playerBtnFg",
    )

    val rippleConfiguration = RippleConfiguration(
        color = if (isPrimaryAction || (useSolidMode && isActive)) Color.Black else Color.White,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.24f,
            focusedAlpha = 0.24f,
            draggedAlpha = 0.24f,
            hoveredAlpha = 0.24f,
        ),
    )

    val showGlassChrome = !(hideBackground && !isPrimaryAction && !isActive)

    CompositionLocalProvider(
        LocalContentColor provides animatedFg,
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    applyGlassDragScale(
                        pressProgress = interactiveHighlight.pressProgress,
                        offset = interactiveHighlight.offset,
                        nativeButtonLift = true,
                    )
                    scaleX *= scale
                    scaleY *= scale
                }
                .clip(shape = shape)
                .background(color = animatedBg, shape = shape)
                .then(
                    if (showGlassChrome) {
                        Modifier
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.White.copy(alpha = if (isPrimaryAction) 0.32f else 0.16f),
                                        0.5f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.12f),
                                    ),
                                )
                            }
                            .border(
                                width = 0.75.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        if (isActive && !useSolidMode) PlayerActiveAccent.copy(alpha = 0.65f)
                                        else Color.White.copy(alpha = 0.34f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                shape = shape,
                            )
                    } else {
                        Modifier
                    },
                )
                .then(if (enabled) interactiveHighlight.modifier else Modifier)
                .then(if (enabled) interactiveHighlight.gestureModifier else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    enabled = enabled,
                    onClick = {},
                )
                .padding(paddingValues = contentPadding)
                .tvFocusRing(isTv = isTv),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
