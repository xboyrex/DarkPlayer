package com.mayank.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val safePadding = WindowInsets.safeDrawing.union(WindowInsets.displayCutout).asPaddingValues()

    val sheetBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xEE141916),
            Color(0xF20D110F),
        ),
    )

    AnimatedVisibility(
        modifier = modifier
            .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
            .padding(
                start = if (isLandscape) 12.dp else 12.dp,
                end = if (isLandscape) (safePadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 12.dp) else 12.dp,
                top = if (isLandscape) 12.dp else 0.dp,
                bottom = 12.dp,
            ),
        visible = show,
        enter = if (isLandscape) {
            slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
                initialOffsetX = { it / 2 },
            ) + scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
            ) + fadeIn(tween(150))
        } else {
            slideInVertically(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
                initialOffsetY = { it / 2 },
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
            ) + fadeIn(tween(150))
        },
        exit = if (isLandscape) {
            slideOutHorizontally(
                animationSpec = tween(130),
                targetOffsetX = { it / 2 },
            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(120)) + fadeOut(tween(110))
        } else {
            slideOutVertically(
                animationSpec = tween(130),
                targetOffsetY = { it / 2 },
            ) + scaleOut(targetScale = 0.96f, animationSpec = tween(120)) + fadeOut(tween(110))
        },
    ) {
        val shape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        Modifier
                            .fillMaxHeight()
                            .widthIn(min = 310.dp, max = 380.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                    },
                )
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(sheetBrush)
                    .padding(top = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x44F4EFE8)),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF4EFE8),
                    )
                }
                Spacer(Modifier.height(8.dp))
                val overlayDarkScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color(0xFFF4EFE8),
                    onSurfaceVariant = Color(0xFFB4BEB6),
                    surface = Color(0xFF141916),
                    surfaceContainer = Color(0xFF1C231F),
                    surfaceContainerHigh = Color(0xFF232B26),
                    surfaceContainerHighest = Color(0xFF2B342E),
                    outline = Color(0xFF4A564D),
                    outlineVariant = Color(0xFF333D36),
                )
                MaterialTheme(colorScheme = overlayDarkScheme) {
                    CompositionLocalProvider(LocalContentColor provides Color(0xFFF4EFE8)) {
                        content()
                    }
                }
            }
        }
    }
}
