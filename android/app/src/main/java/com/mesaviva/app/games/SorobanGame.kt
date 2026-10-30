package com.mesaviva.app.games

import kotlin.math.pow
import kotlin.random.Random

/**
 * Soroban: ábaco japonés de 13 varillas.
 *
 * Cada varilla tiene una cuenta celestial de 5 y cuatro cuentas terrestres
 * de 1. El reto pide formar un número; el jugador mueve las cuentas y
 * comprueba. Diez retos completan la partida.
 */
enum class SorobanPhase { Playing, Complete }

data class SorobanState(
    val heaven: List<Boolean> = List(RODS) { false },
    val earth: List<Int> = List(RODS) { 0 },
    val target: Long = 0,
    val level: Int = 1,
    val solved: Int = 0,
    val score: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val selectedRod: Int = 0,
    val phase: SorobanPhase = SorobanPhase.Playing,
    val message: String? = null,
    val history: List<String> = emptyList(),
) {
    companion object {
        const val RODS = 13
    }
}

data class SorobanResult(val state: SorobanState, val correct: Boolean? = null, val error: String? = null)

class SorobanGame(
    private val targetCount: Int = 10,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)

    fun newGame(level: Int = 1): SorobanState {
        val cleanLevel = level.coerceIn(1, 10)
        return SorobanState(
            target = randomTarget(cleanLevel),
            level = cleanLevel,
            history = listOf("Reto 1 · nivel $cleanLevel"),
        )
    }

    fun digit(state: SorobanState, rod: Int): Int {
        if (rod !in 0 until SorobanState.RODS) return 0
        val five = if (state.heaven.getOrElse(rod) { false }) 5 else 0
        return five + state.earth.getOrElse(rod) { 0 }
    }

    fun value(state: SorobanState): Long {
        var total = 0L
        for (rod in 0 until SorobanState.RODS) {
            val factor = 10.0.pow(rod.toDouble()).toLong()
            total += digit(state, rod).toLong() * factor
        }
        return total
    }

    fun selectRod(state: SorobanState, rod: Int): SorobanState {
        if (rod !in 0 until SorobanState.RODS) return state
        return state.copy(selectedRod = rod, message = null)
    }

    fun toggleHeaven(state: SorobanState, rod: Int = state.selectedRod): SorobanState {
        if (state.phase == SorobanPhase.Complete || rod !in 0 until SorobanState.RODS) return state
        val heaven = state.heaven.toMutableList()
        heaven[rod] = !heaven[rod]
        return state.copy(heaven = heaven, selectedRod = rod, message = null)
    }

    fun changeEarth(state: SorobanState, delta: Int, rod: Int = state.selectedRod): SorobanState {
        if (state.phase == SorobanPhase.Complete || rod !in 0 until SorobanState.RODS) return state
        val earth = state.earth.toMutableList()
        earth[rod] = (earth[rod] + delta).coerceIn(0, 4)
        return state.copy(earth = earth, selectedRod = rod, message = null)
    }

    fun clearRod(state: SorobanState, rod: Int = state.selectedRod): SorobanState {
        if (state.phase == SorobanPhase.Complete || rod !in 0 until SorobanState.RODS) return state
        val earth = state.earth.toMutableList()
        val heaven = state.heaven.toMutableList()
        earth[rod] = 0
        heaven[rod] = false
        return state.copy(earth = earth, heaven = heaven, selectedRod = rod, message = null)
    }

    fun clearAll(state: SorobanState): SorobanState =
        state.copy(
            heaven = List(SorobanState.RODS) { false },
            earth = List(SorobanState.RODS) { 0 },
            message = null,
        )

    fun check(state: SorobanState): SorobanResult {
        if (state.phase == SorobanPhase.Complete) return SorobanResult(state, error = "soroban.error.complete")
        val current = value(state)
        if (current != state.target) {
            return SorobanResult(
                state.copy(
                    streak = 0,
                    message = "soroban.wrong",
                    history = (state.history + "✗ ${format(current)} ≠ ${format(state.target)}").takeLast(60),
                ),
                correct = false,
            )
        }
        val solved = state.solved + 1
        val streak = state.streak + 1
        val points = 60 + state.level * 15 + state.streak * 5
        val entry = "✓ ${format(state.target)} · +$points"
        if (solved >= targetCount) {
            return SorobanResult(
                state.copy(
                    solved = solved,
                    score = state.score + points,
                    streak = streak,
                    bestStreak = maxOf(state.bestStreak, streak),
                    phase = SorobanPhase.Complete,
                    message = "soroban.complete",
                    history = (state.history + entry).takeLast(60),
                ),
                correct = true,
            )
        }
        val nextLevel = state.level.coerceAtMost(10)
        return SorobanResult(
            state.copy(
                heaven = List(SorobanState.RODS) { false },
                earth = List(SorobanState.RODS) { 0 },
                target = randomTarget(nextLevel),
                level = nextLevel,
                solved = solved,
                score = state.score + points,
                streak = streak,
                bestStreak = maxOf(state.bestStreak, streak),
                message = "soroban.correct",
                history = (state.history + entry + "Reto ${solved + 1} · nivel $nextLevel").takeLast(60),
            ),
            correct = true,
        )
    }

    fun nextLevel(state: SorobanState): SorobanState {
        if (state.phase == SorobanPhase.Complete) return state
        val level = (state.level + 1).coerceAtMost(10)
        return state.copy(
            heaven = List(SorobanState.RODS) { false },
            earth = List(SorobanState.RODS) { 0 },
            target = randomTarget(level),
            level = level,
            message = null,
            history = (state.history + "Reto ${state.solved + 1} · nivel $level").takeLast(60),
        )
    }

    fun botTarget(state: SorobanState): SorobanState {
        // El bot "resuelve" el reto actual para demostrar el valor en el ábaco.
        val target = state.target
        val heaven = MutableList(SorobanState.RODS) { false }
        val earth = MutableList(SorobanState.RODS) { 0 }
        for (rod in 0 until SorobanState.RODS) {
            val factor = 10.0.pow(rod.toDouble()).toLong()
            val digit = ((target / factor) % 10).toInt()
            heaven[rod] = digit >= 5
            earth[rod] = digit % 5
        }
        return state.copy(heaven = heaven, earth = earth, message = null)
    }

    fun format(value: Long): String = value.toString()

    private fun randomTarget(level: Int): Long {
        val digits = when {
            level <= 1 -> 1
            level <= 3 -> 2
            level <= 5 -> 3
            level <= 7 -> 4
            level <= 9 -> 5
            else -> 6
        }
        val min = if (digits == 1) 1L else 10.0.pow((digits - 1).toDouble()).toLong()
        val max = 10.0.pow(digits.toDouble()).toLong() - 1
        return random.nextLong(min, max + 1)
    }
}
