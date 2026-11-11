package dev.anilbeesetti.nextplayer.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.model.NovaSwitchStyle
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaAccentColor
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaHapticsEnabled
import dev.anilbeesetti.nextplayer.core.ui.glass.LocalNovaSwitchStyle
import dev.anilbeesetti.nextplayer.core.ui.glass.novaHaptic
import io.github.neoruaa.glasmio.ui.GlasmioSwitch

/**
 * Universal toggle switch for Nova Player supporting:
 * 1. `GLASMIO_LIQUID` — Authentic Glasmio iOS 27 `GlasmioSwitch` (63x28.dp track + 37x24.dp elastic liquid lens thumb)
 * 2. `MX_PRO_CLASSIC` — Sleek MX Player Pro cyan/blue pill switch
 * 3. `MATERIAL_EXPRESSIVE` — Material 3 Expressive switch
 * Fixes Bug #3, #4, #5, #6, #7, #8, #9, #15 where broken/ugly custom switches were used.
 */
@Composable
fun NextSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedIcon: ImageVector = NextIcons.Check,
) {
    val switchStyle = LocalNovaSwitchStyle.current
    val view = LocalView.current
    val hapticsEnabled = LocalNovaHapticsEnabled.current
    val accentColor = LocalNovaAccentColor.current

    when (switchStyle) {
        NovaSwitchStyle.GLASMIO_LIQUID -> {
            GlasmioSwitch(
                checked = checked,
                onCheckedChange = { newValue ->
                    view.novaHaptic(14L, 140, hapticsEnabled)
                    onCheckedChange?.invoke(newValue)
                },
                modifier = modifier,
                enabled = enabled,
                onTintColor = Color(0xFF30D158),
            )
        }

        NovaSwitchStyle.MX_PRO_CLASSIC -> {
            val fraction by animateFloatAsState(
                targetValue = if (checked) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                label = "mxSwitch",
            )
            val trackColor = if (checked) {
                accentColor.copy(alpha = 0.45f)
            } else {
                Color(0xFF494D58).copy(alpha = 0.55f)
            }
            val thumbColor = if (checked) accentColor else Color(0xFFB8BDC8)
            Box(
                modifier = modifier
                    .size(width = 46.dp, height = 24.dp)
                    .then(
                        if (onCheckedChange != null && enabled) {
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Switch,
                            ) {
                                view.novaHaptic(12L, 120, hapticsEnabled)
                                onCheckedChange(!checked)
                            }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 16.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(trackColor),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .graphicsLayer {
                            translationX = fraction * 22.dp.toPx()
                        }
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(thumbColor)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                )
            }
        }

        NovaSwitchStyle.MATERIAL_EXPRESSIVE -> {
            val thumbContent: (@Composable () -> Unit)? = if (checked) {
                {
                    Icon(
                        imageVector = checkedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                null
            }

            Switch(
                checked = checked,
                onCheckedChange = if (onCheckedChange != null) {
                    {
                        view.novaHaptic(12L, 120, hapticsEnabled)
                        onCheckedChange(it)
                    }
                } else {
                    null
                },
                modifier = modifier,
                enabled = enabled,
                thumbContent = thumbContent,
            )
        }
    }
}
