package dev.xboyrex.darkplayer.core.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.xboyrex.darkplayer.core.ui.glass.GlasmioTheme
import dev.xboyrex.darkplayer.core.ui.glass.RegisterPopupBlur

@Composable
fun NextDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    dialogProperties: DialogProperties = NextDialogDefaults.dialogProperties,
) {
    val configuration = LocalConfiguration.current
    val sheetShape = RoundedCornerShape(34.dp)
    RegisterPopupBlur(active = true)

    AlertDialog(
        title = title,
        shape = sheetShape,
        containerColor = GlasmioTheme.colors.elevatedBackground.copy(alpha = 0.96f),
        titleContentColor = GlasmioTheme.colors.content,
        textContentColor = GlasmioTheme.colors.secondaryContent,
        text = {
            val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
            SideEffect {
                dialogWindowProvider?.window?.let { window ->
                    window.setDimAmount(0.58f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 48
                        }
                    }
                }
            }
            Column { content() }
        },
        modifier = modifier
            .widthIn(max = configuration.screenWidthDp.dp - NextDialogDefaults.dialogMargin * 2)
            .border(
                width = 0.8.dp,
                color = GlasmioTheme.colors.subtleStroke,
                shape = sheetShape,
            ),
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = dialogProperties,
    )
}

@Composable
fun NextDialogWithDoneAndCancelButtons(
    title: String,
    onDoneClick: () -> Unit,
    onDismissClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    NextDialog(
        title = { Text(text = title) },
        confirmButton = { DoneButton(onClick = onDoneClick) },
        dismissButton = { CancelButton(onClick = onDismissClick) },
        onDismissRequest = onDismissClick,
        content = content,
    )
}

object NextDialogDefaults {
    val dialogProperties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = true,
    )
    val dialogMargin: Dp = 16.dp
}
