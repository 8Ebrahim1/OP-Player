package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern
import java.io.IOException

/** A resolved episode: the URL to play, its label, and the pattern it came from. */
data class EpisodeTarget(
    val url: String,
    val label: String,
    val pattern: EpisodePattern? = null
)

/**
 * Outcome of an episode lookup.
 *
 * Previously every failure collapsed into `null`, which made "last episode",
 * "server unreachable" and "took too long" indistinguishable both for the user
 * and while debugging.
 */
sealed interface EpisodeResolutionResult {
    data class Found(val target: EpisodeTarget) : EpisodeResolutionResult
    data object NotFound : EpisodeResolutionResult
    data object Timeout : EpisodeResolutionResult
    data object NetworkUnavailable : EpisodeResolutionResult
}

/**
 * Finds the next or previous episode of the video that is playing.
 *
 * The player depends on this interface only, so tests can inject a fake instead
 * of reaching the network.
 */
interface EpisodeResolver {
    suspend fun resolve(request: PlaybackRequest, forward: Boolean): EpisodeResolutionResult
}

/** True when the request carries enough information to walk between episodes. */
fun PlaybackRequest.supportsEpisodeNavigation(): Boolean {
    if (pattern != null) return true
    if (!uri.startsWith("http", ignoreCase = true)) return false
    return EpisodeNavigator.hasMarker(uri)
}

/**
 * Default resolver: episode pattern first, then HEAD/GET probing of the URL.
 *
 * The probe is injectable so the real network path (not just a fake resolver)
 * can be covered by tests, and it returns a typed result, so "the device is
 * offline" is no longer flattened into "there is no next episode".
 */
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
        // Belt and braces: anything the probe did not classify is still a
        // network problem, never a missing episode.
        EpisodeResolutionResult.NetworkUnavailable
    }

    companion object {
        /** Shared instance for production use. */
        val Default: NetworkEpisodeResolver by lazy { NetworkEpisodeResolver() }
    }
}
