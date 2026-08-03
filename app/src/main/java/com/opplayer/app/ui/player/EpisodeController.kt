package com.opplayer.app.ui.player

import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeResolver
import com.opplayer.app.player.PlaybackRequest
import com.opplayer.app.player.supportsEpisodeNavigation
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

class EpisodeController(
    private val resolver: EpisodeResolver,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) {

    fun supports(request: PlaybackRequest): Boolean = request.supportsEpisodeNavigation()

    suspend fun resolve(request: PlaybackRequest, forward: Boolean): EpisodeResolutionResult {
        if (!supports(request)) return EpisodeResolutionResult.NotFound

        return try {
            withTimeoutOrNull(timeoutMs) {
                resolver.resolve(request, forward)
            } ?: EpisodeResolutionResult.Timeout
        } catch (error: IOException) {
            EpisodeResolutionResult.NetworkUnavailable
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 8_000L
    }
}
