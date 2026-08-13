package com.opplayer.app.ui.screens

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opplayer.app.MainActivity
import com.opplayer.app.R
import com.opplayer.app.player.OrientationPolicy
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlayerGestureKind
import com.opplayer.app.player.PlayerGestures
import com.opplayer.app.player.PlayerOrientation
import com.opplayer.app.player.subtitle.SubtitleOptions
import com.opplayer.app.ui.SubtitleStyleViewModel
import com.opplayer.app.ui.components.HelpSheet
import com.opplayer.app.ui.components.PlayerGestureHud
import com.opplayer.app.ui.components.PlayerSeekPreviewHud
import com.opplayer.app.ui.components.PlayerSettingsSheet
import com.opplayer.app.ui.components.SubtitleOption
import com.opplayer.app.ui.components.SubtitleOverlay
import com.opplayer.app.ui.components.SubtitlePlaybackControls
import com.opplayer.app.ui.components.SubtitleSettingsSheet
import com.opplayer.app.ui.components.playerHelpEntries
import com.opplayer.app.ui.player.PlayerBufferingIndicator
import com.opplayer.app.ui.player.PlayerErrorState
import com.opplayer.app.ui.player.PlayerSeekHint
import com.opplayer.app.ui.player.PlayerSurface
import com.opplayer.app.ui.player.PlayerTopBar
import com.opplayer.app.ui.player.PlayerViewModel
import com.opplayer.app.ui.player.enterPip
import com.opplayer.app.ui.player.rememberPlayerControlsState
import com.opplayer.app.ui.player.supportsPip
import com.opplayer.app.ui.player.updatePipParams
import com.opplayer.app.util.findActivity
import kotlinx.coroutines.delay

private const val SEEK_HINT_TIMEOUT_MS = 800L
private const val HUD_TIMEOUT_MS = 900L

private data class GestureHudState(val kind: PlayerGestureKind, val value: Float)

class PlayerViewModelHost : ViewModel(), ViewModelStoreOwner {

    override val viewModelStore = ViewModelStore()

    fun clear() {
        viewModelStore.clear()
    }

    override fun onCleared() {
        viewModelStore.clear()
        super.onCleared()
    }
}

