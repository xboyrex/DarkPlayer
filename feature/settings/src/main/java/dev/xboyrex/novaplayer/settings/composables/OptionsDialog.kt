package dev.xboyrex.novaplayer.settings.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.xboyrex.novaplayer.core.ui.components.CancelButton
import dev.xboyrex.novaplayer.core.ui.components.NovaDialog
import dev.xboyrex.novaplayer.core.ui.components.tvListFocus

@Composable
fun OptionsDialog(text: String, onDismissClick: () -> Unit, options: LazyListScope.() -> Unit) {
    NovaDialog(
        onDismissRequest = onDismissClick,
        title = { Text(text = text) },
        content = {
            HorizontalDivider()
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .selectableGroup()
                    .tvListFocus(),
                content = options,
            )
            HorizontalDivider()
        },
        dismissButton = { CancelButton(onClick = onDismissClick) },
        confirmButton = {},
    )
}
