package com.opplayer.app.player.subtitle

import com.opplayer.app.R
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.PlayerEngine
import com.opplayer.app.player.PlayerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubtitleUiState(
    val disabled: Boolean = false,
    val offsetMs: Long = 0L,
    val externalName: String? = null,
    val externalUri: String? = null,
    val usingExternal: Boolean = false,
    val candidates: List<SubtitleFileCandidate> = emptyList(),
    val embeddedTracks: List<EmbeddedTrackInfo> = emptyList()
)

class SubtitleController(
    private val scope: CoroutineScope,
    private val engine: PlayerEngine,
    private val source: SubtitleSource,
    positionMs: StateFlow<Long>,
    private val onMessage: (PlayerMessage) -> Unit
) {

    private val _state = MutableStateFlow(SubtitleUiState())
    val state: StateFlow<SubtitleUiState> = _state.asStateFlow()

    private val externalCues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    private val embeddedTimeline = MutableStateFlow(EmbeddedSubtitleTimeline())

    val text: StateFlow<String?> = combine(
        positionMs,
        externalCues,
        embeddedTimeline,
        _state
    ) { position, external, embedded, subtitle ->
        when {
            subtitle.disabled -> null
            external.isNotEmpty() -> external.textAt(position, subtitle.offsetMs)
            else -> embedded.textAt(position, subtitle.offsetMs)
        }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private var activeRequest: PlaybackRequest? = null
    private var preferEmbedded = false
    private var job: Job? = null

    fun onOpen(request: PlaybackRequest) {
        job?.cancel()

        activeRequest = request
        preferEmbedded = false
        externalCues.value = emptyList()
        embeddedTimeline.value = EmbeddedSubtitleTimeline()

        _state.update { current ->
            current.copy(
                offsetMs = 0L,
                externalName = null,
                externalUri = null,
                usingExternal = false,
                candidates = emptyList(),
                embeddedTracks = emptyList()
            )
        }

        job = scope.launch { loadFor(request) }
    }

    fun onCue(atMs: Long, text: String?) {
        embeddedTimeline.update { timeline -> timeline.withCueGroup(atMs, text) }
    }

    fun onSeek() {
        embeddedTimeline.value = EmbeddedSubtitleTimeline()
    }

    fun onTracks(tracks: List<EmbeddedTrackInfo>) {
        _state.update { it.copy(embeddedTracks = tracks) }
    }

    fun onChoice(choice: SubtitleChoice) {
        when (choice) {
            SubtitleChoice.Off -> disable()

            SubtitleChoice.CurrentExternal -> {
                preferEmbedded = false
                enable()
            }

            is SubtitleChoice.ExternalFile -> {
                val candidate = _state.value.candidates.getOrNull(choice.index) ?: return
                selectExternal(candidate.uri)
            }

            is SubtitleChoice.EmbeddedTrack -> selectEmbedded(choice.index)
        }
    }

    fun onFilePicked(uri: String) = selectExternal(uri)

    fun setOffset(offsetMs: Long) {
        _state.update { it.copy(offsetMs = offsetMs.coerceIn(-OFFSET_LIMIT_MS, OFFSET_LIMIT_MS)) }
    }

    fun release() {
        job?.cancel()
        job = null
        activeRequest = null
    }

    private suspend fun loadFor(request: PlaybackRequest) {
        val candidates = runCatching { source.findCandidates(request.uri) }
            .getOrDefault(emptyList())

        if (isStale(request)) return
        _state.update { it.copy(candidates = candidates) }

        val explicit = request.subtitleUrl?.takeIf { it.isNotBlank() }
        loadExternal(request, explicit ?: candidates.firstOrNull()?.uri, explicit != null)
    }

    private suspend fun loadExternal(request: PlaybackRequest, uri: String?, explicit: Boolean) {
        if (preferEmbedded) return

        if (uri == null) {
            if (!isStale(request)) clearExternal()
            return
        }

        val loaded = runCatching { source.load(uri) }.getOrNull()

        if (isStale(request)) return

        if (loaded == null) {
            clearExternal()
            if (explicit) onMessage(PlayerMessage(R.string.subtitle_load_failed, long = true))
            return
        }

        externalCues.value = loaded.cues.sortedForPlayback()
        _state.update {
            it.copy(
                externalName = loaded.name,
                externalUri = loaded.uri,
                usingExternal = true
            )
        }

        if (!_state.value.disabled) {
            onMessage(PlayerMessage(R.string.subtitle_loaded, argument = loaded.name))
        }
    }

    private fun selectExternal(uri: String) {
        val request = activeRequest ?: return

        preferEmbedded = false
        enable()

        job?.cancel()
        job = scope.launch { loadExternal(request, uri, explicit = true) }
    }

    private fun selectEmbedded(index: Int) {
        job?.cancel()
        preferEmbedded = true
        clearExternal()
        embeddedTimeline.value = EmbeddedSubtitleTimeline()
        enable()
        engine.selectEmbeddedTextTrack(index)
    }

    private fun clearExternal() {
        externalCues.value = emptyList()
        _state.update { it.copy(externalName = null, externalUri = null, usingExternal = false) }
    }

    private fun enable() {
        _state.update { it.copy(disabled = false) }
        engine.setTextTracksEnabled(true)
    }

    private fun disable() {
        _state.update { it.copy(disabled = true) }
        engine.setTextTracksEnabled(false)
    }

    private fun isStale(request: PlaybackRequest): Boolean {
        val current = activeRequest ?: return true
        return current.key != request.key || current.uri != request.uri
    }

    companion object {
        const val OFFSET_LIMIT_MS = 60_000L
    }
}
