package com.mesaviva.app.games

import kotlin.random.Random

enum class ReversiColor { White, Black }

enum class ReversiPhase { Play, MatchEnd }

data class ReversiState(
    val board: List<Int>,
    val turn: ReversiColor,
    val phase: ReversiPhase = ReversiPhase.Play,
    val lastMove: Int? = null,
    val lastFlips: List<Int> = emptyList(),
    val winner: ReversiColor? = null,
    val draw: Boolean = false,
    val passed: Boolean = false,
    val history: List<String> = emptyList(),
) {
    val whiteCount: Int get() = board.count { it == 1 }
    val blackCount: Int get() = board.count { it == 2 }
}

data class ReversiResult(val state: ReversiState, val error: String? = null)

/**
 * Reversi 8×8 para la mesa nativa.
 *
 * Las blancas abren. Si un jugador no tiene jugadas, pierde el turno; si
 * ambos se quedan sin jugadas, gana quien tenga más fichas. El bot usa
 * esquinas, movilidad y capturas, con profundidad configurable.
 */
class ReversiGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val SIZE = 8
        val DIRECTIONS = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1, 0 to 1,
            1 to -1, 1 to 0, 1 to 1,
        )

        fun initial(): ReversiState {
            val board = MutableList(SIZE * SIZE) { 0 }
            board[27] = 1
            board[36] = 1
            board[28] = 2
            board[35] = 2
            return ReversiState(board = board, turn = ReversiColor.White)
        }

        fun file(index: Int): Char = ('a' + index % SIZE)
        fun rank(index: Int): Int = SIZE - index / SIZE
        fun square(index: Int): String = "${file(index)}${rank(index)}"
        fun other(color: ReversiColor): ReversiColor =
            if (color == ReversiColor.White) ReversiColor.Black else ReversiColor.White
    }

    fun legalMoves(state: ReversiState, color: ReversiColor = state.turn): Map<Int, List<Int>> {
        if (state.phase == ReversiPhase.MatchEnd) return emptyMap()
        val mine = if (color == ReversiColor.White) 1 else 2
        val theirs = if (color == ReversiColor.White) 2 else 1
        val result = mutableMapOf<Int, List<Int>>()
        for (index in state.board.indices) {
            if (state.board[index] != 0) continue
            val flips = mutableListOf<Int>()
            val file = index % SIZE
            val rank = index / SIZE
            for ((df, dr) in DIRECTIONS) {
                var f = file + df
                var r = rank + dr
                val line = mutableListOf<Int>()
                while (f in 0 until SIZE && r in 0 until SIZE) {
                    val cell = r * SIZE + f
                    when (state.board[cell]) {
                        theirs -> line.add(cell)
                        mine -> {
                            if (line.isNotEmpty()) flips.addAll(line)
                            break
                        }
                        else -> break
                    }
                    f += df
                    r += dr
                }
            }
            if (flips.isNotEmpty()) result[index] = flips.distinct()
        }
        return result
    }

    fun move(state: ReversiState, index: Int): ReversiResult {
        if (state.phase == ReversiPhase.MatchEnd) return ReversiResult(state, "reversi.error.matchOver")
        val flips = legalMoves(state)[index] ?: return ReversiResult(state, "reversi.error.invalid")
        val board = state.board.toMutableList()
        board[index] = if (state.turn == ReversiColor.White) 1 else 2
        flips.forEach { board[it] = board[index] }
        val entry = "${if (state.turn == ReversiColor.White) "B" else "N"} ${square(index)} +${flips.size}"
        var next = state.copy(
            board = board,
            turn = other(state.turn),
            lastMove = index,
            lastFlips = flips,
            passed = false,
            history = (state.history + entry).takeLast(120),
        )
        next = normalizeTurn(next)
        return ReversiResult(next)
    }

    fun pass(state: ReversiState): ReversiResult {
        if (state.phase == ReversiPhase.MatchEnd) return ReversiResult(state, "reversi.error.matchOver")
        if (legalMoves(state).isNotEmpty()) return ReversiResult(state, "reversi.error.mustPlay")
        return ReversiResult(normalizeTurn(state.copy(turn = other(state.turn), passed = true)))
    }

    fun resign(state: ReversiState): ReversiResult {
        if (state.phase == ReversiPhase.MatchEnd) return ReversiResult(state, "reversi.error.matchOver")
        return ReversiResult(
            state.copy(
                phase = ReversiPhase.MatchEnd,
                winner = other(state.turn),
            ),
        )
    }

    private fun normalizeTurn(state: ReversiState): ReversiState {
        if (legalMoves(state, state.turn).isNotEmpty()) return state
        val rival = other(state.turn)
        if (legalMoves(state, rival).isNotEmpty()) {
            return state.copy(turn = rival, passed = true)
        }
        val white = state.board.count { it == 1 }
        val black = state.board.count { it == 2 }
        return state.copy(
            phase = ReversiPhase.MatchEnd,
            winner = when {
                white > black -> ReversiColor.White
                black > white -> ReversiColor.Black
                else -> null
            },
            draw = white == black,
        )
    }

    fun botMove(state: ReversiState, difficulty: String = "normal"): Int? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") return moves.keys.randomOrNull(random)
        val depth = if (difficulty == "hard") 3 else 1
        var best = Int.MIN_VALUE
        val pool = mutableListOf<Int>()
        for (move in moves.keys) {
            val next = move(state, move).state
            val value = search(next, depth - 1, state.turn)
            if (value > best) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (value == best) {
                pool.add(move)
            }
        }
        return pool.randomOrNull(random) ?: moves.keys.first()
    }

    private fun search(state: ReversiState, depth: Int, root: ReversiColor): Int {
        if (state.phase == ReversiPhase.MatchEnd) {
            val diff = state.whiteCount - state.blackCount
            return if (root == ReversiColor.White) diff * 1000 else -diff * 1000
        }
        if (depth <= 0) return evaluate(state, root)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return evaluate(state, root)
        return if (state.turn == root) {
            moves.keys.maxOf { search(move(state, it).state, depth - 1, root) }
        } else {
            moves.keys.minOf { search(move(state, it).state, depth - 1, root) }
        }
    }

    private fun evaluate(state: ReversiState, root: ReversiColor): Int {
        val mine = if (root == ReversiColor.White) 1 else 2
        val theirs = if (root == ReversiColor.White) 2 else 1
        val corners = intArrayOf(0, 7, 56, 63)
        var score = 0
        for (index in state.board.indices) {
            val value = when {
                index in corners -> 30
                index % 8 == 0 || index % 8 == 7 || index / 8 == 0 || index / 8 == 7 -> 6
                else -> 1
            }
            when (state.board[index]) {
                mine -> score += value
                theirs -> score -= value
            }
        }
        val mobility = legalMoves(state, root).size - legalMoves(state, other(root)).size
        return score * 4 + mobility * 3
    }

    fun winnerName(state: ReversiState, whiteName: String, blackName: String): String = when (state.winner) {
        ReversiColor.White -> whiteName
        ReversiColor.Black -> blackName
        null -> ""
    }
}
