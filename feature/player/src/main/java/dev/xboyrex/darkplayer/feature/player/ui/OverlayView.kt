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
import androidx.compose.foundation.layout.width
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
    val endPadding = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateEndPadding(layoutDirection = layoutDirection)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key1 = show) {
        if (show && isTv) {
            focusRequester.requestFocusUntilLanded(attempts = 5)
        }
    }

    val sheetShape = if (configuration.isPortrait) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
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
            slideInVertically(tween(240)) { it } + fadeIn(tween(180))
        } else {
            slideInHorizontally(tween(240)) { it } + fadeIn(tween(180))
        },
        exit = if (configuration.isPortrait) {
            slideOutVertically(tween(200)) { it } + fadeOut(tween(150))
        } else {
            slideOutHorizontally(tween(200)) { it } + fadeOut(tween(150))
        },
    ) {
        Surface(
            shape = sheetShape,
            color = Color(0xFF080A10).copy(alpha = 0.84f),
            contentColor = Color.White,
            modifier = modifier.then(
                other = if (configuration.isPortrait) {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = 0.58f)
                } else {
                    Modifier
                        .width(350.dp)
                        .fillMaxHeight()
                },
            ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    modifier = Modifier
                        .focusRequester(focusRequester = focusRequester)
                        .focusGroup()
                        .padding(top = 20.dp)
                        .padding(end = endPadding),
                ) {
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                title = "Audio Track",
                show = true,
            ) {
                Text(text = "Sample content")
            }
        }
    }
}
