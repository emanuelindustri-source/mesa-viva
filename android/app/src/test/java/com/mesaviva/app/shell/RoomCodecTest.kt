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
