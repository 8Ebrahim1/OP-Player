package com.opplayer.app.player

import com.opplayer.app.BuildConfig

import android.net.Uri
import com.opplayer.app.data.EpisodePattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object EpisodeNavigator {
    private val USER_AGENT: String = "OPPlayer/" + BuildConfig.VERSION_NAME + " (Android)"
    private const val TIMEOUT_MS = 8_000

    private const val MAX_SEASON_LOOKAHEAD = 1

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

    fun previousCandidates(url: String): List<Candidate> {
        val marker = findMarker(url) ?: return emptyList()
        if (marker.episodeValue <= 1) return emptyList()

        return listOf(
            Candidate(
                url = buildUrl(url, marker, null, marker.episodeValue - 1),
                season = marker.seasonValue,
                episode = marker.episodeValue - 1
            )
        )
    }

    suspend fun resolveNext(url: String): Candidate? = resolveFirstAvailable(nextCandidates(url))

    suspend fun resolvePrevious(url: String): Candidate? =
        resolveFirstAvailable(previousCandidates(url))

    private suspend fun resolveFirstAvailable(candidates: List<Candidate>): Candidate? =
        withContext(Dispatchers.IO) {
            candidates.firstOrNull { exists(it.url) }
        }

    suspend fun isAvailable(url: String): Boolean = withContext(Dispatchers.IO) { exists(url) }

    suspend fun resolvePattern(pattern: EpisodePattern, forward: Boolean): EpisodePattern? {
        val candidate = (if (forward) pattern.next() else pattern.previous()) ?: return null
        return withContext(Dispatchers.IO) { if (exists(candidate.url)) candidate else null }
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

    private fun exists(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false

        val headResult = runCatching {
            val connection = openConnection(url, "HEAD")
            val code = connection.responseCode
            connection.disconnect()
            code
        }.getOrNull() ?: return probeWithRange(url)

        return when {
            headResult in 200..299 -> true
            headResult == 403 || headResult == 405 || headResult == 501 -> probeWithRange(url)
            else -> false
        }
    }

    private fun probeWithRange(url: String): Boolean = runCatching {
        val connection = openConnection(url, "GET").apply {
            setRequestProperty("Range", "bytes=0-0")
        }
        val code = connection.responseCode
        val readable = if (code in 200..299) {
            runCatching { connection.inputStream.use { it.read() } }.isSuccess
        } else {
            false
        }
        connection.disconnect()
        readable
    }.getOrDefault(false)

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }

    private fun IntRange.shift(offset: Int): IntRange =
        IntRange(first + offset, last + offset)

    private fun Int.pad(width: Int): String =
        toString().padStart(width.coerceAtLeast(1), '0')
}
