package com.opplayer.app.player.subtitle

import android.content.Context

interface SubtitleSource {

    suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate>

    suspend fun load(uri: String): LoadedSubtitle?
}

class AndroidSubtitleSource(private val context: Context) : SubtitleSource {

    override suspend fun findCandidates(videoUri: String): List<SubtitleFileCandidate> =
        SubtitleLocator.findCandidates(context, videoUri)

    override suspend fun load(uri: String): LoadedSubtitle? =
        SubtitleLoader.load(context, uri)
}
