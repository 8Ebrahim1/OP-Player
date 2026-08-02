package com.opplayer.app.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import com.opplayer.app.player.subtitle.EmbeddedTrackInfo

/**
 * The production [PlayerEngine]: a thin, event translating wrapper around
 * ExoPlayer.
 *
 * Everything media3 specific lives here, which is what allows the view model
 * and its collaborators to be covered by plain JVM unit tests.
 */
class ExoPlayerEngine(val exoPlayer: ExoPlayer) : PlayerEngine {

    private var listener: PlayerEngineListener? = null
    private var textGroups: List<Tracks.Group> = emptyList()
    private var errorRes: Int? = null
    private var preparing = false
    private var released = false

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                preparing = false
            }
            emitStatus()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listener?.onIsPlayingChanged(isPlaying)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            listener?.onPositionDiscontinuity(reason == Player.DISCONTINUITY_REASON_SEEK)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            listener?.onVideoAspectChanged(
                OrientationPolicy.aspectOf(
                    width = videoSize.width,
                    height = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
                )
            )
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            errorRes = error?.let { MediaFactory.errorMessageRes(it) }
            if (error != null) preparing = false
            emitStatus()
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues
                .mapNotNull { cue -> cue.text?.toString()?.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .takeIf { it.isNotEmpty() }

            val atMs = if (cueGroup.presentationTimeUs > 0L) {
                cueGroup.presentationTimeUs / 1_000L
            } else {
                currentPosition
            }

            listener?.onEmbeddedCue(atMs, text)
        }

        override fun onTracksChanged(tracks: Tracks) {
            val groups = tracks.groups.filter { group -> group.type == C.TRACK_TYPE_TEXT }
            textGroups = groups

            listener?.onEmbeddedTracksChanged(
                groups.mapIndexed { index, group ->
                    EmbeddedTrackInfo(
                        index = index,
                        language = runCatching {
                            group.mediaTrackGroup.getFormat(0).language
                        }.getOrNull(),
                        selected = group.isSelected
                    )
                }
            )
        }
    }

    init {
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, true)
        exoPlayer.setHandleAudioBecomingNoisy(true)
        exoPlayer.addListener(playerListener)
    }

    override val currentPosition: Long
        get() = if (released) 0L else exoPlayer.currentPosition.coerceAtLeast(0L)

    override val duration: Long
        get() = if (released) 0L else exoPlayer.duration

    override val isPlaying: Boolean
        get() = !released && exoPlayer.isPlaying

    override fun setListener(listener: PlayerEngineListener?) {
        this.listener = listener
    }

    override fun prepare(request: PlaybackRequest, startPositionMs: Long) {
        if (released) return

        errorRes = null
        preparing = true
        textGroups = emptyList()

        exoPlayer.setMediaItem(MediaFactory.buildMediaItem(request))
        if (startPositionMs > 0L) exoPlayer.seekTo(startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        emitStatus()
    }

    override fun play() {
        if (!released) exoPlayer.play()
    }

    override fun pause() {
        if (!released) exoPlayer.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (released) return
        val duration = exoPlayer.duration
        val target = positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(if (duration > 0L) target.coerceAtMost(duration) else target)
    }

    override fun setSpeed(speed: Float) {
        if (!released) exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    override fun retry() {
        if (released) return

        errorRes = null
        preparing = true
        exoPlayer.prepare()
        exoPlayer.play()

        emitStatus()
    }

    override fun setTextTracksEnabled(enabled: Boolean) {
        if (released) return

        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        if (!enabled) builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        exoPlayer.trackSelectionParameters = builder
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    override fun selectEmbeddedTextTrack(index: Int) {
        if (released) return

        val builder = exoPlayer.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

        textGroups.getOrNull(index)?.let { group ->
            builder.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
        }

        exoPlayer.trackSelectionParameters = builder.build()
    }

    override fun release() {
        if (released) return

        released = true
        listener = null
        textGroups = emptyList()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    private fun emitStatus() {
        if (released) return

        val error = errorRes
        val status = when {
            error != null -> PlaybackStatus.Error(error)
            else -> when (exoPlayer.playbackState) {
                Player.STATE_IDLE -> PlaybackStatus.Idle
                Player.STATE_BUFFERING ->
                    if (preparing) PlaybackStatus.Preparing else PlaybackStatus.Buffering

                Player.STATE_READY -> PlaybackStatus.Ready
                Player.STATE_ENDED -> PlaybackStatus.Ended
                else -> PlaybackStatus.Idle
            }
        }

        listener?.onStatusChanged(status)
    }
}
