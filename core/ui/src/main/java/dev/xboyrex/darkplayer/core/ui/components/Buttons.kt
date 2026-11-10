package dev.xboyrex.darkplayer.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.xboyrex.darkplayer.core.ui.R

@Composable
fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilledTonalButton(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier.tvFocusRing(shape = RoundedCornerShape(percent = 50)),
    ) {
        Text(
            text = stringResource(R.string.done),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun CancelButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.tvFocusRing(shape = RoundedCornerShape(percent = 50)),
    ) {
        Text(
            text = stringResource(R.string.cancel),
            fontWeight = FontWeight.Medium,
        )
    }
}
