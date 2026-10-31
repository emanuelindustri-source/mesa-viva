package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Ludo para 2–4 jugadores en la mesa nativa.
 *
 * Cuatro fichas por color, salida con 6, capturas, casillas seguras, tres
 * seises seguidos pierden el turno, turno extra por 6/captura/meta y meta
 * exacta. Se puede jugar contra bots o en modo pasar el teléfono.
 */
enum class LudoColor { Red, Green, Yellow, Blue }

data class LudoPlayer(
    val name: String,
    val isBot: Boolean,
    val color: LudoColor,
    val captures: Int = 0,
)

data class LudoToken(
    val id: String,
    val color: LudoColor,
    val slot: Int,
    val progress: Int,
)

data class LudoMove(val tokenId: String, val from: Int, val to: Int)

enum class LudoPhase { Roll, Move, MatchEnd }

data class LudoState(
    val players: List<LudoPlayer>,
    val tokens: List<LudoToken>,
    val turn: Int,
    val phase: LudoPhase,
    val dice: Int? = null,
    val lastRoll: Int? = null,
    val sixStreak: Int = 0,
    val lastMove: LudoMove? = null,
    val winnerIndex: Int? = null,
)

data class LudoResult(val state: LudoState, val error: String? = null)

class LudoGame(
    playerCount: Int,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
    private val variant: String = "ludo",
) {
    private val random = Random(seed)
    private val count = playerCount.coerceIn(2, 4)
    private val isAeroplane = variant == "aeroplane"

    companion object {
        const val FINISH = 57

        val TRACK: List<Pair<Int, Int>> = listOf(
            6 to 14, 6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9,
            5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8,
            0 to 7,
            0 to 6, 1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6,
            6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0,
            7 to 0,
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
            9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6,
            14 to 7,
            14 to 8, 13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8,
            8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14,
            7 to 14,
        )

        val START = mapOf(
            LudoColor.Red to 0,
            LudoColor.Green to 13,
            LudoColor.Yellow to 26,
            LudoColor.Blue to 39,
        )

        val HOME = mapOf(
            LudoColor.Red to listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9),
            LudoColor.Green to listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7),
            LudoColor.Yellow to listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5),
            LudoColor.Blue to listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7),
        )

        val BASE = mapOf(
            LudoColor.Red to listOf(1 to 10, 4 to 10, 1 to 13, 4 to 13),
            LudoColor.Green to listOf(1 to 1, 4 to 1, 1 to 4, 4 to 4),
            LudoColor.Yellow to listOf(10 to 1, 13 to 1, 10 to 4, 13 to 4),
            LudoColor.Blue to listOf(10 to 10, 13 to 10, 10 to 13, 13 to 13),
        )

        val SAFE = setOf(0, 8, 13, 21, 26, 34, 39, 47)

        /** Casillas de salto de Aeroplane Chess (cada cuatro pasos). */
        val AEROPLANE_JUMPS = setOf(4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48)

        fun colorsForPlayers(count: Int): List<LudoColor> = when {
            count <= 2 -> listOf(LudoColor.Red, LudoColor.Yellow)
            count == 3 -> listOf(LudoColor.Red, LudoColor.Green, LudoColor.Yellow)
            else -> listOf(LudoColor.Red, LudoColor.Green, LudoColor.Yellow, LudoColor.Blue)
        }

        fun absolute(color: LudoColor, progress: Int): Int =
            ((START[color] ?: 0) + progress) % TRACK.size

        fun tokenCoord(token: LudoToken): Pair<Int, Int> {
            if (token.progress == -1) return BASE[token.color]?.getOrNull(token.slot) ?: (0 to 0)
            if (token.progress in 52 until FINISH) return HOME[token.color]?.getOrNull(token.progress - 52) ?: (7 to 7)
            if (token.progress >= FINISH) return 7 to 7
            return TRACK[absolute(token.color, token.progress)]
        }
    }

    fun newMatch(): LudoState {
        val colors = colorsForPlayers(count)
        val players = colors.mapIndexed { index, color ->
            LudoPlayer(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> listOf("El Silbón", "La Sayona", "El Hachero")[index - 1]
                },
                isBot = !allHumans && index > 0,
                color = color,
            )
        }
        val tokens = colors.flatMap { color ->
            (0 until 4).map { slot -> LudoToken("${color.name.lowercase()}-$slot", color, slot, -1) }
        }
        return LudoState(players = players, tokens = tokens, turn = 0, phase = LudoPhase.Roll)
    }

    fun tokensOf(state: LudoState, color: LudoColor): List<LudoToken> =
        state.tokens.filter { it.color == color }

    fun legalMoves(state: LudoState, seatIndex: Int = state.turn, die: Int? = state.dice): List<LudoMove> {
        if (die == null || die !in 1..6) return emptyList()
        val color = state.players.getOrNull(seatIndex)?.color ?: return emptyList()
        val moves = mutableListOf<LudoMove>()
        for (token in tokensOf(state, color)) {
            if (token.progress >= FINISH) continue
            if (token.progress == -1) {
                if (die == 6) moves.add(LudoMove(token.id, -1, 0))
                continue
            }
            val to = token.progress + die
            if (to <= FINISH) moves.add(LudoMove(token.id, token.progress, jumpTarget(color, to)))
        }
        return moves
    }

    private fun jumpTarget(color: LudoColor, progress: Int): Int {
        if (!isAeroplane || progress > 51) return progress
        return if (absolute(color, progress) in AEROPLANE_JUMPS) (progress + 4).coerceAtMost(FINISH) else progress
    }

    fun finishedCount(state: LudoState, seatIndex: Int): Int {
        val color = state.players.getOrNull(seatIndex)?.color ?: return 0
        return tokensOf(state, color).count { it.progress >= FINISH }
    }

    fun roll(state: LudoState): LudoResult {
        if (state.phase == LudoPhase.MatchEnd) return LudoResult(state, "ludo.error.matchOver")
        if (state.phase != LudoPhase.Roll) return LudoResult(state, "ludo.error.invalid")
        val die = random.nextInt(1, 7)
        val sixStreak = if (die == 6) state.sixStreak + 1 else 0
        if (sixStreak >= 3) {
            return LudoResult(
                state.copy(
                    phase = LudoPhase.Roll,
                    turn = nextSeat(state),
                    dice = null,
                    lastRoll = die,
                    sixStreak = 0,
                ),
                "ludo.error.threeSixes",
            )
        }
        val moves = legalMoves(state.copy(dice = die), state.turn, die)
        if (moves.isEmpty()) {
            return LudoResult(
                state.copy(
                    phase = LudoPhase.Roll,
                    turn = nextSeat(state),
                    dice = null,
                    lastRoll = die,
                    sixStreak = 0,
                ),
                "ludo.error.noMoves",
            )
        }
        return LudoResult(state.copy(phase = LudoPhase.Move, dice = die, lastRoll = die, sixStreak = sixStreak))
    }

    fun move(state: LudoState, tokenId: String): LudoResult {
        if (state.phase != LudoPhase.Move || state.dice == null) return LudoResult(state, "ludo.error.invalid")
        val move = legalMoves(state).firstOrNull { it.tokenId == tokenId } ?: return LudoResult(state, "ludo.error.invalidMove")
        val die = state.dice
        val moving = state.tokens.firstOrNull { it.id == tokenId } ?: return LudoResult(state, "ludo.error.invalidMove")
        val captured = mutableListOf<LudoToken>()
        if (move.to <= 51 && absolute(moving.color, move.to) !in SAFE) {
            val destination = absolute(moving.color, move.to)
            for (token in state.tokens) {
                if (token.color == moving.color) continue
                if (token.progress !in 0..51) continue
                if (absolute(token.color, token.progress) == destination) captured.add(token)
            }
        }
        val capturedIds = captured.map { it.id }.toSet()
        val tokens = state.tokens.map { token ->
            when {
                token.id == moving.id -> token.copy(progress = move.to)
                token.id in capturedIds -> token.copy(progress = -1)
                else -> token
            }
        }
        val seat = state.players.getOrNull(state.turn)
        val players = state.players.mapIndexed { index, player ->
            if (index == state.turn) player.copy(captures = player.captures + captured.size) else player
        }
        val next = state.copy(
            players = players,
            tokens = tokens,
            dice = null,
            lastRoll = die,
            lastMove = LudoMove(move.tokenId, move.from, move.to),
            sixStreak = if (die == 6) state.sixStreak else 0,
            phase = LudoPhase.Roll,
            turn = state.turn,
        )
        val won = tokensOf(next, moving.color).all { it.progress >= FINISH }
        if (won) {
            return LudoResult(next.copy(phase = LudoPhase.MatchEnd, winnerIndex = state.turn))
        }
        val extra = die == 6 || captured.isNotEmpty() || move.to >= FINISH ||
            (isAeroplane && move.to > move.from + die)
        return LudoResult(next.copy(turn = if (extra) state.turn else nextSeat(state)))
    }

    fun resign(state: LudoState): LudoResult {
        if (state.phase == LudoPhase.MatchEnd) return LudoResult(state, "ludo.error.matchOver")
        val winner = state.players.indices.firstOrNull { it != state.turn } ?: state.turn
        return LudoResult(state.copy(phase = LudoPhase.MatchEnd, winnerIndex = winner))
    }

    fun botAction(state: LudoState): LudoAction? {
        val player = state.players.getOrNull(state.turn) ?: return null
        if (!player.isBot || state.phase == LudoPhase.MatchEnd) return null
        if (state.phase == LudoPhase.Roll) return LudoAction.Roll
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        val color = player.color
        var best = Double.NEGATIVE_INFINITY
        val pool = mutableListOf<LudoMove>()
        for (move in moves) {
            val captured = if (move.to <= 51 && absolute(color, move.to) !in SAFE) {
                state.tokens.count { token ->
                    token.color != color && token.progress in 0..51 &&
                        absolute(token.color, token.progress) == absolute(color, move.to)
                }
            } else {
                0
            }
            val value = captured * 120.0 + (if (move.to >= FINISH) 100 else 0) + (if (move.from == -1) 35 else 0) +
                move.to * 0.25 + random.nextDouble() * 3
            if (value > best + 1) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (kotlin.math.abs(value - best) <= 1) {
                pool.add(move)
            }
        }
        val chosen = pool.randomOrNull(random) ?: moves.first()
        return LudoAction.Move(chosen.tokenId)
    }

    fun scoreFor(state: LudoState, seatIndex: Int): Int = finishedCount(state, seatIndex)

    fun winnerName(state: LudoState): String =
        state.players.getOrNull(state.winnerIndex ?: -1)?.name ?: "—"

    private fun nextSeat(state: LudoState): Int = (state.turn + 1) % state.players.size.coerceAtLeast(1)
}

sealed class LudoAction {
    data object Roll : LudoAction()
    data class Move(val tokenId: String) : LudoAction()
}
