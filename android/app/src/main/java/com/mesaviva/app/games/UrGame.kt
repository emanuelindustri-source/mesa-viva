package com.mesaviva.app.games

import kotlin.random.Random

enum class UrSide { White, Black }

enum class UrPhase { Play, MatchEnd }

data class UrPiece(val id: Int, val side: UrSide, val position: Int = -1)

data class UrState(
    val pieces: List<UrPiece>,
    val turn: UrSide,
    val dice: Int = 0,
    val phase: UrPhase = UrPhase.Play,
    val winner: UrSide? = null,
    val lastPiece: Int? = null,
    val lastFrom: Int = -1,
    val lastTo: Int = -1,
    val captured: Boolean = false,
    val extraTurn: Boolean = false,
    val history: List<String> = emptyList(),
)

data class UrResult(val state: UrState, val error: String? = null)

/**
 * Juego Real de Ur simplificado para dos jugadores.
 *
 * Carrera de 20 casillas con cuatro fichas por bando. Las casillas 0, 4, 8,
 * 13 y 16 son rosetas: son seguras y dan turno extra. Se entra con cualquier
 * tirada, se captura al rival fuera de una roseta y hay que llegar exacto a
 * la meta 20. Gana quien termina sus cuatro fichas.
 */
class UrGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val TRACK = 20
        val ROSETTES = setOf(0, 4, 8, 13, 16)
        const val PIECES_PER_SIDE = 4

        fun initial(): UrState {
            val pieces = (0 until PIECES_PER_SIDE).map { UrPiece(it, UrSide.White) } +
                (0 until PIECES_PER_SIDE).map { UrPiece(it + PIECES_PER_SIDE, UrSide.Black) }
            return UrState(pieces = pieces, turn = UrSide.White)
        }

        fun other(side: UrSide): UrSide = if (side == UrSide.White) UrSide.Black else UrSide.White
        fun label(position: Int): String = if (position < 0) "inicio" else position.toString()
    }

    fun piecesOf(state: UrState, side: UrSide): List<UrPiece> = state.pieces.filter { it.side == side }

    fun finishedOf(state: UrState, side: UrSide): Int =
        state.pieces.count { it.side == side && it.position == TRACK }

    fun legalMoves(state: UrState): List<Int> {
        if (state.phase == UrPhase.MatchEnd || state.dice !in 1..4) return emptyList()
        return state.pieces.filter { piece ->
            piece.side == state.turn && piece.position < TRACK &&
                (piece.position < 0 || piece.position + state.dice <= TRACK)
        }.map { it.id }
    }

    fun roll(state: UrState, value: Int = random.nextInt(1, 5)): UrResult {
        if (state.phase == UrPhase.MatchEnd) return UrResult(state, "ur.error.matchOver")
        val dice = value.coerceIn(1, 4)
        val rolled = state.copy(dice = dice, lastPiece = null, captured = false, extraTurn = false)
        if (legalMoves(rolled).isEmpty()) {
            val next = rolled.copy(
                turn = other(state.turn),
                dice = 0,
                history = (state.history + "${prefix(state.turn)} sin jugada").takeLast(120),
            )
            return UrResult(next)
        }
        return UrResult(rolled.copy(history = (state.history + "${prefix(state.turn)} 🎲 $dice").takeLast(120)))
    }

    fun move(state: UrState, pieceId: Int): UrResult {
        if (state.phase == UrPhase.MatchEnd) return UrResult(state, "ur.error.matchOver")
        if (state.dice !in 1..4) return UrResult(state, "ur.error.rollFirst")
        val piece = state.pieces.firstOrNull { it.id == pieceId }
            ?: return UrResult(state, "ur.error.invalid")
        if (piece.side != state.turn) return UrResult(state, "ur.error.notYourTurn")
        if (piece.position == TRACK) return UrResult(state, "ur.error.finished")
        val from = piece.position
        val target = if (from < 0) state.dice - 1 else from + state.dice
        if (target > TRACK) return UrResult(state, "ur.error.exact")

        var captured = false
        val pieces = state.pieces.toMutableList()
        pieces[pieceId] = piece.copy(position = target)
        if (target < TRACK && target !in ROSETTES) {
            for (index in pieces.indices) {
                val rival = pieces[index]
                if (rival.side != state.turn && rival.position == target) {
                    pieces[index] = rival.copy(position = -1)
                    captured = true
                }
            }
        }
        val finished = pieces[pieceId].position == TRACK
        val winner = if (piecesOf(state.copy(pieces = pieces), state.turn).all { it.position == TRACK }) {
            state.turn
        } else {
            null
        }
        val extra = target in ROSETTES || captured || finished
        val entry = buildString {
            append(prefix(state.turn))
            append(" ")
            append(label(from))
            append(" → ")
            append(if (finished) "meta" else target.toString())
            if (captured) append(" ⚔")
            if (extra) append(" +")
        }
        val next = state.copy(
            pieces = pieces,
            dice = 0,
            turn = if (winner != null || extra) state.turn else other(state.turn),
            phase = if (winner != null) UrPhase.MatchEnd else UrPhase.Play,
            winner = winner,
            lastPiece = pieceId,
            lastFrom = from,
            lastTo = target,
            captured = captured,
            extraTurn = extra,
            history = (state.history + entry).takeLast(120),
        )
        return UrResult(next)
    }

    fun resign(state: UrState): UrResult {
        if (state.phase == UrPhase.MatchEnd) return UrResult(state, "ur.error.matchOver")
        return UrResult(state.copy(phase = UrPhase.MatchEnd, winner = other(state.turn)))
    }

    fun botDice(): Int = random.nextInt(1, 5)

    fun botChoice(state: UrState): Int? = botAction(state)?.takeIf { it.first == 0 }?.second

    fun botAction(state: UrState): Pair<Int, Int>? {
        if (state.phase == UrPhase.MatchEnd) return null
        if (state.dice !in 1..4) return 1 to random.nextInt(1, 5)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        val scored = moves.map { id ->
            val piece = state.pieces[id]
            val target = if (piece.position < 0) state.dice - 1 else piece.position + state.dice
            var value = target
            if (target == TRACK) value += 1000
            if (target in ROSETTES && target < TRACK) value += 80
            if (target < TRACK) {
                val captures = state.pieces.count {
                    it.side != state.turn && it.position == target
                }
                if (captures > 0 && target !in ROSETTES) value += 200 * captures
            }
            id to value
        }
        return scored.maxByOrNull { it.second }
    }

    private fun prefix(side: UrSide): String = if (side == UrSide.White) "B" else "N"
}
