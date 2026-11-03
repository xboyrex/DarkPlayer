package dev.xboyrex.darkplayer.feature.player.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.media3.common.PlaybackException
import dev.xboyrex.darkplayer.core.ui.R as coreUiR
import dev.xboyrex.darkplayer.feature.player.model.DecoderRecoveryState
import dev.xboyrex.darkplayer.feature.player.model.DecoderRecoveryStatus
import dev.xboyrex.darkplayer.feature.player.model.DecoderTrackType
import dev.xboyrex.darkplayer.feature.player.model.labelRes

@Composable
private fun ApplyPlayerDialogBlur() {
    val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
    SideEffect {
        dialogWindowProvider?.window?.let { window ->
            window.setDimAmount(0.55f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 46
                }
            }
        }
    }
}

@Composable
internal fun PlayerErrorDialogs(
    decoderRecoveryState: DecoderRecoveryState,
    playbackError: PlaybackException?,
    hasNextMediaItem: Boolean,
    onTryDecoderFallback: () -> Unit,
    onPlayNextVideo: () -> Unit,
    onExit: () -> Unit,
) {
    val unsupportedMode = decoderRecoveryState.unsupportedMode
    val recoveryTrackType = decoderRecoveryState.trackType
    if (
        decoderRecoveryState.status == DecoderRecoveryStatus.AWAITING_CONFIRMATION &&
        unsupportedMode != null &&
        recoveryTrackType != null
    ) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF212633),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.90f),
            shape = RoundedCornerShape(26.dp),
            title = { Text(text = stringResource(coreUiR.string.decoder)) },
            text = {
                ApplyPlayerDialogBlur()
                Text(
                    text = stringResource(
                        id = when (recoveryTrackType) {
                            DecoderTrackType.VIDEO ->
                                coreUiR.string.video_decoder_mode_not_supported

                            DecoderTrackType.AUDIO ->
                                coreUiR.string.audio_decoder_mode_not_supported
                        },
                        stringResource(unsupportedMode.labelRes),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onTryDecoderFallback) {
                    Text(text = stringResource(coreUiR.string.okay), color = Color(0xFFFACC15))
                }
            },
        )
    }

    val allDecoderModesFailed = decoderRecoveryState.status == DecoderRecoveryStatus.FAILED
    val showPlayerError = allDecoderModesFailed ||
        (
            decoderRecoveryState.status == DecoderRecoveryStatus.NONE &&
                playbackError != null
            )
    if (showPlayerError) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF212633),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.90f),
            shape = RoundedCornerShape(26.dp),
            title = { Text(text = stringResource(coreUiR.string.error_playing_video)) },
            text = {
                ApplyPlayerDialogBlur()
                Text(
                    text = playbackError?.message
                        ?: stringResource(
                            id = if (allDecoderModesFailed) {
                                coreUiR.string.no_supported_decoder
                            } else {
                                coreUiR.string.unknown_error
                            },
                        ),
                )
            },
            confirmButton = {
                if (hasNextMediaItem) {
                    TextButton(onClick = onPlayNextVideo) {
                        Text(text = stringResource(coreUiR.string.play_next_video), color = Color(0xFFFACC15))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onExit) {
                    Text(text = stringResource(coreUiR.string.exit), color = Color.White.copy(alpha = 0.85f))
                }
            },
        )
    }
}
