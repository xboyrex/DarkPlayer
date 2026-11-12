package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.xboyrex.darkplayer.core.model.NovaSwitchStyle
import dev.xboyrex.darkplayer.core.ui.glass.LocalNovaSwitchStyle
import dev.xboyrex.darkplayer.core.ui.designsystem.NovaFluidSlider

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceSlider(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isFirstItem: Boolean = false,
    isLastItem: Boolean = false,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trailingContent: @Composable (() -> Unit)? = null,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null,
) {
    val stepSize = (valueRange.endInclusive - valueRange.start) / 20f
    val switchStyle = LocalNovaSwitchStyle.current

    NextSegmentedListItem(
        modifier = modifier.onPreviewKeyEvent { event ->
            if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> {
                    onValueChange((value - stepSize).coerceIn(valueRange))
                    true
                }

                Key.DirectionRight -> {
                    onValueChange((value + stepSize).coerceIn(valueRange))
                    true
                }

                Key.Enter, Key.DirectionCenter -> {
                    if (onReset != null) {
                        onReset()
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        },
        enabled = enabled,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (description != null) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (trailingContent != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingContent()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (switchStyle == NovaSwitchStyle.GLASMIO_LIQUID && steps == 0) {
                    NovaFluidSlider(
                        value = value.coerceIn(valueRange),
                        onValueChange = onValueChange,
                        valueRange = valueRange,
                        enabled = enabled,
                        onValueChangeFinished = { onValueChangeFinished?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                } else {
                    Slider(
                        value = value.coerceIn(valueRange),
                        valueRange = valueRange,
                        steps = steps,
                        enabled = enabled,
                        onValueChange = onValueChange,
                        onValueChangeFinished = { onValueChangeFinished?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        },
    )
}
