package com.mesaviva.app.games

import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Dados Mentirosos para 2–6 jugadores.
 *
 * Cada jugador empieza con cinco dados. En tu turno subes la puja (cantidad
 * y cara) o dudas de la anterior. Quien pierde la duda pierde un dado. Gana
 * el último jugador con dados. El bot calcula expectativas y arriesga según
 * la dificultad.
 */
enum class DadosPhase { Bid, Reveal, MatchEnd }

data class DadosPlayer(val name: String, val isBot: Boolean, val avatar: String = "🎲")

data class DadosBid(val qty: Int, val face: Int, val bidder: Int)

data class DadosReveal(
    val dice: List<List<Int>>,
    val bid: DadosBid,
    val challenger: Int,
    val actual: Int,
    val bidTrue: Boolean,
    val loser: Int,
)

data class DadosLog(
    val kind: String,
    val player: Int,
    val qty: Int = 0,
    val face: Int = 0,
    val actual: Int = 0,
    val bidTrue: Boolean = false,
)

data class DadosState(
    val players: List<DadosPlayer>,
    val diceCount: List<Int>,
    val dice: List<List<Int>>,
    val turn: Int,
    val bid: DadosBid? = null,
    val phase: DadosPhase = DadosPhase.Bid,
    val round: Int = 1,
    val starter: Int = 0,
    val reveal: DadosReveal? = null,
    val winner: Int? = null,
    val reason: String? = null,
    val history: List<DadosLog> = emptyList(),
)

data class DadosResult(val state: DadosState, val error: String? = null)

sealed class DadosAction {
    data class Bid(val qty: Int, val face: Int) : DadosAction()
    data object Doubt : DadosAction()
    data object Continue : DadosAction()
}

