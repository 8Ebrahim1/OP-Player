package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.player.PlayerGestureKind
import kotlin.math.roundToInt

@Composable
fun PlayerGestureHud(
    kind: PlayerGestureKind,
    value: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when (kind) {
                PlayerGestureKind.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
                PlayerGestureKind.BRIGHTNESS -> Icons.Filled.BrightnessHigh
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (kind) {
                PlayerGestureKind.VOLUME -> stringResource(R.string.gesture_volume)
                PlayerGestureKind.BRIGHTNESS -> stringResource(R.string.gesture_brightness)
            },
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .width(150.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.25f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
