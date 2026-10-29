package com.mesaviva.app.shell

import com.mesaviva.app.games.BriscaGame
import com.mesaviva.app.games.BriscaPhase
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DominoGame
import com.mesaviva.app.games.MancalaGame
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.TrucoGame
import com.mesaviva.app.games.TrucoPhase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCodecTest {

    @Test
    fun damasRoundTripKeepsBoardTurnAndCaptures() {
        val game = DamasGame(42)
        val state = game.newMatch()
        val decoded = RoomCodec.damasFromJson(RoomCodec.damasToJson(state))
        assertNotNull(decoded)
        assertEquals(state.board, decoded!!.board)
        assertEquals(state.turn, decoded.turn)
        assertEquals(state.phase, decoded.phase)
        assertEquals(state.captures, decoded.captures)
        assertEquals(DamasColor.White, decoded.turn)
    }

    @Test
    fun damasRoundTripKeepsHistoryAndLastMove() {
        val game = DamasGame(7)
        val initial = game.newMatch()
        val result = game.move(initial, 40, 33)
        assertNull(result.error)
        assertEquals(1, result.state.history.size)
        val decoded = RoomCodec.damasFromJson(RoomCodec.damasToJson(result.state))
        assertNotNull(decoded)
        assertEquals(result.state.history, decoded!!.history)
        assertEquals(result.state.lastMove, decoded.lastMove)
        assertEquals(DamasColor.Black, decoded.turn)
        assertEquals(result.state.board, decoded.board)
    }

    @Test
    fun damasRejectsAnIncompleteBoard() {
        val payload = JSONObject()
            .put("board", JSONArray().put("W"))
            .put("turn", "white")
            .put("phase", "play")
        assertNull(RoomCodec.damasFromJson(payload))
    }

    @Test
    fun damasKeepsMatchEndState() {
        val game = DamasGame(3)
        val match = game.newMatch().copy(
            phase = DamasPhase.MatchEnd,
            winner = DamasColor.Black,
            draw = false,
            reason = "damas.reason.resign",
        )
        val decoded = RoomCodec.damasFromJson(RoomCodec.damasToJson(match))
        assertNotNull(decoded)
        assertEquals(DamasPhase.MatchEnd, decoded!!.phase)
        assertEquals(DamasColor.Black, decoded.winner)
        assertEquals("damas.reason.resign", decoded.reason)
    }

    @Test
    fun mancalaRoundTripKeepsPitsAndTurn() {
        val game = MancalaGame(11)
        val state = game.newMatch()
        val decoded = RoomCodec.mancalaFromJson(RoomCodec.mancalaToJson(state))
        assertNotNull(decoded)
        assertEquals(state.pits, decoded!!.pits)
        assertEquals(MancalaSide.White, decoded.turn)
        assertEquals(0, decoded.whiteStore)
        assertEquals(0, decoded.blackStore)
    }

    @Test
    fun mancalaSimulatedTurnSurvivesTheWire() {
        val game = MancalaGame(19)
        val result = game.sow(game.newMatch(), 0)
        assertNull(result.error)
        val decoded = RoomCodec.mancalaFromJson(RoomCodec.mancalaToJson(result.state))
        assertNotNull(decoded)
        assertEquals(result.state.pits, decoded!!.pits)
        assertEquals(result.state.turn, decoded.turn)
        assertEquals(result.state.lastPit, decoded.lastPit)
    }

    @Test
    fun mancalaRejectsWrongPitCount() {
        val payload = JSONObject()
            .put("pits", JSONArray().put(4).put(4))
            .put("turn", "white")
        assertNull(RoomCodec.mancalaFromJson(payload))
    }

    @Test
    fun snakesLadderMovesTheSeatAndPassesTheTurn() {
        val state = SnakesRoomRules.newMatch(
            listOf("Ana", "Beto", "Carla", "Diego"),
            listOf(false, false, true, true),
        )
        val next = SnakesRoomRules.roll(state, 0, 3)
        assertEquals(22, next.positions[0])
        assertEquals("ladder", next.lastEvent)
        assertEquals(1, next.turn)
        assertEquals(3, next.dice)
    }

    @Test
    fun snakesSnakeSendsTheSeatBack() {
        val base = SnakesRoomRules.newMatch(listOf("Ana", "Beto"), listOf(false, false))
        val state = base.copy(positions = listOf(14, 0))
        val next = SnakesRoomRules.roll(state, 0, 3)
        assertEquals(4, next.positions[0])
        assertEquals("snake", next.lastEvent)
        assertEquals(1, next.turn)
    }

    @Test
    fun snakesNeedsTheExactNumberToWin() {
        val base = SnakesRoomRules.newMatch(listOf("Ana", "Beto"), listOf(false, false))
        val state = base.copy(positions = listOf(98, 0))
        val next = SnakesRoomRules.roll(state, 0, 3)
        assertEquals(98, next.positions[0])
        assertEquals("needExact", next.lastEvent)
        assertEquals(1, next.turn)
    }

    @Test
    fun snakesRejectsAnOutOfTurnRoll() {
        val state = SnakesRoomRules.newMatch(listOf("Ana", "Beto"), listOf(false, false))
        val next = SnakesRoomRules.roll(state, 1, 6)
        assertEquals(state, next)
    }

    @Test
    fun snakesWinsOnExactlyOneHundred() {
        val base = SnakesRoomRules.newMatch(listOf("Ana", "Beto"), listOf(false, false))
        val state = base.copy(positions = listOf(99, 0))
        val next = SnakesRoomRules.roll(state, 0, 1)
        assertEquals(100, next.positions[0])
        assertEquals(0, next.winner)
        assertEquals("win", next.lastEvent)
        assertTrue(next.finished)
    }

    @Test
    fun snakesRoundTripKeepsFourSeats() {
        val state = SnakesRoomRules.newMatch(
            listOf("Ana", "Beto", "Carla", "Diego"),
            listOf(false, true, true, true),
        )
        val decoded = RoomCodec.snakesFromJson(RoomCodec.snakesToJson(state))
        assertNotNull(decoded)
        assertEquals(state.names, decoded!!.names)
        assertEquals(state.bots, decoded.bots)
        assertEquals(state.positions, decoded.positions)
        assertEquals(state.turn, decoded.turn)
    }

    @Test
    fun dominoRedactsEveryHandExceptTheViewer() {
        val game = DominoGame(4, teams = false, target = 100, seed = 31L)
        val state = game.newMatch()
        val decoded = RoomCodec.dominoFromJson(RoomCodec.dominoToJson(state, 1))
        assertNotNull(decoded)
        assertEquals(state.players[1].hand.size, decoded!!.players[1].hand.size)
        assertEquals(0, decoded.players[0].hand.size)
        assertEquals(state.players[0].hand.size, decoded.handCounts[0])
        assertEquals(state.players[1].hand, decoded.players[1].hand)
    }

    @Test
    fun dominoRoundTripKeepsChainEndsAndScores() {
        val game = DominoGame(2, teams = false, target = 100, seed = 41L)
        val state = game.newMatch()
        val decoded = RoomCodec.dominoFromJson(RoomCodec.dominoToJson(state, 0))
        assertNotNull(decoded)
        assertEquals(state.chain, decoded!!.chain)
        assertEquals(state.left, decoded.left)
        assertEquals(state.right, decoded.right)
        assertEquals(state.turn, decoded.turn)
        assertEquals(state.handNo, decoded.handNo)
        assertEquals(state.target, decoded.target)
        assertEquals(state.boneyard.size, decoded.boneyard.size)
        assertEquals(state.players.map { it.score }, decoded.players.map { it.score })
    }

    @Test
    fun dominoRejectsAnInvalidPlayerCount() {
        val payload = JSONObject()
            .put("players", JSONArray().put(JSONObject().put("name", "Solo")))
            .put("turn", 0)
        assertNull(RoomCodec.dominoFromJson(payload))
    }

    @Test
    fun dominoDoesNotLeakTheBoneyardTiles() {
        val game = DominoGame(4, teams = false, target = 100, seed = 13L)
        val state = game.newMatch()
        val payload = RoomCodec.dominoToJson(state, 2)
        assertEquals(state.boneyard.size, payload.optInt("boneyard"))
        assertEquals(false, payload.has("boneyardTiles"))
    }

    @Test
    fun damasBotsAreDeterministicWithTheSameSeed() {
        val first = DamasGame(2026)
        val second = DamasGame(2026)
        val moveA = first.botMove(first.newMatch(), "easy")
        val moveB = second.botMove(second.newMatch(), "easy")
        assertEquals(moveA, moveB)
    }

    @Test
    fun mancalaPitsAreDeterministicWithTheSameSeed() {
        val first = MancalaGame(77)
        val second = MancalaGame(77)
        val stateA = first.newMatch()
        val stateB = second.newMatch()
        assertEquals(first.botPit(stateA), second.botPit(stateB))
        assertTrue(first.botPit(stateA) in 0..5)
    }

    @Test
    fun briscaCodecHidesRivalHandsAndKeepsTrump() {
        val game = BriscaGame(playerCount = 2, target = 100, seed = 42)
        val state = game.newMatch()
        val json = RoomCodec.briscaToJson(state, viewerSeat = 0)
        val seats = json.getJSONArray("seats")
        val own = seats.getJSONObject(0).getJSONArray("hand")
        val rival = seats.getJSONObject(1).getJSONArray("hand")
        assertEquals(3, own.length())
        assertEquals(3, rival.length())
        for (index in 0 until rival.length()) {
            assertTrue(rival.getJSONObject(index).getString("id").startsWith("h1-"))
        }
        val encoded = json.toString()
        for (card in state.hands[1]) {
            assertTrue("La carta rival no debe viajar: ${card.id}", !encoded.contains("\"id\":\"${card.id}\""))
        }
        assertEquals(state.trumpCard?.id, RoomCodec.briscaFromJson(json)?.trumpCard?.id)
    }

    @Test
    fun briscaCodecRoundTripKeepsScoresTurnAndHistory() {
        val game = BriscaGame(playerCount = 2, target = 100, seed = 11)
        var state = game.newMatch()
        val action = game.botAction(state)
        if (action is com.mesaviva.app.games.BriscaAction.PlayCard) {
            state = game.playCard(state, action.cardId).state
        }
        val decoded = RoomCodec.briscaFromJson(RoomCodec.briscaToJson(state, 1))
        assertNotNull(decoded)
        assertEquals(state.scores, decoded!!.scores)
        assertEquals(state.turn, decoded.turn)
        assertEquals(state.handNo, decoded.handNo)
        assertEquals(state.history, decoded.history)
        assertEquals(state.phase, decoded.phase)
    }

    @Test
    fun trucoCodecHidesRivalHandAndCarriesPendingCall() {
        val game = TrucoGame(target = 30, seed = 9)
        var state = game.newMatch()
        state = game.startPlay(state).state
        state = game.callTruco(state, 0).state
        val json = RoomCodec.trucoToJson(state, viewerSeat = 0)
        val seats = json.getJSONArray("seats")
        val rival = seats.getJSONObject(1).getJSONArray("hand")
        assertEquals(3, rival.length())
        for (index in 0 until rival.length()) {
            assertTrue(rival.getJSONObject(index).getString("id").startsWith("th1-"))
        }
        val encoded = json.toString()
        for (card in state.hands[1]) {
            assertTrue("La carta rival no debe viajar: ${card.id}", !encoded.contains("\"id\":\"${card.id}\""))
        }
        val decoded = RoomCodec.trucoFromJson(json)
        assertNotNull(decoded)
        assertEquals(TrucoPhase.Play, decoded!!.phase)
        assertEquals(2, decoded.pending?.stake)
        assertEquals(1, decoded.pending?.awaiting)
        assertEquals(state.history, decoded.history)
    }
}
