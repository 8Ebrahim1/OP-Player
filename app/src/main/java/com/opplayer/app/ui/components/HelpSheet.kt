package com.opplayer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.ui.localization.LocalizedWindow

data class HelpEntry(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(
    title: String,
    entries: List<HelpEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {

        LocalizedWindow {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                entries.forEach { entry -> HelpRow(entry) }
            }
        }
    }
}

@Composable
private fun HelpRow(entry: HelpEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,

            modifier = Modifier
                .size(24.dp)
                .clearAndSetSemantics { }
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun libraryHelpEntries(): List<HelpEntry> = listOf(
    HelpEntry(
        icon = Icons.Default.Add,
        title = stringResource(R.string.help_library_add_title),
        body = stringResource(R.string.help_library_add_body)
    ),
    HelpEntry(
        icon = Icons.Default.Link,
        title = stringResource(R.string.help_library_tabs_title),
        body = stringResource(R.string.help_library_tabs_body)
    ),
    HelpEntry(
        icon = Icons.Default.PlayArrow,
        title = stringResource(R.string.help_library_play_title),
        body = stringResource(R.string.help_library_play_body)
    ),
    HelpEntry(
        icon = Icons.Default.Favorite,
        title = stringResource(R.string.help_library_favorite_title),
        body = stringResource(R.string.help_library_favorite_body)
    ),
    HelpEntry(
        icon = Icons.Default.History,
        title = stringResource(R.string.help_library_reset_title),
        body = stringResource(R.string.help_library_reset_body)
    ),
    HelpEntry(
        icon = Icons.Default.Delete,
        title = stringResource(R.string.help_library_delete_title),
        body = stringResource(R.string.help_library_delete_body)
    ),
    HelpEntry(
        icon = Icons.Default.Tune,
        title = stringResource(R.string.help_library_advanced_title),
        body = stringResource(R.string.help_library_advanced_body)
    )
)

@Composable
fun deviceHelpEntries(): List<HelpEntry> = listOf(
    HelpEntry(
        icon = Icons.Default.Lock,
        title = stringResource(R.string.help_device_permission_title),
        body = stringResource(R.string.help_device_permission_body)
    ),
    HelpEntry(
        icon = Icons.Default.Folder,
        title = stringResource(R.string.help_device_folders_title),
        body = stringResource(R.string.help_device_folders_body)
    ),
    HelpEntry(
        icon = Icons.Default.Search,
        title = stringResource(R.string.help_device_search_title),
        body = stringResource(R.string.help_device_search_body)
    ),
    HelpEntry(
        icon = Icons.Default.Refresh,
        title = stringResource(R.string.help_device_refresh_title),
        body = stringResource(R.string.help_device_refresh_body)
    ),
    HelpEntry(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        title = stringResource(R.string.help_device_back_title),
        body = stringResource(R.string.help_device_back_body)
    ),
    HelpEntry(
        icon = Icons.Default.History,
        title = stringResource(R.string.help_device_resume_title),
        body = stringResource(R.string.help_device_resume_body)
    )
)

@Composable
fun playerHelpEntries(): List<HelpEntry> = listOf(
    HelpEntry(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        title = stringResource(R.string.help_player_back_title),
        body = stringResource(R.string.help_player_back_body)
    ),
    HelpEntry(
        icon = Icons.Default.SkipNext,
        title = stringResource(R.string.help_player_next_title),
        body = stringResource(R.string.help_player_next_body)
    ),
    HelpEntry(
        icon = Icons.Default.AspectRatio,
        title = stringResource(R.string.help_player_scale_title),
        body = stringResource(R.string.help_player_scale_body)
    ),
    HelpEntry(
        icon = Icons.Default.PictureInPictureAlt,
        title = stringResource(R.string.help_player_pip_title),
        body = stringResource(R.string.help_player_pip_body)
    ),
    HelpEntry(
        icon = Icons.Default.Fullscreen,
        title = stringResource(R.string.help_player_fullscreen_title),
        body = stringResource(R.string.help_player_fullscreen_body)
    ),
    HelpEntry(
        icon = Icons.Default.Subtitles,
        title = stringResource(R.string.help_player_subtitles_title),
        body = stringResource(R.string.help_player_subtitles_body)
    ),
    HelpEntry(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.help_player_settings_title),
        body = stringResource(R.string.help_player_settings_body)
    ),
    HelpEntry(
        icon = Icons.Default.TouchApp,
        title = stringResource(R.string.help_player_gestures_title),
        body = stringResource(R.string.help_player_gestures_body)
    ),
    HelpEntry(
        icon = Icons.Default.Speed,
        title = stringResource(R.string.help_player_speed_title),
        body = stringResource(R.string.help_player_speed_body)
    ),
    HelpEntry(
        icon = Icons.Default.ScreenRotation,
        title = stringResource(R.string.help_player_rotate_title),
        body = stringResource(R.string.help_player_rotate_body)
    ),
    HelpEntry(
        icon = Icons.Default.Timer,
        title = stringResource(R.string.help_player_autonext_title),
        body = stringResource(R.string.help_player_autonext_body)
    )
)

@Composable
fun subtitleHelpEntries(): List<HelpEntry> = listOf(
    HelpEntry(
        icon = Icons.Default.Subtitles,
        title = stringResource(R.string.help_subtitle_enable_title),
        body = stringResource(R.string.help_subtitle_enable_body)
    ),
    HelpEntry(
        icon = Icons.Default.Timer,
        title = stringResource(R.string.help_subtitle_sync_title),
        body = stringResource(R.string.help_subtitle_sync_body)
    ),
    HelpEntry(
        icon = Icons.Default.SwapHoriz,
        title = stringResource(R.string.help_subtitle_track_title),
        body = stringResource(R.string.help_subtitle_track_body)
    ),
    HelpEntry(
        icon = Icons.Default.UploadFile,
        title = stringResource(R.string.help_subtitle_file_title),
        body = stringResource(R.string.help_subtitle_file_body)
    ),
    HelpEntry(
        icon = Icons.Default.Palette,
        title = stringResource(R.string.help_subtitle_color_title),
        body = stringResource(R.string.help_subtitle_color_body)
    ),
    HelpEntry(
        icon = Icons.Default.Tune,
        title = stringResource(R.string.help_subtitle_size_title),
        body = stringResource(R.string.help_subtitle_size_body)
    ),
    HelpEntry(
        icon = Icons.Default.Style,
        title = stringResource(R.string.help_subtitle_style_title),
        body = stringResource(R.string.help_subtitle_style_body)
    )
)

@Composable
fun settingsHelpEntries(): List<HelpEntry> = listOf(
    HelpEntry(
        icon = Icons.Default.Language,
        title = stringResource(R.string.help_settings_language_title),
        body = stringResource(R.string.help_settings_language_body)
    ),
    HelpEntry(
        icon = Icons.Default.SwapHoriz,
        title = stringResource(R.string.help_settings_direction_title),
        body = stringResource(R.string.help_settings_direction_body)
    ),
    HelpEntry(
        icon = Icons.Default.Replay,
        title = stringResource(R.string.help_settings_tour_title),
        body = stringResource(R.string.help_settings_tour_body)
    )
)
