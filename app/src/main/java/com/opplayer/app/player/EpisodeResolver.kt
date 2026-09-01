package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern
import java.io.IOException

data class EpisodeTarget(
    val url: String,
    val label: String,
    val pattern: EpisodePattern? = null
)

sealed interface EpisodeResolutionResult {
    data class Found(val target: EpisodeTarget) : EpisodeResolutionResult
    data object NotFound : EpisodeResolutionResult
    data object Timeout : EpisodeResolutionResult
    data object NetworkUnavailable : EpisodeResolutionResult
}

interface EpisodeResolver {
    suspend fun resolve(request: PlaybackRequest, forward: Boolean): EpisodeResolutionResult
}

fun PlaybackRequest.supportsEpisodeNavigation(): Boolean {
    if (source == PlaybackRequest.Source.DEVICE) return folderId != null
    if (pattern != null) return true
    if (!uri.startsWith("http", ignoreCase = true)) return false
    return EpisodeNavigator.hasMarker(uri)
}

class NetworkEpisodeResolver(
    private val probe: AvailabilityProbe = HttpAvailabilityProbe()
) : EpisodeResolver {

    override suspend fun resolve(
        request: PlaybackRequest,
        forward: Boolean
    ): EpisodeResolutionResult = try {
        val pattern = request.pattern

        if (pattern != null) {
            when (val result = EpisodeNavigator.resolvePattern(pattern, forward, probe)) {
                is EpisodeNavigator.PatternResolution.Found -> EpisodeResolutionResult.Found(
                    EpisodeTarget(
                        url = result.pattern.url,
                        label = result.pattern.label(),
                        pattern = result.pattern
                    )
                )

                EpisodeNavigator.PatternResolution.NetworkUnavailable ->
                    EpisodeResolutionResult.NetworkUnavailable

                EpisodeNavigator.PatternResolution.NotFound -> EpisodeResolutionResult.NotFound
            }
        } else {
            val result = if (forward) {
                EpisodeNavigator.resolveNext(request.uri, probe)
            } else {
                EpisodeNavigator.resolvePrevious(request.uri, probe)
            }

            when (result) {
                is EpisodeNavigator.Resolution.Found -> EpisodeResolutionResult.Found(
                    EpisodeTarget(
                        url = result.candidate.url,
                        label = EpisodeNavigator.label(result.candidate)
                    )
                )

                EpisodeNavigator.Resolution.NetworkUnavailable ->
                    EpisodeResolutionResult.NetworkUnavailable

                EpisodeNavigator.Resolution.NotFound -> EpisodeResolutionResult.NotFound
            }
        }
    } catch (error: IOException) {

        EpisodeResolutionResult.NetworkUnavailable
    }

    companion object {

        val Default: NetworkEpisodeResolver by lazy { NetworkEpisodeResolver() }
    }
}
