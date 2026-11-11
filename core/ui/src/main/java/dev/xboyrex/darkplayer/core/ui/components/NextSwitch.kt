package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.xboyrex.darkplayer.core.ui.designsystem.NextIcons
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioSwitch

@Composable
fun NextSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") checkedIcon: ImageVector = NextIcons.Check,
) {
    GlasmioSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}
