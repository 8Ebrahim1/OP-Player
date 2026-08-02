package com.opplayer.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormattersTest {

    @Test
    fun zeroAndNegativeDurationsAreClamped() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:00", formatDuration(-5_000L))
        assertEquals("\u06f0\u06f0:\u06f0\u06f0", formatDuration(0L, persianDigits = true))
    }

    @Test
    fun durationsUseLatinDigitsByDefault() {
        assertEquals("12:03", formatDuration(12L * 60_000L + 3_000L))
    }

    @Test
    fun durationsUsePersianDigitsWhenRequested() {
        assertEquals(
            "\u06f1\u06f2:\u06f0\u06f3",
            formatDuration(12L * 60_000L + 3_000L, persianDigits = true)
        )
    }

    @Test
    fun hoursAreIncludedWhenNeeded() {
        assertEquals("2:05:09", formatDuration(2L * 3_600_000L + 5L * 60_000L + 9_000L))
        assertEquals(
            "\u06f2:\u06f0\u06f5:\u06f0\u06f9",
            formatDuration(2L * 3_600_000L + 5L * 60_000L + 9_000L, persianDigits = true)
        )
    }

    @Test
    fun emptySizeForNonPositiveValues() {
        assertEquals("", formatSize(0L))
        assertEquals("", formatSize(-1L))
        assertEquals("", formatSize(0L, persianDigits = true))
    }

    @Test
    fun megabytesAreRenderedWithoutDecimals() {
        assertEquals("12 MB", formatSize(12L * 1024L * 1024L))
        assertEquals("\u06f1\u06f2 MB", formatSize(12L * 1024L * 1024L, persianDigits = true))
    }

    @Test
    fun gigabytesKeepOneDecimal() {
        assertEquals("1.5 GB", formatSize((1.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals(
            "\u06f1\u066b\u06f5 GB",
            formatSize((1.5 * 1024 * 1024 * 1024).toLong(), persianDigits = true)
        )
    }

    @Test
    fun countsFollowTheRequestedDigits() {
        assertEquals("7", formatCount(7))
        assertEquals("\u06f7", formatCount(7, persianDigits = true))
        assertEquals("\u06f1\u06f2\u06f0", formatCount(120, persianDigits = true))
    }

    @Test
    fun localizeDigitsAlsoMapsTheDecimalSeparator() {
        assertEquals("1.25", "1.25".localizeDigits(persian = false))
        assertEquals("\u06f1\u066b\u06f2\u06f5", "1.25".localizeDigits(persian = true))
    }

    @Test
    fun latinDigitConversionHandlesPersianAndArabicDigits() {
        assertEquals("2024", "\u06f2\u06f0\u06f2\u06f4".toLatinDigits())
        assertEquals("1450", "\u0661\u0664\u0665\u0660".toLatinDigits())
        assertEquals("E07", "E\u06f0\u06f7".toLatinDigits())
    }

    @Test
    fun latinDigitConversionNormalisesPersianSeparators() {
        assertEquals("1.5", "\u06f1\u066b\u06f5".toLatinDigits())
        assertEquals("1,500", "\u06f1\u060c\u06f5\u06f0\u06f0".toLatinDigits())
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
