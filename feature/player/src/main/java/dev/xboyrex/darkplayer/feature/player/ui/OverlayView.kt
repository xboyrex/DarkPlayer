package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.requestFocusUntilLanded
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls

/**
 * Compact, minimal NextPlayer-styled floating overlay sheet.
 */
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
    val useSolidMode = LocalUseMaterialYouControls.current
    val layoutDirection = LocalLayoutDirection.current
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection = layoutDirection)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    val sheetShape = RoundedCornerShape(22.dp)
    val sheetBgColor = if (useSolidMode) {
        Color(0xFF141821)
    } else {
        Color(0xFF0E121A).copy(alpha = 0.94f)
    }

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
            slideInVertically(spring(dampingRatio = 0.84f, stiffness = 420f)) { it / 2 } +
                scaleIn(spring(dampingRatio = 0.84f, stiffness = 420f), initialScale = 0.95f) +
                fadeIn(tween(150))
        } else {
            slideInHorizontally(spring(dampingRatio = 0.84f, stiffness = 420f)) { it / 2 } +
                scaleIn(spring(dampingRatio = 0.84f, stiffness = 420f), initialScale = 0.95f) +
                fadeIn(tween(150))
        },
        exit = if (configuration.isPortrait) {
            slideOutVertically(tween(180)) { it / 2 } +
                scaleOut(tween(150), targetScale = 0.96f) +
                fadeOut(tween(140))
        } else {
            slideOutHorizontally(tween(180)) { it / 2 } +
                scaleOut(tween(150), targetScale = 0.96f) +
                fadeOut(tween(140))
        },
    ) {
        Surface(
            shape = sheetShape,
            color = sheetBgColor,
            contentColor = Color.White,
            modifier = modifier
                .padding(if (configuration.isPortrait) 10.dp else 12.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = sheetShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = sheetShape,
                )
                .then(
                    other = if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    } else {
                        Modifier
                            .widthIn(max = 336.dp)
                            .fillMaxWidth(fraction = 0.40f)
                            .fillMaxHeight()
                    },
                ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester = focusRequester)
                        .focusGroup()
                        .padding(top = 8.dp)
                        .padding(end = endPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.6.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    content()
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOverlayView() {
    NextPlayerTheme {
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
