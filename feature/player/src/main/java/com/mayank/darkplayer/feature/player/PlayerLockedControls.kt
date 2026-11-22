package com.mayank.darkplayer.feature.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mayank.darkplayer.core.ui.R
import com.mayank.darkplayer.feature.player.buttons.PlayerButton

@Composable
fun PlayerLockedControls(
    onUnlockControls: () -> Unit,
    modifier: Modifier = Modifier,
    unlockButtonModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(start = 16.dp, top = 24.dp),
    ) {
        PlayerButton(
            modifier = unlockButtonModifier.border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = CircleShape,
            ),
            containerColor = MaterialTheme.colorScheme.primary,
            onClick = onUnlockControls,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = stringResource(R.string.controls_unlock),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
