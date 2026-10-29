package dev.xboyrex.darkplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    contentColor: Color = Color.White.copy(alpha = 0.90f),
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = contentColor,
        modifier = modifier
            .clip(shape = shape)
            .background(color = backgroundColor)
            .border(
                width = 0.6.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .padding(
                vertical = 2.dp,
                horizontal = 6.dp,
            ),
    )
}
