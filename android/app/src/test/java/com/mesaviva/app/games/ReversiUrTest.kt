package com.mesaviva.app.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReversiUrTest {

    @Test
    fun reversiStartsWithFourDiscsAndFourMoves() {
        val game = ReversiGame(7)
        val state = ReversiGame.initial()
        assertEquals(2, state.whiteCount)
        assertEquals(2, state.blackCount)
        assertEquals(4, game.legalMoves(state).size)
    }

    @Test
    fun reversiMoveFlipsDiscsAndChangesTurn() {
        val game = ReversiGame(11)
        val state = ReversiGame.initial()
        val move = game.legalMoves(state).keys.first()
        val result = game.move(state, move)
        assertNull(result.error)
        assertNotNull(result.state.lastMove)
        assertTrue(result.state.lastFlips.isNotEmpty())
        assertEquals(ReversiColor.Black, result.state.turn)
        assertTrue(result.state.whiteCount >= 4)
    }

    @Test
    fun reversiRejectsAnIllegalMove() {
        val game = ReversiGame(3)
        val state = ReversiGame.initial()
        val result = game.move(state, 0)
        assertEquals("reversi.error.invalid", result.error)
    }

    @Test
    fun reversiBotReturnsALegalMove() {
        val game = ReversiGame(5)
        val state = ReversiGame.initial()
        val move = game.botMove(state, "normal")
        assertNotNull(move)
        assertTrue(game.legalMoves(state).containsKey(move!!))
    }

    @Test
    fun reversiResignEndsTheMatch() {
        val game = ReversiGame(9)
        val state = game.resign(ReversiGame.initial()).state
        assertEquals(ReversiPhase.MatchEnd, state.phase)
        assertEquals(ReversiColor.Black, state.winner)
    }

    @Test
    fun urStartsWithEightPiecesOffBoard() {
        val state = UrGame.initial()
        assertEquals(8, state.pieces.size)
        assertEquals(8, state.pieces.count { it.position < 0 })
        assertEquals(UrSide.White, state.turn)
    }

    @Test
    fun urRollOpensTheTurnAndMovesAPiece() {
        val game = UrGame(13)
        val rolled = game.roll(UrGame.initial(), 4).state
        assertEquals(4, rolled.dice)
        assertEquals(4, game.legalMoves(rolled).size)
        val moved = game.move(rolled, 0).state
        assertEquals(3, moved.pieces[0].position)
        assertEquals(UrSide.Black, moved.turn)
        assertEquals(0, moved.dice)
    }

    @Test
    fun urRosetteGrantsAnExtraTurn() {
        val game = UrGame(17)
        val rolled = game.roll(UrGame.initial(), 1).state
        val moved = game.move(rolled, 0).state
        assertEquals(0, moved.pieces[0].position)
        assertEquals(UrSide.White, moved.turn)
        assertTrue(moved.extraTurn)
    }

    @Test
    fun urCaptureSendsTheRivalHome() {
        val game = UrGame(19)
        val base = UrGame.initial()
        val pieces = base.pieces.map { piece ->
            when (piece.id) {
                0 -> piece.copy(position = 2)
                4 -> piece.copy(position = 5)
                else -> piece
            }
        }
        val rolled = game.roll(base.copy(pieces = pieces), 3).state
        val moved = game.move(rolled, 0).state
        assertEquals(5, moved.pieces[0].position)
        assertEquals(-1, moved.pieces[4].position)
        assertTrue(moved.captured)
    }

    @Test
    fun urNeedsAnExactRollToFinish() {
        val game = UrGame(23)
        val base = UrGame.initial()
        val pieces = base.pieces.map { piece ->
            when (piece.id) {
                0 -> piece.copy(position = 19)
                else -> piece
            }
        }
        val rolled = game.roll(base.copy(pieces = pieces), 4).state
        assertTrue(game.legalMoves(rolled).isNotEmpty())
        assertTrue(0 !in game.legalMoves(rolled))
    }

    @Test
    fun urBotMovesStayLegal() {
        val game = UrGame(29)
        val rolled = game.roll(UrGame.initial(), 2).state
        val choice = game.botChoice(rolled)
        assertNotNull(choice)
        assertTrue(game.legalMoves(rolled).contains(choice))
    }
}
