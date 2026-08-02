package com.opplayer.app.player

import android.net.Uri
import com.opplayer.app.data.EpisodePattern

/**
 * Reads and rewrites the season/episode marker inside a media URL.
 *
 * Availability checks are delegated to an [AvailabilityProbe], so the parsing
 * rules here stay pure and unit testable, and a network failure keeps its own
 * identity all the way up to the UI instead of collapsing into "not found".
 */
object EpisodeNavigator {

    private const val MAX_SEASON_LOOKAHEAD = 1

    /** How far back the first episode of a season looks into the previous one. */
    const val MAX_PREVIOUS_SEASON_EPISODE = 24

    private val defaultProbe: AvailabilityProbe by lazy { HttpAvailabilityProbe() }

    data class Marker(
        val seasonRange: IntRange?,
        val seasonValue: Int?,
        val seasonPad: Int,
        val episodeRange: IntRange,
        val episodeValue: Int,
        val episodePad: Int
    )

    data class Candidate(
        val url: String,
        val season: Int?,
        val episode: Int
    )

    /** Outcome of walking a list of candidates. */
    sealed interface Resolution {
        data class Found(val candidate: Candidate) : Resolution
        data object NotFound : Resolution
        data object NetworkUnavailable : Resolution
    }

    // S01E02 / s1.e2 / S01 - E02
    private val seasonEpisodeRegex = Regex("""[Ss](\d{1,3})[._\-\s]{0,4}[Ee](\d{1,3})""")

    // Episode 07 / episode.7
    private val episodeWordRegex = Regex("""(?i)episode[._\-\s]{0,3}(\d{1,3})""")

    private val episodeShortRegex =
        Regex("""(?<![A-Za-z0-9])[Ee][Pp]?[._\-\s]{0,2}(\d{1,3})(?![0-9])""")

    fun hasMarker(url: String): Boolean = findMarker(url) != null

    fun findMarker(url: String): Marker? {
        val queryStart = url.indexOfFirst { it == '?' || it == '#' }
        val pathPart = if (queryStart >= 0) url.substring(0, queryStart) else url
        val fileStart = pathPart.lastIndexOf('/') + 1
        if (fileStart >= pathPart.length) return null

        val fileName = pathPart.substring(fileStart)

        seasonEpisodeRegex.findAll(fileName).lastOrNull()?.let { match ->
            val season = match.groupValues[1]
            val episode = match.groupValues[2]
            val seasonGroup = match.groups[1] ?: return@let
            val episodeGroup = match.groups[2] ?: return@let

            return Marker(
                seasonRange = seasonGroup.range.shift(fileStart),
                seasonValue = season.toIntOrNull(),
                seasonPad = season.length,
                episodeRange = episodeGroup.range.shift(fileStart),
                episodeValue = episode.toIntOrNull() ?: return@let,
                episodePad = episode.length
            )
        }

        val fallback = episodeWordRegex.findAll(fileName).lastOrNull()
            ?: episodeShortRegex.findAll(fileName).lastOrNull()

        val group = fallback?.groups?.get(1) ?: return null
        val value = group.value.toIntOrNull() ?: return null

        return Marker(
            seasonRange = null,
            seasonValue = null,
            seasonPad = 0,
            episodeRange = group.range.shift(fileStart),
            episodeValue = value,
            episodePad = group.value.length
        )
    }

    fun buildUrl(url: String, marker: Marker, season: Int?, episode: Int): String {
        val edits = mutableListOf<Pair<IntRange, String>>()
        edits += marker.episodeRange to episode.pad(marker.episodePad)

        if (season != null && marker.seasonRange != null) {
            edits += marker.seasonRange to season.pad(marker.seasonPad)
        }

        var result = url
        edits.sortedByDescending { it.first.first }.forEach { (range, value) ->
            result = result.substring(0, range.first) + value + result.substring(range.last + 1)
        }
        return result
    }

    fun nextCandidates(url: String): List<Candidate> {
        val marker = findMarker(url) ?: return emptyList()
        val candidates = mutableListOf<Candidate>()

        candidates += Candidate(
            url = buildUrl(url, marker, null, marker.episodeValue + 1),
            season = marker.seasonValue,
            episode = marker.episodeValue + 1
        )

        val season = marker.seasonValue
        if (season != null) {
            for (step in 1..MAX_SEASON_LOOKAHEAD) {
                candidates += Candidate(
                    url = buildUrl(url, marker, season + step, 1),
                    season = season + step,
                    episode = 1
                )
            }
        }

        return candidates
    }

