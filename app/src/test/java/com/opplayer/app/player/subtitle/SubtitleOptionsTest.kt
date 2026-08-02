package com.opplayer.app.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleOptionsTest {

    private fun build(
        disabled: Boolean = false,
        usingExternal: Boolean = false,
        externalName: String? = null,
        externalUri: String? = null,
        candidates: List<SubtitleFileCandidate> = emptyList(),
        embeddedTracks: List<EmbeddedTrackInfo> = emptyList()
    ): List<SubtitleOptionItem> = SubtitleOptions.build(
        disabled = disabled,
        usingExternal = usingExternal,
        externalName = externalName,
        externalUri = externalUri,
        candidates = candidates,
        embeddedTracks = embeddedTracks,
        offLabel = "Off",
        embeddedLabel = "Track"
    )

    @Test
    fun `off is always the first option`() {
        val options = build()

        assertEquals(SubtitleOptions.ID_OFF, options.first().id)
        assertEquals("Off", options.first().label)
    }

    @Test
    fun `off is selected only when subtitles are disabled`() {
        assertFalse(build().first().selected)
        assertTrue(build(disabled = true).first().selected)
    }

    @Test
    fun `the loaded subtitle is offered when it exists`() {
        val options = build(
            usingExternal = true,
            externalName = "movie.srt",
            externalUri = "content://sub"
        )

        val current = options.single { it.id == SubtitleOptions.ID_CURRENT }
        assertEquals("movie.srt", current.label)
        assertTrue(current.selected)
    }

    @Test
    fun `the loaded subtitle is not selected while subtitles are off`() {
        val options = build(
            disabled = true,
            usingExternal = true,
            externalName = "movie.srt"
        )

        assertFalse(options.single { it.id == SubtitleOptions.ID_CURRENT }.selected)
    }

    @Test
    fun `no current row without a loaded subtitle`() {
        assertTrue(build().none { it.id == SubtitleOptions.ID_CURRENT })
    }

    @Test
    fun `the already loaded file is not listed twice`() {
        val options = build(
            usingExternal = true,
            externalName = "movie.srt",
            externalUri = "content://sub/1",
            candidates = listOf(
                SubtitleFileCandidate(uri = "content://sub/1", name = "movie.srt"),
                SubtitleFileCandidate(uri = "content://sub/2", name = "movie.fa.srt")
            )
        )

        val files = options.filter { it.id.startsWith(SubtitleOptions.PREFIX_FILE) }
        assertEquals(1, files.size)
        assertEquals(SubtitleOptions.PREFIX_FILE + "1", files.single().id)
        assertEquals("movie.fa.srt", files.single().label)
    }

    @Test
    fun `embedded tracks are labelled with their language`() {
        val options = build(
            embeddedTracks = listOf(
                EmbeddedTrackInfo(index = 0, language = "en", selected = true),
                EmbeddedTrackInfo(index = 1, language = null, selected = false)
            )
        )

        val tracks = options.filter { it.id.startsWith(SubtitleOptions.PREFIX_TRACK) }
        assertEquals(listOf("Track 1 (en)", "Track 2"), tracks.map { it.label })
        assertEquals(
            listOf(SubtitleOptions.PREFIX_TRACK + "0", SubtitleOptions.PREFIX_TRACK + "1"),
            tracks.map { it.id }
        )
    }

    @Test
    fun `an embedded track is not selected while an external file is used`() {
        val tracks = listOf(EmbeddedTrackInfo(index = 0, language = "en", selected = true))

        assertTrue(build(embeddedTracks = tracks).last().selected)
        assertFalse(
            build(usingExternal = true, externalName = "movie.srt", embeddedTracks = tracks)
                .last().selected
        )
        assertFalse(build(disabled = true, embeddedTracks = tracks).last().selected)
    }

    @Test
    fun `every generated id can be parsed back`() {
        val options = build(
            usingExternal = true,
            externalName = "movie.srt",
            candidates = listOf(SubtitleFileCandidate(uri = "content://sub/2", name = "fa.srt")),
            embeddedTracks = listOf(EmbeddedTrackInfo(index = 3, language = "fa", selected = false))
        )

        assertTrue(options.all { SubtitleOptions.parse(it.id) != null })
        assertEquals(SubtitleChoice.Off, SubtitleOptions.parse(SubtitleOptions.ID_OFF))
        assertEquals(
            SubtitleChoice.CurrentExternal,
            SubtitleOptions.parse(SubtitleOptions.ID_CURRENT)
        )
        assertEquals(
            SubtitleChoice.ExternalFile(0),
            SubtitleOptions.parse(SubtitleOptions.PREFIX_FILE + "0")
        )
        assertEquals(
            SubtitleChoice.EmbeddedTrack(3),
            SubtitleOptions.parse(SubtitleOptions.PREFIX_TRACK + "3")
        )
    }

    @Test
    fun `unknown ids are rejected`() {
        assertNull(SubtitleOptions.parse("nonsense"))
        assertNull(SubtitleOptions.parse(SubtitleOptions.PREFIX_FILE + "abc"))
        assertNull(SubtitleOptions.parse(SubtitleOptions.PREFIX_TRACK + ""))
    }
}
