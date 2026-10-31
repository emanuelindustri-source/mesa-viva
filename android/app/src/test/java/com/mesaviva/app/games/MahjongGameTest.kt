package com.mesaviva.app.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MahjongGameTest {

    private fun tile(id: Int, suit: MahjongSuit, rank: Int) = MahjongTile(id, suit, rank)

    private fun hand(vararg pairs: Pair<MahjongSuit, Int>, startId: Int = 0): List<MahjongTile> =
        pairs.mapIndexed { index, (suit, rank) -> tile(startId + index, suit, rank) }

    private fun baseState(
        variant: MahjongVariant = MahjongVariant.American,
        players: List<MahjongPlayer> = (0 until 4).map { MahjongPlayer(seat = it) },
        wall: List<MahjongTile> = listOf(tile(900, MahjongSuit.Dots, 9)),
        turn: Int = 0,
        drawn: MahjongTile? = null,
    ) = MahjongState(
        variant = variant,
        players = players,
        wall = wall,
        turn = turn,
        dealer = 0,
        round = 1,
        drawn = drawn,
    )

    @Test
    fun `american wall has 152 tiles and the deal conserves every tile`() {
        val game = MahjongGame(MahjongVariant.American, seed = 5)
        val state = game.newMatch()
        assertEquals(13, state.players[1].hand.size)
        assertEquals(14, state.players[0].hand.size)
        val total = state.wall.size +
            state.players.sumOf { it.hand.size + it.flowers.size } +
            state.players.sumOf { player -> player.melds.sumOf { it.tiles.size } }
        assertEquals(152, total)
    }

    @Test
    fun `riichi wall has 136 tiles without flowers or jokers`() {
        val game = MahjongGame(MahjongVariant.Riichi, seed = 6)
        val state = game.newMatch()
        val total = state.wall.size + state.players.sumOf { it.hand.size + it.flowers.size }
        assertEquals(136, total)
        assertTrue(state.players.all { it.flowers.isEmpty() })
        assertTrue(state.players.none { player -> player.hand.any { it.isJoker } })
    }

    @Test
    fun `american hand accepts four triplets and a pair`() {
        val game = MahjongGame(MahjongVariant.American, seed = 1)
        val winning = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 1, MahjongSuit.Dots to 1,
            MahjongSuit.Dots to 2, MahjongSuit.Dots to 2, MahjongSuit.Dots to 2,
            MahjongSuit.Dots to 3, MahjongSuit.Dots to 3, MahjongSuit.Dots to 3,
            MahjongSuit.Bams to 5, MahjongSuit.Bams to 5, MahjongSuit.Bams to 5,
            MahjongSuit.Cracks to 9, MahjongSuit.Cracks to 9,
        )
        assertTrue(game.canWinHand(winning, emptyList(), MahjongVariant.American))
    }

    @Test
    fun `american hand rejects sequences because the card uses melds only`() {
        val game = MahjongGame(MahjongVariant.American, seed = 1)
        val sequence = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 2, MahjongSuit.Dots to 3,
            MahjongSuit.Dots to 4, MahjongSuit.Dots to 5, MahjongSuit.Dots to 6,
            MahjongSuit.Bams to 1, MahjongSuit.Bams to 2, MahjongSuit.Bams to 3,
            MahjongSuit.Bams to 7, MahjongSuit.Bams to 7, MahjongSuit.Bams to 7,
            MahjongSuit.Cracks to 9, MahjongSuit.Cracks to 9,
        )
        assertFalse(game.canWinHand(sequence, emptyList(), MahjongVariant.American))
        assertTrue(game.canWinHand(sequence, emptyList(), MahjongVariant.Riichi))
    }

    @Test
    fun `riichi accepts seven pairs and a closed sequence hand`() {
        val game = MahjongGame(MahjongVariant.Riichi, seed = 2)
        val sevenPairs = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 1,
            MahjongSuit.Dots to 3, MahjongSuit.Dots to 3,
            MahjongSuit.Bams to 2, MahjongSuit.Bams to 2,
            MahjongSuit.Bams to 6, MahjongSuit.Bams to 6,
            MahjongSuit.Cracks to 4, MahjongSuit.Cracks to 4,
            MahjongSuit.Cracks to 8, MahjongSuit.Cracks to 8,
            MahjongSuit.Wind to 1, MahjongSuit.Wind to 1,
        )
        assertTrue(game.canWinHand(sevenPairs, emptyList(), MahjongVariant.Riichi))

        val sequence = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 2, MahjongSuit.Dots to 3,
            MahjongSuit.Bams to 4, MahjongSuit.Bams to 5, MahjongSuit.Bams to 6,
            MahjongSuit.Cracks to 7, MahjongSuit.Cracks to 8, MahjongSuit.Cracks to 9,
            MahjongSuit.Wind to 2, MahjongSuit.Wind to 2, MahjongSuit.Wind to 2,
            MahjongSuit.Dragon to 1, MahjongSuit.Dragon to 1,
        )
        assertTrue(game.canWinHand(sequence, emptyList(), MahjongVariant.Riichi))
    }

    @Test
    fun `american joker completes a missing tile in a triplet`() {
        val game = MahjongGame(MahjongVariant.American, seed = 3)
        val withJoker = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 1,
            MahjongSuit.Dots to 2, MahjongSuit.Dots to 2, MahjongSuit.Dots to 2,
            MahjongSuit.Dots to 3, MahjongSuit.Dots to 3, MahjongSuit.Dots to 3,
            MahjongSuit.Bams to 5, MahjongSuit.Bams to 5, MahjongSuit.Bams to 5,
            MahjongSuit.Cracks to 9, MahjongSuit.Cracks to 9,
            MahjongSuit.Joker to 1,
        )
        assertTrue(game.canWinHand(withJoker, emptyList(), MahjongVariant.American))
        assertFalse(game.canWinHand(withJoker, emptyList(), MahjongVariant.Riichi))
    }

    @Test
    fun `discard advances the turn and the next player draws`() {
        val game = MahjongGame(MahjongVariant.American, seed = 4)
        val state = baseState(
            players = listOf(
                MahjongPlayer(0, hand = listOf(tile(1, MahjongSuit.Dots, 5), tile(2, MahjongSuit.Bams, 1))),
                MahjongPlayer(1, hand = hand(MahjongSuit.Cracks to 1, MahjongSuit.Cracks to 2, MahjongSuit.Cracks to 3)),
                MahjongPlayer(2),
                MahjongPlayer(3),
            ),
            wall = listOf(tile(50, MahjongSuit.Bams, 9)),
        )
        val result = game.discard(state, 1)
        assertEquals(null, result.error)
        assertEquals(1, result.state.turn)
        assertEquals(1, result.state.players[0].discards.size)
        assertEquals(0, result.state.wall.size)
        assertEquals(4, result.state.players[1].hand.size)
        assertEquals(MahjongSuit.Bams, result.state.players[1].hand.last().suit)
    }

    @Test
    fun `pon claim moves two tiles to a meld and leaves the claimer to discard`() {
        val game = MahjongGame(MahjongVariant.American, seed = 5)
        val tileA = tile(10, MahjongSuit.Dragon, 1)
        val tileB = tile(11, MahjongSuit.Dragon, 1)
        val state = baseState(
            players = listOf(
                MahjongPlayer(0, hand = listOf(tile(1, MahjongSuit.Dots, 9))),
                MahjongPlayer(
                    1,
                    hand = listOf(tileA, tileB) + hand(MahjongSuit.Bams to 1, MahjongSuit.Bams to 2, MahjongSuit.Bams to 3),
                ),
                MahjongPlayer(2),
                MahjongPlayer(3),
            ),
            drawn = null,
        ).copy(
            lastDiscard = tile(99, MahjongSuit.Dragon, 1),
            lastDiscardBy = 0,
            turn = 0,
        )
        val claims = game.claimsFor(state, 0, tile(99, MahjongSuit.Dragon, 1))
        val pon = claims.firstOrNull { it.kind == MahjongClaimKind.Pon && it.seat == 1 }
        assertNotNull(pon)
        val claimed = game.claim(state.copy(pending = claims), pon!!)
        assertEquals(null, claimed.error)
        assertEquals(1, claimed.state.players[1].melds.size)
        assertEquals(MahjongMeldKind.Pon, claimed.state.players[1].melds.first().kind)
        assertEquals(3, claimed.state.players[1].melds.first().tiles.size)
        assertEquals(1, claimed.state.turn)
        assertTrue(claimed.state.pending.isEmpty())
    }

    @Test
    fun `riichi declaration requires tenpai and spends the stick`() {
        val game = MahjongGame(MahjongVariant.Riichi, seed = 7)
        val ready = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 1, MahjongSuit.Dots to 1,
            MahjongSuit.Dots to 2, MahjongSuit.Dots to 2, MahjongSuit.Dots to 2,
            MahjongSuit.Dots to 3, MahjongSuit.Dots to 3, MahjongSuit.Dots to 3,
            MahjongSuit.Dots to 4, MahjongSuit.Dots to 4, MahjongSuit.Dots to 4,
            MahjongSuit.Dots to 5, MahjongSuit.Cracks to 9,
        )
        val state = baseState(
            variant = MahjongVariant.Riichi,
            players = listOf(
                MahjongPlayer(0, hand = ready, score = 250),
                MahjongPlayer(1, hand = hand(MahjongSuit.Bams to 1, MahjongSuit.Bams to 2, MahjongSuit.Bams to 3), score = 250),
                MahjongPlayer(2, score = 250),
                MahjongPlayer(3, score = 250),
            ),
            wall = listOf(tile(500, MahjongSuit.Cracks, 9)),
        )
        val result = game.declareRiichi(state, ready.last().id)
        assertEquals(null, result.error)
        assertTrue(result.state.players[0].riichi)
        assertEquals(240, result.state.players[0].score)
    }

    @Test
    fun `tsumo transfers points and keeps the table at a constant total`() {
        val game = MahjongGame(MahjongVariant.American, seed = 8)
        val winningHand = hand(
            MahjongSuit.Dots to 1, MahjongSuit.Dots to 1, MahjongSuit.Dots to 1,
            MahjongSuit.Dots to 2, MahjongSuit.Dots to 2, MahjongSuit.Dots to 2,
            MahjongSuit.Dots to 3, MahjongSuit.Dots to 3, MahjongSuit.Dots to 3,
            MahjongSuit.Bams to 5, MahjongSuit.Bams to 5, MahjongSuit.Bams to 5,
            MahjongSuit.Cracks to 9, MahjongSuit.Cracks to 9,
        )
        val state = baseState(
            players = (0 until 4).map { seat ->
                MahjongPlayer(seat, hand = if (seat == 0) winningHand else emptyList(), score = 250)
            },
            drawn = winningHand.last(),
        )
        val before = state.players.sumOf { it.score }
        val result = game.declareTsumo(state)
        assertEquals(null, result.error)
        assertEquals(MahjongPhase.HandEnd, result.state.phase)
        assertEquals(0, result.state.winner)
        assertEquals(before, result.state.players.sumOf { it.score })
        assertTrue(result.state.players[1].score < 250)
        assertTrue(result.state.players[0].score > 250)
    }

    @Test
    fun `four hands close the match and the best score wins`() {
        val game = MahjongGame(MahjongVariant.American, seed = 9)
        val state = game.newMatch().copy(phase = MahjongPhase.HandEnd, round = 4)
        val ended = game.nextHand(state)
        assertEquals(MahjongPhase.MatchEnd, ended.phase)
        assertNotNull(game.matchWinner(ended))
    }

    @Test
    fun `bot discard is always one of its own tiles`() {
        val game = MahjongGame(MahjongVariant.Riichi, seed = 10)
        val state = game.newMatch().copy(turn = 1)
        val tileId = game.botDiscard(state)
        assertNotNull(tileId)
        assertTrue(state.players[1].hand.any { it.id == tileId })
    }
}
