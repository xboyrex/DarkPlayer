package dev.xboyrex.darkplayer.feature.player.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = Color(0xFF0A0E17).copy(alpha = 0.56f)
private val PlayerLiquidGlassFillBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color(0xFF0A0E17).copy(alpha = 0.62f),
    ),
)
private val PlayerLiquidGlassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.44f),
        Color.White.copy(alpha = 0.12f),
    ),
)

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
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 780f),
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

    val activeAccent = Color(0xFFFACC15)

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
        LocalContentColor provides when {
            !enabled -> Color.White.copy(alpha = 0.40f)
            isActive -> activeAccent
            else -> Color.White
        },
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = if (enabled) 6.dp else 0.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = Color.Black.copy(alpha = 0.65f),
                )
                .clip(shape = shape)
                .background(
                    brush = if (isActive) {
                        Brush.verticalGradient(
                            listOf(
                                activeAccent.copy(alpha = 0.28f),
                                Color(0xFF0A0E17).copy(alpha = 0.72f),
                            ),
                        )
                    } else if (enabled) {
                        PlayerLiquidGlassFillBrush
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0A0E17).copy(alpha = 0.25f),
                                Color(0xFF0A0E17).copy(alpha = 0.25f),
                            ),
                        )
                    },
                )
                .border(
                    width = 1.dp,
                    brush = if (isActive) {
                        Brush.verticalGradient(
                            listOf(
                                activeAccent.copy(alpha = 0.75f),
                                activeAccent.copy(alpha = 0.25f),
                            ),
                        )
                    } else {
                        PlayerLiquidGlassBorderBrush
                    },
                    shape = shape,
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
