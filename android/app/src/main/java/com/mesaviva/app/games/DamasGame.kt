package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Damas inglesas para la mesa nativa.
 *
 * Captura obligatoria, cadenas de captura, coronación y empate por 80
 * jugadas sin captura ni coronación. El bot usa alfa-beta con profundidad
 * configurable y nunca recibe una jugada legal fuera de turno.
 */
enum class DamasColor { White, Black }

data class DamasChecker(val color: DamasColor, val king: Boolean = false)

data class DamasMove(
    val from: Int,
    val to: Int,
    val captured: Int? = null,
    val promotion: Boolean = false,
)

enum class DamasPhase { Play, MatchEnd }

data class DamasState(
    val board: List<DamasChecker?>,
    val turn: DamasColor,
    val mustContinueFrom: Int? = null,
    val phase: DamasPhase = DamasPhase.Play,
    val sinceCapture: Int = 0,
    val lastMove: DamasMove? = null,
    val winner: DamasColor? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
    val captures: Map<DamasColor, Int> = mapOf(DamasColor.White to 0, DamasColor.Black to 0),
)

data class DamasResult(val state: DamasState, val error: String? = null)

class DamasGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val SIZE = 8
        const val DRAW_PLIES = 80

        fun fileOf(index: Int): Int = index % SIZE
        fun rankOf(index: Int): Int = index / SIZE
        fun indexOf(file: Int, rank: Int): Int = rank * SIZE + file
        fun onBoard(file: Int, rank: Int): Boolean =
            file in 0 until SIZE && rank in 0 until SIZE

        fun isDark(index: Int): Boolean = (fileOf(index) + rankOf(index)) % 2 == 1

        fun other(color: DamasColor): DamasColor =
            if (color == DamasColor.White) DamasColor.Black else DamasColor.White

        fun square(index: Int): String {
            val file = fileOf(index)
            val rank = rankOf(index)
            return "${('a' + file)}${SIZE - rank}"
        }

        fun initialBoard(): List<DamasChecker?> {
            val board = MutableList<DamasChecker?>(SIZE * SIZE) { null }
            for (index in 0 until SIZE * SIZE) {
                if (!isDark(index)) continue
                when (rankOf(index)) {
                    in 0..2 -> board[index] = DamasChecker(DamasColor.Black)
                    in 5..7 -> board[index] = DamasChecker(DamasColor.White)
                }
            }
            return board
        }
    }

    fun newMatch(): DamasState = DamasState(
        board = initialBoard(),
        turn = DamasColor.White,
    )

    fun piecesOf(state: DamasState, color: DamasColor): Int =
        state.board.count { it?.color == color }

    fun kingsOf(state: DamasState, color: DamasColor): Int =
        state.board.count { it?.color == color && it.king }

    private fun directions(piece: DamasChecker): List<Pair<Int, Int>> =
        if (piece.king) {
            listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        } else if (piece.color == DamasColor.White) {
            listOf(-1 to -1, 1 to -1)
        } else {
            listOf(1 to -1, 1 to 1)
        }

    private fun jumpsFrom(board: List<DamasChecker?>, from: Int, piece: DamasChecker): List<DamasMove> {
        val moves = mutableListOf<DamasMove>()
        val file = fileOf(from)
        val rank = rankOf(from)
        for ((df, dr) in directions(piece)) {
            val midFile = file + df
            val midRank = rank + dr
            val toFile = file + df * 2
            val toRank = rank + dr * 2
            if (!onBoard(midFile, midRank) || !onBoard(toFile, toRank)) continue
            val mid = indexOf(midFile, midRank)
            val to = indexOf(toFile, toRank)
            val victim = board[mid]
            if (victim != null && victim.color != piece.color && board[to] == null) {
                val promotion = !piece.king && if (piece.color == DamasColor.White) toRank == 0 else toRank == SIZE - 1
                moves.add(DamasMove(from, to, captured = mid, promotion = promotion))
            }
        }
        return moves
    }

    private fun quietMovesFrom(board: List<DamasChecker?>, from: Int, piece: DamasChecker): List<DamasMove> {
        val moves = mutableListOf<DamasMove>()
        val file = fileOf(from)
        val rank = rankOf(from)
        for ((df, dr) in directions(piece)) {
            val toFile = file + df
            val toRank = rank + dr
            if (!onBoard(toFile, toRank)) continue
            val to = indexOf(toFile, toRank)
            if (board[to] != null) continue
            val promotion = !piece.king && if (piece.color == DamasColor.White) toRank == 0 else toRank == SIZE - 1
            moves.add(DamasMove(from, to, promotion = promotion))
        }
        return moves
    }

    fun legalMoves(state: DamasState): List<DamasMove> {
        if (state.phase == DamasPhase.MatchEnd) return emptyList()
        val color = state.turn
        val board = state.board
        val captures = mutableListOf<DamasMove>()
        val quiets = mutableListOf<DamasMove>()

        val forced = state.mustContinueFrom
        if (forced != null) {
            val piece = board[forced] ?: return emptyList()
            if (piece.color != color) return emptyList()
            return jumpsFrom(board, forced, piece)
        }

        for (index in board.indices) {
            val piece = board[index] ?: continue
            if (piece.color != color) continue
            captures.addAll(jumpsFrom(board, index, piece))
            quiets.addAll(quietMovesFrom(board, index, piece))
        }
        return if (captures.isNotEmpty()) captures else quiets
    }

    /** Aplica una jugada sin validar; la validación vive en [move]. */
    private fun apply(state: DamasState, move: DamasMove, record: Boolean = true): DamasState {
        val board = state.board.toMutableList()
        val piece = board[move.from] ?: return state
        board[move.from] = null
        if (move.captured != null) board[move.captured] = null
        val promoted = move.promotion
        board[move.to] = if (promoted) DamasChecker(piece.color, king = true) else piece

        val nextColor = other(state.turn)
        val movedPiece = board[move.to] ?: piece
        val more = !promoted && jumpsFrom(board, move.to, movedPiece).isNotEmpty()
        val entry = buildString {
            append(if (piece.color == DamasColor.White) "B" else "N")
            append(": ")
            append(square(move.from))
            append("-")
            append(square(move.to))
            if (move.captured != null) append(" x")
            if (promoted) append(" ♛")
        }
        return state.copy(
            board = board,
            turn = if (more) state.turn else nextColor,
            mustContinueFrom = if (more) move.to else null,
            sinceCapture = if (move.captured != null || promoted) 0 else state.sinceCapture + 1,
            lastMove = move,
            captures = if (move.captured != null) {
                state.captures + (piece.color to (state.captures[piece.color] ?: 0) + 1)
            } else {
                state.captures
            },
            history = if (record) (state.history + entry).takeLast(120) else state.history,
        )
    }

    private fun checkEnd(state: DamasState): DamasState {
        if (state.mustContinueFrom != null) return state

        if (state.sinceCapture >= DRAW_PLIES) {
            return state.copy(
                phase = DamasPhase.MatchEnd,
                draw = true,
                reason = "damas.reason.draw",
                winner = null,
            )
        }

        val nextMoves = legalMoves(state)
        if (nextMoves.isEmpty()) {
            return state.copy(
                phase = DamasPhase.MatchEnd,
                winner = other(state.turn),
                reason = "damas.reason.noMoves",
            )
        }

        if (piecesOf(state, state.turn) == 0) {
            return state.copy(
                phase = DamasPhase.MatchEnd,
                winner = other(state.turn),
                reason = "damas.reason.noPieces",
            )
        }

        return state
    }

    fun move(state: DamasState, from: Int, to: Int): DamasResult {
        if (state.phase == DamasPhase.MatchEnd) return DamasResult(state, "damas.error.matchOver")
        val move = legalMoves(state).firstOrNull { it.from == from && it.to == to }
            ?: return DamasResult(state, "damas.error.invalidMove")
        val next = apply(state, move)
        val finished = checkEnd(next)
        return DamasResult(finished)
    }

    fun resign(state: DamasState): DamasResult {
        if (state.phase == DamasPhase.MatchEnd) return DamasResult(state, "damas.error.matchOver")
        return DamasResult(
            state.copy(
                phase = DamasPhase.MatchEnd,
                winner = other(state.turn),
                reason = "damas.reason.resign",
            ),
        )
    }

    fun botMove(state: DamasState, difficulty: String = "normal", rng: Random = random): DamasMove? {
        if (state.phase == DamasPhase.MatchEnd) return null
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") {
            val captures = moves.filter { it.captured != null }
            val pool = if (captures.isNotEmpty()) captures else moves
            return pool[rng.nextInt(pool.size)]
        }
        val depth = when (difficulty) {
            "hard" -> 4
            else -> 3
        }
        val root = state.turn
        var best = Int.MIN_VALUE
        val pool = mutableListOf<DamasMove>()
        for (move in moves.sortedByDescending { it.captured != null }) {
            val value = search(apply(state, move, record = false), depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, root)
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

    private fun search(state: DamasState, depth: Int, alphaIn: Int, betaIn: Int, root: DamasColor): Int {
        var alpha = alphaIn
        var beta = betaIn
        if (state.phase == DamasPhase.MatchEnd) {
            return when {
                state.draw -> 0
                state.winner == root -> 100_000 + depth
                else -> -100_000 - depth
            }
        }
        if (depth <= 0) return evaluate(state, root)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return if (state.turn == root) -100_000 + depth else 100_000 - depth
        val ordered = moves.sortedByDescending { it.captured != null }
        if (state.turn == root) {
            var best = Int.MIN_VALUE
            for (move in ordered) {
                val value = search(apply(state, move, record = false), depth - 1, alpha, beta, root)
                if (value > best) best = value
                if (value > alpha) alpha = value
                if (alpha >= beta) break
            }
            return best
        }
        var best = Int.MAX_VALUE
        for (move in ordered) {
            val value = search(apply(state, move, record = false), depth - 1, alpha, beta, root)
            if (value < best) best = value
            if (value < beta) beta = value
            if (alpha >= beta) break
        }
        return best
    }

    private fun evaluate(state: DamasState, root: DamasColor): Int =
        material(state, root) - material(state, other(root))

    private fun material(state: DamasState, color: DamasColor): Int {
        var score = 0
        for (index in state.board.indices) {
            val piece = state.board[index] ?: continue
            if (piece.color != color) continue
            val rank = rankOf(index)
            val advancement = if (color == DamasColor.White) SIZE - 1 - rank else rank
            score += (if (piece.king) 190 else 100) + advancement * 3
            if (fileOf(index) in 2..5) score += 4
        }
        return score
    }

    fun winnerName(state: DamasState, whiteName: String, blackName: String): String = when (state.winner) {
        DamasColor.White -> whiteName
        DamasColor.Black -> blackName
        null -> ""
    }
}
