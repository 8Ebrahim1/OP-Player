package com.opplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opplayer.app.R
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.ui.components.GlassBackground
import com.opplayer.app.ui.screens.DeviceVideosScreen
import com.opplayer.app.ui.screens.LibraryScreen
import com.opplayer.app.ui.screens.PlayerScreen
import com.opplayer.app.ui.theme.OpBackground
import com.opplayer.app.ui.theme.OpSurface

@Composable
fun OPPlayerApp(
    initialRequest: PlaybackRequest? = null,
    onInitialRequestHandled: () -> Unit = {},
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val library by libraryViewModel.library.collectAsStateWithLifecycle()
    val localPositions by libraryViewModel.localPositions.collectAsStateWithLifecycle()

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var playback by remember { mutableStateOf(initialRequest) }

    val handledCallback by rememberUpdatedState(onInitialRequestHandled)

    LaunchedEffect(initialRequest) {
        val request = initialRequest ?: return@LaunchedEffect
        playback = request
        handledCallback()
    }

    val onSavePosition: (PlaybackRequest, Long) -> Unit = { request, position ->
        when (request.source) {
            PlaybackRequest.Source.LIBRARY ->
                libraryViewModel.saveLibraryProgress(
                    id = request.key,
                    url = request.uri,
                    pattern = request.pattern,
                    episodeLabel = request.episodeLabel,
                    positionMs = position
                )

            PlaybackRequest.Source.DEVICE ->
                libraryViewModel.saveDevicePosition(request.key, position)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OpBackground)
        ) {
            val currentPlayback = playback

            if (currentPlayback != null) {
                PlayerScreen(
                    request = currentPlayback,
                    onSavePosition = onSavePosition,
                    onClose = { playback = null }
                )
            } else {
                GlassBackground()

                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(
                            containerColor = OpSurface.copy(alpha = 0.92f),
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = selectedSection == 0,
                                onClick = { selectedSection = 0 },
                                icon = {
                                    Icon(Icons.Default.Link, contentDescription = null)
                                },
                                label = { Text(stringResource(R.string.tab_links)) }
                            )

                            NavigationBarItem(
                                selected = selectedSection == 1,
                                onClick = { selectedSection = 1 },
                                icon = {
                                    Icon(Icons.Default.Smartphone, contentDescription = null)
                                },
                                label = { Text(stringResource(R.string.tab_device)) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        when (selectedSection) {
                            0 -> LibraryScreen(
                                videos = library,
                                onPlay = { item ->
                                    val resumeUrl = item.currentUrl ?: item.url
                                    val resumePattern = if (item.currentUrl != null) {
                                        item.currentPattern ?: item.pattern
                                    } else {
                                        item.pattern
                                    }

                                    playback = PlaybackRequest(
                                        key = item.id,
                                        title = item.title,
                                        uri = resumeUrl,
                                        subtitleUrl = item.subtitleUrl,
                                        startPositionMs = item.positionMs,
                                        source = PlaybackRequest.Source.LIBRARY,
                                        pattern = resumePattern,
                                        episodeLabel = item.currentLabel
                                    )
                                },
                                onAdd = { title, url, subtitleUrl, pattern ->
                                    libraryViewModel.addVideo(title, url, subtitleUrl, pattern)
                                },
                                onToggleFavorite = libraryViewModel::toggleFavorite,
                                onResetProgress = libraryViewModel::resetProgress,
                                onDelete = libraryViewModel::removeVideo
                            )

                            else -> DeviceVideosScreen(
                                localPositions = localPositions,
                                onPlay = { video, resumePosition ->
                                    playback = PlaybackRequest(
                                        key = video.uri,
                                        title = video.name,
                                        uri = video.uri,
                                        startPositionMs = resumePosition,
                                        source = PlaybackRequest.Source.DEVICE
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
