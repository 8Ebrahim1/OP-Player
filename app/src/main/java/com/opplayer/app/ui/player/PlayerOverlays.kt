package com.opplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opplayer.app.R

/** Label shown at the edge of the screen after a double tap seek. */
@Composable
fun PlayerSeekHint(forward: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = if (forward) {
            stringResource(R.string.seek_forward)
        } else {
            stringResource(R.string.seek_backward)
        },
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(horizontal = 36.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

/** Spinner shown while the player is buffering. */
@Composable
fun PlayerBufferingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = Color.White
    )
}
