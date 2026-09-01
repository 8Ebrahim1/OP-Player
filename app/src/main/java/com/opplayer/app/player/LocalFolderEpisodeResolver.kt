package com.opplayer.app.player

import android.content.Context
import com.opplayer.app.data.LocalVideoRepository
import com.opplayer.app.data.VideoFolder

/**
 * Walks the folder a device video was opened from, so the next/previous control also works
 * for offline files where there is no episode pattern to probe.
 */
class LocalFolderEpisodeResolver(
    private val loadFolders: suspend () -> List<VideoFolder>
) : EpisodeResolver {

    override suspend fun resolve(
        request: PlaybackRequest,
        forward: Boolean
    ): EpisodeResolutionResult {
        val folderId = request.folderId ?: return EpisodeResolutionResult.NotFound

        val folder = runCatching { loadFolders() }
            .getOrNull()
            ?.firstOrNull { it.id == folderId }
            ?: return EpisodeResolutionResult.NotFound

        val index = folder.videos.indexOfFirst { it.uri == request.uri }
        if (index < 0) return EpisodeResolutionResult.NotFound

        val target = folder.videos.getOrNull(if (forward) index + 1 else index - 1)
            ?: return EpisodeResolutionResult.NotFound

        return EpisodeResolutionResult.Found(EpisodeTarget(url = target.uri, label = target.name))
    }
}

/** Device videos are resolved from the media store, links keep probing the server. */
class SourceAwareEpisodeResolver(
    private val device: EpisodeResolver,
    private val network: EpisodeResolver = NetworkEpisodeResolver.Default
) : EpisodeResolver {

    override suspend fun resolve(
        request: PlaybackRequest,
        forward: Boolean
    ): EpisodeResolutionResult = when (request.source) {
        PlaybackRequest.Source.DEVICE -> device.resolve(request, forward)
        PlaybackRequest.Source.LIBRARY -> network.resolve(request, forward)
    }

    companion object {

        fun create(context: Context): EpisodeResolver {
            val repository = LocalVideoRepository(context.applicationContext)
            return SourceAwareEpisodeResolver(
                device = LocalFolderEpisodeResolver { repository.loadFolders() }
            )
        }
    }
}
