package com.opplayer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.opplayer.app.R
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.data.VideoItem
import com.opplayer.app.ui.components.AddLinkDialog
import com.opplayer.app.ui.components.EmptyState
import com.opplayer.app.ui.components.GlassTabs
import com.opplayer.app.ui.components.LibraryVideoCard
import com.opplayer.app.ui.components.ScreenHeader

@Composable
fun LibraryScreen(
    videos: List<VideoItem>,
    onPlay: (VideoItem) -> Unit,
    onAdd: (title: String, url: String, subtitleUrl: String?, pattern: EpisodePattern?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onResetProgress: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<VideoItem?>(null) }

    val tabTitles = listOf(
        stringResource(R.string.section_library),
        stringResource(R.string.section_favorites),
        stringResource(R.string.section_recent)
    )

    val visibleVideos = when (selectedTab) {
        1 -> videos.filter { it.isFavorite }
        2 -> videos.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }
        else -> videos.sortedByDescending { it.addedAt }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        ScreenHeader(
            brand = stringResource(R.string.brand),
            title = stringResource(R.string.library_title),
            action = {
                FilledTonalButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.add_link),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        )

        GlassTabs(
            titles = tabTitles,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (visibleVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (selectedTab) {
                    1 -> EmptyState(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.empty_favorites_title),
                        body = stringResource(R.string.empty_favorites_body)
                    )

                    2 -> EmptyState(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.empty_recent_title),
                        body = stringResource(R.string.empty_recent_body)
                    )

                    else -> EmptyState(
                        icon = Icons.Default.VideoLibrary,
                        title = stringResource(R.string.empty_library_title),
                        body = stringResource(R.string.empty_library_body),
                        actionLabel = stringResource(R.string.add_first_video),
                        onAction = { showAddDialog = true }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleVideos, key = { it.id }) { item ->
                    LibraryVideoCard(
                        item = item,
                        onPlay = { onPlay(item) },
                        onToggleFavorite = { onToggleFavorite(item.id) },
                        onResetProgress = { onResetProgress(item.id) },
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddLinkDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, url, subtitleUrl, pattern ->
                showAddDialog = false
                onAdd(title, url, subtitleUrl, pattern)
            }
        )
    }

    val deleteTarget = pendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_body, deleteTarget.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(deleteTarget.id)
                        pendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
