package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opplayer.app.data.SubtitleStyleSettings

@Composable
fun SubtitleOverlay(
    text: String?,
    settings: SubtitleStyleSettings,
    modifier: Modifier = Modifier
) {
    val value = text?.trim()
    if (!settings.enabled || value.isNullOrEmpty()) return

    Box(
        modifier = modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = settings.bottomMarginDp.dp
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = Color(settings.textColorArgb.toInt()),
                fontSize = settings.textSizeSp.sp,
                fontWeight = if (settings.bold) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                shadow = if (settings.outline) {
                    Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 8f)
                } else {
                    null
                }
            ),
            modifier = Modifier
                .background(
                    color = Color(settings.backgroundArgb.toInt()),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
