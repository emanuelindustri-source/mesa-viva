package com.mesaviva.app.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NativeGamesTest {

    @Test
    fun `domino deck has 28 unique tiles`() {
        val deck = DominoGame.doubleSixDeck()
        assertEquals(28, deck.size)
        assertEquals(28, deck.map { it.id }.toSet().size)
        assertEquals(168, deck.sumOf { it.pips })
    }

    @Test
    fun `domino deal gives ten tiles to two players and starts with a tile`() {
        val game = DominoGame(playerCount = 2, teams = false, target = 100, seed = 7)
        val state = game.newMatch()
        assertEquals(19, state.players.sumOf { it.hand.size })
        assertEquals(8, state.boneyard.size)
        assertEquals(1, state.chain.size)
        assertNotNull(state.left)
        assertNotNull(state.right)
        assertTrue(state.turn in 0..1)
    }

    @Test
    fun `domino rejects a tile that does not fit and scores a closed hand`() {
        val game = DominoGame(playerCount = 2, teams = false, target = 200, seed = 9)
        val base = game.newMatch()
        val basePlayers = listOf(
            base.players[0].copy(hand = listOf(DominoTile(6, 6), DominoTile(1, 2))),
            base.players[1].copy(hand = listOf(DominoTile(1, 2), DominoTile(3, 4))),
        )
        val state = base.copy(
            players = basePlayers,
            chain = listOf(DominoTile(6, 6)),
            left = 6,
            right = 6,
            turn = 0,
            phase = DominoPhase.Playing,
        )
        val bad = game.play(state, 0, 1, DominoEnd.Left)
        assertNotNull(bad.error)
        val closedState = state.copy(players = listOf(basePlayers[0].copy(hand = listOf(DominoTile(6, 6))), basePlayers[1]))
        val closed = game.play(closedState, 0, 0, DominoEnd.Left)
        assertEquals(null, closed.error)
        assertEquals(DominoPhase.HandEnd, closed.state.phase)
        assertEquals(10, closed.state.players[0].score)
    }

    @Test
    fun `domino blocks when everyone passes and the boneyard is empty`() {
        val game = DominoGame(playerCount = 2, teams = false, target = 100, seed = 3)
        val base = game.newMatch()
        val blocked = base.copy(
            players = listOf(
                base.players[0].copy(hand = listOf(DominoTile(0, 0))),
                base.players[1].copy(hand = listOf(DominoTile(6, 6))),
            ),
            chain = listOf(DominoTile(1, 1)),
            left = 1,
            right = 1,
            boneyard = emptyList(),
            passes = 1,
            turn = 0,
            phase = DominoPhase.Playing,
        )
        val result = game.pass(blocked, 0)
        assertTrue(result.state.phase == DominoPhase.HandEnd || result.state.phase == DominoPhase.MatchEnd)
        assertTrue(result.state.players[0].score > 0)
    }

    @Test
    fun `domino bot always returns a legal action`() {
        val game = DominoGame(playerCount = 4, teams = true, target = 100, seed = 12)
        val state = game.newMatch().copy(turn = 1)
        val move = game.botMove(state)
        assertNotNull(move)
        assertTrue(move!!.type in listOf(DominoMoveType.Play, DominoMoveType.Draw, DominoMoveType.Pass))
    }

    @Test
    fun `chess starts with twenty legal moves`() {
        val game = ChessGame()
        val state = game.initialState()
        assertEquals(20, game.legalMoves(state).size)
        assertEquals(ChessColor.White, state.turn)
    }

    @Test
    fun `chess detects fools mate and records SAN`() {
        val game = ChessGame()
        var state = game.initialState()
        state = game.move(state, 53, 45)!! // f2-f3
        state = game.move(state, 12, 28)!! // e7-e5
        state = game.move(state, 54, 38)!! // g2-g4
        state = game.move(state, 3, 39)!! // Qd8-h4#
        assertEquals(ChessPhase.MatchEnd, state.phase)
        assertEquals("result.reason.checkmate", state.outcome?.reasonKey)
        assertEquals(ChessColor.Black, state.outcome?.winner)
        assertTrue(state.history.last().contains("Qh4#"))
    }

    @Test
    fun `chess offers all four promotions`() {
        val game = ChessGame()
        val base = game.initialState()
        val board = base.board.toMutableList()
        for (index in board.indices) board[index] = null
        board[7] = ChessPiece(ChessPieceType.King, ChessColor.Black)
        board[63] = ChessPiece(ChessPieceType.King, ChessColor.White)
        board[8] = ChessPiece(ChessPieceType.Pawn, ChessColor.White) // a7
        val state = base.copy(
            board = board,
            turn = ChessColor.White,
            castling = ChessCastling(false, false, false, false),
            enPassant = null,
            positions = listOf("test"),
        )
        val promotions = game.legalMoves(state).filter { it.from == 8 && it.to == 0 }
        assertEquals(4, promotions.size)
        assertEquals(
            setOf(ChessPieceType.Queen, ChessPieceType.Rook, ChessPieceType.Bishop, ChessPieceType.Knight),
            promotions.mapNotNull { it.promo }.toSet(),
        )
    }

    @Test
    fun `ludo creates four tokens per color and legal exits`() {
        val game = LudoGame(playerCount = 4, allHumans = false, seed = 4)
        val state = game.newMatch()
        assertEquals(16, state.tokens.size)
        assertEquals(4, state.players.size)
        val redBase = state.tokens.filter { it.color == LudoColor.Red }
        assertTrue(redBase.all { it.progress == -1 })
        val withSix = state.copy(dice = 6, phase = LudoPhase.Move)
        assertEquals(4, game.legalMoves(withSix).size)
    }

    @Test
    fun `ludo bot rolls and returns a legal move`() {
        val game = LudoGame(playerCount = 2, allHumans = false, seed = 8)
        val state = game.newMatch().copy(turn = 1)
        assertEquals(LudoAction.Roll, game.botAction(state))
        val rolled = game.roll(state).state
        if (rolled.phase == LudoPhase.Move) {
            val action = game.botAction(rolled)
            assertTrue(action is LudoAction.Move)
        }
    }

    @Test
    fun `mancala starts with four seeds and extra turn in store`() {
        val game = MancalaGame(seed = 5)
        val state = game.newMatch()
        assertEquals(48, state.pits.sum())
        val result = game.sow(state, 2) // 4 seeds: 3,4,5,store
        assertEquals(MancalaSide.White, result.state.turn)
        assertEquals(1, result.state.whiteStore)
    }

    @Test
    fun `mancala captures the opposite pit`() {
        val game = MancalaGame(seed = 6)
        val base = game.newMatch()
        val pits = base.pits.toMutableList()
        pits[0] = 1
        pits[1] = 0
        pits[11] = 5
        val state = base.copy(pits = pits.toList(), turn = MancalaSide.White)
        val captured = game.sow(state, 0).state
        assertEquals(6, captured.whiteStore)
        assertEquals(0, captured.pits[11])
    }

    @Test
    fun `mancala bot returns a valid pit`() {
        val game = MancalaGame(seed = 9)
        val state = game.newMatch().copy(turn = MancalaSide.Black)
        val pit = game.botPit(state, "normal")
        assertNotNull(pit)
        assertTrue(pit!! in MancalaGame.BLACK_PITS)
        assertTrue(state.pits[pit] > 0)
    }

    @Test
    fun `backgammon starts with fifteen checkers per side`() {
        val game = BackgammonGame(seed = 3)
        val state = game.newMatch()
        assertEquals(15, state.points.sumOf { if (it > 0) it else 0 })
        assertEquals(-15, state.points.sumOf { if (it < 0) it else 0 })
        assertEquals(BgPhase.Roll, state.phase)
        assertEquals(167, BackgammonGame.pipCount(state, BgSide.White))
    }

    @Test
    fun `backgammon rolls and produces legal moves`() {
        val game = BackgammonGame(seed = 11)
        val rolled = game.roll(game.newMatch()).state
        assertEquals(BgPhase.Move, rolled.phase)
        assertTrue(rolled.movesLeft.isNotEmpty())
        val move = game.legalMoves(rolled).firstOrNull()
        assertNotNull(move)
        val moved = game.move(rolled, move!!.from, move.to).state
        assertTrue(moved.phase == BgPhase.Move || moved.phase == BgPhase.Roll)
    }

    @Test
    fun `backgammon bears off from home`() {
        val game = BackgammonGame(seed = 12)
        val base = game.newMatch()
        val points = MutableList(24) { 0 }
        points[0] = 15
        val state = base.copy(
            points = points,
            phase = BgPhase.Move,
            turn = BgSide.White,
            dice = 1 to 2,
            movesLeft = listOf(1),
        )
        val move = game.legalMoves(state).firstOrNull { it.to == 24 }
        assertNotNull(move)
        val after = game.move(state, move!!.from, move.to).state
        assertEquals(1, after.offWhite)
    }

    @Test
    fun `chess bot returns a legal move`() {
        val game = ChessGame()
        val state = game.initialState()
        val move = game.botMove(state, "normal", Random(5))
        assertNotNull(move)
        assertTrue(game.legalMoves(state).any { it.from == move!!.from && it.to == move.to })
    }
}
