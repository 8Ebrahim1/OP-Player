package com.opplayer.app.player.subtitle

import android.content.Context

/**
 * Discovery and loading of external subtitle files.
 *
 * `SubtitleLocator` and `SubtitleLoader` both need a `Context`, which makes them
 * unusable from a plain JVM test. This interface hides that dependency so the
 * subtitle logic can be exercised with an in-memory implementation.
 */
interface SubtitleSource {

    /** Subtitle files that sit next to [videoUri], best match first. */
    suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate>

    /** Parses a subtitle file, or returns null when it cannot be read. */
    suspend fun load(uri: String): LoadedSubtitle?
}

/** Production implementation, backed by the existing locator and loader. */
class AndroidSubtitleSource(private val context: Context) : SubtitleSource {

    override suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate> =
        SubtitleLocator.findCandidates(context, videoUri)

    override suspend fun load(uri: String): LoadedSubtitle? =
        SubtitleLoader.load(context, uri)
}
