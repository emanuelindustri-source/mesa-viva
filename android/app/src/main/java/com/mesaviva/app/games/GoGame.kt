package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Go para 9×9 y 13×13 en la mesa nativa.
 *
 * Capturas por grupos y libertades, ko simple, dos pases para contar,
 * komi 6.5 y conteo de territorio por áreas. El bot prioriza capturas,
 * libertades y el centro sin hacer trampas.
 */
enum class GoPhase { Play, MatchEnd }

data class GoState(
    val size: Int,
    val board: List<Int>,
    val turn: Int,
    val passes: Int = 0,
    val ko: Int? = null,
    val capturesBlack: Int = 0,
    val capturesWhite: Int = 0,
    val moveNo: Int = 0,
    val phase: GoPhase = GoPhase.Play,
    val lastMove: Int? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val blackScore: Double = 0.0,
    val whiteScore: Double = 0.0,
    val history: List<String> = emptyList(),
)

data class GoResult(val state: GoState, val error: String? = null)

data class GoAttempt(
    val board: List<Int>,
    val captured: List<Int>,
    val ko: Int?,
)

class GoGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val KOMI = 6.5

        fun other(color: Int): Int = if (color == 1) 2 else 1

        fun columnLetter(index: Int, size: Int): String {
            val file = index % size
            val rank = index / size
            return "${('A' + file)}${size - rank}"
        }

        fun neighbors(index: Int, size: Int): List<Int> {
            val file = index % size
            val rank = index / size
            val out = mutableListOf<Int>()
            if (file > 0) out.add(index - 1)
            if (file < size - 1) out.add(index + 1)
            if (rank > 0) out.add(index - size)
            if (rank < size - 1) out.add(index + size)
            return out
        }

        fun group(board: List<Int>, start: Int, size: Int): Pair<List<Int>, List<Int>> {
            val color = board.getOrNull(start) ?: 0
            val stones = mutableListOf<Int>()
            val liberties = linkedSetOf<Int>()
            val seen = mutableSetOf<Int>()
            val queue = ArrayDeque<Int>()
            queue.add(start)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (!seen.add(current)) continue
                val value = board.getOrNull(current) ?: 0
                if (value == 0) {
                    liberties.add(current)
                    continue
                }
                if (value != color) continue
                stones.add(current)
                for (next in neighbors(current, size)) {
                    val neighbour = board.getOrNull(next) ?: 0
                    if (neighbour == 0) liberties.add(next)
                    else if (neighbour == color && next !in seen) queue.add(next)
                }
            }
            return stones to liberties.toList()
        }

        fun scoreBoard(state: GoState): GoScore {
            val size = state.size
            val visited = mutableSetOf<Int>()
            var blackTerritory = 0
            var whiteTerritory = 0
            var blackStones = 0
            var whiteStones = 0
            for (index in state.board.indices) {
                when (state.board[index]) {
                    1 -> blackStones += 1
                    2 -> whiteStones += 1
                }
                if (state.board[index] != 0 || index in visited) continue
                val region = mutableListOf<Int>()
                val borders = mutableSetOf<Int>()
                val queue = ArrayDeque<Int>()
                queue.add(index)
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    if (!visited.add(current)) continue
                    region.add(current)
                    for (next in neighbors(current, size)) {
                        val value = state.board.getOrNull(next) ?: 0
                        if (value == 0) queue.add(next) else borders.add(value)
                    }
                }
                if (borders.size == 1) {
                    when (borders.first()) {
                        1 -> blackTerritory += region.size
                        2 -> whiteTerritory += region.size
                    }
                }
            }
            return GoScore(
                black = (blackStones + blackTerritory).toDouble(),
                white = whiteStones + whiteTerritory + KOMI,
                blackTerritory = blackTerritory,
                whiteTerritory = whiteTerritory,
                blackStones = blackStones,
                whiteStones = whiteStones,
            )
        }
    }

    fun newMatch(size: Int = 9): GoState {
        val safeSize = if (size == 13) 13 else 9
        return GoState(
            size = safeSize,
            board = List(safeSize * safeSize) { 0 },
            turn = 1,
        )
    }

    private fun attempt(state: GoState, index: Int, color: Int): GoAttempt? {
        if (index !in state.board.indices) return null
        if (state.board[index] != 0) return null
        if (state.ko == index) return null
        val board = state.board.toMutableList()
        board[index] = color
        val captured = mutableListOf<Int>()
        val opponent = other(color)
        for (next in neighbors(index, state.size)) {
            if (board[next] != opponent) continue
            val (stones, liberties) = group(board, next, state.size)
            if (liberties.isEmpty()) {
                for (stone in stones) {
                    board[stone] = 0
                    captured.add(stone)
                }
            }
        }
        val (ownStones, ownLiberties) = group(board, index, state.size)
        if (ownLiberties.isEmpty()) return null
        val ko = if (captured.size == 1 && ownStones.size == 1 && ownLiberties.size == 1) captured[0] else null
        return GoAttempt(board, captured, ko)
    }

    fun legalMoves(state: GoState, color: Int = state.turn): List<Int> {
        if (state.phase == GoPhase.MatchEnd) return emptyList()
        val out = mutableListOf<Int>()
        for (index in state.board.indices) {
            if (state.board[index] != 0) continue
            if (attempt(state, index, color) != null) out.add(index)
        }
        return out
    }

    fun play(state: GoState, index: Int): GoResult {
        if (state.phase == GoPhase.MatchEnd) return GoResult(state, "go.error.matchOver")
        val attempted = attempt(state, index, state.turn) ?: return GoResult(state, "go.error.invalidMove")
        val entry = "${if (state.turn == 1) "●" else "○"} ${columnLetter(index, state.size)}"
        val capturedEntries = attempted.captured.map { columnLetter(it, state.size) }
        return GoResult(
            state.copy(
                board = attempted.board,
                ko = attempted.ko,
                passes = 0,
                capturesBlack = state.capturesBlack + if (state.turn == 1) attempted.captured.size else 0,
                capturesWhite = state.capturesWhite + if (state.turn == 2) attempted.captured.size else 0,
                turn = other(state.turn),
                lastMove = index,
                moveNo = state.moveNo + 1,
                history = (state.history + buildString {
                    append(entry)
                    if (capturedEntries.isNotEmpty()) append(" ×${capturedEntries.size}")
                }).takeLast(120),
            ),
        )
    }

    fun pass(state: GoState): GoResult {
        if (state.phase == GoPhase.MatchEnd) return GoResult(state, "go.error.matchOver")
        val passes = state.passes + 1
        val next = state.copy(
            passes = passes,
            ko = null,
            lastMove = null,
            turn = other(state.turn),
            history = (state.history + "…").takeLast(120),
        )
        return GoResult(if (passes >= 2) finishScore(next) else next)
    }

    fun resign(state: GoState): GoResult {
        if (state.phase == GoPhase.MatchEnd) return GoResult(state, "go.error.matchOver")
        return GoResult(
            state.copy(
                phase = GoPhase.MatchEnd,
                winner = other(state.turn),
                reason = "go.reason.resign",
            ),
        )
    }

    fun finishScore(state: GoState): GoState {
        val score = scoreBoard(state)
        val winner = when {
            score.black > score.white -> 1
            score.white > score.black -> 2
            else -> 0
        }
        return state.copy(
            phase = GoPhase.MatchEnd,
            winner = winner.takeIf { it != 0 },
            draw = winner == 0,
            reason = if (winner == 0) "go.reason.draw" else "go.reason.score",
            blackScore = score.black,
            whiteScore = score.white,
        )
    }

    fun botMove(state: GoState, difficulty: String = "normal", rng: Random = random): Int? {
        if (state.phase == GoPhase.MatchEnd) return null
        val legal = legalMoves(state, state.turn)
        if (legal.isEmpty()) return null
        if (difficulty == "easy") return legal[rng.nextInt(legal.size)]
        val center = (state.size - 1) / 2.0
        var best = Double.NEGATIVE_INFINITY
        val pool = mutableListOf<Int>()
        for (index in legal) {
            val attempted = attempt(state, index, state.turn) ?: continue
            val (stones, liberties) = group(attempted.board, index, state.size)
            val file = index % state.size
            val rank = index / state.size
            val centrality = (center - abs(center - file) - abs(center - rank))
            val value = attempted.captured.size * 16.0 +
                liberties.size * 2.0 +
                centrality * 0.5 +
                (if (stones.size > 1) 1.5 else 0.0) +
                if (difficulty == "hard") rng.nextDouble() * 0.2 else 0.0
            if (value > best + 0.01) {
                best = value
                pool.clear()
                pool.add(index)
            } else if (abs(value - best) <= 0.01) {
                pool.add(index)
            }
        }
        return pool.randomOrNull(rng) ?: legal.first()
    }

    fun winnerName(state: GoState, blackName: String, whiteName: String): String = when (state.winner) {
        1 -> blackName
        2 -> whiteName
        else -> ""
    }
}

data class GoScore(
    val black: Double,
    val white: Double,
    val blackTerritory: Int,
    val whiteTerritory: Int,
    val blackStones: Int,
    val whiteStones: Int,
)
