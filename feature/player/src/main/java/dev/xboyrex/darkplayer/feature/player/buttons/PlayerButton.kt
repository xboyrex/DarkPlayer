package dev.xboyrex.darkplayer.feature.player.buttons

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.glassOverlayFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.core.ui.glass.rememberReducedMotion
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = Color(0xFF141720).copy(alpha = 0.92f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimaryAction: Boolean = false,
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
    val backdrop = LocalGlassBackdrop.current
    val reduced = rememberReducedMotion()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduced) 0.90f else 1f,
        animationSpec = if (reduced) {
            tween(0)
        } else {
            spring(dampingRatio = 0.55f, stiffness = if (isPressed) 900f else 420f)
        },
        label = "playerBtnScale",
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

    val useLiquidGlass = backdrop != null && GlassSettings.quality != GlassQuality.Off
    val c = MaterialTheme.colorScheme

    val contentTint = when {
        !enabled -> Color.White.copy(alpha = 0.38f)
        !useLiquidGlass && isPrimaryAction -> c.onPrimary
        else -> Color.White
    }

    val rippleConfiguration = RippleConfiguration(
        color = Color.White,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.35f,
            focusedAlpha = 0.35f,
            draggedAlpha = 0.35f,
            hoveredAlpha = 0.35f,
        ),
    )

    CompositionLocalProvider(
        LocalContentColor provides contentTint,
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (!useLiquidGlass && enabled) {
                        Modifier.shadow(
                            elevation = if (isPrimaryAction) 12.dp else 6.dp,
                            shape = shape,
                            ambientColor = if (isPrimaryAction) c.primary.copy(alpha = 0.5f) else Color.Black,
                            spotColor = if (isPrimaryAction) c.primary.copy(alpha = 0.6f) else Color.Black,
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(shape = shape)
                .then(
                    if (useLiquidGlass) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = shape,
                                tint = glassOverlayFill(if (enabled) (if (isPrimaryAction) 0.56f else 0.44f) else 0.22f),
                                blur = if (isPrimaryAction) 24.dp else 20.dp,
                                refractionHeight = if (isPrimaryAction) 14.dp else 11.dp,
                                refractionAmount = if (isPrimaryAction) 24.dp else 18.dp,
                            ),
                        )
                    } else if (isPrimaryAction) {
                        // Solid UI Primary Play/Pause Button: Vibrant studio accent squircle with subtle inner rim
                        Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        c.primary,
                                        c.primary.copy(alpha = 0.88f),
                                    ),
                                ),
                                shape = shape,
                            )
                            .border(
                                width = 1.4.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.38f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                shape = shape,
                            )
                    } else {
                        // Solid UI Control Button: Distinct Matte Obsidian Studio surface with accent-tinted rim
                        val alphaMul = if (enabled) 1f else 0.45f
                        Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF202534).copy(alpha = 0.96f * alphaMul),
                                        Color(0xFF12151E).copy(alpha = 0.98f * alphaMul),
                                    ),
                                ),
                                shape = shape,
                            )
                            .border(
                                width = 1.1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        c.primary.copy(alpha = 0.52f * alphaMul),
                                        Color.White.copy(alpha = 0.18f * alphaMul),
                                    ),
                                ),
                                shape = shape,
                            )
                    },
                )
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
