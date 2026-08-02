package com.opplayer.app.player.fakes

import com.opplayer.app.player.subtitle.LoadedSubtitle
import com.opplayer.app.player.subtitle.SubtitleFileCandidate
import com.opplayer.app.player.subtitle.SubtitleSource

/**
 * Subtitle source backed by two maps.
 *
 * Open so a test can override one method to simulate a slow or failing load.
 */
open class FakeSubtitleSource(
    var candidates: Map<String, List<SubtitleFileCandidate>> = emptyMap(),
    var loaded: Map<String, LoadedSubtitle> = emptyMap()
) : SubtitleSource {

    override suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate> =
        candidates[videoUri].orEmpty()

    override suspend fun load(uri: String): LoadedSubtitle? = loaded[uri]
}
