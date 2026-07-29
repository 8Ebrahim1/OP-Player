package com.opplayer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormattersTest {

    @Test
    fun zeroAndNegativeDurationsAreClamped() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:00", formatDuration(-5_000L))
    }

    @Test
    fun minutesAndSecondsUsePersianDigits() {
        assertEquals("\u06f1\u06f2:\u06f0\u06f3", formatDuration(12L * 60_000L + 3_000L))
    }

    @Test
    fun hoursAreIncludedWhenNeeded() {
        assertEquals(
            "\u06f2:\u06f0\u06f5:\u06f0\u06f9",
            formatDuration(2L * 3_600_000L + 5L * 60_000L + 9_000L)
        )
    }

    @Test
    fun emptySizeForNonPositiveValues() {
        assertEquals("", formatSize(0L))
        assertEquals("", formatSize(-1L))
    }

    @Test
    fun megabytesAreRenderedWithoutDecimals() {
        assertEquals("\u06f1\u06f2 MB", formatSize(12L * 1024L * 1024L))
    }

    @Test
    fun gigabytesKeepOneDecimal() {
        assertEquals("\u06f1.\u06f5 GB", formatSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun latinDigitConversionHandlesPersianAndArabicDigits() {
        assertEquals("2024", "\u06f2\u06f0\u06f2\u06f4".toLatinDigits())
        assertEquals("1450", "\u0661\u0664\u0665\u0660".toLatinDigits())
        assertEquals("E07", "E\u06f0\u06f7".toLatinDigits())
    }

    @Test
    fun persianDigitConversionIsReversible() {
        assertEquals("1985", "1985".toPersianDigits().toLatinDigits())
        assertEquals("\u06f7", 7.toPersianDigits())
    }

    @Test
    fun blankAndUnsupportedSchemesAreRejected() {
        assertFalse(isValidMediaUrl(""))
        assertFalse(isValidMediaUrl("   "))
        assertFalse(isValidMediaUrl("example.com/video.mkv"))
        assertFalse(isValidMediaUrl("javascript:alert(1)"))
        assertFalse(isValidMediaUrl("ftp://example.com/video.mkv"))
    }
}