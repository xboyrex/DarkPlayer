package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.xboyrex.darkplayer.core.ui.R
import dev.xboyrex.darkplayer.core.ui.glass.GlassEmphasis
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioButton
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioButtonSize

@Composable
fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    GlasmioButton(
        enabled = enabled,
        onClick = onClick,
        emphasis = GlassEmphasis.Prominent,
        size = GlasmioButtonSize.Medium,
        modifier = modifier.tvFocusRing(shape = RoundedCornerShape(percent = 50)),
    ) {
        Text(text = stringResource(R.string.done))
    }
}

@Composable
fun CancelButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    GlasmioButton(
        enabled = enabled,
        onClick = onClick,
        emphasis = GlassEmphasis.Regular,
        size = GlasmioButtonSize.Medium,
        modifier = modifier.tvFocusRing(shape = RoundedCornerShape(percent = 50)),
    ) {
        Text(text = stringResource(R.string.cancel))
    }
}
