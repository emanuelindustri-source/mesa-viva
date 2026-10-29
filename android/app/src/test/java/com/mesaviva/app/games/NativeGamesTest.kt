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

    @Test
    fun `damas starts with twelve checkers per side and seven opening moves`() {
        val game = DamasGame(seed = 1)
        val state = game.newMatch()
        assertEquals(12, game.piecesOf(state, DamasColor.White))
        assertEquals(12, game.piecesOf(state, DamasColor.Black))
        assertEquals(DamasColor.White, state.turn)
        assertEquals(7, game.legalMoves(state).size)
    }

    @Test
    fun `damas forces captures and chains`() {
        val game = DamasGame(seed = 2)
        val board = MutableList<DamasChecker?>(64) { null }
        board[17] = DamasChecker(DamasColor.White)
        board[10] = DamasChecker(DamasColor.Black)
        board[62] = DamasChecker(DamasColor.Black)
        val state = game.newMatch().copy(board = board, turn = DamasColor.White)
        val moves = game.legalMoves(state)
        assertEquals(1, moves.size)
        assertEquals(17, moves[0].from)
        assertEquals(3, moves[0].to)
        assertEquals(10, moves[0].captured)
    }

    @Test
    fun `damas promotes a checker on the last row`() {
        val game = DamasGame(seed = 3)
        val board = MutableList<DamasChecker?>(64) { null }
        board[8] = DamasChecker(DamasColor.White)
        board[62] = DamasChecker(DamasColor.Black)
        val state = game.newMatch().copy(board = board, turn = DamasColor.White)
        val result = game.move(state, 8, 1)
        assertEquals(null, result.error)
        assertTrue(result.state.board[1]?.king == true)
        assertEquals(DamasColor.Black, result.state.turn)
    }

    @Test
    fun `damas bot returns a legal move`() {
        val game = DamasGame(seed = 4)
        val state = game.newMatch()
        val move = game.botMove(state, "easy", Random(4))
        assertNotNull(move)
        assertTrue(game.legalMoves(state).any { it.from == move!!.from && it.to == move.to })
    }

    @Test
    fun `go starts with an empty board and komi`() {
        val game = GoGame(seed = 5)
        val state = game.newMatch(9)
        assertEquals(81, state.board.size)
        assertEquals(0, state.board.sum())
        val score = GoGame.scoreBoard(state)
        assertEquals(0.0, score.black, 0.001)
        assertEquals(6.5, score.white, 0.001)
    }

    @Test
    fun `go captures a stone without liberties`() {
        val game = GoGame(seed = 6)
        val base = game.newMatch(9)
        val board = base.board.toMutableList()
        board[1] = 2
        board[0] = 1
        board[2] = 1
        val state = base.copy(board = board, turn = 1)
        val result = game.play(state, 10)
        assertEquals(null, result.error)
        assertEquals(0, result.state.board[1])
        assertEquals(1, result.state.capturesBlack)
        assertEquals(2, result.state.turn)
    }

    @Test
    fun `go bot returns a legal intersection`() {
        val game = GoGame(seed = 7)
        val state = game.newMatch(9)
        val move = game.botMove(state, "normal", Random(7))
        assertNotNull(move)
        assertTrue(game.legalMoves(state).contains(move))
    }

    @Test
    fun `jungle starts with sixteen pieces`() {
        val game = JungleGame(seed = 8)
        val state = game.newMatch()
        assertEquals(16, state.board.count { it != null })
        assertEquals(JungleSide.Red, state.turn)
        assertTrue(game.legalMoves(state).isNotEmpty())
    }

    @Test
    fun `jungle rat eats elephant but elephant cannot eat rat`() {
        val game = JungleGame(seed = 9)
        val base = game.newMatch()
        val ratBoard = MutableList<JunglePiece?>(63) { null }
        ratBoard[0] = JunglePiece(JungleSide.Red, 1)
        ratBoard[1] = JunglePiece(JungleSide.Black, 8)
        val ratState = base.copy(board = ratBoard, turn = JungleSide.Red)
        assertTrue(game.legalMoves(ratState).any { it.from == 0 && it.to == 1 })

        val elephantBoard = MutableList<JunglePiece?>(63) { null }
        elephantBoard[0] = JunglePiece(JungleSide.Black, 8)
        elephantBoard[1] = JunglePiece(JungleSide.Red, 1)
        val elephantState = base.copy(board = elephantBoard, turn = JungleSide.Black)
        assertTrue(game.legalMoves(elephantState).none { it.from == 0 && it.to == 1 })
    }

    @Test
    fun `jungle wins by entering the enemy den`() {
        val game = JungleGame(seed = 10)
        val base = game.newMatch()
        val board = MutableList<JunglePiece?>(63) { null }
        board[4] = JunglePiece(JungleSide.Red, 7)
        board[62] = JunglePiece(JungleSide.Black, 1)
        val state = base.copy(board = board, turn = JungleSide.Red)
        val result = game.move(state, 4, 3)
        assertEquals(null, result.error)
        assertEquals(JunglePhase.MatchEnd, result.state.phase)
        assertEquals(JungleSide.Red, result.state.winner)
    }

    @Test
    fun `xiangqi starts with thirty two pieces and legal moves`() {
        val game = XiangqiGame(seed = 11)
        val state = game.newMatch()
        assertEquals(32, state.board.count { it != null })
        assertTrue(game.legalMoves(state).isNotEmpty())
        assertEquals(XqSide.Red, state.turn)
    }

    @Test
    fun `xiangqi general can fly to capture the rival general`() {
        val game = XiangqiGame(seed = 12)
        val base = game.newMatch()
        val board = MutableList<XqPiece?>(90) { null }
        board[76] = XqPiece(XqSide.Red, XqKind.King)
        board[4] = XqPiece(XqSide.Black, XqKind.King)
        val state = base.copy(board = board, turn = XqSide.Red)
        assertTrue(game.legalMoves(state).any { it.from == 76 && it.to == 4 })
        val result = game.move(state, 76, 4)
        assertEquals(null, result.error)
        assertEquals(XqPhase.MatchEnd, result.state.phase)
        assertEquals(XqSide.Red, result.state.winner)
    }

    @Test
    fun `xiangqi bot returns a legal move`() {
        val game = XiangqiGame(seed = 13)
        val state = game.newMatch()
        val move = game.botMove(state, "easy", Random(13))
        assertNotNull(move)
        assertTrue(game.legalMoves(state).any { it.from == move!!.from && it.to == move.to })
    }

    @Test
    fun `chino starts with ten pieces per player and legal moves`() {
        val game = ChinoGame(seed = 14)
        val state = game.newMatch(2)
        assertEquals(121, state.pieces.size)
        assertEquals(20, state.pieces.count { it != null })
        assertEquals(0, game.reachedTarget(state, 0))
        assertTrue(game.legalFor(state).isNotEmpty())
    }

    @Test
    fun `chino moves to an empty neighbour and switches turn`() {
        val game = ChinoGame(seed = 15)
        val state = game.newMatch(2)
        val move = game.legalFor(state).first { it.jumps == 0 }
        val result = game.move(state, move.from, move.to)
        assertEquals(null, result.error)
        assertEquals(null, result.state.pieces[move.from])
        assertEquals(0, result.state.pieces[move.to])
        assertEquals(1, result.state.turn)
    }

    @Test
    fun `chino bot returns a legal move`() {
        val game = ChinoGame(seed = 16)
        val state = game.newMatch(3)
        val move = game.botMove(state, "normal", Random(16))
        assertNotNull(move)
        assertTrue(game.legalFor(state).any { it.from == move!!.from && it.to == move.to })
    }

    @Test
    fun `dados starts with five dice per player and validates bids`() {
        val game = DadosGame(playerCount = 4, seed = 17)
        val state = game.newMatch()
        assertEquals(20, game.totalDice(state))
        assertTrue(state.diceCount.all { it == 5 })
        assertNotNull(game.placeBid(state, 21, 1).error)
        val valid = game.placeBid(state, 1, 1)
        assertEquals(null, valid.error)
        assertEquals(1, valid.state.turn)
        assertNotNull(valid.state.bid)
    }

    @Test
    fun `dados doubt removes a die from the liar`() {
        val game = DadosGame(playerCount = 4, seed = 18)
        val base = game.newMatch()
        val state = base.copy(
            dice = List(4) { List(5) { 1 } },
            bid = DadosBid(20, 6, 0),
            turn = 1,
            phase = DadosPhase.Bid,
        )
        val result = game.doubt(state)
        assertEquals(null, result.error)
        assertEquals(DadosPhase.Reveal, result.state.phase)
        assertEquals(4, result.state.diceCount[0])
        assertEquals(0, result.state.reveal?.loser)
        assertEquals(false, result.state.reveal?.bidTrue)
    }

    @Test
    fun `dados bot returns an action`() {
        val game = DadosGame(playerCount = 4, seed = 19)
        val state = game.newMatch().copy(turn = 1)
        assertNotNull(game.botAction(state, "normal", Random(19)))
    }
}
