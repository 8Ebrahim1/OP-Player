package com.opplayer.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opplayer.app.R
import com.opplayer.app.data.LocalVideo
import com.opplayer.app.data.MediaAccess
import com.opplayer.app.data.currentMediaAccess
import com.opplayer.app.data.mediaPermissionRequest
import com.opplayer.app.ui.DeviceVideosViewModel
import com.opplayer.app.ui.components.EmptyState
import com.opplayer.app.ui.components.FolderCard
import com.opplayer.app.ui.components.HelpIconButton
import com.opplayer.app.ui.components.HelpSheet
import com.opplayer.app.ui.components.LocalVideoCard
import com.opplayer.app.ui.components.ScreenHeader
import com.opplayer.app.ui.components.deviceHelpEntries

/**
 * Browses the videos stored on the device.
 *
 * Media access is re-evaluated on every ON_RESUME, so granting the permission in
 * system settings and coming back shows the library immediately instead of
 * leaving a permanently empty screen. Partial access (Android 14+) is a first
 * class state: the videos the user shared are listed, with a way to widen the
 * selection.
 */
@Composable
fun DeviceVideosScreen(
    localPositions: Map<String, Long>,
    onPlay: (LocalVideo, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceVideosViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var access by remember { mutableStateOf(context.currentMediaAccess()) }
    var permissionRequested by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            val current = context.currentMediaAccess()
            if (current != access) {
                access = current
            } else if (current != MediaAccess.DENIED) {
                // Partial selections can change without the level changing.
                viewModel.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRequested = true
        access = context.currentMediaAccess()
    }

    LaunchedEffect(access) {
        if (access.canReadAnything) viewModel.refresh()
    }

    BackHandler(enabled = openFolderId != null) {
        openFolderId = null
        query = ""
    }

    val currentFolder = remember(uiState.folders, openFolderId) {
        openFolderId?.let { id -> uiState.folders.firstOrNull { it.id == id } }
    }

    // Filtering runs only when the folder or the query really changes.
    val visibleVideos by remember(currentFolder, query) {
        derivedStateOf {
            currentFolder?.videos.orEmpty().filter {
                query.isBlank() || it.name.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        ScreenHeader(
            brand = stringResource(R.string.brand),
            title = currentFolder?.name ?: stringResource(R.string.device_title),
            action = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentFolder != null) {
                        IconButton(
                            onClick = {
                                openFolderId = null
                                query = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    } else if (access.canReadAnything) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh)
                            )
                        }
                    }

                    HelpIconButton(onClick = { showHelp = true })
                }
            }
        )

        when {
            access == MediaAccess.DENIED -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.permission_title),
                        body = stringResource(R.string.permission_body),
                        actionLabel = if (permissionRequested) {
                            stringResource(R.string.permission_settings)
                        } else {
                            stringResource(R.string.permission_grant)
                        },
                        onAction = {
                            if (permissionRequested) {
                                context.openAppSettings()
                            } else {
                                permissionLauncher.launch(mediaPermissionRequest)
                            }
                        }
                    )
                }
            }

            uiState.isLoading && uiState.folders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.folders.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.VideocamOff,
                        title = stringResource(R.string.no_local_videos),
                        body = if (access == MediaAccess.PARTIAL) {
                            stringResource(R.string.permission_partial_body)
                        } else {
                            stringResource(R.string.permission_body)
                        },
                        actionLabel = if (access == MediaAccess.PARTIAL) {
                            stringResource(R.string.permission_partial_action)
                        } else {
                            stringResource(R.string.refresh)
                        },
                        onAction = {
                            if (access == MediaAccess.PARTIAL) {
                                permissionLauncher.launch(mediaPermissionRequest)
                            } else {
                                viewModel.refresh()
                            }
                        }
                    )
                }
            }

            currentFolder == null -> {
                if (access == MediaAccess.PARTIAL) {
                    PartialAccessNotice(
                        onManage = { permissionLauncher.launch(mediaPermissionRequest) }
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Keyed by bucket id: two folders may share a display name.
                    items(uiState.folders, key = { it.id }) { folder ->
                        FolderCard(folder = folder, onClick = { openFolderId = folder.id })
                    }
                }
            }

            else -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_videos)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                if (visibleVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.Folder,
                            title = stringResource(R.string.no_local_videos),
                            body = stringResource(R.string.empty_recent_body)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(visibleVideos, key = { it.id }) { video ->
                            val resume = localPositions[video.uri] ?: 0L
                            LocalVideoCard(
                                video = video,
                                resumePositionMs = resume,
                                onPlay = { onPlay(video, resume) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Anchored at screen level so help stays reachable in every state,
    // including while the permission prompt is showing.
    if (showHelp) {
        HelpSheet(
            title = stringResource(R.string.help_device_title),
            entries = deviceHelpEntries(),
            onDismiss = { showHelp = false }
        )
    }
}

/** Banner shown when only a hand picked set of videos is visible. */
@Composable
private fun PartialAccessNotice(onManage: () -> Unit) {
    com.opplayer.app.ui.components.InfoBanner(
        text = stringResource(R.string.permission_partial_body),
        actionLabel = stringResource(R.string.permission_partial_action),
        onAction = onManage,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

private fun android.content.Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
