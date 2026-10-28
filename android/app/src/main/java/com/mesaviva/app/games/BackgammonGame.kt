package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Backgammon para dos jugadores en Compose.
 *
 * 15 fichas por bando, barra, capturas, dobles, bear-off y conteo de pips.
 * Blancas van del punto 24 al 1; negras del 1 al 24.
 */
enum class BgSide { White, Black }

enum class BgPhase { Roll, Move, MatchEnd }

data class BgMove(val from: Int, val to: Int, val die: Int, val hit: Boolean = false)

data class BgState(
    val points: List<Int>,
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val offWhite: Int = 0,
    val offBlack: Int = 0,
    val turn: BgSide = BgSide.White,
    val phase: BgPhase = BgPhase.Roll,
    val dice: Pair<Int, Int>? = null,
    val lastDice: Pair<Int, Int>? = null,
    val movesLeft: List<Int> = emptyList(),
    val lastMove: BgMove? = null,
    val winner: BgSide? = null,
)

data class BgResult(val state: BgState, val error: String? = null)

class BackgammonGame(seed: Long = Random.nextLong()) {
    private val random = Random(seed)

    companion object {
        const val HOME_LIMIT = 15

        fun other(side: BgSide): BgSide = if (side == BgSide.White) BgSide.Black else BgSide.White

        fun initialPoints(): List<Int> {
            val points = MutableList(24) { 0 }
            points[23] = 2
            points[12] = 5
            points[7] = 3
            points[5] = 5
            points[0] = -2
            points[11] = -5
            points[16] = -3
            points[18] = -5
            return points
        }

        fun canLand(points: List<Int>, side: BgSide, to: Int): Boolean {
            if (to !in 0..23) return false
            val count = points[to]
            return if (side == BgSide.White) count >= -1 else count <= 1
        }

        fun canBearOff(state: BgState, side: BgSide, from: Int, die: Int): Boolean {
            if (!allHome(state, side)) return false
            if (side == BgSide.White) {
                if (from !in 0..5) return false
                val point = from + 1
                if (die < point) return false
                if (die == point) return true
                return (from + 1 until 6).all { state.points[it] <= 0 }
            }
            if (from !in 18..23) return false
            val point = 24 - from
            if (die < point) return false
            if (die == point) return true
            return (from + 1 until 24).all { state.points[it] >= 0 }
        }

        fun allHome(state: BgState, side: BgSide): Boolean {
            if (if (side == BgSide.White) state.barWhite > 0 else state.barBlack > 0) return false
            return if (side == BgSide.White) {
                state.points.drop(6).all { it <= 0 }
            } else {
                state.points.take(18).all { it >= 0 }
            }
        }

        fun pipCount(state: BgState, side: BgSide): Int {
            var result = if (side == BgSide.White) state.barWhite * 25 else state.barBlack * 25
            for (index in 0 until 24) {
                val count = state.points[index]
                if (side == BgSide.White && count > 0) result += count * (index + 1)
                if (side == BgSide.Black && count < 0) result += -count * (24 - index)
            }
            return result
        }
    }

    fun newMatch(): BgState = BgState(points = initialPoints())

    fun legalMoves(state: BgState, side: BgSide = state.turn): List<BgMove> {
        if (state.phase != BgPhase.Move || state.movesLeft.isEmpty()) return emptyList()
        val moves = mutableListOf<BgMove>()
        val seen = mutableSetOf<Int>()
        for (die in state.movesLeft) {
            if (!seen.add(die)) continue
            val bar = if (side == BgSide.White) state.barWhite else state.barBlack
            if (bar > 0) {
                val to = if (side == BgSide.White) 24 - die else die - 1
                if (canLand(state.points, side, to)) {
                    moves.add(BgMove(-1, to, die, state.points[to] == if (side == BgSide.White) -1 else 1))
                }
                continue
            }
            for (from in 0 until 24) {
                val count = state.points[from]
                if (if (side == BgSide.White) count <= 0 else count >= 0) continue
                val to = if (side == BgSide.White) from - die else from + die
                if (to in 0..23) {
                    if (!canLand(state.points, side, to)) continue
                    moves.add(BgMove(from, to, die, state.points[to] == if (side == BgSide.White) -1 else 1))
                } else if (canBearOff(state, side, from, die)) {
                    moves.add(BgMove(from, 24, die))
                }
            }
        }
        return moves
    }

