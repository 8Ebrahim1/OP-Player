package com.opplayer.app.player.fakes

import com.opplayer.app.player.EpisodeResolutionResult
import com.opplayer.app.player.EpisodeResolver
import com.opplayer.app.player.PlaybackRequest

class FakeEpisodeResolver(
    var result: EpisodeResolutionResult = EpisodeResolutionResult.NotFound,
    private val beforeReturn: suspend () -> Unit = {}
) : EpisodeResolver {

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
