package com.opplayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.data.AppLanguage
import com.opplayer.app.data.AppLayoutDirection
import com.opplayer.app.data.AppSettings
import com.opplayer.app.ui.localization.LocalizedWindow

/**
 * Application wide settings: interface language, layout direction and a way to
 * replay the introduction tour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    settings: AppSettings,
    onLanguageChange: (AppLanguage) -> Unit,
    onLayoutDirectionChange: (AppLayoutDirection) -> Unit,
    onReplayTour: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHelp by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        // A sheet is its own window, so the language chosen in the settings has
        // to be restored here or its strings follow the device locale.
        LocalizedWindow {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                HelpIconButton(onClick = { showHelp = true })
            }

            SettingSection(
                title = stringResource(R.string.language_label),
                body = stringResource(R.string.language_body)
            ) {
                OptionRow(
                    options = listOf(
                        AppLanguage.SYSTEM to stringResource(R.string.language_system),
                        AppLanguage.PERSIAN to stringResource(R.string.language_persian),
                        AppLanguage.ENGLISH to stringResource(R.string.language_english)
                    ),
                    selected = settings.language,
                    onSelect = onLanguageChange
                )
            }

            HorizontalDivider()

            SettingSection(
                title = stringResource(R.string.direction_label),
                body = stringResource(R.string.direction_body)
            ) {
                OptionRow(
                    options = listOf(
                        AppLayoutDirection.AUTO to stringResource(R.string.direction_auto),
                        AppLayoutDirection.RTL to stringResource(R.string.direction_rtl),
                        AppLayoutDirection.LTR to stringResource(R.string.direction_ltr)
                    ),
                    selected = settings.layoutDirection,
                    onSelect = onLayoutDirectionChange
                )
            }

            HorizontalDivider()

            SettingSection(
                title = stringResource(R.string.replay_tour),
                body = stringResource(R.string.replay_tour_body)
            ) {
                OutlinedButton(
                    onClick = onReplayTour,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = stringResource(R.string.replay_tour))
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(R.string.close))
            }
        }
        }
    }

    if (showHelp) {
        HelpSheet(
            title = stringResource(R.string.help_settings_title),
            entries = settingsHelpEntries(),
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun SettingSection(
    title: String,
    body: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

        content()
    }
}

/**
 * Vertical list of mutually exclusive options.
 *
 * The whole row is the touch target and the group is exposed as a radio group,
 * so TalkBack announces "selected, 2 of 3" instead of three unrelated buttons.
 */
@Composable
private fun <T> OptionRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            val isSelected = value == selected

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
