package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opplayer.app.ui.theme.OpBackground
import com.opplayer.app.ui.theme.OpGlass

@Composable
fun GlassBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpBackground)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3F3184).copy(alpha = 0.45f),
                        Color(0xFF090B13),
                        Color(0xFF147E9D).copy(alpha = 0.35f)
                    )
                )
            )
    )
}

fun Modifier.glass(radius: Dp = 18.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(OpGlass.copy(alpha = 0.72f))
    .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(radius)
    )
