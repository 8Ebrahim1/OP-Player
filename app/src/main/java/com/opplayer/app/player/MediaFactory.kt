package com.opplayer.app.player

import com.opplayer.app.BuildConfig

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.opplayer.app.R

object MediaFactory {
    private val USER_AGENT: String = "OPPlayer/" + BuildConfig.VERSION_NAME + " (Android)"

    fun createPlayer(context: Context): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .build()
    }

    fun buildMediaItem(request: PlaybackRequest): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(request.uri)
            .setMediaId(request.key)

        adaptiveMimeType(request.uri)?.let { builder.setMimeType(it) }

        val subtitle = request.subtitleUrl?.takeIf { it.isNotBlank() }
        if (subtitle != null) {
            builder.setSubtitleConfigurations(
                listOf(

                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle))
                        .setMimeType(subtitleMimeType(subtitle))
                        .setLanguage(subtitleLanguage(subtitle))
                        .build()
                )
            )
        }

        return builder.build()
    }

    private fun adaptiveMimeType(uri: String): String? = when (extensionOf(uri)) {
        "m3u8" -> MimeTypes.APPLICATION_M3U8
        "mpd" -> MimeTypes.APPLICATION_MPD
        "ism", "isml" -> MimeTypes.APPLICATION_SS
        "mkv" -> MimeTypes.VIDEO_MATROSKA
        "webm" -> MimeTypes.VIDEO_WEBM
        "mp4", "m4v" -> MimeTypes.VIDEO_MP4
        "avi" -> MimeTypes.VIDEO_AVI
        "ts", "m2ts", "mts" -> MimeTypes.VIDEO_MP2T
        "flv" -> MimeTypes.VIDEO_FLV
        "ogv" -> MimeTypes.VIDEO_OGG
        "mp3" -> MimeTypes.AUDIO_MPEG
        "m4a", "aac" -> MimeTypes.AUDIO_AAC
        "flac" -> MimeTypes.AUDIO_FLAC
        else -> null
    }

    private fun subtitleLanguage(uri: String): String? {
        val name = uri.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val parts = name.substringBeforeLast('.', name).split('.', '_', '-')
        val tag = parts.lastOrNull()?.lowercase() ?: return null

        return tag.takeIf { it.length in 2..3 && it.all { char -> char in 'a'..'z' } }
    }

    private fun subtitleMimeType(uri: String): String = when (extensionOf(uri)) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ssa", "ass" -> MimeTypes.TEXT_SSA
        "ttml", "xml", "dfxp" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }

    private fun extensionOf(uri: String): String =
        uri.substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('.', "")
            .lowercase()

    fun errorMessageRes(error: PlaybackException): Int = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> R.string.error_network

        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> R.string.error_not_found

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> R.string.error_format

        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> R.string.error_drm

        else -> R.string.error_unknown
    }
}
