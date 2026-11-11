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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = Color(0xFF0A0E17).copy(alpha = 0.62f)
private val PlayerGlassBorderBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.24f),
        Color.White.copy(alpha = 0.07f),
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isOutline: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    containerColor: Color = if (LocalUseMaterialYouControls.current) {
        IconButtonDefaults.filledTonalIconButtonColors().containerColor
    } else {
        PlayerButtonBlackAlpha
    },
    contentColor: Color = if (LocalUseMaterialYouControls.current) {
        IconButtonDefaults.filledTonalIconButtonColors().contentColor
    } else {
        Color.White
    },
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Fast 10x spring animation on press/release (solid UI, zero liquid glass)
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 1500f,
        ),
        label = "playerBtnScale",
    )

    LaunchedEffect(
        interactionSource,
        onClick,
        onLongClick,
    ) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis.milliseconds)
                    onLongClick?.let {
                        isLongClick = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it.invoke()
                    }
                }

                is PressInteraction.Release -> {
                    if (!isLongClick) {
                        onClick()
                    }
                }
            }
        }
    }

    val shape = IconButtonDefaults.smallRoundShape
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(
                minWidth = 40.dp,
                minHeight = 40.dp,
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .tvFocusRing(
                isTv = isTv,
                shape = CircleShape,
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {},
                role = Role.Button,
            )
            .background(color = containerColor)
            .border(
                width = if (isOutline) 1.5.dp else 0.6.dp,
                color = if (isOutline) MaterialTheme.colorScheme.outline else Color.White.copy(alpha = 0.14f),
                shape = shape,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalRippleConfiguration provides PlayerButtonRippleConfiguration,
        ) {
            content()
        }
    }
}

private val PlayerButtonRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.3f,
        focusedAlpha = 0.2f,
        draggedAlpha = 0.2f,
        hoveredAlpha = 0.2f,
    ),
)
