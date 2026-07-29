package com.opplayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.ui.AspectRatioFrameLayout
import com.opplayer.app.R
import com.opplayer.app.util.toLatinDigits

val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

const val MIN_SPEED = 0.1f
const val MAX_SPEED = 6f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerSettingsSheet(
    currentSpeed: Float,
    resizeMode: Int,
    autoNextEnabled: Boolean,
    autoRotateEnabled: Boolean,
    gesturesEnabled: Boolean,
    showEpisodeOptions: Boolean,
    onSpeedChange: (Float) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onAutoNextChange: (Boolean) -> Unit,
    onAutoRotateChange: (Boolean) -> Unit,
    onGesturesChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customSpeed by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.speed_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.speed_current, formatSpeed(currentSpeed)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speedPresets.forEach { preset ->
                    FilterChip(
                        selected = isSameSpeed(currentSpeed, preset),
                        onClick = { onSpeedChange(preset) },
                        label = { Text("${formatSpeed(preset)}\u00d7") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = customSpeed,
                    onValueChange = {
                        customSpeed = it
                        customError = false
                    },
                    singleLine = true,
                    isError = customError,
                    label = { Text(stringResource(R.string.speed_custom_label)) },
                    supportingText = {
                        Text(
                            text = if (customError) {
                                stringResource(R.string.speed_custom_invalid)
                            } else {
                                stringResource(R.string.speed_custom_hint)
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val parsed = parseSpeed(customSpeed)
                        if (parsed != null) {
                            onSpeedChange(parsed)
                            customSpeed = ""
                            customError = false
                        } else {
                            customError = true
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.resize_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    AspectRatioFrameLayout.RESIZE_MODE_FIT to R.string.aspect_fit,
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to R.string.aspect_zoom,
                    AspectRatioFrameLayout.RESIZE_MODE_FILL to R.string.aspect_fill
                )

                modes.forEach { (mode, labelRes) ->
                    FilterChip(
                        selected = resizeMode == mode,
                        onClick = { onResizeModeChange(mode) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitchRow(
                title = stringResource(R.string.auto_rotate_label),
                body = stringResource(R.string.auto_rotate_body),
                checked = autoRotateEnabled,
                onCheckedChange = onAutoRotateChange
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitchRow(
                title = stringResource(R.string.gestures_label),
                body = stringResource(R.string.gestures_body),
                checked = gesturesEnabled,
                onCheckedChange = onGesturesChange
            )

            if (showEpisodeOptions) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                SettingSwitchRow(
                    title = stringResource(R.string.auto_next_label),
                    body = stringResource(R.string.auto_next_body),
                    checked = autoNextEnabled,
                    onCheckedChange = onAutoNextChange
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun parseSpeed(input: String): Float? {
    val normalized = input.trim()
        .toLatinDigits()
        .replace('\u066b', '.')
        .replace(',', '.')
        .replace("\u00d7", "")
        .trim()

    val value = normalized.toFloatOrNull() ?: return null
    if (value < MIN_SPEED || value > MAX_SPEED) return null

    return (Math.round(value * 100f) / 100f)
}

fun formatSpeed(speed: Float): String {
    val text = if (speed % 1f == 0f) {
        speed.toInt().toString()
    } else {
        speed.toString().trimEnd('0').trimEnd('.')
    }
    return text.replace('.', '\u066b').toPersianSpeedDigits()
}

private fun String.toPersianSpeedDigits(): String = map { ch ->
    if (ch in '0'..'9') ('\u06f0' + (ch - '0')) else ch
}.joinToString("")

private fun isSameSpeed(a: Float, b: Float): Boolean = kotlin.math.abs(a - b) < 0.001f
