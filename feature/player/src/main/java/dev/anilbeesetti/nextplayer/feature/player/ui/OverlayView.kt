package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.ui.components.requestFocusUntilLanded
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme

/**
 * MX Player Pro / Liquid Glass Translucent Right-Side Panel Container
 * Matches Screenshot_20261110-155643, 155646, 155719, 155720, 155721_MX Player Pro.png (#21, #22, #24, #25, #26).
 */
@Composable
fun BoxScope.OverlayView(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    widthFraction: Float = 0.42f,
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

    val panelShape = if (configuration.isPortrait) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
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
        enter = fadeIn() + if (configuration.isPortrait) {
            slideInVertically { it }
        } else {
            slideInHorizontally { it }
        },
        exit = fadeOut() + if (configuration.isPortrait) {
            slideOutVertically { it }
        } else {
            slideOutHorizontally { it }
        },
    ) {
        Surface(
            shape = panelShape,
            color = Color(0xFF10141E).copy(alpha = 0.88f),
            contentColor = Color.White,
            modifier = modifier
                .then(
                    other = if (configuration.isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = 0.56f)
                    } else {
                        Modifier
                            .fillMaxWidth(fraction = widthFraction)
                            .fillMaxHeight()
                    },
                )
                .border(
                    width = 0.8.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = panelShape,
                ),
        ) {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester = focusRequester)
                    .focusGroup()
                    .padding(top = 20.dp)
                    .padding(end = endPadding),
            ) {
                if (title.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 22.dp),
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                }
                content()
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
                Text(text = "Surround - Hindi")
            }
        }
    }
}
