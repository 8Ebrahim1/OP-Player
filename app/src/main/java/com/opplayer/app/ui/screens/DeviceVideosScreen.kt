package com.opplayer.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opplayer.app.R
import com.opplayer.app.data.LocalVideo
import com.opplayer.app.ui.DeviceVideosViewModel
import com.opplayer.app.ui.components.EmptyState
import com.opplayer.app.ui.components.FolderCard
import com.opplayer.app.ui.components.LocalVideoCard
import com.opplayer.app.ui.components.ScreenHeader

private val requiredPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
fun DeviceVideosScreen(
    localPositions: Map<String, Long>,
    onPlay: (LocalVideo, Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceVideosViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
        if (granted) viewModel.refresh()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !uiState.hasLoadedOnce) viewModel.refresh()
    }

    BackHandler(enabled = openFolder != null) {
        openFolder = null
        query = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        val currentFolder = openFolder

        ScreenHeader(
            brand = stringResource(R.string.brand),
            title = currentFolder ?: stringResource(R.string.device_title),
            action = {
                if (currentFolder != null) {
                    IconButton(
                        onClick = {
                            openFolder = null
                            query = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                } else if (hasPermission) {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            }
        )

        when {
            !hasPermission -> {
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
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } else {
                                permissionLauncher.launch(requiredPermission)
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
                        body = stringResource(R.string.permission_body),
                        actionLabel = stringResource(R.string.refresh),
                        onAction = { viewModel.refresh() }
                    )
                }
            }

            currentFolder == null -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.folders, key = { it.name }) { folder ->
                        FolderCard(folder = folder, onClick = { openFolder = folder.name })
                    }
                }
            }

            else -> {
                val folder = uiState.folders.firstOrNull { it.name == currentFolder }
                val videos = folder?.videos.orEmpty().filter {
                    query.isBlank() || it.name.contains(query, ignoreCase = true)
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.search_videos)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                if (videos.isEmpty()) {
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
                        items(videos, key = { it.id }) { video ->
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
}
