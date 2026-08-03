package com.opplayer.app.ui.player

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.exoplayer.ExoPlayer
import com.opplayer.app.R
import com.opplayer.app.player.DefaultPlayerFactory
import com.opplayer.app.player.EpisodeNavigator
import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeResolver
import com.opplayer.app.player.EpisodeTarget
import com.opplayer.app.player.ExoPlayerEngine
import com.opplayer.app.player.NetworkEpisodeResolver
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlaybackStatus
import com.opplayer.app.player.PlayerEngine
import com.opplayer.app.player.PlayerEngineListener
import com.opplayer.app.player.PlayerFactory
import com.opplayer.app.player.PlayerMessage
import com.opplayer.app.player.ProgressSaver
import com.opplayer.app.player.VideoScaleMode
import com.opplayer.app.player.subtitle.AndroidSubtitleSource
import com.opplayer.app.player.subtitle.EmbeddedTrackInfo
import com.opplayer.app.player.subtitle.SubtitleChoice
import com.opplayer.app.player.subtitle.SubtitleController
import com.opplayer.app.player.subtitle.SubtitleSource
import com.opplayer.app.player.subtitle.SubtitleUiState
import com.opplayer.app.player.supportsEpisodeNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    initialRequest: PlaybackRequest,
    engine: PlayerEngine,
    subtitleSource: SubtitleSource,
    episodeResolver: EpisodeResolver = NetworkEpisodeResolver.Default,
    private val progressManager: ProgressManager = ProgressManager(),
    episodeTimeoutMs: Long = EpisodeController.DEFAULT_TIMEOUT_MS
) : ViewModel() {

    constructor(
        application: Application,
        initialRequest: PlaybackRequest,
        playerFactory: PlayerFactory = DefaultPlayerFactory,
        episodeResolver: EpisodeResolver = NetworkEpisodeResolver.Default
    ) : this(
        initialRequest = initialRequest,
        engine = playerFactory.create(application),
        subtitleSource = AndroidSubtitleSource(application),
        episodeResolver = episodeResolver
    )

    private val playbackController = PlaybackController(engine)
    private val episodeController = EpisodeController(episodeResolver, episodeTimeoutMs)

    val player: ExoPlayer? = (engine as? ExoPlayerEngine)?.exoPlayer

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            request = initialRequest,
            status = PlaybackStatus.Preparing,
            canNavigateEpisodes = initialRequest.supportsEpisodeNavigation()
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val messageChannel = Channel<PlayerMessage>(Channel.BUFFERED)

    val messages: Flow<PlayerMessage> = messageChannel.receiveAsFlow()

    private val positionMs = MutableStateFlow(0L)
    private val isPlaying = MutableStateFlow(false)

    private val subtitleController = SubtitleController(
        scope = viewModelScope,
        engine = engine,
        source = subtitleSource,
        positionMs = positionMs,
        onMessage = ::send
    )

    val subtitleState: StateFlow<SubtitleUiState> = subtitleController.state

    val subtitleText: StateFlow<String?> = subtitleController.text

    private var episodeJob: Job? = null
    private var released = false

    private val engineListener = object : PlayerEngineListener {

        override fun onStatusChanged(status: PlaybackStatus) {
            _uiState.update { it.copy(status = status) }
            syncPosition()
            if (status == PlaybackStatus.Ended) onPlaybackEnded()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@PlayerViewModel.isPlaying.value = isPlaying
            syncPosition()
        }

        override fun onPositionDiscontinuity(isSeek: Boolean) {
            syncPosition()
            if (isSeek) subtitleController.onSeek()
        }

        override fun onVideoAspectChanged(aspect: Float) {
            _uiState.update { it.copy(videoAspect = aspect) }
        }

        override fun onEmbeddedCue(atMs: Long, text: String?) {
            subtitleController.onCue(atMs, text)
        }

        override fun onEmbeddedTracksChanged(tracks: List<EmbeddedTrackInfo>) {
            subtitleController.onTracks(tracks)
        }
    }

    init {
        playbackController.setListener(engineListener)
        startPositionTicker()
        startProgressAutoSave()
        open(initialRequest)
    }

    fun setProgressSaver(saver: (PlaybackRequest, Long) -> Unit) {
        progressManager.setSaver(ProgressSaver { request, position -> saver(request, position) })
    }

    fun clearProgressSaver() {
        progressManager.clearSaver()
    }

    fun onRequest(request: PlaybackRequest) {
        if (request.isSameItemAs(_uiState.value.request)) return
        saveProgress()
        open(request)
    }

    fun setSpeed(value: Float) {
        _uiState.update { it.copy(speed = value) }
        playbackController.setSpeed(value)
    }

    fun setScaleMode(mode: VideoScaleMode) {
        _uiState.update { it.copy(scaleMode = mode) }
    }

    fun cycleScaleMode(): VideoScaleMode {
        val next = _uiState.value.scaleMode.next()
        setScaleMode(next)
        return next
    }

    fun setFullscreen(value: Boolean) {
        _uiState.update { it.copy(isFullscreen = value) }
    }

    fun toggleFullscreen() = setFullscreen(!_uiState.value.isFullscreen)

    fun setAutoNextEnabled(value: Boolean) {
        _uiState.update { it.copy(autoNextEnabled = value) }
    }

    fun setAutoRotateEnabled(value: Boolean) {
        _uiState.update { it.copy(autoRotateEnabled = value) }
    }

    fun setGesturesEnabled(value: Boolean) {
        _uiState.update { it.copy(gesturesEnabled = value) }
    }

    fun playPause() = playbackController.togglePlayPause()

    fun pauseForBackground() {
        playbackController.pause()
        saveProgress()
    }

    fun seekBy(forward: Boolean) {
        playbackController.seekBy(if (forward) SEEK_STEP_MS else -SEEK_STEP_MS)
        syncPosition()
        saveProgress()
    }

    fun retry() {
        _uiState.update { it.copy(status = PlaybackStatus.Preparing) }
        playbackController.retry()
    }

    fun saveProgress() {
        progressManager.save(
            request = _uiState.value.request,
            positionMs = playbackController.currentPosition,
            durationMs = playbackController.duration
        )
    }

    fun navigateEpisode(forward: Boolean) {
        val state = _uiState.value
        if (state.isResolvingEpisode) return

        if (!state.canNavigateEpisodes) {
            send(PlayerMessage(R.string.episode_pattern_missing))
            return
        }

        val request = state.request
        _uiState.update { it.copy(isResolvingEpisode = true) }

        episodeJob?.cancel()
        episodeJob = viewModelScope.launch {
            saveProgress()

            val result = episodeController.resolve(request, forward)
            _uiState.update { it.copy(isResolvingEpisode = false) }

            when (result) {
                is EpisodeResolutionResult.Found -> playEpisode(request, result.target)

                EpisodeResolutionResult.NotFound ->
                    send(PlayerMessage(R.string.next_episode_not_found, long = true))

                EpisodeResolutionResult.Timeout ->
                    send(PlayerMessage(R.string.episode_resolve_timeout, long = true))

                EpisodeResolutionResult.NetworkUnavailable ->
                    send(PlayerMessage(R.string.episode_network_unavailable, long = true))
            }
        }
    }

    fun onSubtitleChoice(choice: SubtitleChoice) = subtitleController.onChoice(choice)

    fun onSubtitleFilePicked(uri: String) = subtitleController.onFilePicked(uri)

    fun setSubtitleOffset(offsetMs: Long) {
        subtitleController.setOffset(offsetMs)
        syncPosition()
    }

    override fun onCleared() {
        release()
        super.onCleared()
    }

    @VisibleForTesting
    fun releaseResources() = release()

    private fun release() {
        if (released) return
        released = true

        saveProgress()
        progressManager.clearSaver()
        episodeJob?.cancel()
        subtitleController.release()
        playbackController.release()
    }

    private fun open(request: PlaybackRequest) {
        episodeJob?.cancel()
        positionMs.value = request.startPositionMs.coerceAtLeast(0L)

        _uiState.update { state ->
            state.copy(
                request = request,
                status = PlaybackStatus.Preparing,
                videoAspect = 0f,
                isResolvingEpisode = false,
                canNavigateEpisodes = request.supportsEpisodeNavigation()
            )
        }

        subtitleController.onOpen(request)

        playbackController.prepare(request, request.startPositionMs)
        playbackController.setSpeed(_uiState.value.speed)
    }

    private fun playEpisode(current: PlaybackRequest, target: EpisodeTarget) {
        val next = current.copy(
            uri = target.url,
            subtitleUrl = nextSubtitleUrl(current, target),
            startPositionMs = 0L,
            pattern = target.pattern,
            episodeLabel = target.label
        )

        progressManager.saveExact(next, 0L)
        send(PlayerMessage(R.string.now_playing_episode, argument = target.label))
        open(next)
    }

    private fun nextSubtitleUrl(current: PlaybackRequest, target: EpisodeTarget): String? {
        val subtitle = current.subtitleUrl?.takeIf { it.isNotBlank() } ?: return null
        if (!subtitle.startsWith("http", ignoreCase = true)) return null

        val subtitleMarker = EpisodeNavigator.findMarker(subtitle) ?: return null
        val targetMarker = EpisodeNavigator.findMarker(target.url) ?: return null

        return EpisodeNavigator.buildUrl(
            url = subtitle,
            marker = subtitleMarker,
            season = targetMarker.seasonValue,
            episode = targetMarker.episodeValue
        )
    }

    private fun onPlaybackEnded() {
        saveProgress()

        val state = _uiState.value
        if (!state.autoNextEnabled || !state.canNavigateEpisodes) return
        navigateEpisode(forward = true)
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            isPlaying.collectLatest { playing ->
                if (!playing) return@collectLatest
                while (currentCoroutineContext().isActive) {
                    syncPosition()
                    delay(POSITION_TICK_MS)
                }
            }
        }
    }

    private fun startProgressAutoSave() {
        viewModelScope.launch {
            isPlaying.drop(1).collectLatest { playing ->
                if (!playing) {
                    saveProgress()
                    return@collectLatest
                }

                while (currentCoroutineContext().isActive) {
                    delay(PROGRESS_SAVE_INTERVAL_MS)
                    saveProgress()
                }
            }
        }
    }

    private fun syncPosition() {
        positionMs.value = playbackController.currentPosition
    }

    private fun send(message: PlayerMessage) {
        messageChannel.trySend(message)
    }

    companion object {
        const val SEEK_STEP_MS = 15_000L
        const val SUBTITLE_OFFSET_LIMIT_MS = SubtitleController.OFFSET_LIMIT_MS

        private const val POSITION_TICK_MS = 100L
        private const val PROGRESS_SAVE_INTERVAL_MS = 30_000L

        fun factory(
            application: Application,
            request: PlaybackRequest
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { PlayerViewModel(application, request) }
        }
    }
}
