package com.opplayer.app.ui.screens

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.opplayer.app.MainActivity
import com.opplayer.app.R
import com.opplayer.app.data.EpisodePattern
import com.opplayer.app.player.EpisodeNavigator
import com.opplayer.app.player.MediaFactory
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlayerGestureKind
import com.opplayer.app.player.PlayerGestures
import com.opplayer.app.ui.components.PlayerGestureHud
import com.opplayer.app.ui.components.PlayerSettingsSheet
import com.opplayer.app.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEEK_STEP_MS = 15_000L
private const val SEEK_HINT_TIMEOUT_MS = 800L
private const val HUD_TIMEOUT_MS = 900L
private const val LANDSCAPE_ASPECT_MIN = 1.2f
private const val PORTRAIT_ASPECT_MAX = 0.9f
private const val CONTROLLER_TIMEOUT_MS = 4_000

private val resizeModeCycle = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_FILL
)

private fun resizeModeLabelRes(mode: Int): Int = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> R.string.aspect_zoom
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> R.string.aspect_fill
    else -> R.string.aspect_fit
}

private data class GestureHudState(val kind: PlayerGestureKind, val value: Float)

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
    val scope = rememberCoroutineScope()

    var activeRequest by remember { mutableStateOf(request) }
    var activePattern by remember { mutableStateOf(request.pattern) }

    var speed by remember { mutableFloatStateOf(1f) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
    var showSettings by remember { mutableStateOf(false) }
    var autoNextEnabled by remember { mutableStateOf(true) }
    var autoRotateEnabled by remember { mutableStateOf(true) }
    var gesturesEnabled by remember { mutableStateOf(true) }
    var isResolvingNext by remember { mutableStateOf(false) }
    var endedTick by remember { mutableIntStateOf(0) }
    var videoAspect by remember { mutableFloatStateOf(0f) }
    var seekHint by remember { mutableStateOf<Boolean?>(null) }
    var seekHintTick by remember { mutableIntStateOf(0) }
    var hudState by remember { mutableStateOf<GestureHudState?>(null) }
    var hudTick by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    val isInPip = mainActivity?.isInPipMode?.value ?: false

    val pipUnavailable = stringResource(R.string.pip_unavailable)
    val noPatternMessage = stringResource(R.string.episode_pattern_missing)
    val notFoundMessage = stringResource(R.string.next_episode_not_found)

    val player = remember {
        MediaFactory.createPlayer(context).apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
        }
    }

    val isOnlineLink = remember(activeRequest.uri) {
        activeRequest.uri.startsWith("http", ignoreCase = true)
    }

    val canNavigateEpisodes = remember(activeRequest.uri, activePattern) {
        activePattern != null || (isOnlineLink && EpisodeNavigator.hasMarker(activeRequest.uri))
    }

    fun currentResumePosition(): Long {
        val duration = player.duration
        val position = player.currentPosition
        return if (duration > 0 && position >= duration - 3_000) 0L else position.coerceAtLeast(0L)
    }

    fun showControls() {
        playerView?.showController()
    }

    fun toggleControls() {
        val view = playerView ?: return
        if (view.isControllerFullyVisible) view.hideController() else view.showController()
    }

    fun cycleResizeMode() {
        val index = resizeModeCycle.indexOf(resizeMode)
        val next = resizeModeCycle[(index + 1).mod(resizeModeCycle.size)]
        resizeMode = next
        playerView?.resizeMode = next
        showControls()
        Toast.makeText(context, context.getString(resizeModeLabelRes(next)), Toast.LENGTH_SHORT)
            .show()
    }

    fun startPlayback(url: String, pattern: EpisodePattern?, label: String) {
        errorMessage = null
        activePattern = pattern
        val nextRequest = activeRequest.copy(
            uri = url,
            subtitleUrl = null,
            startPositionMs = 0L,
            pattern = pattern,
            episodeLabel = label
        )

        activeRequest = nextRequest
        onSavePosition(nextRequest, 0L)

        Toast.makeText(
            context,
            context.getString(R.string.now_playing_episode, label),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun navigateEpisode(forward: Boolean) {
        if (isResolvingNext) return

        if (!canNavigateEpisodes) {
            Toast.makeText(context, noPatternMessage, Toast.LENGTH_SHORT).show()
            return
        }

        val currentUrl = activeRequest.uri
        val pattern = activePattern
        isResolvingNext = true

        scope.launch {
            onSavePosition(activeRequest, currentResumePosition())

            if (pattern != null) {
                val candidate = EpisodeNavigator.resolvePattern(pattern, forward)
                isResolvingNext = false

                if (candidate == null) {
                    Toast.makeText(context, notFoundMessage, Toast.LENGTH_LONG).show()
                    return@launch
                }

                startPlayback(candidate.url, candidate, candidate.label())
            } else {
                val candidate = if (forward) {
                    EpisodeNavigator.resolveNext(currentUrl)
                } else {
                    EpisodeNavigator.resolvePrevious(currentUrl)
                }

                isResolvingNext = false

                if (candidate == null) {
                    Toast.makeText(context, notFoundMessage, Toast.LENGTH_LONG).show()
                    return@launch
                }

                startPlayback(candidate.url, null, EpisodeNavigator.label(candidate))
            }
        }
    }

    fun seekBy(forward: Boolean) {
        val duration = player.duration
        val delta = if (forward) SEEK_STEP_MS else -SEEK_STEP_MS
        val target = (player.currentPosition + delta).coerceAtLeast(0L)
        val safeTarget = if (duration > 0) target.coerceAtMost(duration) else target

        player.seekTo(safeTarget)
        seekHint = forward
        seekHintTick++
    }

    val gestures = remember(activity) {
        PlayerGestures(
            context = context,
            activity = activity,
            isEnabled = { gesturesEnabled },
            onTap = { toggleControls() },
            onSeek = { forward -> seekBy(forward) },
            onIndicator = { kind, value ->
                hudState = GestureHudState(kind, value)
                hudTick++
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

    LaunchedEffect(request) {
        if (request.uri != activeRequest.uri || request.key != activeRequest.key) {
            activeRequest = request
            activePattern = request.pattern
        }
    }

    LaunchedEffect(activeRequest.uri) {
        isBuffering = true
        videoAspect = 0f
        player.setMediaItem(MediaFactory.buildMediaItem(activeRequest))
        if (activeRequest.startPositionMs > 0) player.seekTo(activeRequest.startPositionMs)
        player.playbackParameters = PlaybackParameters(speed)
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) endedTick++
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val ratio = if (videoSize.pixelWidthHeightRatio > 0f) {
                    videoSize.pixelWidthHeightRatio
                } else {
                    1f
                }
                val width = videoSize.width * ratio
                val height = videoSize.height.toFloat()

                videoAspect = if (width > 0f && height > 0f) width / height else 0f
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = context.getString(MediaFactory.errorMessageRes(error))
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                if (error == null) errorMessage = null
            }
        }

        player.addListener(listener)

        onDispose {
            onSavePosition(activeRequest, currentResumePosition())
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(endedTick) {
        if (endedTick > 0 && autoNextEnabled && canNavigateEpisodes) {
            navigateEpisode(forward = true)
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && mainActivity?.isInPipMode?.value != true) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(5_000)
            if (player.isPlaying) onSavePosition(activeRequest, currentResumePosition())
        }
    }

    DisposableEffect(mainActivity, player) {
        mainActivity?.onUserLeaveAction = {
            if (player.isPlaying) enterPip(mainActivity, player)
        }
        onDispose { mainActivity?.onUserLeaveAction = null }
    }

    val desiredOrientation = when {
        isInPip -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        videoAspect > 0f && videoAspect <= PORTRAIT_ASPECT_MAX && (autoRotateEnabled || isFullscreen) ->
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

        isFullscreen -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        autoRotateEnabled && videoAspect >= LANDSCAPE_ASPECT_MIN ->
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    DisposableEffect(desiredOrientation, activity) {
        activity?.requestedOrientation = desiredOrientation
        onDispose { }
    }

    DisposableEffect(isFullscreen, isInPip, activity) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            if (isFullscreen && !isInPip) {
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
            val window = activity?.window ?: return@onDispose
            WindowInsetsControllerCompat(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler { if (isFullscreen) isFullscreen = false else onClose() }

    if (showSettings) {
        PlayerSettingsSheet(
            currentSpeed = speed,
            resizeMode = resizeMode,
            autoNextEnabled = autoNextEnabled,
            autoRotateEnabled = autoRotateEnabled,
            gesturesEnabled = gesturesEnabled,
            showEpisodeOptions = canNavigateEpisodes,
            onSpeedChange = { value ->
                speed = value
                player.playbackParameters = PlaybackParameters(value)
            },
            onResizeModeChange = { mode ->
                resizeMode = mode
                playerView?.resizeMode = mode
            },
            onAutoNextChange = { autoNextEnabled = it },
            onAutoRotateChange = { autoRotateEnabled = it },
            onGesturesChange = { gesturesEnabled = it },
            onDismiss = { showSettings = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = CONTROLLER_TIMEOUT_MS
                    keepScreenOn = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowRewindButton(true)
                    setShowFastForwardButton(true)
                    setResizeMode(resizeMode)
                    setOnTouchListener(gestures)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        }
                    )
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerView = this
                    showController()
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
                view.useController = !isInPip
                if (isInPip) view.hideController()
            },
            onRelease = { view ->
                view.setOnTouchListener(null)
                view.player = null
                playerView = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInPip && controlsVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }

                Text(
                    text = listOfNotNull(
                        activeRequest.title,
                        activeRequest.episodeLabel
                    ).joinToString("  ·  "),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                if (canNavigateEpisodes) {
                    if (isResolvingNext) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .padding(end = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { navigateEpisode(forward = true) }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardDoubleArrowRight,
                                contentDescription = stringResource(R.string.next_episode),
                                tint = Color.White
                            )
                        }
                    }
                }

                IconButton(onClick = { cycleResizeMode() }) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = stringResource(R.string.resize_mode),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        if (supportsPip(context.packageManager)) {
                            enterPip(mainActivity, player)
                        } else {
                            Toast.makeText(context, pipUnavailable, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = stringResource(R.string.pip),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isFullscreen = !isFullscreen
                        showControls()
                    }
                ) {
                    Icon(
                        imageVector = if (isFullscreen) {
                            Icons.Default.FullscreenExit
                        } else {
                            Icons.Default.Fullscreen
                        },
                        contentDescription = if (isFullscreen) {
                            stringResource(R.string.exit_fullscreen)
                        } else {
                            stringResource(R.string.fullscreen)
                        },
                        tint = Color.White
                    )
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = Color.White
                    )
                }
            }
        }

        val hud = hudState
        if (hud != null && !isInPip) {
            PlayerGestureHud(
                kind = hud.kind,
                value = hud.value,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val hint = seekHint
        if (hint != null && !isInPip && hud == null) {
            Text(
                text = if (hint) {
                    stringResource(R.string.seek_forward)
                } else {
                    stringResource(R.string.seek_backward)
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(if (hint) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 36.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        val message = errorMessage
        if (message != null && !isInPip) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Button(
                        onClick = {
                            errorMessage = null
                            player.prepare()
                            player.play()
                        }
                    ) {
                        Text(stringResource(R.string.retry))
                    }

                    if (canNavigateEpisodes) {
                        Button(
                            onClick = { navigateEpisode(forward = true) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(stringResource(R.string.next_episode))
                        }
                    }
                }
            }
        }
    }
}

private fun supportsPip(packageManager: PackageManager): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

private fun enterPip(activity: MainActivity?, player: Player) {
    if (activity == null) return
    if (!supportsPip(activity.packageManager)) return

    val videoSize = player.videoSize
    val ratio = if (videoSize.width > 0 && videoSize.height > 0) {
        val raw = videoSize.width.toFloat() / videoSize.height.toFloat()
        val clamped = raw.coerceIn(0.45f, 2.35f)
        Rational((clamped * 1000).toInt(), 1000)
    } else {
        Rational(16, 9)
    }

    runCatching {
        activity.enterPictureInPictureMode(
            PictureInPictureParams.Builder().setAspectRatio(ratio).build()
        )
    }
}
