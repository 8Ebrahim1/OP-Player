package com.opplayer.app.player.fakes

import com.opplayer.app.player.subtitle.LoadedSubtitle
import com.opplayer.app.player.subtitle.SubtitleFileCandidate
import com.opplayer.app.player.subtitle.SubtitleSource

open class FakeSubtitleSource(
    var candidates: Map<String, List<SubtitleFileCandidate>> = emptyMap(),
    var loaded: Map<String, LoadedSubtitle> = emptyMap()
) : SubtitleSource {

    override suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate> =
        candidates[videoUri].orEmpty()

    override suspend fun load(uri: String): LoadedSubtitle? = loaded[uri]
}
