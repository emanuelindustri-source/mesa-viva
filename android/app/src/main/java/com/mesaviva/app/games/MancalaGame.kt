package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Mancala (Kalah) para dos jugadores en Compose.
 *
 * Cada lado tiene 6 hoyos y un almacén. Se siembra en sentido contrario a las
 * agujas del reloj, sin dejar semillas en el almacén rival. Si la última
 * semilla cae en tu almacén, repites; si cae en un hoyo vacío de tu lado,
 * capturas el hoyo opuesto. La partida termina cuando un lado queda vacío.
 */
enum class MancalaSide { White, Black }

enum class MancalaPhase { Play, MatchEnd }

data class MancalaState(
    val pits: List<Int>,
    val turn: MancalaSide,
    val phase: MancalaPhase,
    val lastPit: Int? = null,
    val lastCapture: Int? = null,
    val winner: MancalaSide? = null,
) {
    val whiteStore: Int get() = pits.getOrElse(6) { 0 }
    val blackStore: Int get() = pits.getOrElse(13) { 0 }
}

data class MancalaResult(val state: MancalaState, val error: String? = null)

class MancalaGame(seed: Long = Random.nextLong()) {
    private val random = Random(seed)

    companion object {
        val WHITE_PITS = listOf(0, 1, 2, 3, 4, 5)
        val BLACK_PITS = listOf(7, 8, 9, 10, 11, 12)
        const val WHITE_STORE = 6
        const val BLACK_STORE = 13

        fun sidePits(side: MancalaSide): List<Int> = if (side == MancalaSide.White) WHITE_PITS else BLACK_PITS
        fun store(side: MancalaSide): Int = if (side == MancalaSide.White) WHITE_STORE else BLACK_STORE
        fun opposite(side: MancalaSide): MancalaSide = if (side == MancalaSide.White) MancalaSide.Black else MancalaSide.White
        fun captureOpposite(pit: Int): Int = 12 - pit
    }

    fun newMatch(): MancalaState = MancalaState(
        pits = listOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4, 4, 0),
        turn = MancalaSide.White,
        phase = MancalaPhase.Play,
    )

    fun sow(state: MancalaState, pit: Int): MancalaResult {
        if (state.phase == MancalaPhase.MatchEnd) return MancalaResult(state, "mancala.error.matchOver")
        if (pit !in sidePits(state.turn)) return MancalaResult(state, "mancala.error.invalidPit")
        val pits = state.pits.toMutableList()
        var seeds = pits.getOrElse(pit) { 0 }
        if (seeds <= 0) return MancalaResult(state, "mancala.error.emptyPit")
        pits[pit] = 0
        var cursor = pit
        val opponentStore = store(opposite(state.turn))
        while (seeds > 0) {
            cursor = (cursor + 1) % 14
            if (cursor == opponentStore) continue
            pits[cursor] = pits[cursor] + 1
            seeds -= 1
        }
        val myStore = store(state.turn)
        var extra = false
        var lastCapture: Int? = null
        if (cursor == myStore) {
            extra = true
        } else if (cursor in sidePits(state.turn) && pits[cursor] == 1) {
            val opposite = captureOpposite(cursor)
            val captured = pits[opposite]
            if (captured > 0) {
                pits[opposite] = 0
                pits[cursor] = 0
                pits[myStore] = pits[myStore] + captured + 1
                lastCapture = captured + 1
            }
        }
        var next = state.copy(
            pits = pits.toList(),
            turn = if (extra) state.turn else opposite(state.turn),
            lastPit = cursor,
            lastCapture = lastCapture,
        )
        if (!extra) next = checkEnd(next)
        return MancalaResult(next)
    }

    fun botPit(state: MancalaState, difficulty: String = "normal"): Int? {
        if (state.phase == MancalaPhase.MatchEnd) return null
        val moves = sidePits(state.turn).filter { state.pits[it] > 0 }
        if (moves.isEmpty()) return null
        if (difficulty == "easy") return moves.randomOrNull(random)
        val depth = if (difficulty == "hard") 4 else 2
        val root = state.turn
        var best = Double.NEGATIVE_INFINITY
        val pool = mutableListOf<Int>()
        for (pit in moves) {
            val next = sow(state, pit).state
            val value = -search(next, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, root)
            if (value > best + 0.5) {
                best = value
                pool.clear()
                pool.add(pit)
            } else if (kotlin.math.abs(value - best) <= 0.5) {
                pool.add(pit)
            }
        }
        return pool.randomOrNull(random) ?: moves.first()
    }

    private fun search(state: MancalaState, depth: Int, alphaStart: Double, betaStart: Double, root: MancalaSide): Double {
        var alpha = alphaStart
        var beta = betaStart
        if (state.phase == MancalaPhase.MatchEnd) {
            val mine = if (root == MancalaSide.White) state.whiteStore else state.blackStore
            val theirs = if (root == MancalaSide.White) state.blackStore else state.whiteStore
            return (mine - theirs) * 1000.0 + if (mine > theirs) 500 else if (mine < theirs) -500 else 0
        }
        if (depth <= 0) return evaluate(state, root)
        val moves = sidePits(state.turn).filter { state.pits[it] > 0 }
        if (moves.isEmpty()) return evaluate(state, root)
        val maximizing = state.turn == root
        var best = if (maximizing) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        for (pit in moves) {
            val next = sow(state, pit).state
            val value = search(next, depth - 1, alpha, beta, root)
            if (maximizing) {
                if (value > best) best = value
                if (value > alpha) alpha = value
            } else {
                if (value < best) best = value
                if (value < beta) beta = value
            }
            if (alpha >= beta) break
        }
        return best
    }

    private fun evaluate(state: MancalaState, root: MancalaSide): Double {
        val mine = if (root == MancalaSide.White) state.whiteStore else state.blackStore
        val theirs = if (root == MancalaSide.White) state.blackStore else state.whiteStore
        val myPits = sidePits(root).sumOf { state.pits[it] }
        val theirPits = sidePits(opposite(root)).sumOf { state.pits[it] }
        return (mine - theirs) * 10.0 + (myPits - theirPits) * 0.4
    }

    private fun checkEnd(state: MancalaState): MancalaState {
        val whiteEmpty = WHITE_PITS.all { state.pits[it] == 0 }
        val blackEmpty = BLACK_PITS.all { state.pits[it] == 0 }
        if (!whiteEmpty && !blackEmpty) return state
        val pits = state.pits.toMutableList()
        var white = pits[WHITE_STORE]
        var black = pits[BLACK_STORE]
        for (pit in WHITE_PITS) {
            white += pits[pit]
            pits[pit] = 0
        }
        for (pit in BLACK_PITS) {
            black += pits[pit]
            pits[pit] = 0
        }
        pits[WHITE_STORE] = white
        pits[BLACK_STORE] = black
        val winner = when {
            white > black -> MancalaSide.White
            black > white -> MancalaSide.Black
            else -> null
        }
        return state.copy(
            pits = pits.toList(),
            phase = MancalaPhase.MatchEnd,
            winner = winner,
        )
    }
}
