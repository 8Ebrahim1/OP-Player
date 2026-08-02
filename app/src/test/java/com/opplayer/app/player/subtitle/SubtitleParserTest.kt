package com.opplayer.app.player.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleParserTest {

    @Test
    fun `parses srt with crlf and html tags`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:03,500\r\n<i>Hello</i> world\r\n\r\n" +
            "2\r\n00:00:04,000 --> 00:00:06,000\r\nSecond line\r\n"

        val cues = SubtitleParser.parse("movie.fa.srt", srt.toByteArray(Charsets.UTF_8))

        assertEquals(2, cues.size)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals(3_500L, cues[0].endMs)
        assertEquals("Hello world", cues[0].text)
        assertEquals(4_000L, cues[1].startMs)
    }

    @Test
    fun `parses webvtt with dot separator and header`() {
        val vtt = "WEBVTT\n\n00:00:02.000 --> 00:00:04.000\nfirst\n\n" +
            "00:01:00.000 --> 00:01:02.000\nsecond\n"

        val cues = SubtitleParser.parse("clip.vtt", vtt.toByteArray(Charsets.UTF_8))

        assertEquals(2, cues.size)
        assertEquals(2_000L, cues[0].startMs)
        assertEquals(60_000L, cues[1].startMs)
        assertEquals("second", cues[1].text)
    }

    @Test
    fun `parses ass dialogue lines using format order`() {
        val ass = """
            [Script Info]
            Title: sample

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,{\pos(10,10)}Salam\Ndonya
        """.trimIndent()

        val cues = SubtitleParser.parse("episode.ass", ass.toByteArray(Charsets.UTF_8))

        assertEquals(1, cues.size)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals(3_000L, cues[0].endMs)
        assertEquals("Salam\ndonya", cues[0].text)
    }

    @Test
    fun `decodes windows-1256 persian subtitle without crashing`() {
        val bytes = byteArrayOf(
            0x31, 0x0D, 0x0A,
            0x30, 0x30, 0x3A, 0x30, 0x30, 0x3A, 0x30, 0x31, 0x2C, 0x30, 0x30, 0x30,
            0x20, 0x2D, 0x2D, 0x3E, 0x20,
            0x30, 0x30, 0x3A, 0x30, 0x30, 0x3A, 0x30, 0x32, 0x2C, 0x30, 0x30, 0x30,
            0x0D, 0x0A,
            0xD3.toByte(), 0xE1.toByte(), 0xC7.toByte(), 0xE3.toByte(),
            0x0D, 0x0A
        )

        val cues = SubtitleParser.parse("fa.srt", bytes)

        assertEquals(1, cues.size)
        assertTrue(cues[0].text.isNotBlank())
    }
}
