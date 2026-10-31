package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Janggi (ajedrez coreano) para dos jugadores.
 *
 * Tablero 9×10, palacio, río, caballo y elefante bloqueados, cañón con
 * pantalla, soldados que avanzan o se mueven de lado y generales que no
 * pueden quedar enfrentados.
 */
enum class JanggiSide { Han, Cho }

enum class JanggiKind { General, Guard, Elephant, Horse, Chariot, Cannon, Soldier }

data class JanggiPiece(val side: JanggiSide, val kind: JanggiKind)

data class JanggiMove(
    val from: Int,
    val to: Int,
    val captured: JanggiKind? = null,
)

enum class JanggiPhase { Play, MatchEnd }

data class JanggiState(
    val board: List<JanggiPiece?>,
    val turn: JanggiSide,
    val phase: JanggiPhase = JanggiPhase.Play,
    val lastMove: JanggiMove? = null,
    val check: JanggiSide? = null,
    val winner: JanggiSide? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class JanggiResult(val state: JanggiState, val error: String? = null)

class JanggiGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val COLS = 9
        const val ROWS = 10

        val GLYPHS: Map<JanggiKind, String> = mapOf(
            JanggiKind.General to "將",
            JanggiKind.Guard to "士",
            JanggiKind.Elephant to "象",
            JanggiKind.Horse to "馬",
            JanggiKind.Chariot to "車",
            JanggiKind.Cannon to "包",
            JanggiKind.Soldier to "卒",
        )

        val VALUES: Map<JanggiKind, Int> = mapOf(
            JanggiKind.General to 10_000,
            JanggiKind.Chariot to 900,
            JanggiKind.Cannon to 450,
            JanggiKind.Horse to 400,
            JanggiKind.Elephant to 250,
            JanggiKind.Guard to 200,
            JanggiKind.Soldier to 100,
        )

        fun indexOf(col: Int, row: Int): Int = row * COLS + col
        fun colOf(index: Int): Int = index % COLS
        fun rowOf(index: Int): Int = index / COLS
        fun inBoard(col: Int, row: Int): Boolean = col in 0 until COLS && row in 0 until ROWS
        fun other(side: JanggiSide): JanggiSide = if (side == JanggiSide.Cho) JanggiSide.Han else JanggiSide.Cho

        fun initialBoard(): List<JanggiPiece?> {
            val board = MutableList<JanggiPiece?>(COLS * ROWS) { null }
            val back = listOf(
                JanggiKind.Chariot, JanggiKind.Horse, JanggiKind.Elephant,
                JanggiKind.Guard, null, JanggiKind.Guard,
                JanggiKind.Elephant, JanggiKind.Horse, JanggiKind.Chariot,
            )
            for ((col, kind) in back.withIndex()) {
                if (kind != null) {
                    board[indexOf(col, 0)] = JanggiPiece(JanggiSide.Han, kind)
                    board[indexOf(col, 9)] = JanggiPiece(JanggiSide.Cho, kind)
                }
            }
            board[indexOf(4, 1)] = JanggiPiece(JanggiSide.Han, JanggiKind.General)
            board[indexOf(4, 8)] = JanggiPiece(JanggiSide.Cho, JanggiKind.General)
            board[indexOf(1, 2)] = JanggiPiece(JanggiSide.Han, JanggiKind.Cannon)
            board[indexOf(7, 2)] = JanggiPiece(JanggiSide.Han, JanggiKind.Cannon)
            board[indexOf(1, 7)] = JanggiPiece(JanggiSide.Cho, JanggiKind.Cannon)
            board[indexOf(7, 7)] = JanggiPiece(JanggiSide.Cho, JanggiKind.Cannon)
            for (col in listOf(0, 2, 4, 6, 8)) {
                board[indexOf(col, 3)] = JanggiPiece(JanggiSide.Han, JanggiKind.Soldier)
                board[indexOf(col, 6)] = JanggiPiece(JanggiSide.Cho, JanggiKind.Soldier)
            }
            return board
        }

        fun inPalace(index: Int, side: JanggiSide): Boolean {
            val col = colOf(index)
            val row = rowOf(index)
            if (col !in 3..5) return false
            return if (side == JanggiSide.Han) row in 0..2 else row in 7..9
        }

        fun coord(index: Int): String = "${('A' + colOf(index))}${ROWS - rowOf(index)}"
    }

    fun newMatch(): JanggiState = JanggiState(board = initialBoard(), turn = JanggiSide.Cho)

    private fun addTarget(
        moves: MutableList<JanggiMove>,
        board: List<JanggiPiece?>,
        from: Int,
        to: Int,
        side: JanggiSide,
    ): Boolean {
        val target = board[to]
        if (target != null && target.side == side) return false
        moves.add(JanggiMove(from, to, target?.kind))
        return target == null
    }

    private fun pseudoMoves(state: JanggiState, from: Int): List<JanggiMove> {
        val piece = state.board.getOrNull(from) ?: return emptyList()
        val board = state.board
        val moves = mutableListOf<JanggiMove>()
        val col = colOf(from)
        val row = rowOf(from)
        val side = piece.side

        fun step(dc: Int, dr: Int, limit: Int, palaceOnly: Boolean = false) {
            for (distance in 1..limit) {
                val nc = col + dc * distance
                val nr = row + dr * distance
                if (!inBoard(nc, nr)) return
                val to = indexOf(nc, nr)
                if (palaceOnly && !inPalace(to, side)) return
                if (!addTarget(moves, board, from, to, side)) return
            }
        }

        when (piece.kind) {
            JanggiKind.Chariot -> {
                step(1, 0, 8)
                step(-1, 0, 8)
                step(0, 1, 9)
                step(0, -1, 9)
            }
            JanggiKind.Cannon -> {
                for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    var nc = col + dc
                    var nr = row + dr
                    var screen = false
                    while (inBoard(nc, nr)) {
                        val to = indexOf(nc, nr)
                        val target = board[to]
                        if (!screen) {
                            if (target == null) {
                                moves.add(JanggiMove(from, to))
                            } else {
                                screen = true
                            }
                        } else if (target != null) {
                            if (target.side != side && target.kind != JanggiKind.Cannon) {
                                moves.add(JanggiMove(from, to, target.kind))
                            }
                            break
                        }
                        nc += dc
                        nr += dr
                    }
                }
            }
            JanggiKind.Horse -> {
                val jumps = listOf(
                    Triple(1, 2, 0 to 1), Triple(2, 1, 1 to 0),
                    Triple(-1, 2, 0 to 1), Triple(-2, 1, -1 to 0),
                    Triple(1, -2, 0 to -1), Triple(2, -1, 1 to 0),
                    Triple(-1, -2, 0 to -1), Triple(-2, -1, -1 to 0),
                )
                for ((dc, dr, leg) in jumps) {
                    val lc = col + leg.first
                    val lr = row + leg.second
                    if (!inBoard(lc, lr) || board[indexOf(lc, lr)] != null) continue
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }
            JanggiKind.Elephant -> {
                val jumps = listOf(
                    2 to 3, 3 to 2, 2 to -3, 3 to -2,
                    -2 to 3, -3 to 2, -2 to -3, -3 to -2,
                )
                for ((dc, dr) in jumps) {
                    val lc = col + if (abs(dc) > abs(dr)) dc / 2 else 0
                    val lr = row + if (abs(dr) > abs(dc)) dr / 2 else 0
                    if (!inBoard(lc, lr) || board[indexOf(lc, lr)] != null) continue
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }
            JanggiKind.Guard -> {
                for ((dc, dr) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    if (!inPalace(indexOf(nc, nr), side)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }
            JanggiKind.General -> {
                for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    if (!inPalace(indexOf(nc, nr), side)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }
            JanggiKind.Soldier -> {
                val forward = if (side == JanggiSide.Han) 1 else -1
                for ((dc, dr) in listOf(0 to forward, 1 to 0, -1 to 0)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }
        }
        return moves.distinctBy { it.to }
    }

    fun legalMoves(state: JanggiState): List<JanggiMove> {
        if (state.phase == JanggiPhase.MatchEnd) return emptyList()
        val side = state.turn
        val moves = mutableListOf<JanggiMove>()
        for (from in state.board.indices) {
            val piece = state.board[from] ?: continue
            if (piece.side != side) continue
            for (move in pseudoMoves(state, from)) {
                val next = applyRaw(state, move)
                if (isAttacked(next.board, generalSquare(next.board, side), other(side))) continue
                if (generalsFacing(next.board)) continue
                moves.add(move)
            }
        }
        return moves
    }

    fun move(state: JanggiState, move: JanggiMove): JanggiResult {
        if (state.phase == JanggiPhase.MatchEnd) return JanggiResult(state, "janggi.error.matchOver")
        val legal = legalMoves(state).firstOrNull { it.from == move.from && it.to == move.to }
            ?: return JanggiResult(state, "janggi.error.invalidMove")
        val next = applyRaw(state, legal)
        return JanggiResult(finish(next, state.turn))
    }

    fun resign(state: JanggiState): JanggiResult {
        if (state.phase == JanggiPhase.MatchEnd) return JanggiResult(state, "janggi.error.matchOver")
        val winner = other(state.turn)
        return JanggiResult(
            state.copy(
                phase = JanggiPhase.MatchEnd,
                winner = winner,
                reason = "janggi.reason.resign",
                history = (state.history + "Rendición · gana ${sideName(winner)}").takeLast(160),
            ),
        )
    }

    fun botMove(state: JanggiState, difficulty: String = "normal"): JanggiMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") return moves.random(random)
        val side = state.turn
        val scored = moves.map { move ->
            val next = applyRaw(state, move)
            val material = material(next, side) - material(next, other(side))
            val capture = state.board.getOrNull(move.to)?.kind?.let { VALUES[it] } ?: 0
            val check = if (isAttacked(next.board, generalSquare(next.board, other(side)), side)) 80 else 0
            move to (material + capture * 2 + check)
        }
        if (difficulty != "hard") return scored.maxByOrNull { it.second }?.first
        var best = scored.maxByOrNull { it.second }!!.first
        var bestScore = Int.MIN_VALUE
        for ((move, base) in scored.sortedByDescending { it.second }.take(12)) {
            val next = applyRaw(state, move)
            val replies = legalMoves(next)
            val worst = replies.maxOfOrNull { reply ->
                val after = applyRaw(next, reply)
                material(next, side) - material(after, side) + (next.board.getOrNull(reply.to)?.kind?.let { VALUES[it] } ?: 0)
            } ?: 0
            val score = base - worst
            if (score > bestScore) {
                bestScore = score
                best = move
            }
        }
        return best
    }

    fun sideName(side: JanggiSide): String = if (side == JanggiSide.Han) "Han" else "Cho"
    fun winnerName(state: JanggiState): String = state.winner?.let { sideName(it) } ?: ""

    private fun applyRaw(state: JanggiState, move: JanggiMove): JanggiState {
        val board = state.board.toMutableList()
        val mover = state.turn
        val piece = board[move.from] ?: return state
        val captured = board[move.to]
        board[move.from] = null
        board[move.to] = piece
        val entry = "${sideName(mover)}: ${coord(move.from)}-${coord(move.to)}" +
            if (captured != null) "x${GLYPHS[captured.kind]}" else ""
        return state.copy(
            board = board,
            turn = other(mover),
            lastMove = move,
            history = (state.history + entry).takeLast(160),
        )
    }

    private fun finish(next: JanggiState, mover: JanggiSide): JanggiState {
        val check = isAttacked(next.board, generalSquare(next.board, other(mover)), mover)
        val replies = legalMoves(next.copy(turn = other(mover)))
        if (replies.isEmpty()) {
            return next.copy(
                phase = JanggiPhase.MatchEnd,
                winner = mover,
                check = other(mover),
                reason = "janggi.reason.mate",
                history = (next.history + "${sideName(mover)} da mate").takeLast(160),
            )
        }
        return next.copy(
            check = if (check) other(mover) else null,
            history = (next.history + if (check) "Jaque a ${sideName(other(mover))}" else "").filter { it.isNotBlank() }.takeLast(160),
        )
    }

    private fun material(state: JanggiState, side: JanggiSide): Int =
        state.board.sumOf { if (it != null && it.side == side) VALUES[it.kind] ?: 0 else 0 }

    private fun generalSquare(board: List<JanggiPiece?>, side: JanggiSide): Int =
        board.indexOfFirst { it != null && it.side == side && it.kind == JanggiKind.General }

    private fun isAttacked(board: List<JanggiPiece?>, target: Int, bySide: JanggiSide): Boolean {
        if (target < 0) return false
        val state = JanggiState(board = board, turn = bySide)
        for (from in board.indices) {
            val piece = board[from] ?: continue
            if (piece.side != bySide) continue
            if (pseudoMoves(state, from).any { it.to == target }) return true
        }
        return false
    }

    private fun generalsFacing(board: List<JanggiPiece?>): Boolean {
        val han = generalSquare(board, JanggiSide.Han)
        val cho = generalSquare(board, JanggiSide.Cho)
        if (han < 0 || cho < 0) return false
        if (colOf(han) != colOf(cho)) return false
        val min = minOf(rowOf(han), rowOf(cho))
        val max = maxOf(rowOf(han), rowOf(cho))
        for (row in min + 1 until max) {
            if (board[indexOf(colOf(han), row)] != null) return false
        }
        return true
    }
}
