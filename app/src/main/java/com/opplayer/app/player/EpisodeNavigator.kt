package com.opplayer.app.player

import android.net.Uri
import com.opplayer.app.data.EpisodePattern
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object EpisodeNavigator {

    private const val MAX_SEASON_LOOKAHEAD = 1

    const val MAX_PREVIOUS_SEASON_EPISODE = 24

    /**
     * Candidates are probed in concurrent batches instead of one by one, otherwise a
     * previous-season lookup (up to [MAX_PREVIOUS_SEASON_EPISODE] requests) can never finish
     * inside the caller's timeout budget. Four keeps the burst small enough not to look like
     * abuse to a CDN that throttles.
     */
    private const val PROBE_BATCH_SIZE = 4

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

    sealed interface Resolution {
        data class Found(val candidate: Candidate) : Resolution
        data object NotFound : Resolution
        data object NetworkUnavailable : Resolution
    }

    private val seasonEpisodeRegex = Regex("""[Ss](\d{1,3})[._\-\s]{0,4}[Ee](\d{1,3})""")

    private val episodeWordRegex = Regex("""(?i)episode[._\-\s]{0,3}(\d{1,3})""")

    private val episodeShortRegex =
        Regex("""(?<![A-Za-z0-9])[Ee][Pp]?[._\-\s]{0,2}(\d{1,3})(?![0-9])""")

    private val bareNumberRegex = Regex("""(?<![A-Za-z0-9])(\d{1,4})(?![A-Za-z0-9])""")

    private val resolutionValues =
        setOf(144, 240, 360, 480, 540, 576, 720, 1080, 1440, 2160, 4320)

    private const val TAG_MASK = ' '

    fun hasMarker(url: String): Boolean = findMarker(url) != null

    fun findMarker(url: String): Marker? {
        val queryStart = url.indexOfFirst { it == '?' || it == '#' }
        val pathPart = if (queryStart >= 0) url.substring(0, queryStart) else url
        val fileStart = pathPart.lastIndexOf('/') + 1
        if (fileStart >= pathPart.length) return null

        val name = DecodedName(pathPart.substring(fileStart))
        val fileName = name.text

        seasonEpisodeRegex.findAll(fileName).lastOrNull()?.let { match ->
            val seasonGroup = match.groups[1] ?: return@let
            val episodeGroup = match.groups[2] ?: return@let
            val episodeValue = episodeGroup.value.toIntOrNull() ?: return@let

            return Marker(
                seasonRange = name.rawRange(seasonGroup.range, fileStart),
                seasonValue = seasonGroup.value.toIntOrNull(),
                seasonPad = seasonGroup.value.length,
                episodeRange = name.rawRange(episodeGroup.range, fileStart),
                episodeValue = episodeValue,
                episodePad = episodeGroup.value.length
            )
        }

        val labelled = episodeWordRegex.findAll(fileName).lastOrNull()
            ?: episodeShortRegex.findAll(fileName).lastOrNull()

        val token = labelled?.groups?.get(1)?.let { Token(it.range, it.value) }
            ?: bareEpisodeToken(fileName)
            ?: return null

        val value = token.text.toIntOrNull() ?: return null

        return Marker(
            seasonRange = null,
            seasonValue = null,
            seasonPad = 0,
            episodeRange = name.rawRange(token.range, fileStart),
            episodeValue = value,
            episodePad = token.text.length
        )
    }

    /**
     * Long release names such as "Prince of Tennis - 077.[SS][480][MixFlixTop].mkv" carry no
     * SxxExx marker, so the trailing standalone number is the episode. Bracketed tags are masked
     * and resolution or year values skipped, otherwise "[480]" or "2023" wins instead.
     */
    private fun bareEpisodeToken(fileName: String): Token? {
        val base = maskTags(fileName.substringBeforeLast('.'))

        return bareNumberRegex.findAll(base)
            .mapNotNull { match -> match.groups[1]?.let { Token(it.range, it.value) } }
            .toList()
            .lastOrNull { !it.isNoise() }
    }

    private fun Token.isNoise(): Boolean {
        val value = text.toIntOrNull() ?: return true
        if (value in resolutionValues) return true
        return text.length == 4 && value in 1900..2099
    }

    private fun maskTags(value: String): String {
        val chars = value.toCharArray()
        var depth = 0

        chars.indices.forEach { index ->
            when (chars[index]) {
                '[', '(', '{' -> {
                    depth++
                    chars[index] = TAG_MASK
                }

                ']', ')', '}' -> {
                    if (depth > 0) depth--
                    chars[index] = TAG_MASK
                }

                else -> if (depth > 0) chars[index] = TAG_MASK
            }
        }

        return String(chars)
    }

    private data class Token(val range: IntRange, val text: String)

    /**
     * Percent-encoded names have to be matched decoded, because "%20" hides the separator in
     * front of the episode number, yet rewritten in place. Every decoded index therefore keeps a
     * pointer back into the original text.
     */
    private class DecodedName(raw: String) {

        val text: String

        private val starts: IntArray
        private val ends: IntArray

        init {
            val builder = StringBuilder(raw.length)
            val startList = ArrayList<Int>(raw.length)
            val endList = ArrayList<Int>(raw.length)

            var index = 0
            while (index < raw.length) {
                val octet = decodeOctet(raw, index)
                if (octet != null) {
                    builder.append(octet)
                    startList += index
                    endList += index + 2
                    index += 3
                } else {
                    builder.append(raw[index])
                    startList += index
                    endList += index
                    index++
                }
            }

            text = builder.toString()
            starts = startList.toIntArray()
            ends = endList.toIntArray()
        }

        fun rawRange(range: IntRange, offset: Int): IntRange {
            if (range.isEmpty() || range.first < 0 || range.last >= starts.size) {
                return IntRange(range.first + offset, range.last + offset)
            }
            return IntRange(starts[range.first] + offset, ends[range.last] + offset)
        }

        private fun decodeOctet(raw: String, index: Int): Char? {
            if (raw[index] != '%' || index + 2 >= raw.length) return null

            val high = raw[index + 1].digitToIntOrNull(16) ?: return null
            val low = raw[index + 2].digitToIntOrNull(16) ?: return null

            return ((high shl 4) or low).toChar()
        }
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

    private suspend fun resolveFirstAvailable(
        candidates: List<Candidate>,
        probe: AvailabilityProbe
    ): Resolution = coroutineScope {
        var offline = false

        candidates.chunked(PROBE_BATCH_SIZE).forEach { batch ->
            val results = batch
                .map { candidate -> async { candidate to probe.probe(candidate.url) } }
                .awaitAll()

            results.firstOrNull { (_, result) -> result == AvailabilityResult.Available }
                ?.let { (candidate, _) -> return@coroutineScope Resolution.Found(candidate) }

            if (results.any { (_, result) -> result == AvailabilityResult.NetworkUnavailable }) {
                offline = true
            }
        }

        if (offline) Resolution.NetworkUnavailable else Resolution.NotFound
    }

    suspend fun isAvailable(url: String, probe: AvailabilityProbe = defaultProbe): Boolean =
        probe.probe(url) == AvailabilityResult.Available

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

    private fun Int.pad(width: Int): String = toString().padStart(width.coerceAtLeast(1), '0')
}
