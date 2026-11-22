package com.mayank.darkplayer.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.theme.calmPressScale

@Composable
fun RadioTextButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val rowBg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(140),
        label = "radio_row_bg",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calmPressScale(pressedScale = 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = LocalContentColor.current.copy(alpha = 0.72f),
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = if (selected) accent else LocalContentColor.current,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
