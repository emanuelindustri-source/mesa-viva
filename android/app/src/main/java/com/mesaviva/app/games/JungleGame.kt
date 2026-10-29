package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Jungle / Dou Shou Qi para la mesa nativa.
 *
 * Río, trampas, guaridas, saltos de tigre y león, y la regla de la rata
 * que come al elefante. El bot usa alfa-beta con profundidad corta para
 * mantener la partida fluida en el teléfono.
 */
enum class JungleSide { Red, Black }

data class JunglePiece(val side: JungleSide, val rank: Int)

data class JungleMove(val from: Int, val to: Int, val jump: Boolean = false)

enum class JunglePhase { Play, MatchEnd }

data class JungleState(
    val board: List<JunglePiece?>,
    val turn: JungleSide,
    val phase: JunglePhase = JunglePhase.Play,
    val lastMove: JungleMove? = null,
    val capturesRed: Int = 0,
    val capturesBlack: Int = 0,
    val moveNo: Int = 0,
    val winner: JungleSide? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class JungleResult(val state: JungleState, val error: String? = null)

class JungleGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val COLS = 7
        const val ROWS = 9
        const val RED_DEN = 59
        const val BLACK_DEN = 3

        fun indexOf(col: Int, row: Int): Int = row * COLS + col
        fun colOf(index: Int): Int = index % COLS
        fun rowOf(index: Int): Int = index / COLS
        fun inBounds(col: Int, row: Int): Boolean = col in 0 until COLS && row in 0 until ROWS
        fun other(side: JungleSide): JungleSide = if (side == JungleSide.Red) JungleSide.Black else JungleSide.Red

        val TRAP_OWNER: Map<Int, JungleSide> = mapOf(
            indexOf(2, 0) to JungleSide.Black,
            indexOf(4, 0) to JungleSide.Black,
            indexOf(3, 1) to JungleSide.Black,
            indexOf(2, 8) to JungleSide.Red,
            indexOf(4, 8) to JungleSide.Red,
            indexOf(3, 7) to JungleSide.Red,
        )

        val WATER: Set<Int> = buildSet {
            for (row in 3..5) {
                for (col in listOf(1, 2, 4, 5)) add(indexOf(col, row))
            }
        }

        val ANIMALS = listOf("", "🐭", "🐱", "🐶", "🐺", "🐆", "🐯", "🦁", "🐘")
        val RANKS = listOf("", "rata", "gato", "perro", "lobo", "leopardo", "tigre", "león", "elefante")
        val RANK_VALUE = listOf(0, 2, 3, 4, 6, 8, 10, 12, 16)

        fun denFor(side: JungleSide): Int = if (side == JungleSide.Red) RED_DEN else BLACK_DEN
        fun enemyDen(side: JungleSide): Int = if (side == JungleSide.Red) BLACK_DEN else RED_DEN
        fun isWater(index: Int): Boolean = index in WATER
        fun isTrap(index: Int): Boolean = TRAP_OWNER.containsKey(index)

        fun inEnemyTrap(index: Int, side: JungleSide): Boolean =
            TRAP_OWNER[index]?.let { it != side } == true

        fun initialBoard(): List<JunglePiece?> {
            val board = MutableList<JunglePiece?>(COLS * ROWS) { null }
            val layout = listOf(
                Triple(6, JungleSide.Black, indexOf(0, 0)),
                Triple(7, JungleSide.Black, indexOf(6, 0)),
                Triple(2, JungleSide.Black, indexOf(1, 1)),
                Triple(3, JungleSide.Black, indexOf(5, 1)),
                Triple(8, JungleSide.Black, indexOf(0, 2)),
                Triple(4, JungleSide.Black, indexOf(2, 2)),
                Triple(5, JungleSide.Black, indexOf(4, 2)),
                Triple(1, JungleSide.Black, indexOf(6, 2)),
                Triple(1, JungleSide.Red, indexOf(0, 6)),
                Triple(5, JungleSide.Red, indexOf(2, 6)),
                Triple(4, JungleSide.Red, indexOf(4, 6)),
                Triple(8, JungleSide.Red, indexOf(6, 6)),
                Triple(3, JungleSide.Red, indexOf(1, 7)),
                Triple(2, JungleSide.Red, indexOf(5, 7)),
                Triple(7, JungleSide.Red, indexOf(0, 8)),
                Triple(6, JungleSide.Red, indexOf(6, 8)),
            )
            for ((rank, side, index) in layout) board[index] = JunglePiece(side, rank)
            return board
        }

        fun coord(index: Int): String = "${('A' + colOf(index))}${ROWS - rowOf(index)}"

        fun canCapture(attacker: JunglePiece, defender: JunglePiece, from: Int, to: Int): Boolean {
            if (isWater(from) && !isWater(to)) return false
            if (TRAP_OWNER[to] == attacker.side) return true
            if (attacker.rank == 1 && defender.rank == 8) return true
            if (attacker.rank == 8 && defender.rank == 1) return false
            return attacker.rank >= defender.rank
        }
    }

    fun newMatch(): JungleState = JungleState(
        board = initialBoard(),
        turn = JungleSide.Red,
    )

    fun legalMoves(state: JungleState, side: JungleSide = state.turn): List<JungleMove> {
        if (state.phase == JunglePhase.MatchEnd) return emptyList()
        val moves = mutableListOf<JungleMove>()
        val board = state.board
        val directions = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        for (from in board.indices) {
            val piece = board[from] ?: continue
            if (piece.side != side) continue
            val col = colOf(from)
            val row = rowOf(from)
            for ((dc, dr) in directions) {
                val nc = col + dc
                val nr = row + dr
                if (!inBounds(nc, nr)) continue
                val to = indexOf(nc, nr)
                if (to == denFor(side)) continue
                if (isWater(to) && piece.rank != 1) continue
                val target = board[to]
                if (target != null && target.side == side) continue
                if (target != null && !canCapture(piece, target, from, to)) continue
                moves.add(JungleMove(from, to))
            }

            if (piece.rank == 6 || piece.rank == 7) {
                for ((dc, dr) in directions) {
                    var nc = col + dc
                    var nr = row + dr
                    if (!inBounds(nc, nr) || !isWater(indexOf(nc, nr))) continue
                    val path = mutableListOf<Int>()
                    while (inBounds(nc, nr) && isWater(indexOf(nc, nr))) {
                        path.add(indexOf(nc, nr))
                        nc += dc
                        nr += dr
                    }
                    if (!inBounds(nc, nr) || path.isEmpty()) continue
                    val to = indexOf(nc, nr)
                    if (to == denFor(side)) continue
                    val target = board[to]
                    if (target != null && target.side == side) continue
                    if (path.any { board[it]?.rank == 1 }) continue
                    if (target != null && !canCapture(piece, target, from, to)) continue
                    moves.add(JungleMove(from, to, jump = true))
                }
            }
        }
        return moves
    }

    private fun pieceCount(board: List<JunglePiece?>, side: JungleSide): Int =
        board.count { it?.side == side }

    private fun material(board: List<JunglePiece?>, side: JungleSide): Int =
        board.sumOf { if (it?.side == side) it.rank else 0 }

    private fun applyRaw(state: JungleState, move: JungleMove, record: Boolean = true): JungleState {
        val piece = state.board.getOrNull(move.from) ?: return state
        val board = state.board.toMutableList()
        val captured = board[move.to]
        board[move.to] = piece
        board[move.from] = null
        val mover = piece.side
        val rival = other(mover)
        val entry = "${if (mover == JungleSide.Red) "R" else "N"}: ${coord(move.from)}-${coord(move.to)}" +
            (if (move.jump) " ↷" else "") +
            (if (captured != null) " x" else "")
        val next = state.copy(
            board = board,
            turn = rival,
            lastMove = move,
            moveNo = state.moveNo + 1,
            capturesRed = state.capturesRed + if (mover == JungleSide.Red && captured != null) 1 else 0,
            capturesBlack = state.capturesBlack + if (mover == JungleSide.Black && captured != null) 1 else 0,
            history = if (record) (state.history + entry).takeLast(120) else state.history,
        )
        if (move.to == enemyDen(mover)) {
            return next.copy(
                phase = JunglePhase.MatchEnd,
                winner = mover,
                reason = "jungle.reason.den",
            )
        }
        if (pieceCount(board, rival) == 0) {
            return next.copy(
                phase = JunglePhase.MatchEnd,
                winner = mover,
                reason = "jungle.reason.pieces",
            )
        }
        if (legalMoves(next, rival).isEmpty()) {
            return next.copy(
                phase = JunglePhase.MatchEnd,
                winner = mover,
                reason = "jungle.reason.blocked",
            )
        }
        return next
    }

    fun move(state: JungleState, from: Int, to: Int): JungleResult {
        if (state.phase == JunglePhase.MatchEnd) return JungleResult(state, "jungle.error.matchOver")
        val move = legalMoves(state).firstOrNull { it.from == from && it.to == to }
            ?: return JungleResult(state, "jungle.error.invalidMove")
        return JungleResult(applyRaw(state, move))
    }

    fun resign(state: JungleState): JungleResult {
        if (state.phase == JunglePhase.MatchEnd) return JungleResult(state, "jungle.error.matchOver")
        return JungleResult(
            state.copy(
                phase = JunglePhase.MatchEnd,
                winner = other(state.turn),
                reason = "jungle.reason.resign",
            ),
        )
    }

    fun botMove(state: JungleState, difficulty: String = "normal", rng: Random = random): JungleMove? {
        if (state.phase == JunglePhase.MatchEnd) return null
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") {
            val captures = moves.filter { state.board[it.to] != null }
            val pool = if (captures.isNotEmpty()) captures else moves
            return pool[rng.nextInt(pool.size)]
        }
        val depth = if (difficulty == "hard") 3 else 2
        val root = state.turn
        var best = Int.MIN_VALUE
        val pool = mutableListOf<JungleMove>()
        for (move in moves.sortedByDescending { state.board[it.to] != null }) {
            val value = search(applyRaw(state, move, record = false), depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, root)
            if (value > best + 2) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (abs(value - best) <= 2) {
                pool.add(move)
            }
        }
        return pool.randomOrNull(rng) ?: moves.first()
    }

    private fun search(state: JungleState, depth: Int, alphaIn: Int, betaIn: Int, root: JungleSide): Int {
        var alpha = alphaIn
        var beta = betaIn
        if (state.phase == JunglePhase.MatchEnd) {
            return if (state.winner == root) 100_000 + depth else -100_000 - depth
        }
        if (depth <= 0) return evaluate(state, root)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return if (state.turn == root) -100_000 else 100_000
        val maximizing = state.turn == root
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        for (move in moves) {
            val value = search(applyRaw(state, move, record = false), depth - 1, alpha, beta, root)
            if (maximizing) {
                if (value > best) best = value
                if (best > alpha) alpha = best
            } else {
                if (value < best) best = value
                if (best < beta) beta = best
            }
            if (alpha >= beta) break
        }
        return best
    }

    private fun evaluate(state: JungleState, side: JungleSide): Int {
        val rival = other(side)
        var score = (material(state.board, side) - material(state.board, rival)) * 10
        val target = enemyDen(side)
        val tc = colOf(target)
        val tr = rowOf(target)
        for (index in state.board.indices) {
            val piece = state.board[index] ?: continue
            val distance = abs(colOf(index) - tc) + abs(rowOf(index) - tr)
            val advance = (14 - distance) * (if (piece.rank == 1) 0.6 else 0.35)
            score += if (piece.side == side) advance.toInt() else -advance.toInt()
            if (inEnemyTrap(index, piece.side)) {
                val value = RANK_VALUE.getOrElse(piece.rank) { 0 }
                score += if (piece.side == side) -value * 2 else value * 2
            }
        }
        return score
    }

    fun winnerName(state: JungleState, redName: String, blackName: String): String = when (state.winner) {
        JungleSide.Red -> redName
        JungleSide.Black -> blackName
        null -> ""
    }
}
