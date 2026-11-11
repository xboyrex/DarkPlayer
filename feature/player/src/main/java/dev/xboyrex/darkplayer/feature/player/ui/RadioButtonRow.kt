package dev.xboyrex.darkplayer.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RadioButtonRow(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val mxBlue = Color(0xFF38BDF8)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(
                horizontal = 8.dp,
                vertical = 11.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = mxBlue,
                unselectedColor = Color.White.copy(alpha = 0.45f),
            ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.86f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
