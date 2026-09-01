package com.opplayer.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalNameOrderTest {

    @Test
    fun `digit runs are compared numerically`() {
        val sorted = listOf("Episode 10.mkv", "Episode 9.mkv", "Episode 1.mkv")
            .sortedWith(NaturalNameOrder)

        assertEquals(listOf("Episode 1.mkv", "Episode 9.mkv", "Episode 10.mkv"), sorted)
    }

    @Test
    fun `zero padding does not change the order`() {
        val sorted = listOf(
            "Prince of Tennis - 078.mkv",
            "Prince of Tennis - 076.mkv",
            "Prince of Tennis - 077.mkv"
        ).sortedWith(NaturalNameOrder)

        assertEquals(
            listOf(
                "Prince of Tennis - 076.mkv",
                "Prince of Tennis - 077.mkv",
                "Prince of Tennis - 078.mkv"
            ),
            sorted
        )
    }

    @Test
    fun `text is compared without case`() {
        assertTrue(NaturalNameOrder.compare("alpha.mkv", "Beta.mkv") < 0)
    }

    @Test
    fun `a prefix sorts before the longer name`() {
        assertTrue(NaturalNameOrder.compare("clip.mkv", "clip part 2.mkv") < 0)
    }

    @Test
    fun `equal names compare equal`() {
        assertEquals(0, NaturalNameOrder.compare("S01E02.mkv", "s01e02.mkv"))
    }
}