class DadosGame(
    playerCount: Int = 4,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = playerCount.coerceIn(2, 6)

    private val names = listOf(
        "Tú", "El Silbón", "La Sayona", "El Hachero", "La Llorona", "El Duende",
    )

    fun newMatch(): DadosState {
        val players = (0 until count).map { index ->
            DadosPlayer(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> names.getOrElse(index) { "Bot ${index + 1}" }
                },
                isBot = !allHumans && index > 0,
                avatar = listOf("🦜", "🐆", "🦇", "🌙", "🔥", "👻").getOrElse(index) { "🎲" },
            )
        }
        val counts = List(count) { 5 }
        val dice = players.indices.map { roll(5) }
        return DadosState(
            players = players,
            diceCount = counts,
            dice = dice,
            turn = 0,
        )
    }

    private fun roll(n: Int): List<Int> = List(n) { random.nextInt(1, 7) }

    fun totalDice(state: DadosState): Int =
        state.players.indices.sumOf { if (state.diceCount[it] > 0) state.diceCount[it] else 0 }

    fun alive(state: DadosState): List<Int> =
        state.players.indices.filter { state.diceCount[it] > 0 }

    private fun nextAlive(state: DadosState, from: Int): Int {
        for (step in 1..state.players.size) {
            val index = (from + step) % state.players.size
            if (state.diceCount[index] > 0) return index
        }
        return from
    }

    private fun rollAll(state: DadosState): DadosState = state.copy(
        dice = state.players.indices.map { seat ->
            if (state.diceCount[seat] > 0) roll(state.diceCount[seat]) else emptyList()
        },
    )

    private fun startRound(state: DadosState, starter: Int): DadosState {
        val rolled = rollAll(state)
        return rolled.copy(
            round = state.round + 1,
            starter = starter,
            turn = starter,
            bid = null,
            reveal = null,
            phase = DadosPhase.Bid,
        )
    }

    private fun matchEnd(state: DadosState, winner: Int): DadosState = state.copy(
        phase = DadosPhase.MatchEnd,
        winner = winner,
        reason = "dados.reason.lastDie",
    )

    fun placeBid(state: DadosState, qty: Int, face: Int): DadosResult {
        if (state.phase == DadosPhase.MatchEnd) return DadosResult(state, "dados.error.matchOver")
        if (state.phase != DadosPhase.Bid) return DadosResult(state, "dados.error.notNow")
        val total = totalDice(state)
        if (face !in 1..6) return DadosResult(state, "dados.error.invalidBid")
        if (qty < 1 || qty > total) return DadosResult(state, "dados.error.invalidBid")
        val current = state.bid
        if (current != null) {
            val higher = qty > current.qty || (qty == current.qty && face > current.face)
            if (!higher) return DadosResult(state, "dados.error.bidTooLow")
        }
        val bid = DadosBid(qty, face, state.turn)
        return DadosResult(
            state.copy(
                bid = bid,
                turn = nextAlive(state, state.turn),
                history = (state.history + DadosLog("bid", state.turn, qty, face)).takeLast(80),
            ),
        )
    }

    fun doubt(state: DadosState): DadosResult {
        if (state.phase == DadosPhase.MatchEnd) return DadosResult(state, "dados.error.matchOver")
        if (state.phase != DadosPhase.Bid) return DadosResult(state, "dados.error.notNow")
        val bid = state.bid ?: return DadosResult(state, "dados.error.noBid")
        val allDice = alive(state).flatMap { state.dice.getOrElse(it) { emptyList() } }
        val actual = allDice.count { it == bid.face }
        val bidTrue = actual >= bid.qty
        val loser = if (bidTrue) state.turn else bid.bidder
        val counts = state.diceCount.toMutableList()
        counts[loser] = max(0, counts[loser] - 1)
        val reveal = DadosReveal(
            dice = state.dice,
            bid = bid,
            challenger = state.turn,
            actual = actual,
            bidTrue = bidTrue,
            loser = loser,
        )
        val logs = mutableListOf(
            DadosLog("doubt", state.turn, bid.qty, bid.face),
            DadosLog("reveal", state.turn, bid.qty, bid.face, actual, bidTrue),
            DadosLog("lostDie", loser),
        )
        val remaining = state.players.indices.filter { counts[it] > 0 }
        if (remaining.size <= 1) {
            val winner = remaining.firstOrNull() ?: loser
            logs.add(DadosLog("won", winner))
            return DadosResult(
                matchEnd(
                    state.copy(
                        diceCount = counts,
                        reveal = reveal,
                        phase = DadosPhase.Reveal,
                        history = (state.history + logs).takeLast(80),
                    ),
                    winner,
                ),
            )
        }
        val nextTurn = if (counts[loser] > 0) loser else nextAlive(state.copy(diceCount = counts), loser)
        return DadosResult(
            state.copy(
                diceCount = counts,
                reveal = reveal,
                phase = DadosPhase.Reveal,
                turn = nextTurn,
                history = (state.history + logs).takeLast(80),
            ),
        )
    }

    fun continueRound(state: DadosState): DadosResult {
        if (state.phase == DadosPhase.MatchEnd) return DadosResult(state, "dados.error.matchOver")
        if (state.phase != DadosPhase.Reveal) return DadosResult(state, "dados.error.notNow")
        val remaining = alive(state)
        if (remaining.size <= 1) {
            val winner = remaining.firstOrNull() ?: return DadosResult(state, "dados.error.notNow")
            return DadosResult(matchEnd(state, winner))
        }
        val loser = state.reveal?.loser ?: state.turn
        val starter = if (state.diceCount[loser] > 0) loser else nextAlive(state, loser)
        return DadosResult(startRound(state, starter))
    }

    fun botAction(state: DadosState, difficulty: String = "normal", rng: Random = random): DadosAction? {
        if (state.phase == DadosPhase.MatchEnd) return null
        if (state.players.getOrNull(state.turn)?.isBot != true) return null
        if (state.phase == DadosPhase.Reveal) return DadosAction.Continue

        val mine = state.dice.getOrElse(state.turn) { emptyList() }
        val total = totalDice(state)
        val counts = (1..6).map { face -> face to mine.count { it == face } }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.second }.thenByDescending { it.first })
        val best = counts.firstOrNull() ?: (1 to 1)
        val others = max(0, total - mine.size)
        val expected = best.second + others / 6.0
        val sd = sqrt(others * (1.0 / 6.0) * (5.0 / 6.0))
        val current = state.bid

        if (current != null) {
            val risk = when (difficulty) {
                "hard" -> 1.05
                "easy" -> 1.6
                else -> 1.3
            }
            val threshold = expected + sd * risk
            if (current.qty > threshold || current.qty >= total) return DadosAction.Doubt

            var qty = current.qty
            var face = best.first
            if (face <= current.face) {
                val alt = counts.firstOrNull { it.first > current.face && it.second > 0 }
                if (alt != null) {
                    face = alt.first
                } else {
                    qty = current.qty + 1
                    face = best.first
                }
            }
            if (qty < current.qty) qty = current.qty
            if (qty == current.qty && face <= current.face) qty = current.qty + 1
            if (qty > total) return DadosAction.Doubt
            if (qty < 1) qty = 1
            return DadosAction.Bid(qty, face)
        }

        val aggressive = when (difficulty) {
            "hard" -> 1
            "normal" -> 0
            else -> -1
        }
        var qty = max(1, Math.round(expected + aggressive * 0.3).toInt())
        if (qty > total) qty = total
        return DadosAction.Bid(qty, best.first)
    }

    fun winnerName(state: DadosState): String =
        state.winner?.let { state.players.getOrNull(it)?.name } ?: ""
}
