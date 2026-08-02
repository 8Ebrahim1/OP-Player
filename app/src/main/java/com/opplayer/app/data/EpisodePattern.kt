package com.opplayer.app.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A numbered episode link: [prefix] + zero padded number + [suffix].
 *
 * The constructor rejects values that cannot describe a real series, most
 * importantly `step == 0`, which used to make [next] return the very same
 * episode forever. Persisted data is repaired before construction by
 * [EpisodePatternSerializer], so an old or hand edited file can never crash the
 * app while decoding.
 */
@Serializable(with = EpisodePatternSerializer::class)
@Parcelize
data class EpisodePattern(
    val prefix: String,
    val suffix: String,
    val episode: Int,
    val pad: Int = 1,
    val step: Int = 1
) : Parcelable {

    init {
        require(step > 0) { "Episode step must be positive, was $step" }
        require(episode >= 0) { "Episode must not be negative, was $episode" }
        require(pad > 0) { "Padding must be positive, was $pad" }
    }

    val url: String get() = urlFor(episode)

    fun urlFor(value: Int): String = prefix + value.toString().padStart(pad, '0') + suffix

    fun next(): EpisodePattern? {
        val value = episode + step
        return if (value in 0..MAX_EPISODE) copy(episode = value) else null
    }

    fun previous(): EpisodePattern? {
        val value = episode - step
        return if (value >= 0) copy(episode = value) else null
    }

    fun label(): String = "E" + episode.toString().padStart(2, '0')

    companion object {
        const val MAX_EPISODE = 9999

        /**
         * Builds a pattern from untrusted values, clamping them into the valid
         * range instead of throwing.
         */
        fun normalized(
            prefix: String,
            suffix: String,
            episode: Int,
            pad: Int,
            step: Int
        ): EpisodePattern = EpisodePattern(
            prefix = prefix,
            suffix = suffix,
            episode = episode.coerceIn(0, MAX_EPISODE),
            pad = pad.coerceAtLeast(1),
            step = step.coerceAtLeast(1)
        )
    }
}

@Serializable
@SerialName("EpisodePattern")
private data class EpisodePatternSurrogate(
    val prefix: String,
    val suffix: String,
    val episode: Int,
    val pad: Int = 1,
    val step: Int = 1
)

/**
 * Serializer that normalizes on the way in.
 *
 * Legacy libraries contain patterns with `step = 0` or `pad = 0`; decoding them
 * through the validating constructor would throw and take the whole library
 * with it, so the values are repaired first.
 */
object EpisodePatternSerializer : KSerializer<EpisodePattern> {

    private val delegate = EpisodePatternSurrogate.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: EpisodePattern) {
        delegate.serialize(
            encoder,
            EpisodePatternSurrogate(
                prefix = value.prefix,
                suffix = value.suffix,
                episode = value.episode,
                pad = value.pad,
                step = value.step
            )
        )
    }

    override fun deserialize(decoder: Decoder): EpisodePattern {
        val surrogate = delegate.deserialize(decoder)

        return EpisodePattern.normalized(
            prefix = surrogate.prefix,
            suffix = surrogate.suffix,
            episode = surrogate.episode,
            pad = surrogate.pad,
            step = surrogate.step
        )
    }
}
