package com.opplayer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalProgressTest {

    @Test
    fun keepsEverythingBelowTheLimit() {
        val entries = mapOf(
            "a" to LocalProgress(1_000L, 10L),
            "b" to LocalProgress(2_000L, 20L)
        )
        assertEquals(entries, trimProgress(entries, 5))
    }

    @Test
    fun keepsTheMostRecentlyWatchedEntries() {
        val entries = mapOf(
            "old-but-long" to LocalProgress(9_000_000L, 100L),
            "recent-but-short" to LocalProgress(3_000L, 900L)
        )
        val trimmed = trimProgress(entries, 1)
        assertEquals(setOf("recent-but-short"), trimmed.keys)
    }

    @Test
    fun fallsBackToPositionWhenTimestampsAreEqual() {
        val entries = mapOf(
            "short" to LocalProgress(1_000L, 50L),
            "long" to LocalProgress(8_000L, 50L)
        )
        assertEquals(setOf("long"), trimProgress(entries, 1).keys)
    }

    @Test
    fun returnsEmptyForNonPositiveLimit() {
        val entries = mapOf("a" to LocalProgress(1_000L, 10L))
        assertTrue(trimProgress(entries, 0).isEmpty())
    }

    @Test
    fun defaultTimestampIsZero() {
        assertEquals(0L, LocalProgress(1_000L).updatedAt)
    }
}
