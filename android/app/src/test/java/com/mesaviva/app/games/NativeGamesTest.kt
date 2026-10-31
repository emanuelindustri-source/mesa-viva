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

    @Test
    fun `caida deck has forty unique cards and rank points`() {
        val deck = CaidaGame.spanishDeck()
        assertEquals(40, deck.size)
        assertEquals(40, deck.map { it.id }.toSet().size)
        assertEquals(1, CaidaGame.rankPoints(7))
        assertEquals(2, CaidaGame.rankPoints(10))
        assertEquals(3, CaidaGame.rankPoints(11))
        assertEquals(4, CaidaGame.rankPoints(12))
    }

    @Test
    fun `caida detects calls`() {
        val game = CaidaGame(playerCount = 4, seed = 20)
        val triple = listOf(
            SpanishCard("a", SpanishSuit.Oros, 7),
            SpanishCard("b", SpanishSuit.Copas, 7),
            SpanishCard("c", SpanishSuit.Espadas, 7),
        )
        assertEquals("trivilin", game.detectCanto(triple)?.key)
        val vigia = listOf(
            SpanishCard("a", SpanishSuit.Oros, 5),
            SpanishCard("b", SpanishSuit.Copas, 5),
            SpanishCard("c", SpanishSuit.Espadas, 6),
        )
        assertEquals("vigia", game.detectCanto(vigia)?.key)
        val registrico = listOf(
            SpanishCard("a", SpanishSuit.Oros, 1),
            SpanishCard("b", SpanishSuit.Copas, 10),
            SpanishCard("c", SpanishSuit.Espadas, 11),
        )
        assertEquals("registrico", game.detectCanto(registrico)?.key)
    }

    @Test
    fun `caida deals four delivery cards and three cards per player`() {
        val game = CaidaGame(playerCount = 4, seed = 21)
        val state = game.newMatch()
        assertEquals(CaidaPhase.Entrega, state.phase)
        assertEquals(40, state.deck.size)
        val result = game.deliver(state)
        assertEquals(null, result.error)
        assertEquals(CaidaPhase.Play, result.state.phase)
        assertEquals(4, result.state.delivery?.cards?.size)
        assertTrue(result.state.hands.all { it.size == 3 })
        assertEquals(24, result.state.deck.size)
    }

    @Test
    fun `caida captures the matching table card and everything above it`() {
        val game = CaidaGame(playerCount = 2, seed = 22)
        val base = game.newMatch()
        val seven = SpanishCard("mine", SpanishSuit.Oros, 7)
        val tableSeven = SpanishCard("table", SpanishSuit.Copas, 7)
        val top = SpanishCard("top", SpanishSuit.Espadas, 3)
        val state = base.copy(
            phase = CaidaPhase.Play,
            turn = 0,
            hands = listOf(listOf(seven, SpanishCard("other", SpanishSuit.Bastos, 2)), emptyList()),
            table = listOf(tableSeven, top),
            deck = emptyList(),
            scores = listOf(0, 0),
        )
        val result = game.playCard(state, "mine")
        assertEquals(null, result.error)
        assertEquals(6, result.state.scores[0])
        assertTrue(result.state.table.isEmpty())
        assertTrue(result.state.lastCapture?.mesa == true)
    }

    @Test
    fun `caida claims a canto before playing`() {
        val game = CaidaGame(playerCount = 2, seed = 23)
        val base = game.newMatch()
        val hand = listOf(
            SpanishCard("a", SpanishSuit.Oros, 4),
            SpanishCard("b", SpanishSuit.Copas, 4),
            SpanishCard("c", SpanishSuit.Espadas, 4),
        )
        val state = base.copy(
            phase = CaidaPhase.Play,
            turn = 0,
            hands = listOf(hand, emptyList()),
        )
        val result = game.claimCanto(state)
        assertEquals(null, result.error)
        assertEquals(3, result.state.scores[0])
        assertEquals(1, result.state.claimedHands[0].size)
    }

    @Test
    fun `caida bot delivers or plays`() {
        val game = CaidaGame(playerCount = 4, seed = 24)
        val state = game.newMatch().copy(dealer = 1, turn = 2)
        assertEquals(CaidaAction.Deliver, game.botAction(state))
    }

    @Test
    fun `furia deck has one hundred eight unique cards`() {
        val deck = FuriaGame.deck()
        assertEquals(108, deck.size)
        assertEquals(108, deck.map { it.id }.toSet().size)
        assertEquals(50, FuriaGame.cardPoints(deck.first { it.action == FuriaAction.Wild }))
    }

    @Test
    fun `furia deals seven cards and starts with a non wild`() {
        val game = FuriaGame(playerCount = 4, seed = 25)
        val state = game.newMatch()
        assertEquals(4, state.seats.size)
        assertTrue(state.hands.all { it.size == 7 })
        assertEquals(1, state.discard.size)
        assertTrue(state.discard.first().action != FuriaAction.Wild)
    }

    @Test
    fun `furia matches color rank and wild`() {
        val game = FuriaGame(playerCount = 2, seed = 26)
        val base = game.newMatch()
        val redTop = FuriaCard("top", FuriaSuit.Magma, 5)
        val state = base.copy(discard = listOf(redTop), color = FuriaSuit.Magma)
        assertTrue(game.canPlayOn(FuriaCard("red", FuriaSuit.Magma, 9), state))
        assertTrue(game.canPlayOn(FuriaCard("five", FuriaSuit.Sol, 5), state))
        assertTrue(game.canPlayOn(FuriaCard("wild", FuriaSuit.Wild, -1, FuriaAction.Wild), state))
        assertTrue(!game.canPlayOn(FuriaCard("blue", FuriaSuit.Mar, 9), state))
    }

    @Test
    fun `furia playing a matching number changes the active color`() {
        val game = FuriaGame(playerCount = 2, seed = 29)
        val base = game.newMatch()
        val redNine = FuriaCard("red-nine", FuriaSuit.Magma, 9)
        val state = base.copy(
            phase = FuriaPhase.Play,
            turn = 0,
            hands = listOf(listOf(redNine, FuriaCard("extra", FuriaSuit.Sol, 2)), listOf(FuriaCard("other", FuriaSuit.Sol, 1))),
            discard = listOf(FuriaCard("top", FuriaSuit.Selva, 9)),
            color = FuriaSuit.Selva,
        )
        val result = game.play(state, "red-nine")
        assertEquals(null, result.error)
        assertEquals(FuriaSuit.Magma, result.state.color)
    }

    @Test
    fun `furia wild requires a color and changes it`() {
        val game = FuriaGame(playerCount = 2, seed = 27)
        val base = game.newMatch()
        val wild = FuriaCard("wild", FuriaSuit.Wild, -1, FuriaAction.Wild)
        val state = base.copy(
            phase = FuriaPhase.Play,
            turn = 0,
            hands = listOf(listOf(wild), listOf(FuriaCard("other", FuriaSuit.Sol, 1))),
            discard = listOf(FuriaCard("top", FuriaSuit.Sol, 2)),
            color = FuriaSuit.Sol,
        )
        assertNotNull(game.play(state, "wild", null).error)
        val result = game.play(state, "wild", FuriaSuit.Mar)
        assertEquals(null, result.error)
        assertEquals(FuriaSuit.Mar, result.state.color)
        assertEquals(FuriaPhase.MatchEnd, result.state.phase)
    }

    @Test
    fun `furia bot returns an action`() {
        val game = FuriaGame(playerCount = 4, seed = 28)
        val state = game.newMatch().copy(turn = 1)
        assertNotNull(game.botAction(state))
    }

    @Test
    fun `poker deck has fifty two unique cards`() {
        val deck = PokerGame.deck()
        assertEquals(52, deck.size)
        assertEquals(52, deck.map { it.id }.toSet().size)
        assertEquals("A♠", PokerGame.cardLabel(PokerCard("s", PokerSuit.Spades, 14)))
    }

    @Test
    fun `poker starts with two hole cards and blinds`() {
        val game = PokerGame(playerCount = 4, seed = 30)
        val state = game.newMatch()
        assertEquals(4, state.players.size)
        assertTrue(state.players.all { it.hole.size == 2 })
        assertEquals(30, game.potTotal(state))
        assertEquals(20, state.currentBet)
        assertEquals(PokerStreet.Preflop, state.street)
    }

    @Test
    fun `poker evaluates hand categories`() {
        val game = PokerGame(playerCount = 2, seed = 31)
        fun card(rank: Int, suit: PokerSuit) = PokerCard("$suit-$rank", suit, rank)
        val royal = listOf(
            card(14, PokerSuit.Spades), card(13, PokerSuit.Spades), card(12, PokerSuit.Spades),
            card(11, PokerSuit.Spades), card(10, PokerSuit.Spades),
        )
        assertEquals(8, game.evaluateHand(royal).category)
        val quads = listOf(
            card(9, PokerSuit.Spades), card(9, PokerSuit.Hearts), card(9, PokerSuit.Diamonds),
            card(9, PokerSuit.Clubs), card(2, PokerSuit.Spades),
        )
        assertEquals(7, game.evaluateHand(quads).category)
        val full = listOf(
            card(8, PokerSuit.Spades), card(8, PokerSuit.Hearts), card(8, PokerSuit.Diamonds),
            card(3, PokerSuit.Clubs), card(3, PokerSuit.Spades),
        )
        assertEquals(6, game.evaluateHand(full).category)
        val flush = listOf(
            card(14, PokerSuit.Hearts), card(9, PokerSuit.Hearts), card(7, PokerSuit.Hearts),
            card(4, PokerSuit.Hearts), card(2, PokerSuit.Hearts),
        )
        assertEquals(5, game.evaluateHand(flush).category)
        val straight = listOf(
            card(9, PokerSuit.Spades), card(8, PokerSuit.Hearts), card(7, PokerSuit.Diamonds),
            card(6, PokerSuit.Clubs), card(5, PokerSuit.Spades),
        )
        assertEquals(4, game.evaluateHand(straight).category)
        val pair = listOf(
            card(10, PokerSuit.Spades), card(10, PokerSuit.Hearts), card(7, PokerSuit.Diamonds),
            card(4, PokerSuit.Clubs), card(2, PokerSuit.Spades),
        )
        assertEquals(1, game.evaluateHand(pair).category)
    }

    @Test
    fun `poker fold awards the pot`() {
        val game = PokerGame(playerCount = 2, seed = 32)
        val state = game.newMatch()
        val result = game.fold(state)
        assertEquals(null, result.error)
        assertEquals(PokerStreet.HandEnd, result.state.street)
        assertEquals(1, result.state.handWinner?.seat)
        assertEquals(810, result.state.players[1].chips)
    }

    @Test
    fun `poker check and call keep the hand alive`() {
        val game = PokerGame(playerCount = 2, seed = 33)
        val state = game.newMatch()
        val called = game.call(state)
        assertEquals(null, called.error)
        val checked = game.check(called.state)
        assertEquals(null, checked.error)
        assertEquals(PokerStreet.Flop, checked.state.street)
    }

    @Test
    fun `poker bot returns an action`() {
        val game = PokerGame(playerCount = 4, seed = 34)
        val state = game.newMatch().copy(turn = 1)
        assertNotNull(game.botAction(state))
    }

    @Test
    fun `poker showdown awards the best kicker`() {
        val game = PokerGame(playerCount = 2, seed = 35)
        val base = game.newMatch()
        fun c(rank: Int, suit: PokerSuit) = PokerCard("$suit-$rank", suit, rank)
        val community = listOf(
            c(4, PokerSuit.Diamonds), c(9, PokerSuit.Hearts), c(6, PokerSuit.Diamonds),
            c(4, PokerSuit.Hearts), c(6, PokerSuit.Hearts),
        )
        val state = base.copy(
            players = listOf(
                base.players[0].copy(hole = listOf(c(14, PokerSuit.Diamonds), c(2, PokerSuit.Clubs)), folded = false, totalBet = 20),
                base.players[1].copy(hole = listOf(c(10, PokerSuit.Clubs), c(3, PokerSuit.Clubs)), folded = false, totalBet = 20),
            ),
            community = community,
            street = PokerStreet.River,
            currentBet = 0,
        )
        val result = game.resolveShowdownForTest(state)
        assertEquals(0, result.state.handWinner?.seat)
        assertTrue((result.state.players[0].chips) > 800)
    }

    @Test
    fun `poker showdown splits main and side pots`() {
        val game = PokerGame(playerCount = 3, seed = 36)
        val base = game.newMatch()
        fun c(rank: Int, suit: PokerSuit) = PokerCard("$suit-$rank", suit, rank)
        val community = listOf(
            c(2, PokerSuit.Clubs), c(7, PokerSuit.Diamonds), c(9, PokerSuit.Hearts),
            c(11, PokerSuit.Spades), c(4, PokerSuit.Clubs),
        )
        val state = base.copy(
            players = listOf(
                base.players[0].copy(hole = listOf(c(14, PokerSuit.Spades), c(14, PokerSuit.Hearts)), totalBet = 10),
                base.players[1].copy(hole = listOf(c(13, PokerSuit.Diamonds), c(13, PokerSuit.Hearts)), totalBet = 20),
                base.players[2].copy(hole = listOf(c(12, PokerSuit.Diamonds), c(12, PokerSuit.Hearts)), totalBet = 20),
            ),
            community = community,
            street = PokerStreet.River,
            currentBet = 0,
        )
        val result = game.resolveShowdownForTest(state)
        assertEquals(0, result.state.handWinner?.seat)
        assertTrue((result.state.players[0].chips) > 800)
        assertTrue((result.state.players[1].chips) > 800)
    }

    @Test
    fun `brisca deck has forty unique cards and 120 points`() {
        val deck = BriscaGame.deck()
        assertEquals(40, deck.size)
        assertEquals(40, deck.map { it.id }.toSet().size)
        assertEquals(120, deck.sumOf { BriscaGame.points(it) })
    }

    @Test
    fun `brisca deal leaves three cards each and a trump`() {
        val game = BriscaGame(playerCount = 2, target = 100, seed = 11)
        val state = game.newMatch()
        assertEquals(3, state.hands[0].size)
        assertEquals(3, state.hands[1].size)
        assertEquals(34, state.deck.size + if (state.trumpCard != null) 1 else 0)
        assertNotNull(state.trumpCard)
        assertEquals(state.trumpCard?.suit, state.trumpSuit)
        assertEquals(11, BriscaGame.points(BriscaGame.deck().first { it.rank == 1 }))
    }

    @Test
    fun `brisca trump beats a higher card of another suit`() {
        val game = BriscaGame(seed = 5)
        val lead = SpanishCard("espadas-1", SpanishSuit.Espadas, 1)
        val trump = SpanishCard("oros-2", SpanishSuit.Oros, 2)
        val trick = listOf(
            BriscaPlay(0, lead),
            BriscaPlay(1, trump),
        )
        assertEquals(1, game.winningPlay(trick, SpanishSuit.Oros)?.seat)
    }

    @Test
    fun `brisca plays a whole hand and scores every card`() {
        val game = BriscaGame(playerCount = 2, target = 100, seed = 42)
        var state = game.newMatch()
        var guard = 0
        while (state.phase == BriscaPhase.Playing && guard++ < 400) {
            val action = game.botAction(state)
            val card = when (action) {
                is BriscaAction.PlayCard -> game.handOf(state, state.turn).first { it.id == action.cardId }
                else -> game.handOf(state, state.turn).firstOrNull()
            }
            if (card == null) break
            val result = game.playCard(state, card.id)
            assertEquals(null, result.error)
            state = result.state
        }
        assertTrue(state.phase == BriscaPhase.HandEnd || state.phase == BriscaPhase.MatchEnd)
        assertEquals(120, state.scores.sum())
        assertTrue(state.history.size >= 20)
    }

    @Test
    fun `truco tanto uses the best pair and figures are worth zero`() {
        val hand = listOf(
            SpanishCard("oros-7", SpanishSuit.Oros, 7),
            SpanishCard("copas-7", SpanishSuit.Copas, 7),
            SpanishCard("espadas-6", SpanishSuit.Espadas, 6),
        )
        assertEquals(33, TrucoGame.tanto(hand))
        val figures = listOf(
            SpanishCard("oros-12", SpanishSuit.Oros, 12),
            SpanishCard("copas-11", SpanishSuit.Copas, 11),
            SpanishCard("espadas-7", SpanishSuit.Espadas, 7),
        )
        assertEquals(7, TrucoGame.tanto(figures))
    }

    @Test
    fun `truco deals three cards and accepts envido`() {
        val game = TrucoGame(target = 30, seed = 9)
        val start = game.newMatch()
        assertEquals(3, start.hands[0].size)
        assertEquals(3, start.hands[1].size)
        assertEquals(TrucoPhase.Envido, start.phase)
        val called = game.callEnvido(start, start.mano, "envido")
        assertNotNull(called.state.pending)
        val resolved = game.respond(called.state, true)
        assertEquals(TrucoPhase.Play, resolved.state.phase)
        assertNotNull(resolved.state.envidoReveal)
        assertEquals(2, resolved.state.scores.sum())
    }

    @Test
    fun `truco accepted raises the stake and rejection awards the previous stake`() {
        val game = TrucoGame(target = 30, seed = 12)
        var state = game.startPlay(game.newMatch()).state
        val called = game.callTruco(state, 0)
        assertEquals(2, called.state.pending?.stake)
        state = game.respond(called.state, true).state
        assertEquals(2, state.handStake)
        assertEquals(TrucoPhase.Play, state.phase)
        val first = game.botPlayCard(state, state.turn)!!
        state = game.playCard(state, first.id).state
        val second = game.botPlayCard(state, state.turn)!!
        state = game.playCard(state, second.id).state
        val retruco = game.callTruco(state, state.turn)
        assertEquals(3, retruco.state.pending?.stake)
        val rejected = game.respond(retruco.state, false)
        assertEquals(TrucoPhase.HandEnd, rejected.state.phase)
        assertEquals(2, rejected.state.scores.sum())
    }

    @Test
    fun `truco bot can finish a match without getting stuck`() {
        val game = TrucoGame(target = 15, seed = 21)
        var state = game.newMatch()
        var guard = 0
        while (state.phase != TrucoPhase.MatchEnd && guard++ < 600) {
            val pending = state.pending
            state = when {
                pending != null -> {
                    val accept = if (pending.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
                        game.botAcceptsEnvido(state, pending.awaiting, pending.stake)
                    } else {
                        game.botAcceptsTruco(state, pending.awaiting, pending.stake)
                    }
                    game.respond(state, accept).state
                }
                state.phase == TrucoPhase.Envido -> {
                    if (game.botWantsEnvido(state, state.turn)) {
                        game.callEnvido(state, state.turn).state
                    } else {
                        game.startPlay(state).state
                    }
                }
                state.phase == TrucoPhase.Play -> {
                    if (state.turn == 0 && game.botShouldCallTruco(state, 0)) {
                        game.callTruco(state, 0).state
                    } else if (state.turn == 1 && game.botShouldCallTruco(state, 1)) {
                        game.callTruco(state, 1).state
                    } else {
                        val card = game.botPlayCard(state, state.turn)!!
                        game.playCard(state, card.id).state
                    }
                }
                state.phase == TrucoPhase.HandEnd -> game.nextHand(state).state
                else -> state
            }
        }
        assertEquals(TrucoPhase.MatchEnd, state.phase)
        assertTrue(state.scores.any { it >= 15 })
        assertTrue(state.history.isNotEmpty())
    }

    @Test
    fun `soroban value follows the rods and check solves the target`() {
        val game = SorobanGame(seed = 5)
        var state = game.newGame(1)
        assertEquals(0L, game.value(state))
        state = game.botTarget(state)
        assertEquals(state.target, game.value(state))
        val result = game.check(state)
        assertEquals(true, result.correct)
        assertEquals(1, result.state.solved)
        assertTrue(result.state.score > 0)
        assertTrue(result.state.target > 0)
    }

    @Test
    fun `soroban completes ten challenges`() {
        val game = SorobanGame(targetCount = 10, seed = 8)
        var state = game.newGame(1)
        var guard = 0
        while (state.phase == SorobanPhase.Playing && guard++ < 20) {
            state = game.botTarget(state)
            val result = game.check(state)
            state = result.state
        }
        assertEquals(SorobanPhase.Complete, state.phase)
        assertEquals(10, state.solved)
        assertTrue(state.score > 0)
        assertTrue(state.bestStreak >= 1)
    }

    @Test
    fun `shogi initial board has forty pieces and legal moves`() {
        val game = ShogiGame(seed = 3)
        val state = game.newMatch()
        assertEquals(81, state.board.size)
        assertEquals(40, state.board.count { it != null })
        assertTrue(game.legalMoves(state).isNotEmpty())
        assertEquals(0, state.turn)
    }

    @Test
    fun `shogi promotes a pawn in the promotion zone`() {
        val game = ShogiGame(seed = 4)
        val base = game.newMatch()
        val board = base.board.toMutableList()
        board[ShogiGame.index(2, 0)] = ShogiPiece(0, ShogiKind.Pawn)
        board[ShogiGame.index(6, 0)] = null
        val state = base.copy(board = board)
        val move = ShogiMove(ShogiGame.index(2, 0), ShogiGame.index(1, 0), promote = true)
        val result = game.move(state, move)
        assertEquals(null, result.error)
        assertTrue(result.state.board[ShogiGame.index(1, 0)]?.promoted == true)
    }

    @Test
    fun `shogi capture adds the piece to the hand`() {
        val game = ShogiGame(seed = 6)
        val base = game.newMatch()
        val board = base.board.toMutableList()
        board[ShogiGame.index(4, 4)] = ShogiPiece(0, ShogiKind.Rook)
        board[ShogiGame.index(3, 4)] = ShogiPiece(1, ShogiKind.Pawn)
        val state = base.copy(board = board)
        val move = ShogiMove(ShogiGame.index(4, 4), ShogiGame.index(3, 4))
        val result = game.move(state, move)
        assertEquals(null, result.error)
        assertEquals(1, result.state.hands[0][ShogiKind.Pawn])
        assertEquals(null, result.state.board[ShogiGame.index(4, 4)])
    }

    @Test
    fun `janggi initial board has thirty two pieces and legal moves`() {
        val game = JanggiGame(seed = 7)
        val state = game.newMatch()
        assertEquals(90, state.board.size)
        assertEquals(32, state.board.count { it != null })
        assertTrue(game.legalMoves(state).isNotEmpty())
        assertEquals(JanggiSide.Cho, state.turn)
    }

    @Test
    fun `janggi cannon jumps one screen to capture`() {
        val game = JanggiGame(seed = 9)
        val board = MutableList<JanggiPiece?>(JanggiGame.COLS * JanggiGame.ROWS) { null }
        board[JanggiGame.indexOf(4, 0)] = JanggiPiece(JanggiSide.Han, JanggiKind.General)
        board[JanggiGame.indexOf(4, 9)] = JanggiPiece(JanggiSide.Cho, JanggiKind.General)
        board[JanggiGame.indexOf(4, 5)] = JanggiPiece(JanggiSide.Han, JanggiKind.Soldier)
        board[JanggiGame.indexOf(0, 0)] = JanggiPiece(JanggiSide.Cho, JanggiKind.Cannon)
        board[JanggiGame.indexOf(0, 1)] = JanggiPiece(JanggiSide.Han, JanggiKind.Soldier)
        board[JanggiGame.indexOf(0, 2)] = JanggiPiece(JanggiSide.Han, JanggiKind.Soldier)
        val state = JanggiState(board = board, turn = JanggiSide.Cho)
        val result = game.move(state, JanggiMove(JanggiGame.indexOf(0, 0), JanggiGame.indexOf(0, 2)))
        assertEquals(null, result.error)
        assertEquals(JanggiKind.Cannon, result.state.board[JanggiGame.indexOf(0, 2)]?.kind)
        assertTrue(result.state.history.any { it.contains("x") })
    }
}