    fun roll(state: BgState): BgResult {
        if (state.phase == BgPhase.MatchEnd) return BgResult(state, "bg.error.matchOver")
        if (state.phase != BgPhase.Roll) return BgResult(state, "bg.error.invalid")
        val first = random.nextInt(1, 7)
        val second = random.nextInt(1, 7)
        val movesLeft = if (first == second) List(4) { first } else listOf(first, second)
        val rolled = state.copy(
            dice = first to second,
            lastDice = first to second,
            movesLeft = movesLeft,
            phase = BgPhase.Move,
        )
        if (legalMoves(rolled).isEmpty()) {
            return BgResult(
                rolled.copy(
                    phase = BgPhase.Roll,
                    turn = other(state.turn),
                    dice = null,
                    movesLeft = emptyList(),
                ),
                "bg.error.noMoves",
            )
        }
        return BgResult(rolled)
    }

    fun pass(state: BgState): BgResult {
        if (state.phase != BgPhase.Move || legalMoves(state).isNotEmpty()) {
            return BgResult(state, "bg.error.invalid")
        }
        return BgResult(
            state.copy(
                phase = BgPhase.Roll,
                turn = other(state.turn),
                dice = null,
                movesLeft = emptyList(),
            ),
        )
    }

    fun move(state: BgState, from: Int, to: Int): BgResult {
        if (state.phase != BgPhase.Move) return BgResult(state, "bg.error.invalid")
        val move = legalMoves(state).firstOrNull { it.from == from && it.to == to }
            ?: return BgResult(state, "bg.error.invalidMove")
        val side = state.turn
        val sign = if (side == BgSide.White) 1 else -1
        val points = state.points.toMutableList()
        var barWhite = state.barWhite
        var barBlack = state.barBlack
        var offWhite = state.offWhite
        var offBlack = state.offBlack
        if (move.from == -1) {
            if (side == BgSide.White) barWhite = (barWhite - 1).coerceAtLeast(0) else barBlack = (barBlack - 1).coerceAtLeast(0)
        } else {
            points[move.from] = points[move.from] - sign
        }
        if (move.to == 24) {
            if (side == BgSide.White) offWhite += 1 else offBlack += 1
        } else {
            if (points[move.to] == -sign) {
                if (side == BgSide.White) barBlack += 1 else barWhite += 1
                points[move.to] = 0
            }
            points[move.to] = points[move.to] + sign
        }
        val movesLeft = state.movesLeft.toMutableList()
        val index = movesLeft.indexOf(move.die)
        if (index >= 0) movesLeft.removeAt(index)
        val next = state.copy(
            points = points.toList(),
            barWhite = barWhite,
            barBlack = barBlack,
            offWhite = offWhite,
            offBlack = offBlack,
            movesLeft = movesLeft.toList(),
            dice = null,
            lastMove = move,
        )
        val off = if (side == BgSide.White) offWhite else offBlack
        if (off >= HOME_LIMIT) return BgResult(next.copy(phase = BgPhase.MatchEnd, winner = side))
        if (movesLeft.isNotEmpty() && legalMoves(next.copy(phase = BgPhase.Move), side).isNotEmpty()) {
            return BgResult(next.copy(phase = BgPhase.Move, turn = side))
        }
        return BgResult(
            next.copy(
                phase = BgPhase.Roll,
                turn = other(side),
                movesLeft = emptyList(),
                dice = null,
            ),
        )
    }

    fun resign(state: BgState): BgResult {
        if (state.phase == BgPhase.MatchEnd) return BgResult(state, "bg.error.matchOver")
        return BgResult(state.copy(phase = BgPhase.MatchEnd, winner = other(state.turn)))
    }

    fun botMove(state: BgState): BgMove? {
        if (state.phase != BgPhase.Move) return null
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        var best = Double.NEGATIVE_INFINITY
        val pool = mutableListOf<BgMove>()
        for (move in moves) {
            val value = (if (move.hit) 120 else 0) +
                (if (move.to == 24) 110 else 0) +
                (if (move.from == -1) 70 else 0) +
                (if (move.to >= 18) 20 else 0) +
                move.die + random.nextDouble() * 8
            if (value > best + 1) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (kotlin.math.abs(value - best) <= 1) {
                pool.add(move)
            }
        }
        return pool.randomOrNull(random) ?: moves.first()
    }

    fun winnerName(state: BgState): String = when (state.winner) {
        BgSide.White -> "Tú"
        BgSide.Black -> "Bot"
        null -> "—"
    }
}
