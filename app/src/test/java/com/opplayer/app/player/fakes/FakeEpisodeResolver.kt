package com.opplayer.app.player.fakes

import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeResolver
import com.opplayer.app.player.PlaybackRequest

/**
 * Episode resolver that answers with a canned [result].
 *
 * [beforeReturn] can suspend, which is how the timeout behaviour is exercised
 * without touching the network.
 */
class FakeEpisodeResolver(
    var result: EpisodeResolutionResult = EpisodeResolutionResult.NotFound,
    private val beforeReturn: suspend () -> Unit = {}
) : EpisodeResolver {

    /** Every call, as (request, forward). */
    val calls = mutableListOf<Pair<PlaybackRequest, Boolean>>()

    override suspend fun resolve(
        request: PlaybackRequest,
        forward: Boolean
    ): EpisodeResolutionResult {
        calls += request to forward
        beforeReturn()
        return result
    }
}
