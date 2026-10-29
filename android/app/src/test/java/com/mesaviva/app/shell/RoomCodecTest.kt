package com.mesaviva.app.shell

import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.MancalaGame
import com.mesaviva.app.games.MancalaSide
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
}
