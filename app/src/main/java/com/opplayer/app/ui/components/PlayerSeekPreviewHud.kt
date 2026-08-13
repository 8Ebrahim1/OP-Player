package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opplayer.app.ui.localization.usePersianDigits
import com.opplayer.app.util.formatDuration
import kotlin.math.abs

/** Shown while the user drags horizontally, before the seek is committed on release. */
@Composable
fun PlayerSeekPreviewHud(
    positionMs: Long,
    deltaMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val persianDigits = usePersianDigits()
    val forward = deltaMs >= 0L

    val position = formatDuration(positionMs, persianDigits)
    val total = if (durationMs > 0L) formatDuration(durationMs, persianDigits) else null
    val delta = (if (forward) "+" else "-") + formatDuration(abs(deltaMs), persianDigits)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (forward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = listOfNotNull(position, total).joinToString(" / "),
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = delta,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
