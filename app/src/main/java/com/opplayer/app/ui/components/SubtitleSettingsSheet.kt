package com.opplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.data.SubtitleStyleSettings
import com.opplayer.app.ui.localization.LocalizedWindow
import com.opplayer.app.ui.localization.usePersianDigits
import com.opplayer.app.util.formatCount
import com.opplayer.app.util.localizeDigits
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

/** One selectable subtitle source: off, an external file, or an embedded track. */
data class SubtitleOption(
    val id: String,
    val label: String,
    val selected: Boolean
)

/** Playback related controls, only available while a video is open. */
data class SubtitlePlaybackControls(
    val offsetMs: Long,
    val onOffsetChange: (Long) -> Unit,
    val options: List<SubtitleOption>,
    val onOptionSelected: (SubtitleOption) -> Unit,
    val onPickFile: () -> Unit,
    val embeddedOnly: Boolean = false
)

/** Offsets offered as one tap shortcuts, in milliseconds. */
private val offsetSteps = listOf(-500L, -100L, 100L, 500L)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    settings: SubtitleStyleSettings,
    onSettingsChange: (SubtitleStyleSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    playback: SubtitlePlaybackControls? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val persianDigits = usePersianDigits()
    var showHelp by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // A sheet is its own window, so the language chosen in the settings has
        // to be restored here or its strings follow the device locale.
        LocalizedWindow {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.subtitle_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                HelpIconButton(onClick = { showHelp = true })
            }

            SubtitleSwitchRow(
                label = stringResource(R.string.subtitle_enabled),
                checked = settings.enabled,
                onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) }
            )

            SubtitlePreview(settings)

            if (playback != null) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.subtitle_sync),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.subtitle_sync_value,
                        formatOffset(playback.offsetMs, persianDigits)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // The step labels used to be hardcoded Persian literals, so
                    // they stayed Persian inside the English interface.
                    offsetSteps.forEach { step ->
                        OutlinedButton(
                            onClick = { playback.onOffsetChange(playback.offsetMs + step) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = formatOffset(step, persianDigits))
                        }
                    }
                }
                TextButton(onClick = { playback.onOffsetChange(0L) }) {
                    Text(text = stringResource(R.string.subtitle_sync_reset))
                }
                Text(
                    text = stringResource(R.string.subtitle_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (playback.embeddedOnly) {
                    Text(
                        text = stringResource(R.string.subtitle_delay_only_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()
                Text(
                    text = stringResource(R.string.subtitle_track),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (playback.options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.subtitle_none_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.selectableGroup()) {
                    playback.options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option.selected,
                                    role = Role.RadioButton,
                                    onClick = { playback.onOptionSelected(option) }
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (option.selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(24.dp))
                            }
                            Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedButton(
                    onClick = playback.onPickFile,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(text = stringResource(R.string.subtitle_pick_file)) }
            }

            HorizontalDivider()
            Text(
                text = stringResource(R.string.subtitle_text_color),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ColorRow(
                colors = SubtitleStyleSettings.TEXT_COLORS,
                selected = settings.textColorArgb,
                onSelect = { onSettingsChange(settings.copy(textColorArgb = it)) },
                nameOf = { textColorName(it) }
            )

            Text(
                text = stringResource(R.string.subtitle_background),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ColorRow(
                colors = SubtitleStyleSettings.BACKGROUND_COLORS,
                selected = settings.backgroundArgb,
                onSelect = { onSettingsChange(settings.copy(backgroundArgb = it)) },
                nameOf = { backgroundColorName(it) }
            )

            Text(
                text = stringResource(
                    R.string.subtitle_size_value,
                    formatCount(settings.textSizeSp.roundToInt(), persianDigits)
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.textSizeSp,
                onValueChange = { onSettingsChange(settings.copy(textSizeSp = it)) },
                valueRange = SubtitleStyleSettings.MIN_TEXT_SIZE_SP..SubtitleStyleSettings.MAX_TEXT_SIZE_SP
            )

            Text(
                text = stringResource(
                    R.string.subtitle_position_value,
                    formatCount(settings.bottomMarginDp.roundToInt(), persianDigits)
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = settings.bottomMarginDp,
                onValueChange = { onSettingsChange(settings.copy(bottomMarginDp = it)) },
                valueRange = SubtitleStyleSettings.MIN_BOTTOM_MARGIN_DP..SubtitleStyleSettings.MAX_BOTTOM_MARGIN_DP
            )

            SubtitleSwitchRow(
                label = stringResource(R.string.subtitle_bold),
                checked = settings.bold,
                onCheckedChange = { onSettingsChange(settings.copy(bold = it)) }
            )
            SubtitleSwitchRow(
                label = stringResource(R.string.subtitle_outline),
                checked = settings.outline,
                onCheckedChange = { onSettingsChange(settings.copy(outline = it)) }
            )

            TextButton(onClick = onReset) {
                Text(text = stringResource(R.string.subtitle_style_reset))
            }
        }
        }
    }

    if (showHelp) {
        HelpSheet(
            title = stringResource(R.string.help_subtitle_title),
            entries = subtitleHelpEntries(),
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun SubtitleSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SubtitlePreview(settings: SubtitleStyleSettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xFF101014), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        SubtitleOverlay(
            text = stringResource(R.string.subtitle_preview_text),
            settings = settings.copy(enabled = true, bottomMarginDp = 0f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    nameOf: @Composable (Long) -> String
) {
    val selectedLabel = stringResource(R.string.color_selected)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { value ->
            val isSelected = value == selected
            // A bare colour circle announced nothing but "button"; the swatch
            // name and its state are now exposed to screen readers.
            val name = nameOf(value)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(value.toInt()), CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    )
                    .semantics {
                        contentDescription = name
                        if (isSelected) {
                            stateDescription = selectedLabel
                        }
                    }
            )
        }
    }
}

/**
 * Formats a subtitle offset in seconds with one decimal, using the digits and
 * the decimal separator of the active interface language.
 */
private fun formatOffset(offsetMs: Long, persianDigits: Boolean): String {
    val seconds = offsetMs / 1000.0
    val sign = when {
        offsetMs > 0L -> "+"
        offsetMs < 0L -> "\u2212"
        else -> ""
    }
    val absolute = String.format(Locale.US, "%.1f", abs(seconds))
    return (sign + absolute).localizeDigits(persianDigits)
}
