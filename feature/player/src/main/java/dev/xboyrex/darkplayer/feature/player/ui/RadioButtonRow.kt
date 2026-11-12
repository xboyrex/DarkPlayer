package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xboyrex.darkplayer.feature.player.LocalUseMaterialYouControls

@Composable
fun RadioButtonRow(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val useSolidMode = LocalUseMaterialYouControls.current
    val rowShape = RoundedCornerShape(10.dp)
    val rowBg = when {
        selected && useSolidMode -> Color.White.copy(alpha = 0.14f)
        selected -> Color(0xFFFACC15).copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    val activeColor = if (useSolidMode) Color.White else Color(0xFFFACC15)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp)
            .clip(shape = rowShape)
            .background(rowBg)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.scale(0.88f),
            colors = RadioButtonDefaults.colors(
                selectedColor = activeColor,
                unselectedColor = Color.White.copy(alpha = 0.48f),
            ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.85f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