@Composable
fun PlayerScreen(
    request: PlaybackRequest,
    onSavePosition: (PlaybackRequest, Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val mainActivity = activity as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val application = context.applicationContext as Application

    val storeOwner: PlayerViewModelHost = viewModel()

    DisposableEffect(storeOwner) {
        onDispose { storeOwner.clear() }
    }

    val playerViewModel: PlayerViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        factory = PlayerViewModel.factory(application, request)
    )
    val subtitleStyleViewModel: SubtitleStyleViewModel = viewModel()

    val state by playerViewModel.uiState.collectAsStateWithLifecycle()
    val subtitle by playerViewModel.subtitleState.collectAsStateWithLifecycle()
    val subtitleText by playerViewModel.subtitleText.collectAsStateWithLifecycle()
    val subtitleStyle by subtitleStyleViewModel.settings.collectAsStateWithLifecycle()

    val isInPip = mainActivity?.isInPipMode?.value ?: false
    val controls = rememberPlayerControlsState()

    var showSettings by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var seekHint by remember { mutableStateOf<Boolean?>(null) }
    var seekHintTick by remember { mutableIntStateOf(0) }
    var hudState by remember { mutableStateOf<GestureHudState?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }

    val currentProgressSaver by rememberUpdatedState(onSavePosition)

    DisposableEffect(playerViewModel) {
        playerViewModel.setProgressSaver { savedRequest, position ->
            currentProgressSaver(savedRequest, position)
        }
        onDispose { playerViewModel.clearProgressSaver() }
    }

    LaunchedEffect(request) { playerViewModel.onRequest(request) }

    LaunchedEffect(playerViewModel, context) {
        playerViewModel.messages.collect { message ->
            val text = message.argument
                ?.let { context.getString(message.textRes, it) }
                ?: context.getString(message.textRes)

            Toast.makeText(
                context,
                text,
                if (message.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        if (pickedUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            playerViewModel.onSubtitleFilePicked(pickedUri.toString())
        }
    }

    val gestures = remember(activity, playerViewModel) {
        PlayerGestures(
            context = context,
            activity = activity,
            isEnabled = { playerViewModel.uiState.value.gesturesEnabled },
            onTap = { controls.toggle() },
            onSeek = { forward ->
                playerViewModel.seekBy(forward)
                seekHint = forward
                seekHintTick++
            },
            onIndicator = { kind, value ->
                hudState = GestureHudState(kind, value)
                hudTick++
            },
            onSeekDragStart = { playerViewModel.startSeekDrag() },
            onSeekDrag = { fraction -> playerViewModel.updateSeekDrag(fraction) },
            onSeekDragEnd = { commit ->
                if (commit) playerViewModel.commitSeekDrag() else playerViewModel.cancelSeekDrag()
                controls.show()
            }
        )
    }

    DisposableEffect(gestures) {
        onDispose { gestures.release() }
    }

    LaunchedEffect(seekHintTick) {
        if (seekHint != null) {
            delay(SEEK_HINT_TIMEOUT_MS)
            seekHint = null
        }
    }

    LaunchedEffect(hudTick) {
        if (hudState != null) {
            delay(HUD_TIMEOUT_MS)
            hudState = null
        }
    }

    var videoBounds by remember { mutableStateOf<Rect?>(null) }

    DisposableEffect(lifecycleOwner, mainActivity, playerViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && mainActivity?.isInPipMode?.value != true) {
                playerViewModel.pauseForBackground()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(mainActivity, playerViewModel) {
        mainActivity?.onUserLeaveAction = {
            val player = playerViewModel.player
            if (player != null && player.isPlaying) enterPip(mainActivity, player, videoBounds)
        }
        onDispose { mainActivity?.onUserLeaveAction = null }
    }

    LaunchedEffect(mainActivity, videoBounds, playerViewModel.player) {
        val player = playerViewModel.player
        if (player != null) updatePipParams(mainActivity, player, videoBounds)
    }

    val orientation = OrientationPolicy.resolve(
        videoAspect = state.videoAspect,
        isFullscreen = state.isFullscreen,
        autoRotateEnabled = state.autoRotateEnabled,
        isInPip = isInPip
    )

    DisposableEffect(orientation, activity) {
        activity?.requestedOrientation = orientation.toActivityInfo()
        onDispose { }
    }

    DisposableEffect(state.isFullscreen, isInPip, activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            if (state.isFullscreen && !isInPip) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val handleBack = {
        if (state.isFullscreen) playerViewModel.setFullscreen(false) else onClose()
    }

    BackHandler(onBack = handleBack)

    val errorMessage = state.errorRes?.let { stringResource(it) }
    val offLabel = stringResource(R.string.subtitle_off)
    val embeddedLabel = stringResource(R.string.subtitle_embedded_track)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        playerViewModel.player?.let { player ->
            PlayerSurface(
                player = player,
                scaleMode = state.scaleMode,
                controlsEnabled = !isInPip,
                controls = controls,
                touchListener = gestures,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        videoBounds = Rect(
                            bounds.left.toInt(),
                            bounds.top.toInt(),
                            bounds.right.toInt(),
                            bounds.bottom.toInt()
                        )
                    }
            )
        }

        SubtitleOverlay(
            text = subtitleText,
            settings = subtitleStyle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        if (!isInPip && controls.visible) {
            PlayerTopBar(
                title = state.displayTitle,
                isFullscreen = state.isFullscreen,
                showEpisodeButton = state.canNavigateEpisodes,
                isResolvingEpisode = state.isResolvingEpisode,
                isLocalQueue = state.request.source == PlaybackRequest.Source.DEVICE,
                onBack = handleBack,
                onNextEpisode = { playerViewModel.navigateEpisode(forward = true) },
                onCycleScale = {
                    val next = playerViewModel.cycleScaleMode()
                    Toast.makeText(context, context.getString(next.labelRes), Toast.LENGTH_SHORT)
                        .show()
                },
                onPip = {
                    val player = playerViewModel.player
                    if (supportsPip(context.packageManager) && player != null) {
                        enterPip(mainActivity, player, videoBounds)
                    } else {
                        Toast.makeText(context, R.string.pip_unavailable, Toast.LENGTH_SHORT).show()
                    }
                },
                onToggleFullscreen = {
                    playerViewModel.toggleFullscreen()
                    controls.show()
                },
                onSubtitles = { showSubtitleSheet = true },
                onSettings = { showSettings = true },
                onHelp = {
                    showHelp = true
                    controls.show()
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        val hud = hudState
        if (hud != null && !isInPip) {
            PlayerGestureHud(
                kind = hud.kind,
                value = hud.value,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val seekPreview = state.seekPreview
        if (seekPreview != null && !isInPip) {
            PlayerSeekPreviewHud(
                positionMs = seekPreview.positionMs,
                deltaMs = seekPreview.deltaMs,
                durationMs = seekPreview.durationMs,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val hint = seekHint
        if (hint != null && !isInPip && hud == null && seekPreview == null) {
            PlayerSeekHint(
                forward = hint,
                modifier = Modifier.align(
                    if (hint) Alignment.CenterEnd else Alignment.CenterStart
                )
            )
        }

        if (state.isBuffering && errorMessage == null) {
            PlayerBufferingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (errorMessage != null && !isInPip) {
            PlayerErrorState(
                message = errorMessage,
                showNextEpisode = state.canNavigateEpisodes,
                onRetry = { playerViewModel.retry() },
                onNextEpisode = { playerViewModel.navigateEpisode(forward = true) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    if (showHelp) {
        HelpSheet(
            title = stringResource(R.string.help_player_title),
            entries = playerHelpEntries(),
            onDismiss = { showHelp = false }
        )
    }

    if (showSettings) {
        PlayerSettingsSheet(
            currentSpeed = state.speed,
            scaleMode = state.scaleMode,
            autoNextEnabled = state.autoNextEnabled,
            autoRotateEnabled = state.autoRotateEnabled,
            gesturesEnabled = state.gesturesEnabled,
            showEpisodeOptions = state.canNavigateEpisodes,
            onSpeedChange = { playerViewModel.setSpeed(it) },
            onScaleModeChange = { playerViewModel.setScaleMode(it) },
            onAutoNextChange = { playerViewModel.setAutoNextEnabled(it) },
            onAutoRotateChange = { playerViewModel.setAutoRotateEnabled(it) },
            onGesturesChange = { playerViewModel.setGesturesEnabled(it) },
            onDismiss = { showSettings = false }
        )
    }

    if (showSubtitleSheet) {
        val options = SubtitleOptions.build(
            disabled = subtitle.disabled,
            usingExternal = subtitle.usingExternal,
            externalName = subtitle.externalName,
            externalUri = subtitle.externalUri,
            candidates = subtitle.candidates,
            embeddedTracks = subtitle.embeddedTracks,
            offLabel = offLabel,
            embeddedLabel = embeddedLabel
        ).map { item -> SubtitleOption(id = item.id, label = item.label, selected = item.selected) }

        SubtitleSettingsSheet(
            settings = subtitleStyle,
            onSettingsChange = { updated -> subtitleStyleViewModel.update { updated } },
            onReset = { subtitleStyleViewModel.reset() },
            onDismiss = { showSubtitleSheet = false },
            playback = SubtitlePlaybackControls(
                offsetMs = subtitle.offsetMs,
                onOffsetChange = { playerViewModel.setSubtitleOffset(it) },
                options = options,
                onOptionSelected = { option ->
                    SubtitleOptions.parse(option.id)?.let { playerViewModel.onSubtitleChoice(it) }
                },
                onPickFile = { subtitlePicker.launch(arrayOf("*/*")) },
                embeddedOnly = !subtitle.usingExternal
            )
        )
    }
}

private fun PlayerOrientation.toActivityInfo(): Int = when (this) {
    PlayerOrientation.SENSOR_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    PlayerOrientation.SENSOR_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    PlayerOrientation.UNSPECIFIED -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}
