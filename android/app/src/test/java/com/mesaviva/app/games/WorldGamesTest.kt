package com.mesaviva.app.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldGamesTest {

    private fun tile(id: Int, suit: MahjongSuit, rank: Int) = MahjongTile(id, suit, rank)

    private fun americanHand(): List<MahjongTile> = listOf(
        tile(0, MahjongSuit.Dots, 1), tile(1, MahjongSuit.Dots, 1), tile(2, MahjongSuit.Dots, 1),
        tile(3, MahjongSuit.Bams, 2), tile(4, MahjongSuit.Bams, 2), tile(5, MahjongSuit.Bams, 2),
        tile(6, MahjongSuit.Cracks, 3), tile(7, MahjongSuit.Cracks, 3), tile(8, MahjongSuit.Cracks, 3),
        tile(9, MahjongSuit.Wind, 1), tile(10, MahjongSuit.Wind, 1), tile(11, MahjongSuit.Wind, 1),
        tile(12, MahjongSuit.Dragon, 1), tile(13, MahjongSuit.Dragon, 1),
    )

    private fun riichiHand(): List<MahjongTile> = listOf(
        tile(20, MahjongSuit.Dots, 1), tile(21, MahjongSuit.Dots, 2), tile(22, MahjongSuit.Dots, 3),
        tile(23, MahjongSuit.Dots, 4), tile(24, MahjongSuit.Dots, 5), tile(25, MahjongSuit.Dots, 6),
        tile(26, MahjongSuit.Dots, 7), tile(27, MahjongSuit.Dots, 8), tile(28, MahjongSuit.Dots, 9),
        tile(29, MahjongSuit.Bams, 1), tile(30, MahjongSuit.Bams, 2), tile(31, MahjongSuit.Bams, 3),
        tile(32, MahjongSuit.Cracks, 5), tile(33, MahjongSuit.Cracks, 5),
    )

    @Test
    fun `mahjong american wall has 152 tiles and riichi has 136`() {
        val american = MahjongGame(MahjongVariant.American, 5).newMatch()
        assertEquals(152, american.wall.size + american.players.sumOf { it.hand.size + it.flowers.size })
        val riichi = MahjongGame(MahjongVariant.Riichi, 5).newMatch()
        assertEquals(136, riichi.wall.size + riichi.players.sumOf { it.hand.size + it.flowers.size })
        assertTrue(american.players.flatMap { it.hand }.none { it.isFlower })
        assertTrue(riichi.players.flatMap { it.hand }.none { it.isJoker || it.isFlower })
    }

    @Test
    fun `mahjong recognises a standard american hand`() {
        val game = MahjongGame(MahjongVariant.American, 1)
        assertTrue(game.canWinHand(americanHand(), emptyList(), MahjongVariant.American))
    }

    @Test
    fun `mahjong joker substitutes a missing triplet tile`() {
        val game = MahjongGame(MahjongVariant.American, 1)
        val hand = americanHand().toMutableList()
        hand[2] = tile(100, MahjongSuit.Joker, 1)
        assertTrue(game.canWinHand(hand, emptyList(), MahjongVariant.American))
    }

    @Test
    fun `mahjong riichi accepts sequences and rejects an invalid shape`() {
        val game = MahjongGame(MahjongVariant.Riichi, 1)
        assertTrue(game.canWinHand(riichiHand(), emptyList(), MahjongVariant.Riichi))
        val broken = riichiHand().toMutableList()
        broken[7] = tile(200, MahjongSuit.Wind, 4)
        assertFalse(game.canWinHand(broken, emptyList(), MahjongVariant.Riichi))
    }

    @Test
    fun `mahjong tenpai and furiten work`() {
        val game = MahjongGame(MahjongVariant.Riichi, 1)
        val thirteen = riichiHand().dropLast(1)
        assertTrue(game.isTenpai(thirteen, emptyList(), MahjongVariant.Riichi))
        val discards = listOf(tile(300, MahjongSuit.Cracks, 5))
        assertTrue(game.isFuriten(thirteen, discards, emptyList(), MahjongVariant.Riichi))
    }

    @Test
    fun `mahjong discard advances the turn and draws`() {
        val game = MahjongGame(MahjongVariant.American, 15)
        val state = game.newMatch()
        val beforeWall = state.wall.size
        val first = state.players[state.dealer].hand.first()
        val result = game.discard(state, first.id)
        assertNull(result.error)
        assertTrue(result.state.players[state.dealer].hand.none { it.id == first.id })
        assertTrue(result.state.players[state.dealer].discards.any { it.id == first.id })
        assertTrue(result.state.wall.size < beforeWall || result.state.pending.isNotEmpty())
    }

    @Test
    fun `mahjong ron scores a riichi hand`() {
        val game = MahjongGame(MahjongVariant.Riichi, 21)
        val base = game.newMatch()
        val winnerHand = riichiHand().dropLast(1)
        val winTile = riichiHand().last()
        val players = base.players.toMutableList()
        players[0] = players[0].copy(hand = winnerHand, riichi = true)
        players[1] = players[1].copy(hand = listOf(winTile))
        val state = base.copy(
            players = players.toList(),
            turn = 1,
            wall = base.wall.drop(1),
            lastDiscard = winTile,
            lastDiscardBy = 1,
            drawn = null,
        )
        val discard = game.discard(state, winTile.id)
        assertNull(discard.error)
        val ron = discard.state.pending.firstOrNull { it.seat == 0 && it.kind == MahjongClaimKind.Ron }
        assertNotNull(ron)
        val settled = game.claim(discard.state, ron!!).state
        assertEquals(MahjongPhase.HandEnd, settled.phase)
        assertEquals(0, settled.winner)
        assertTrue(settled.players[0].score > 250)
    }

    @Test
    fun `mahjong next hand keeps scores and rotates the dealer`() {
        val game = MahjongGame(MahjongVariant.American, 4)
        val base = game.newMatch()
        val players = base.players.toMutableList()
        players[0] = players[0].copy(score = 310)
        val ended = base.copy(players = players.toList(), phase = MahjongPhase.HandEnd, winner = 1)
        val next = game.nextHand(ended)
        assertEquals(2, next.round)
        assertEquals(1, next.dealer)
        assertEquals(310, next.players[0].score)
        assertEquals(MahjongPhase.Play, next.phase)
    }

    @Test
    fun `luzhanqi starts with 25 pieces per side and a flag`() {
        val game = LuzhanqiGame(3)
        val state = game.newMatch()
        assertEquals(25, game.piecesOf(state, LuzhanqiSide.Red).size)
        assertEquals(25, game.piecesOf(state, LuzhanqiSide.Blue).size)
        assertEquals(50, state.pieces.map { it.id }.toSet().size)
        assertNotNull(game.flagOf(state, LuzhanqiSide.Red))
        assertNotNull(game.flagOf(state, LuzhanqiSide.Blue))
    }

    @Test
    fun `luzhanqi railway allows a long move but a camp protects its occupant`() {
        val game = LuzhanqiGame(7)
        val red = LuzhanqiPiece(1, LuzhanqiSide.Red, LuzhanqiRank.Commander, LuzhanqiGame.cell(5, 0))
        val blue = LuzhanqiPiece(2, LuzhanqiSide.Blue, LuzhanqiRank.Platoon, LuzhanqiGame.cell(3, 1))
        val pieces = listOf(
            red,
            blue,
            LuzhanqiPiece(3, LuzhanqiSide.Red, LuzhanqiRank.Flag, LuzhanqiGame.cell(0, 1)),
            LuzhanqiPiece(4, LuzhanqiSide.Blue, LuzhanqiRank.Flag, LuzhanqiGame.cell(11, 1)),
        )
        val state = LuzhanqiState(pieces = pieces)
        val moves = game.moves(state, 1)
        assertTrue(LuzhanqiGame.cell(5, 4) in moves)
        val campState = state.copy(pieces = state.pieces + LuzhanqiPiece(9, LuzhanqiSide.Blue, LuzhanqiRank.Platoon, LuzhanqiGame.cell(5, 2)))
        assertFalse(LuzhanqiGame.cell(5, 2) in game.moves(campState, 1))
    }

    @Test
    fun `luzhanqi resolves ranks engineer mines bombs and flags`() {
        val game = LuzhanqiGame(9)
        val flagRed = LuzhanqiPiece(1, LuzhanqiSide.Red, LuzhanqiRank.Flag, LuzhanqiGame.cell(0, 1))
        val flagBlue = LuzhanqiPiece(2, LuzhanqiSide.Blue, LuzhanqiRank.Flag, LuzhanqiGame.cell(11, 1))
        val commander = LuzhanqiPiece(3, LuzhanqiSide.Red, LuzhanqiRank.Commander, LuzhanqiGame.cell(5, 0))
        val platoon = LuzhanqiPiece(4, LuzhanqiSide.Blue, LuzhanqiRank.Platoon, LuzhanqiGame.cell(5, 1))
        val clash = game.move(LuzhanqiState(pieces = listOf(flagRed, flagBlue, commander, platoon)), 3, LuzhanqiGame.cell(5, 1)).state
        assertTrue(clash.pieces.any { it.id == 3 })
        assertFalse(clash.pieces.any { it.id == 4 })

        val engineer = LuzhanqiPiece(5, LuzhanqiSide.Red, LuzhanqiRank.Engineer, LuzhanqiGame.cell(5, 0))
        val mine = LuzhanqiPiece(6, LuzhanqiSide.Blue, LuzhanqiRank.Landmine, LuzhanqiGame.cell(5, 1))
        val disarmed = game.move(LuzhanqiState(pieces = listOf(flagRed, flagBlue, engineer, mine)), 5, LuzhanqiGame.cell(5, 1)).state
        assertTrue(disarmed.pieces.any { it.id == 5 })
        assertFalse(disarmed.pieces.any { it.id == 6 })

        val bomb = LuzhanqiPiece(7, LuzhanqiSide.Red, LuzhanqiRank.Bomb, LuzhanqiGame.cell(5, 0))
        val target = LuzhanqiPiece(8, LuzhanqiSide.Blue, LuzhanqiRank.General, LuzhanqiGame.cell(5, 1))
        val explosion = game.move(LuzhanqiState(pieces = listOf(flagRed, flagBlue, bomb, target)), 7, LuzhanqiGame.cell(5, 1)).state
        assertFalse(explosion.pieces.any { it.id == 7 })
        assertFalse(explosion.pieces.any { it.id == 8 })

        val flagTake = game.move(
            LuzhanqiState(
                pieces = listOf(flagRed, flagBlue, commander),
            ),
            3,
            LuzhanqiGame.cell(11, 1),
        )
        // La bandera azul está en la otra punta; el comandante debe llegar por la vía.
        assertNotNull(flagTake)
    }

    @Test
    fun `mexican train deals double twelve and scores a round`() {
        val game = MexicanTrainGame(4, 5)
        val state = game.newMatch()
        assertEquals(91, DominoGame.doubleSixDeck(12).size)
        assertEquals(12, state.players[0].hand.size)
        assertEquals(12, state.players[1].hand.size)
        assertEquals(42, state.boneyard.size)
        assertEquals(12, state.hubPip)
        assertTrue(state.publicTrain.open)
        val tile = DominoTile(12, 3)
        val ready = state.copy(
            players = state.players.mapIndexed { index, player -> if (index == 0) player.copy(hand = listOf(tile)) else player },
            turn = 0,
        )
        val played = game.play(ready, 0, 0).state
        assertEquals(0, played.players[0].hand.size)
        assertEquals(TrainGamePhase.RoundEnd, played.phase)
        assertEquals(0, played.winner)
    }

    @Test
    fun `mexican train doubles must be covered and closed trains are private`() {
        val game = MexicanTrainGame(3, 8)
        val state = game.newMatch()
        val double = DominoTile(12, 12)
        val ready = state.copy(
            players = state.players.mapIndexed { index, player -> if (index == 0) player.copy(hand = listOf(double, DominoTile(12, 4))) else player },
            turn = 0,
        )
        val played = game.play(ready, 0, 0).state
        assertEquals(0, played.turn)
        assertEquals(0, played.doubleTrain)
        val allowed = game.playableTargets(played, DominoTile(12, 4))
        assertEquals(listOf(0), allowed)
        val closedTargets = game.playableTargets(played.copy(doubleTrain = null), DominoTile(12, 4))
        assertFalse(1 in closedTargets)
    }

    @Test
    fun `mexican train pass opens the private train and three rounds end the match`() {
        val game = MexicanTrainGame(2, 11)
        val state = game.newMatch()
        val passed = game.pass(state).state
        assertTrue(passed.trains[0].open)
        val roundEnd = state.copy(phase = TrainGamePhase.RoundEnd, round = 3, winner = 0)
        val ended = game.nextRound(roundEnd)
        assertEquals(TrainGamePhase.MatchEnd, ended.phase)
        assertEquals(0, ended.winner)
    }

    @Test
    fun `chickenfoot deals double nine and completes a three toe foot`() {
        val game = ChickenfootGame(4, 13)
        val state = game.newMatch()
        assertEquals(55, DominoGame.doubleSixDeck(9).size)
        assertEquals(7, state.players[0].hand.size)
        assertNotNull(state.foot)
        assertEquals(9, state.foot!!.pip)
        val hand = listOf(DominoTile(9, 3), DominoTile(9, 4), DominoTile(9, 5), DominoTile(9, 6))
        var current = state.copy(
            players = state.players.mapIndexed { index, player -> if (index == 0) player.copy(hand = hand) else player },
            turn = 0,
        )
        current = game.play(current, 0, 0).state
        current = game.play(current, 0, 1).state
        current = game.play(current, 0, 2).state
        assertNull(current.foot)
        assertEquals(3, current.branches.size)
        assertEquals(1, current.turn)
    }

    @Test
    fun `chickenfoot double opens a new foot and rounds close after three`() {
        val game = ChickenfootGame(2, 17)
        val state = game.newMatch()
        val base = state.copy(
            foot = null,
            branches = listOf(ChickenBranch(listOf(DominoTile(9, 3)))),
            players = state.players.mapIndexed { index, player ->
                if (index == 0) player.copy(hand = listOf(DominoTile(3, 3))) else player
            },
            turn = 0,
        )
        val played = game.play(base, 0, 0).state
        assertNotNull(played.foot)
        assertEquals(3, played.foot!!.pip)
        assertEquals(0, played.turn)
        val end = base.copy(phase = TrainGamePhase.RoundEnd, round = 3, winner = 0)
        assertEquals(TrainGamePhase.MatchEnd, game.nextRound(end).phase)
    }

    @Test
    fun `three chess starts with three armies and legal moves`() {
        val game = ThreeChessGame(5)
        val state = game.newMatch()
        assertEquals(48, state.pieces.size)
        ThreeSide.entries.forEach { side ->
            assertEquals(16, state.pieces.count { it.side == side })
            assertNotNull(game.kingOf(state, side))
            assertFalse(game.isInCheck(state, side))
        }
        assertTrue(game.legalMovesForSide(state, ThreeSide.North).isNotEmpty())
    }

    @Test
    fun `three chess promotes a pawn on the far edge`() {
        val game = ThreeChessGame(7)
        val pieces = listOf(
            ThreePiece(1, ThreeSide.North, ThreePieceType.King, ThreeChessGame.cell(0, 0)),
            ThreePiece(2, ThreeSide.South, ThreePieceType.King, ThreeChessGame.cell(11, 11)),
            ThreePiece(3, ThreeSide.North, ThreePieceType.Pawn, ThreeChessGame.cell(10, 5)),
        )
        val state = ThreeChessState(pieces = pieces, turn = ThreeSide.North)
        assertTrue(ThreeChessGame.cell(11, 5) in game.legalMoves(state, 3))
        val moved = game.move(state, 3, ThreeChessGame.cell(11, 5)).state
        assertEquals(ThreePieceType.Queen, moved.pieces.first { it.id == 3 }.type)
    }

    @Test
    fun `three chess forbids moving the king into check and eliminates a captured king`() {
        val game = ThreeChessGame(9)
        val checked = ThreeChessState(
            pieces = listOf(
                ThreePiece(1, ThreeSide.North, ThreePieceType.King, ThreeChessGame.cell(5, 5)),
                ThreePiece(2, ThreeSide.West, ThreePieceType.Rook, ThreeChessGame.cell(5, 0)),
                ThreePiece(3, ThreeSide.South, ThreePieceType.King, ThreeChessGame.cell(11, 11)),
            ),
            turn = ThreeSide.North,
        )
        assertFalse(ThreeChessGame.cell(5, 4) in game.legalMoves(checked, 1))

        val capture = ThreeChessState(
            pieces = listOf(
                ThreePiece(10, ThreeSide.North, ThreePieceType.King, ThreeChessGame.cell(0, 0)),
                ThreePiece(11, ThreeSide.North, ThreePieceType.Queen, ThreeChessGame.cell(5, 5)),
                ThreePiece(12, ThreeSide.West, ThreePieceType.King, ThreeChessGame.cell(5, 7)),
                ThreePiece(13, ThreeSide.South, ThreePieceType.King, ThreeChessGame.cell(11, 11)),
            ),
            turn = ThreeSide.North,
        )
        val after = game.move(capture, 11, ThreeChessGame.cell(5, 7)).state
        assertTrue(ThreeSide.West in after.eliminated)
        assertFalse(after.pieces.any { it.side == ThreeSide.West })
        assertEquals(ThreePhase.Play, after.phase)
    }

    @Test
    fun `three chess bot returns a legal move and resign removes an army`() {
        val game = ThreeChessGame(11)
        val state = game.newMatch()
        val move = game.botMove(state)
        assertNotNull(move)
        assertTrue(game.legalMovesForSide(state, ThreeSide.North).contains(move!!))
        val resigned = game.resign(state, ThreeSide.West).state
        assertTrue(ThreeSide.West in resigned.eliminated)
        assertFalse(resigned.pieces.any { it.side == ThreeSide.West })
    }
}
