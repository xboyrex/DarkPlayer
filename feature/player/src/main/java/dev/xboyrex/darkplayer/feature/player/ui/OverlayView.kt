package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.requestFocusUntilLanded
import dev.xboyrex.darkplayer.core.ui.glass.GlassQuality
import dev.xboyrex.darkplayer.core.ui.glass.GlassSettings
import dev.xboyrex.darkplayer.core.ui.glass.LocalGlassBackdrop
import dev.xboyrex.darkplayer.core.ui.glass.Motion
import dev.xboyrex.darkplayer.core.ui.glass.glassFill
import dev.xboyrex.darkplayer.core.ui.glass.glassSpec
import dev.xboyrex.darkplayer.core.ui.glass.liquidGlass
import dev.xboyrex.darkplayer.core.ui.theme.DarkPlayerTheme

@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val layoutDirection = LocalLayoutDirection.current
    val backdrop = LocalGlassBackdrop.current
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection = layoutDirection)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    val sheetShape = RoundedCornerShape(28.dp)
    val useRealGlass = backdrop != null && GlassSettings.quality != GlassQuality.Off

    AnimatedVisibility(
        modifier = Modifier.align(
            alignment = if (configuration.isPortrait) {
                Alignment.BottomCenter
            } else {
                Alignment.CenterEnd
            },
        ),
        visible = show,
        enter = if (configuration.isPortrait) {
            slideInVertically(tween(260, easing = Motion.Emphasized)) { it } +
                fadeIn(tween(200, easing = Motion.Emphasized))
        } else {
            slideInHorizontally(tween(260, easing = Motion.Emphasized)) { it } +
                fadeIn(tween(200, easing = Motion.Emphasized))
        },
        exit = if (configuration.isPortrait) {
            slideOutVertically(tween(220, easing = Motion.Emphasized)) { it } +
                fadeOut(tween(160, easing = Motion.Emphasized))
        } else {
            slideOutHorizontally(tween(220, easing = Motion.Emphasized)) { it } +
                fadeOut(tween(160, easing = Motion.Emphasized))
        },
    ) {
        Surface(
            shape = sheetShape,
            color = if (useRealGlass) Color.Transparent else Color(0xFF070A12).copy(alpha = 0.72f),
            contentColor = Color.White,
            modifier = modifier
                .padding(if (configuration.isPortrait) 8.dp else 10.dp)
                .then(
                    if (useRealGlass) {
                        Modifier.liquidGlass(
                            backdrop = backdrop,
                            spec = glassSpec(
                                shape = sheetShape,
                                tint = glassFill(0.62f),
                                blur = 26.dp,
                                refractionHeight = 18.dp,
                                refractionAmount = 24.dp,
                            ),
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.06f),
                                ),
                            ),
                            shape = sheetShape,
                        )
                    },
                )
                .then(
                    other = if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.56f)
                    } else {
                        Modifier
                            .fillMaxWidth(fraction = 0.45f)
                            .fillMaxHeight()
                    },
                ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .focusRequester(focusRequester = focusRequester)
                        .focusGroup()
                        .padding(top = 12.dp)
                        .padding(end = endPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.32f)),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(0.8.dp)
                            .background(Color.White.copy(alpha = 0.14f)),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverlayView() {
    DarkPlayerTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            OverlayView(
                modifier = Modifier.align(Alignment.BottomCenter),
                title = "Selector view",
                show = true,
            ) {
                Text(text = "Sample content")
            }
        }
    }
}
