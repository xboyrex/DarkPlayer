package dev.xboyrex.darkplayer.feature.videopicker.composables.vault

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.core.common.extensions.isTelevision
import dev.xboyrex.darkplayer.core.ui.components.requestFocusUntilLanded
import dev.xboyrex.darkplayer.core.ui.components.thenIf
import dev.xboyrex.darkplayer.core.ui.components.tvFocusRing
import dev.xboyrex.darkplayer.core.ui.glass.pressable
import dev.xboyrex.darkplayer.feature.videopicker.screens.vault.VAULT_PIN_LENGTH

/**
 * Row of dots indicating how many PIN digits have been entered so far, out of [VAULT_PIN_LENGTH].
 * Features fast spring pop-in on each digit and shake animation on error.
 */
@Composable
fun PinDotsIndicator(filledCount: Int, error: Boolean, modifier: Modifier = Modifier) {
    val offsetX = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(key1 = error) {
        if (error) {
            for (target in listOf(
                16f,
                -16f,
                10f,
                -10f,
                0f,
            )) {
                offsetX.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 45),
                )
            }
        }
    }

    Row(
        modifier = modifier.offset(x = offsetX.value.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(times = VAULT_PIN_LENGTH) { index ->
            val filled = index < filledCount
            val dotScale by animateFloatAsState(
                targetValue = if (filled) 1.22f else 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 1500f),
                label = "pinDotScale",
            )
            val dotColor by animateColorAsState(
                targetValue = when {
                    error -> MaterialTheme.colorScheme.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFF2C2C2E)
                },
                animationSpec = tween(durationMillis = 90),
                label = "pinDotColor",
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                    }
                    .size(14.dp)
                    .clip(shape = CircleShape)
                    .background(color = dotColor)
                    .border(
                        width = 1.dp,
                        color = if (filled) Color.Transparent else Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/**
 * Sleek minimal Solid UI numeric keypad (0-9, backspace) with fast 10x snappy press physics.
 */
@Composable
fun PinKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val firstKeyFocusRequester = remember { FocusRequester() }
    var isFirstKeyFocused by remember { mutableStateOf(value = false) }

    if (isTv) {
        LaunchedEffect(key1 = Unit) {
            firstKeyFocusRequester.requestFocusUntilLanded(attempts = 20) { isFirstKeyFocused }
        }
    }

    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEachIndexed { colIndex, digit ->
                    PinKey(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .thenIf(condition = rowIndex == 0 && colIndex == 0) {
                                focusRequester(focusRequester = firstKeyFocusRequester)
                                    .onFocusChanged { isFirstKeyFocused = it.hasFocus }
                            },
                        isTv = isTv,
                        label = digit.toString(),
                        onClick = { onDigit(digit) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.weight(weight = 1f))
            PinKey(
                modifier = Modifier.weight(weight = 1f),
                isTv = isTv,
                label = "0",
                onClick = { onDigit('0') },
            )
            PinKeyIcon(
                modifier = Modifier.weight(weight = 1f),
                isTv = isTv,
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun PinKey(
    modifier: Modifier = Modifier,
    isTv: Boolean = false,
    label: String,
    onClick: () -> Unit,
) {
    val keyShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .aspectRatio(ratio = 1.42f)
            .tvFocusRing(
                isTv = isTv,
                shape = keyShape,
            )
            .clip(keyShape)
            .background(Color(0xFF1C1C1E))
            .border(0.75.dp, Color(0x2BFFFFFF), keyShape)
            .pressable(pressedScale = 0.90f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun PinKeyIcon(modifier: Modifier = Modifier, isTv: Boolean = false, onClick: () -> Unit) {
    val keyShape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .aspectRatio(ratio = 1.42f)
            .tvFocusRing(
                isTv = isTv,
                shape = keyShape,
            )
            .clip(keyShape)
            .background(Color(0xFF141416))
            .border(0.75.dp, Color(0x1FFFFFFF), keyShape)
            .pressable(pressedScale = 0.90f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
        )
    }
}
