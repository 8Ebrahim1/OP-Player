package com.opplayer.app.ui.components

import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.util.findActivity
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val SWIPE_TRAVEL_RATIO = 0.7f
private const val MIN_BRIGHTNESS = 0.01f
private const val SYSTEM_BRIGHTNESS_DEFAULT = -1f
private const val INDICATOR_TIMEOUT_MS = 900L

private enum class GestureKind { VOLUME, BRIGHTNESS }

private data class GestureIndicator(val kind: GestureKind, val value: Float)

@Composable
fun PlayerGestureOverlay(
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTap: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }

    var indicator by remember { mutableStateOf<GestureIndicator?>(null) }

    fun volumeFraction(): Float {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        return (current.toFloat() / maxVolume).coerceIn(0f, 1f)
    }

    fun applyVolume(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * maxVolume).roundToInt()
        runCatching {
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun brightnessFraction(): Float {
        val current = activity?.window?.attributes?.screenBrightness ?: SYSTEM_BRIGHTNESS_DEFAULT
        if (current >= 0f) return current.coerceIn(MIN_BRIGHTNESS, 1f)

        val system = runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
        }.getOrDefault(0.5f)

        return system.coerceIn(MIN_BRIGHTNESS, 1f)
    }

    fun applyBrightness(fraction: Float) {
        val window = activity?.window ?: return
        val attributes = window.attributes
        attributes.screenBrightness = fraction.coerceIn(MIN_BRIGHTNESS, 1f)
        window.attributes = attributes
    }

    DisposableEffect(activity) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            val attributes = window.attributes
            attributes.screenBrightness = SYSTEM_BRIGHTNESS_DEFAULT
            window.attributes = attributes
        }
    }

    LaunchedEffect(indicator) {
        if (indicator != null) {
            delay(INDICATOR_TIMEOUT_MS)
            indicator = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                val width = size.width.toFloat()

                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset -> onDoubleTap(offset.x > width / 2f) }
                )
            }
            .pointerInput(enabled, maxVolume) {
                if (!enabled) return@pointerInput

                val width = size.width.toFloat()
                val travel = (size.height.toFloat() * SWIPE_TRAVEL_RATIO).coerceAtLeast(1f)

                var kind = GestureKind.VOLUME
                var startValue = 0f
                var accumulated = 0f

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        kind = if (offset.x > width / 2f) {
                            GestureKind.VOLUME
                        } else {
                            GestureKind.BRIGHTNESS
                        }
                        accumulated = 0f
                        startValue = when (kind) {
                            GestureKind.VOLUME -> volumeFraction()
                            GestureKind.BRIGHTNESS -> brightnessFraction()
                        }
                        indicator = GestureIndicator(kind, startValue)
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulated -= dragAmount

                        val value = (startValue + accumulated / travel).coerceIn(0f, 1f)
                        when (kind) {
                            GestureKind.VOLUME -> applyVolume(value)
                            GestureKind.BRIGHTNESS -> applyBrightness(value)
                        }
                        indicator = GestureIndicator(kind, value)
                    }
                )
            }
    ) {
        val current = indicator
        if (current != null) {
            GestureHud(
                kind = current.kind,
                value = current.value,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun GestureHud(
    kind: GestureKind,
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
                GestureKind.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
                GestureKind.BRIGHTNESS -> Icons.Filled.BrightnessHigh
            },
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (kind) {
                GestureKind.VOLUME -> androidx.compose.ui.res.stringResource(R.string.gesture_volume)
                GestureKind.BRIGHTNESS ->
                    androidx.compose.ui.res.stringResource(R.string.gesture_brightness)
            },
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .width(150.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.25f)
        )
    }
}
