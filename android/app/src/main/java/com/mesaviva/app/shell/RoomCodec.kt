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

    fun snakesToJson(board: SnakesRoomState): JSONObject = JSONObject()
        .put("k", "state")
        .put("game", RoomGame.Snakes.id)
        .put("names", JSONArray(board.names))
        .put("bots", JSONArray(board.bots))
        .put("positions", JSONArray(board.positions))
        .put("turn", board.turn)
        .put("dice", board.dice)
        .put("winner", board.winner ?: JSONObject.NULL)
        .put("lastSeat", board.lastSeat ?: JSONObject.NULL)
        .put("lastFrom", board.lastFrom)
        .put("lastTo", board.lastTo)
        .put("lastEvent", board.lastEvent)

    fun snakesFromJson(json: JSONObject): SnakesRoomState? {
        val namesJson = json.optJSONArray("names") ?: return null
        val botsJson = json.optJSONArray("bots") ?: return null
        val positionsJson = json.optJSONArray("positions") ?: return null
        if (namesJson.length() !in 2..4) return null
        if (positionsJson.length() != namesJson.length()) return null
        val names = (0 until namesJson.length()).map { namesJson.optString(it) }
        val bots = (0 until namesJson.length()).map { botsJson.optBoolean(it) }
        val positions = (0 until positionsJson.length()).map { positionsJson.optInt(it) }
        if (positions.any { it !in 0..100 }) return null
        val winner = if (json.isNull("winner")) null else json.optInt("winner").takeIf { it in names.indices }
        return SnakesRoomState(
            names = names,
            bots = bots,
            positions = positions,
            turn = json.optInt("turn").coerceIn(0, names.lastIndex),
            dice = json.optInt("dice").coerceIn(0, 6),
            winner = winner,
            lastSeat = if (json.isNull("lastSeat")) null else json.optInt("lastSeat").takeIf { it in names.indices },
            lastFrom = json.optInt("lastFrom").coerceIn(0, 100),
            lastTo = json.optInt("lastTo").coerceIn(0, 100),
            lastEvent = json.optString("lastEvent", "move"),
        )
    }

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
