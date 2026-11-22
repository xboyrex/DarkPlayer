package com.mayank.darkplayer.feature.player.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.common.extensions.isTelevision
import com.mayank.darkplayer.core.ui.components.tvFocusRing
import com.mayank.darkplayer.core.ui.theme.DarkPlayerUiPrefs
import com.mayank.darkplayer.core.ui.theme.calmPressScale
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillFill
import com.mayank.darkplayer.core.ui.theme.PlayerControlPillIcon
import com.mayank.darkplayer.feature.player.LocalUseMaterialYouControls
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

val PlayerButtonBlackAlpha = PlayerControlPillFill

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = PlayerControlPillFill,
    contentColor: Color = PlayerControlPillIcon,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }

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

    DarkPlayerUiPrefs.ensureLoaded(context)
    val isDarkBtn = DarkPlayerUiPrefs.playerButtonsDark
    val resolvedContainer by animateColorAsState(
        targetValue = if (containerColor == PlayerControlPillFill) {
            if (isDarkBtn) Color(0xCC121614) else Color(0xE8F4EFE8)
        } else {
            containerColor
        },
        animationSpec = tween(180),
        label = "player_btn_bg",
    )
    val resolvedContent by animateColorAsState(
        targetValue = if (contentColor == PlayerControlPillIcon) {
            if (isDarkBtn) Color(0xFFF4EFE8) else Color(0xFF161B17)
        } else {
            contentColor
        },
        animationSpec = tween(180),
        label = "player_btn_fg",
    )

    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = resolvedContent,
        containerColor = resolvedContainer,
    )

    val rippleConfiguration = if (LocalUseMaterialYouControls.current) {
        LocalRippleConfiguration.current
    } else {
        RippleConfiguration(
            color = Color.White,
            rippleAlpha = RippleAlpha(
                pressedAlpha = 0.4f,
                focusedAlpha = 0.4f,
                draggedAlpha = 0.4f,
                hoveredAlpha = 0.4f,
            ),
        )
    }

    CompositionLocalProvider(
        LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        Box(
            modifier = modifier
                .calmPressScale(pressedScale = 0.90f, enabled = enabled)
                .clip(shape = CircleShape)
                .background(
                    color = if (enabled) colors.containerColor else colors.disabledContainerColor,
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
