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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.requestFocusUntilLanded
import dev.xboyrex.darkplayer.core.ui.theme.NextPlayerTheme
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls

/**
 * Clean NextPlayer-styled floating overlay sheet with proper landscape (45% width)
 * and portrait (62% height) dimensions so content never wraps or clips.
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

    val sheetShape = RoundedCornerShape(24.dp)
    val sheetBgColor = if (useSolidMode) {
        Color(0xFF141821).copy(alpha = 0.96f)
    } else {
        Color(0xFF0E131D).copy(alpha = 0.92f)
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
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                    end = if (configuration.isPortrait) 12.dp else (12.dp + endPadding),
                )
                .then(
                    other = if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.62f)
                    } else {
                        Modifier
                            .fillMaxWidth(fraction = 0.46f)
                            .fillMaxHeight()
                    },
                )
                .shadow(
                    elevation = 24.dp,
                    shape = sheetShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = sheetShape,
                ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester = focusRequester)
                        .focusGroup()
                        .padding(top = 10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.24f)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .height(0.8.dp)
                            .background(Color.White.copy(alpha = 0.10f)),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
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
