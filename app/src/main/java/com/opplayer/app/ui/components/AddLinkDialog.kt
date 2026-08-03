package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.player.LinkPatternDetector
import com.opplayer.app.ui.localization.LocalizedWindow
import com.opplayer.app.util.isValidMediaUrl

@Composable
fun AddLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, subtitleUrl: String?, pattern: EpisodePattern?) -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) }

    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var showUrlError by remember { mutableStateOf(false) }

    var firstUrl by remember { mutableStateOf("") }
    var secondUrl by remember { mutableStateOf("") }
    var showAdvancedError by remember { mutableStateOf(false) }

    val detection = remember(firstUrl, secondUrl) {
        if (firstUrl.isBlank() || secondUrl.isBlank()) {
            null
        } else {
            LinkPatternDetector.detect(firstUrl, secondUrl)
        }
    }

    val detectedPattern = (detection as? LinkPatternDetector.Result.Detected)?.pattern

    AlertDialog(
        onDismissRequest = onDismiss,

        title = { LocalizedWindow { Text(stringResource(R.string.add_link_title)) } },
        text = {
            LocalizedWindow {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                GlassTabs(
                    titles = listOf(
                        stringResource(R.string.link_mode_simple),
                        stringResource(R.string.link_mode_advanced)
                    ),
                    selectedIndex = selectedMode,
                    onSelect = { selectedMode = it },
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedMode == 0) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            showUrlError = false
                        },
                        label = { Text(stringResource(R.string.field_url)) },
                        singleLine = true,
                        isError = showUrlError,
                        supportingText = if (showUrlError) {
                            { Text(stringResource(R.string.invalid_url)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.advanced_link_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = firstUrl,
                        onValueChange = {
                            firstUrl = it
                            showAdvancedError = false
                        },
                        label = { Text(stringResource(R.string.field_url_current)) },
                        singleLine = true,
                        isError = showAdvancedError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = secondUrl,
                        onValueChange = {
                            secondUrl = it
                            showAdvancedError = false
                        },
                        label = { Text(stringResource(R.string.field_url_next)) },
                        singleLine = true,
                        isError = showAdvancedError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PatternFeedback(detection = detection)
                }

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(stringResource(R.string.field_subtitle)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
        },
        confirmButton = {
            LocalizedWindow {
            TextButton(
                onClick = {
                    val cleanSubtitle = subtitle.trim().ifBlank { null }

                    if (selectedMode == 0) {
                        if (isValidMediaUrl(url)) {
                            onConfirm(title.trim(), url.trim(), cleanSubtitle, null)
                        } else {
                            showUrlError = true
                        }
                    } else {
                        val pattern = detectedPattern
                        if (pattern != null && isValidMediaUrl(pattern.url)) {
                            onConfirm(title.trim(), pattern.url, cleanSubtitle, pattern)
                        } else {
                            showAdvancedError = true
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.save_and_play))
            }
            }
        },
        dismissButton = {
            LocalizedWindow {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun PatternFeedback(detection: LinkPatternDetector.Result?) {
    if (detection == null) return

    when (detection) {
        is LinkPatternDetector.Result.Detected -> {
            val pattern = detection.pattern
            val nextUrl = pattern.next()?.url.orEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.pattern_detected,
                        pattern.episode.toString(),
                        pattern.step.toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (nextUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.pattern_preview, nextUrl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        is LinkPatternDetector.Result.Rejected -> {
            val messageRes = when (detection.failure) {
                LinkPatternDetector.Failure.EMPTY -> R.string.pattern_error_empty
                LinkPatternDetector.Failure.IDENTICAL -> R.string.pattern_error_identical
                LinkPatternDetector.Failure.NOT_NUMERIC -> R.string.pattern_error_numeric
                LinkPatternDetector.Failure.NOT_INCREASING -> R.string.pattern_error_increasing
                LinkPatternDetector.Failure.TOO_LONG -> R.string.pattern_error_too_long
            }

            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
