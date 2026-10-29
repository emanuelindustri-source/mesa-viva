package com.mesaviva.app.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayCodecTest {

    @Test
    fun damasReplayRoundTrips() {
        val moves = listOf(40 to 33, 17 to 26, 33 to 26)
        val link = ReplayCodec.encodeDamas(123456789L, moves)
        val parsed = ReplayCodec.parse(link)
        assertEquals(ReplayCodec.DAMAS, parsed?.game)
        assertEquals(123456789L, parsed?.seed)
        assertEquals(ReplayCodec.damasPairs(parsed!!), moves)
    }

    @Test
    fun mancalaReplayRoundTrips() {
        val moves = listOf(0, 1, 2, 5)
        val link = ReplayCodec.encodeMancala(987654321L, moves)
        val parsed = ReplayCodec.parse(link)
        assertEquals(ReplayCodec.MANCALA, parsed?.game)
        assertEquals(987654321L, parsed?.seed)
        assertEquals(moves, parsed?.moves)
    }

    @Test
    fun emptyReplayIsValid() {
        val link = ReplayCodec.encodeMancala(1L, emptyList())
        val parsed = ReplayCodec.parse(link)
        assertEquals(emptyList<Int>(), parsed?.moves)
        assertTrue(link.startsWith("mesaviva://replay"))
    }

    @Test
    fun invalidGameIsRejected() {
        assertNull(ReplayCodec.parse("mesaviva://replay?game=ajedrez&seed=1&moves=1-2"))
    }

    @Test
    fun oddDamasMovesAreRejected() {
        assertNull(ReplayCodec.parse("mesaviva://replay?game=damas&seed=1&moves=40-33.17"))
    }

    @Test
    fun outOfRangeMovesAreRejected() {
        assertNull(ReplayCodec.parse("mesaviva://replay?game=mancala&seed=1&moves=0.14"))
    }

    @Test
    fun nonReplayLinkIsRejected() {
        assertNull(ReplayCodec.parse("mesaviva://join?ws=ws%3A%2F%2Fx&code=ABCDE"))
        assertNull(ReplayCodec.parse("https://example.com/replay?game=damas"))
    }
}
