package com.mesaviva.app.shell

import com.mesaviva.app.games.DamasChecker
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasMove
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DamasState
import com.mesaviva.app.games.MancalaPhase
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.MancalaState
import org.json.JSONArray
import org.json.JSONObject

/**
 * Códec JSON del protocolo de salas nativas.
 *
 * Es puro a propósito: no toca Android ni WebSocket, así que se puede probar
 * en la JVM. El anfitrión valida siempre con los motores Kotlin; esto solo
 * transporta el estado completo.
 */
internal object RoomCodec {

    fun damasToJson(board: DamasState): JSONObject {
        val cells = JSONArray()
        for (piece in board.board) {
            cells.put(
                when {
                    piece == null -> ""
                    piece.color == DamasColor.White && piece.king -> "WK"
                    piece.color == DamasColor.White -> "W"
                    piece.king -> "BK"
                    else -> "B"
                },
            )
        }
        val last = board.lastMove?.let {
            JSONObject()
                .put("from", it.from)
                .put("to", it.to)
                .put("captured", it.captured ?: JSONObject.NULL)
                .put("promotion", it.promotion)
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Damas.id)
            .put("board", cells)
            .put("turn", if (board.turn == DamasColor.White) "white" else "black")
            .put("mustContinueFrom", board.mustContinueFrom ?: JSONObject.NULL)
            .put("phase", if (board.phase == DamasPhase.Play) "play" else "end")
            .put("sinceCapture", board.sinceCapture)
            .put("winner", when (board.winner) {
                DamasColor.White -> "white"
                DamasColor.Black -> "black"
                null -> JSONObject.NULL
            })
            .put("draw", board.draw)
            .put("reason", board.reason ?: JSONObject.NULL)
            .put("history", JSONArray(board.history))
            .put("captures", JSONObject()
                .put("white", board.captures[DamasColor.White] ?: 0)
                .put("black", board.captures[DamasColor.Black] ?: 0))
            .put("lastMove", last)
    }

    fun damasFromJson(json: JSONObject): DamasState? {
        val cells = json.optJSONArray("board") ?: return null
        if (cells.length() != DamasGame.SIZE * DamasGame.SIZE) return null
        val board = MutableList<DamasChecker?>(cells.length()) { null }
        for (index in 0 until cells.length()) {
            board[index] = when (cells.optString(index)) {
                "W" -> DamasChecker(DamasColor.White)
                "WK" -> DamasChecker(DamasColor.White, king = true)
                "B" -> DamasChecker(DamasColor.Black)
                "BK" -> DamasChecker(DamasColor.Black, king = true)
                else -> null
            }
        }
        val last = json.optJSONObject("lastMove")?.let {
            DamasMove(
                from = it.optInt("from"),
                to = it.optInt("to"),
                captured = if (it.isNull("captured")) null else it.optInt("captured"),
                promotion = it.optBoolean("promotion"),
            )
        }
        val captures = json.optJSONObject("captures")
        val history = json.optJSONArray("history")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
        } ?: emptyList()
        return DamasState(
            board = board,
            turn = if (json.optString("turn") == "black") DamasColor.Black else DamasColor.White,
            mustContinueFrom = if (json.isNull("mustContinueFrom")) null else json.optInt("mustContinueFrom"),
            phase = if (json.optString("phase") == "end") DamasPhase.MatchEnd else DamasPhase.Play,
            sinceCapture = json.optInt("sinceCapture"),
            lastMove = last,
            winner = when (json.optString("winner")) {
                "white" -> DamasColor.White
                "black" -> DamasColor.Black
                else -> null
            },
            draw = json.optBoolean("draw"),
            reason = if (json.isNull("reason")) null else json.optString("reason"),
            history = history,
            captures = mapOf(
                DamasColor.White to (captures?.optInt("white") ?: 0),
                DamasColor.Black to (captures?.optInt("black") ?: 0),
            ),
        )
    }

    fun mancalaToJson(board: MancalaState): JSONObject = JSONObject()
        .put("k", "state")
        .put("game", RoomGame.Mancala.id)
        .put("pits", JSONArray(board.pits))
        .put("turn", if (board.turn == MancalaSide.White) "white" else "black")
        .put("phase", if (board.phase == MancalaPhase.Play) "play" else "end")
        .put("lastPit", board.lastPit ?: JSONObject.NULL)
        .put("lastCapture", board.lastCapture ?: JSONObject.NULL)
        .put("winner", when (board.winner) {
            MancalaSide.White -> "white"
            MancalaSide.Black -> "black"
            null -> JSONObject.NULL
        })

    fun mancalaFromJson(json: JSONObject): MancalaState? {
        val pits = json.optJSONArray("pits") ?: return null
        if (pits.length() != 14) return null
        val values = (0 until 14).map { pits.optInt(it) }
        if (values.any { it < 0 }) return null
        return MancalaState(
            pits = values,
            turn = if (json.optString("turn") == "black") MancalaSide.Black else MancalaSide.White,
            phase = if (json.optString("phase") == "end") MancalaPhase.MatchEnd else MancalaPhase.Play,
            lastPit = if (json.isNull("lastPit")) null else json.optInt("lastPit"),
            lastCapture = if (json.isNull("lastCapture")) null else json.optInt("lastCapture"),
            winner = when (json.optString("winner")) {
                "white" -> MancalaSide.White
                "black" -> MancalaSide.Black
                else -> null
            },
        )
    }
}
