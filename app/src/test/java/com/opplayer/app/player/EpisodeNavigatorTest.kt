package com.opplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeNavigatorTest {

    private val seasonUrl =
        "http://example.com/dl/Black.Torch.S01E02.720p.WEB-DL.x264.SoftSub.mkv"

    @Test
    fun hasMarkerDetectsSeasonEpisodePattern() {
        assertTrue(EpisodeNavigator.hasMarker(seasonUrl))
    }

    @Test
    fun hasMarkerRejectsPlainFileName() {
        assertFalse(EpisodeNavigator.hasMarker("http://example.com/dl/movie.mkv"))
    }

    @Test
    fun findMarkerReadsSeasonAndEpisodeValues() {
        val marker = EpisodeNavigator.findMarker(seasonUrl)
        assertNotNull(marker)
        assertEquals(1, marker!!.seasonValue)
        assertEquals(2, marker.episodeValue)
    }

    @Test
    fun findMarkerIgnoresDigitsOutsideTheFileName() {
        val marker = EpisodeNavigator.findMarker("http://s01e05.example.com/dl/movie.mkv")
        assertNull(marker)
    }

    @Test
    fun buildUrlKeepsZeroPadding() {
        val marker = EpisodeNavigator.findMarker(seasonUrl)!!
        val built = EpisodeNavigator.buildUrl(seasonUrl, marker, 1, 9)
        assertTrue(built.contains("S01E09"))
        assertFalse(built.contains("S01E02"))
    }

    @Test
    fun buildUrlSupportsThreeDigitEpisodes() {
        val marker = EpisodeNavigator.findMarker(seasonUrl)!!
        val built = EpisodeNavigator.buildUrl(seasonUrl, marker, 2, 12)
        assertTrue(built.contains("S02E12"))
    }

    @Test
    fun nextCandidatesOffersNextEpisodeThenNextSeason() {
        val candidates = EpisodeNavigator.nextCandidates(seasonUrl)
        assertEquals(2, candidates.size)
        assertEquals(3, candidates[0].episode)
        assertEquals(1, candidates[0].season)
        assertEquals(1, candidates[1].episode)
        assertEquals(2, candidates[1].season)
    }

    @Test
    fun previousCandidatesIsEmptyForFirstEpisode() {
        val firstEpisode =
            "http://example.com/dl/Black.Torch.S01E01.720p.WEB-DL.x264.SoftSub.mkv"
        assertTrue(EpisodeNavigator.previousCandidates(firstEpisode).isEmpty())
    }

    @Test
    fun previousCandidatesStepsOneEpisodeBack() {
        val candidates = EpisodeNavigator.previousCandidates(seasonUrl)
        assertEquals(1, candidates.size)
        assertEquals(1, candidates[0].episode)
    }

    @Test
    fun labelFormatsSeasonAndEpisode() {
        val marker = EpisodeNavigator.findMarker(seasonUrl)!!
        val candidate = EpisodeNavigator.Candidate(
            url = EpisodeNavigator.buildUrl(seasonUrl, marker, 1, 3),
            season = 1,
            episode = 3
        )
        assertEquals("S01E03", EpisodeNavigator.label(candidate))
    }

    @Test
    fun labelFormatsEpisodeOnlyWhenSeasonIsMissing() {
        val candidate = EpisodeNavigator.Candidate(
            url = "http://example.com/dl/Series.E08.mkv",
            season = null,
            episode = 8
        )
        assertEquals("E08", EpisodeNavigator.label(candidate))
    }

    @Test
    fun markerIsDetectedForEpisodeOnlyNames() {
        val marker = EpisodeNavigator.findMarker("http://example.com/dl/Series.Episode.07.mkv")
        assertNotNull(marker)
        assertEquals(7, marker!!.episodeValue)
        assertNull(marker.seasonValue)
    }

    @Test
    fun markerIsDetectedForTaggedReleaseNames() {
        val marker = EpisodeNavigator.findMarker(taggedUrl)

        assertNotNull(marker)
        assertEquals(77, marker!!.episodeValue)
        assertEquals(3, marker.episodePad)
        assertNull(marker.seasonValue)
    }

    @Test
    fun taggedReleaseNamesStepForwardAndBack() {
        val marker = EpisodeNavigator.findMarker(taggedUrl)!!

        assertTrue(
            EpisodeNavigator.buildUrl(taggedUrl, marker, null, 78).endsWith(
                "Prince of Tennis - 078.[SS][480][MixFlixTop].mkv"
            )
        )
        assertTrue(
            EpisodeNavigator.buildUrl(taggedUrl, marker, null, 76).endsWith(
                "Prince of Tennis - 076.[SS][480][MixFlixTop].mkv"
            )
        )
    }

    @Test
    fun percentEncodedNamesAreRewrittenInPlace() {
        val marker = EpisodeNavigator.findMarker(encodedUrl)

        assertNotNull(marker)
        assertEquals(77, marker!!.episodeValue)
        assertTrue(
            EpisodeNavigator.buildUrl(encodedUrl, marker, null, 78).endsWith(
                "Prince%20of%20Tennis%20-%20078.%5BSS%5D%5B480%5D%5BMixFlixTop%5D.mkv"
            )
        )
    }

    @Test
    fun resolutionAndYearTokensAreNotTreatedAsEpisodes() {
        assertNull(EpisodeNavigator.findMarker("http://example.com/dl/Interstellar 2014.mkv"))
        assertNull(EpisodeNavigator.findMarker("http://example.com/dl/Interstellar 1080.mkv"))
        assertNull(EpisodeNavigator.findMarker("http://example.com/dl/Movie.x264.mkv"))
    }

    private companion object {
        const val taggedUrl =
            "https://dl3.gamenub.ir/Anime Series/2023/the-prince-of-tennis/S1/480/" +
                "Prince of Tennis - 077.[SS][480][MixFlixTop].mkv"

        const val encodedUrl =
            "https://dl3.gamenub.ir/Anime%20Series/2023/the-prince-of-tennis/S1/480/" +
                "Prince%20of%20Tennis%20-%20077.%5BSS%5D%5B480%5D%5BMixFlixTop%5D.mkv"
    }
}