    /**
     * Candidates for the previous episode.
     *
     * The first episode of a season is no longer a dead end: S02E01 walks back
     * into the last episode of season one, trying the highest plausible number
     * first. The list is bounded by [MAX_PREVIOUS_SEASON_EPISODE] and probing
     * stops at the first hit.
     */
    fun previousCandidates(url: String): List<Candidate> {
        val marker = findMarker(url) ?: return emptyList()

        if (marker.episodeValue > 1) {
            return listOf(
                Candidate(
                    url = buildUrl(url, marker, null, marker.episodeValue - 1),
                    season = marker.seasonValue,
                    episode = marker.episodeValue - 1
                )
            )
        }

        val season = marker.seasonValue ?: return emptyList()
        if (season <= 1 || marker.seasonRange == null) return emptyList()

        val previousSeason = season - 1
        return (MAX_PREVIOUS_SEASON_EPISODE downTo 1).map { episode ->
            Candidate(
                url = buildUrl(url, marker, previousSeason, episode),
                season = previousSeason,
                episode = episode
            )
        }
    }

    suspend fun resolveNext(
        url: String,
        probe: AvailabilityProbe = defaultProbe
    ): Resolution = resolveFirstAvailable(nextCandidates(url), probe)

    suspend fun resolvePrevious(
        url: String,
        probe: AvailabilityProbe = defaultProbe
    ): Resolution = resolveFirstAvailable(previousCandidates(url), probe)

    /**
     * Probes candidates in order.
     *
     * A network failure is remembered and reported only when no candidate could
     * be confirmed, so one flaky URL does not mask a working one.
     */
    private suspend fun resolveFirstAvailable(
        candidates: List<Candidate>,
        probe: AvailabilityProbe
    ): Resolution {
        var offline = false

        for (candidate in candidates) {
            when (probe.probe(candidate.url)) {
                AvailabilityResult.Available -> return Resolution.Found(candidate)
                AvailabilityResult.NetworkUnavailable -> offline = true
                AvailabilityResult.NotAvailable -> Unit
            }
        }

        return if (offline) Resolution.NetworkUnavailable else Resolution.NotFound
    }

    suspend fun isAvailable(url: String, probe: AvailabilityProbe = defaultProbe): Boolean =
        probe.probe(url) == AvailabilityResult.Available

    /** Resolves the neighbouring episode of a stored [EpisodePattern]. */
    suspend fun resolvePattern(
        pattern: EpisodePattern,
        forward: Boolean,
        probe: AvailabilityProbe = defaultProbe
    ): PatternResolution {
        val candidate = (if (forward) pattern.next() else pattern.previous())
            ?: return PatternResolution.NotFound

        return when (probe.probe(candidate.url)) {
            AvailabilityResult.Available -> PatternResolution.Found(candidate)
            AvailabilityResult.NetworkUnavailable -> PatternResolution.NetworkUnavailable
            AvailabilityResult.NotAvailable -> PatternResolution.NotFound
        }
    }

    /** Outcome of [resolvePattern]. */
    sealed interface PatternResolution {
        data class Found(val pattern: EpisodePattern) : PatternResolution
        data object NotFound : PatternResolution
        data object NetworkUnavailable : PatternResolution
    }

    fun displayName(url: String): String {
        val queryStart = url.indexOfFirst { it == '?' || it == '#' }
        val pathPart = if (queryStart >= 0) url.substring(0, queryStart) else url
        val raw = pathPart.substringAfterLast('/')
        val decoded = runCatching { Uri.decode(raw) }.getOrDefault(raw)
        return decoded.substringBeforeLast('.').ifBlank { decoded }
    }

    fun label(candidate: Candidate): String {
        val episode = candidate.episode.pad(2)
        return candidate.season
            ?.let { "S${it.pad(2)}E$episode" }
            ?: "E$episode"
    }

    private fun IntRange.shift(offset: Int): IntRange = IntRange(first + offset, last + offset)

    private fun Int.pad(width: Int): String = toString().padStart(width.coerceAtLeast(1), '0')
}
