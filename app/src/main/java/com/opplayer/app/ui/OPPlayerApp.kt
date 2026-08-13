package com.opplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opplayer.app.R
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.ui.components.AppSettingsSheet
import com.opplayer.app.ui.components.GlassBackground
import com.opplayer.app.ui.components.SubtitleSettingsSheet
import com.opplayer.app.ui.localization.AppLocalization
import com.opplayer.app.ui.onboarding.OnboardingScreen
import com.opplayer.app.ui.screens.DeviceVideosScreen
import com.opplayer.app.ui.screens.LibraryScreen
import com.opplayer.app.ui.screens.PlayerScreen
import com.opplayer.app.ui.theme.OpBackground
import com.opplayer.app.ui.theme.OpSurface

@Composable
fun OPPlayerApp(
    initialRequest: PlaybackRequest? = null,
    onInitialRequestHandled: () -> Unit = {},
    libraryViewModel: LibraryViewModel = viewModel(),
    subtitleStyleViewModel: SubtitleStyleViewModel = viewModel(),
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val appSettings by appSettingsViewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by appSettingsViewModel.loaded.collectAsStateWithLifecycle()
    val subtitleStyle by subtitleStyleViewModel.settings.collectAsStateWithLifecycle()

    AppLocalization(
        language = appSettings.language,
        layoutDirection = appSettings.layoutDirection
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OpBackground)
        ) {
            when {

                !settingsLoaded -> Unit

                !appSettings.onboardingCompleted -> OnboardingScreen(
                    settings = appSettings,
                    subtitleStyle = subtitleStyle,
                    onLanguageChange = appSettingsViewModel::setLanguage,
                    onLayoutDirectionChange = appSettingsViewModel::setLayoutDirection,
                    onSubtitleTextColorChange = { argb ->
                        subtitleStyleViewModel.update { it.copy(textColorArgb = argb) }
                    },
                    onSubtitleBackgroundChange = { argb ->
                        subtitleStyleViewModel.update { it.copy(backgroundArgb = argb) }
                    },
                    onFinish = appSettingsViewModel::completeOnboarding
                )

                else -> MainContent(
                    initialRequest = initialRequest,
                    onInitialRequestHandled = onInitialRequestHandled,
                    libraryViewModel = libraryViewModel,
                    subtitleStyleViewModel = subtitleStyleViewModel,
                    appSettingsViewModel = appSettingsViewModel
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    initialRequest: PlaybackRequest?,
    onInitialRequestHandled: () -> Unit,
    libraryViewModel: LibraryViewModel,
    subtitleStyleViewModel: SubtitleStyleViewModel,
    appSettingsViewModel: AppSettingsViewModel
) {
    val library by libraryViewModel.library.collectAsStateWithLifecycle()
    val localPositions by libraryViewModel.localPositions.collectAsStateWithLifecycle()
    val subtitleStyle by subtitleStyleViewModel.settings.collectAsStateWithLifecycle()
    val appSettings by appSettingsViewModel.settings.collectAsStateWithLifecycle()

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    var playback by rememberSaveable { mutableStateOf(initialRequest) }
    var showSubtitleStyleSheet by rememberSaveable { mutableStateOf(false) }
    var showAppSettingsSheet by rememberSaveable { mutableStateOf(false) }

    val handledCallback by rememberUpdatedState(onInitialRequestHandled)

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(initialRequest) {
        val request = initialRequest ?: return@LaunchedEffect
        playback = request
        handledCallback()
    }

    LaunchedEffect(libraryViewModel, context) {
        libraryViewModel.messages.collect { textRes ->
            snackbarHostState.showSnackbar(context.getString(textRes))
        }
    }

    val onSavePosition: (PlaybackRequest, Long) -> Unit = remember(libraryViewModel) {
        { request, position -> libraryViewModel.saveProgress(request, position) }
    }

    val currentPlayback = playback

    if (currentPlayback != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerScreen(
                request = currentPlayback,
                onSavePosition = onSavePosition,
                onClose = { playback = null }
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        return
    }

    GlassBackground()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = OpSurface.copy(alpha = 0.92f)
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

                NavigationBarItem(
                    selected = showSubtitleStyleSheet,
                    onClick = { showSubtitleStyleSheet = true },
                    icon = {
                        Icon(Icons.Default.Subtitles, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.tab_subtitle)) }
                )

                NavigationBarItem(
                    selected = showAppSettingsSheet,
                    onClick = { showAppSettingsSheet = true },
                    icon = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { innerPadding ->
        Box(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            source = PlaybackRequest.Source.DEVICE,
                            folderId = video.bucketId
                        )
                    }
                )
            }
        }
    }

    if (showSubtitleStyleSheet) {
        SubtitleSettingsSheet(
            settings = subtitleStyle,
            onSettingsChange = { updated ->
                subtitleStyleViewModel.update { updated }
            },
            onReset = { subtitleStyleViewModel.reset() },
            onDismiss = { showSubtitleStyleSheet = false }
        )
    }

    if (showAppSettingsSheet) {
        AppSettingsSheet(
            settings = appSettings,
            onLanguageChange = appSettingsViewModel::setLanguage,
            onLayoutDirectionChange = appSettingsViewModel::setLayoutDirection,
            onReplayTour = {

                showAppSettingsSheet = false
                appSettingsViewModel.restartOnboarding()
            },
            onDismiss = { showAppSettingsSheet = false }
        )
    }
}
