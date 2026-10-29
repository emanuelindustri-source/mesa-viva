package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Brisca con baraja española de 40 cartas.
 *
 * Dos jugadores o cuatro por parejas. Se reparten tres cartas, se muestra el
 * triunfo (la muestra) y el ganador de cada baza roba primero. El As vale 11
 * puntos, el Tres 10, el Rey 4, el Caballo 3 y la Sota 2. Gana la partida
 * quien alcanza la meta acumulada.
 */
enum class BriscaPhase { Playing, HandEnd, MatchEnd }

data class BriscaSeat(
    val name: String,
    val isBot: Boolean,
    val team: Int? = null,
)

data class BriscaPlay(
    val seat: Int,
    val card: SpanishCard,
)

data class BriscaTrick(
    val plays: List<BriscaPlay>,
    val winner: Int,
    val points: Int,
)

data class BriscaState(
    val seats: List<BriscaSeat>,
    val scores: List<Int>,
    val hands: List<List<SpanishCard>>,
    val deck: List<SpanishCard>,
    val trumpCard: SpanishCard?,
    val trumpSuit: SpanishSuit,
    val leader: Int,
    val turn: Int,
    val dealer: Int,
    val handNo: Int,
    val target: Int,
    val teams: Boolean,
    val phase: BriscaPhase = BriscaPhase.Playing,
    val trick: List<BriscaPlay> = emptyList(),
    val lastTrick: BriscaTrick? = null,
    val capturedPoints: List<Int> = List(seats.size) { 0 },
    val lastPlay: BriscaPlay? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class BriscaResult(val state: BriscaState, val error: String? = null)

sealed class BriscaAction {
    data class PlayCard(val cardId: String) : BriscaAction()
    data object NextHand : BriscaAction()
}

class BriscaGame(
    playerCount: Int = 2,
    private val target: Int = 100,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = if (playerCount >= 4) 4 else 2
    private val useTeams = count == 4

    companion object {
        val RANKS = listOf(1, 2, 3, 4, 5, 6, 7, 10, 11, 12)

        /** De menor a mayor jerarquía dentro de la baraja española. */
        val POWER = mapOf(2 to 1, 4 to 2, 5 to 3, 6 to 4, 7 to 5, 10 to 6, 11 to 7, 12 to 8, 3 to 9, 1 to 10)

        val POINTS = mapOf(1 to 11, 3 to 10, 12 to 4, 11 to 3, 10 to 2)

        fun power(card: SpanishCard): Int = POWER[card.rank] ?: 0

        fun points(card: SpanishCard): Int = POINTS[card.rank] ?: 0

        fun deck(): List<SpanishCard> = buildList {
            for (suit in SpanishSuit.entries) {
                for (rank in RANKS) add(SpanishCard("${suit.name.lowercase()}-$rank", suit, rank))
            }
        }

        fun label(card: SpanishCard): String = "${SpanishCardLabel.rank(card.rank)}${card.suit.symbol}"
    }

    private val botNames = listOf("El Silbón", "La Sayona", "El Hachero")

    fun newMatch(): BriscaState {
        val seats = (0 until count).map { index ->
            BriscaSeat(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> botNames.getOrElse(index - 1) { "Bot ${index + 1}" }
                },
                isBot = !allHumans && index > 0,
                team = if (useTeams) index % 2 else null,
            )
        }
        val deck = shuffle(deck())
        val hands = MutableList(count) { mutableListOf<SpanishCard>() }
        repeat(3) {
            for (seat in 0 until count) {
                hands[seat].add(deck.removeAt(0))
            }
        }
        val trump = deck.removeAt(0)
        val dealer = 0
        val leader = (dealer + 1) % count
        return BriscaState(
            seats = seats,
            scores = List(count) { 0 },
            hands = hands.map { sortHand(it) },
            deck = deck,
            trumpCard = trump,
            trumpSuit = trump.suit,
            leader = leader,
            turn = leader,
            dealer = dealer,
            handNo = 1,
            target = target,
            teams = useTeams,
            capturedPoints = List(count) { 0 },
            history = listOf("Mano 1 · reparte ${seats[dealer].name} · triunfo ${label(trump)}"),
        )
    }

    private fun shuffle(cards: List<SpanishCard>): MutableList<SpanishCard> {
        val copy = cards.toMutableList()
        for (i in copy.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val a = copy[i]
            copy[i] = copy[j]
            copy[j] = a
        }
        return copy
    }

    fun handOf(state: BriscaState, seat: Int): List<SpanishCard> =
        state.hands.getOrElse(seat) { emptyList() }

    fun scoreOf(state: BriscaState, seat: Int): Int = state.scores.getOrElse(seat) { 0 }

    fun teamTotal(state: BriscaState, team: Int): Int =
        state.seats.indices.filter { state.seats[it].team == team }.sumOf { scoreOf(state, it) }

    fun maxScore(state: BriscaState): Int = if (state.teams) {
        state.seats.mapNotNull { it.team }.distinct().maxOfOrNull { teamTotal(state, it) } ?: 0
    } else {
        state.scores.maxOrNull() ?: 0
    }

    /** Devuelve la carta ganadora de una baza. */
    fun winningPlay(trick: List<BriscaPlay>, trumpSuit: SpanishSuit): BriscaPlay? {
        if (trick.isEmpty()) return null
        val trumps = trick.filter { it.card.suit == trumpSuit }
        if (trumps.isNotEmpty()) return trumps.maxByOrNull { power(it.card) }
        val leadSuit = trick.first().card.suit
        val lead = trick.filter { it.card.suit == leadSuit }
        return lead.maxByOrNull { power(it.card) }
    }

    fun playCard(state: BriscaState, cardId: String): BriscaResult {
        if (state.phase != BriscaPhase.Playing) return BriscaResult(state, "brisca.error.notNow")
        val seat = state.turn
        val hand = handOf(state, seat).toMutableList()
        val card = hand.firstOrNull { it.id == cardId }
            ?: return BriscaResult(state, "brisca.error.invalidCard")
        hand.remove(card)
        val hands = state.hands.toMutableList()
        hands[seat] = hand
        val trick = state.trick + BriscaPlay(seat, card)
        val play = BriscaPlay(seat, card)
        val entry = "${state.seats[seat].name}: ${label(card)}"
        var next = state.copy(
            hands = hands,
            trick = trick,
            lastPlay = play,
            turn = (seat + 1) % count,
            history = (state.history + entry).takeLast(180),
        )
        if (trick.size < count) return BriscaResult(next)

        val winnerPlay = winningPlay(trick, state.trumpSuit) ?: return BriscaResult(next)
        val winner = winnerPlay.seat
        val trickPoints = trick.sumOf { points(it.card) }
        val captured = state.capturedPoints.toMutableList()
        captured[winner] += trickPoints
        val resolved = BriscaTrick(trick, winner, trickPoints)
        next = next.copy(
            trick = emptyList(),
            lastTrick = resolved,
            capturedPoints = captured,
            leader = winner,
            turn = winner,
            history = (next.history + "Baza para ${state.seats[winner].name} · +$trickPoints").takeLast(180),
        )
        next = drawAfterTrick(next, winner)
        if (next.hands.all { it.isEmpty() } && next.deck.isEmpty() && next.trumpCard == null) {
            next = closeHand(next)
        }
        return BriscaResult(next)
    }

    private fun drawAfterTrick(state: BriscaState, winner: Int): BriscaState {
        var deck = state.deck.toMutableList()
        var trump = state.trumpCard
        val hands = state.hands.map { it.toMutableList() }.toMutableList()
        val missing = hands.map { (3 - it.size).coerceAtLeast(0) }
        // El ganador roba primero y la muestra se reparte al final.
        for (offset in 0 until count) {
            val seat = (winner + offset) % count
            repeat(missing[seat]) {
                val card = when {
                    deck.isNotEmpty() -> deck.removeAt(0)
                    trump != null -> trump.also { trump = null }
                    else -> null
                } ?: return@repeat
                hands[seat].add(card)
            }
        }
        val recorded = hands.mapIndexed { index, hand ->
            val before = state.hands[index].size
            val after = hand.size
            if (after > before) "${state.seats[index].name} roba $before→$after" else ""
        }.filter { it.isNotBlank() }
        return state.copy(
            deck = deck,
            trumpCard = trump,
            hands = hands.map { sortHand(it) },
            history = if (recorded.isEmpty()) state.history else (state.history + recorded).takeLast(180),
        )
    }

    private fun closeHand(state: BriscaState): BriscaState {
        val scores = state.scores.toMutableList()
        for (seat in 0 until count) {
            scores[seat] += state.capturedPoints[seat]
        }
        val next = state.copy(
            scores = scores,
            phase = BriscaPhase.HandEnd,
            history = (state.history + "Fin de mano · puntos contados").takeLast(180),
        )
        return checkTarget(next)
    }

    fun nextHand(state: BriscaState): BriscaResult {
        if (state.phase != BriscaPhase.HandEnd) return BriscaResult(state, "brisca.error.notNow")
        val dealer = (state.dealer + 1) % count
        val deck = shuffle(deck())
        val hands = MutableList(count) { mutableListOf<SpanishCard>() }
        repeat(3) {
            for (seat in 0 until count) {
                hands[seat].add(deck.removeAt(0))
            }
        }
        val trump = deck.removeAt(0)
        val leader = (dealer + 1) % count
        return BriscaResult(
            state.copy(
                hands = hands.map { sortHand(it) },
                deck = deck,
                trumpCard = trump,
                trumpSuit = trump.suit,
                dealer = dealer,
                leader = leader,
                turn = leader,
                handNo = state.handNo + 1,
                phase = BriscaPhase.Playing,
                trick = emptyList(),
                lastTrick = null,
                capturedPoints = List(count) { 0 },
                lastPlay = null,
                history = (state.history + "Mano ${state.handNo + 1} · reparte ${state.seats[dealer].name} · triunfo ${label(trump)}").takeLast(180),
            ),
        )
    }

    fun resign(state: BriscaState): BriscaResult {
        if (state.phase == BriscaPhase.MatchEnd) return BriscaResult(state, "brisca.error.matchOver")
        val winner = (state.turn + 1) % count
        return BriscaResult(
            state.copy(
                phase = BriscaPhase.MatchEnd,
                winner = winner,
                reason = "brisca.reason.resign",
            ),
        )
    }

    private fun checkTarget(state: BriscaState): BriscaState {
        if (state.phase == BriscaPhase.MatchEnd) return state
        if (maxScore(state) < state.target) return state
        val winners = if (state.teams) {
            val teams = state.seats.mapNotNull { it.team }.distinct()
            val best = teams.maxOf { teamTotal(state, it) }
            state.seats.indices.filter { state.seats[it].team != null && teamTotal(state, state.seats[it].team!!) == best }
        } else {
            val best = state.scores.maxOrNull() ?: 0
            state.scores.indices.filter { state.scores[it] == best }
        }
        return state.copy(
            phase = BriscaPhase.MatchEnd,
            winner = winners.firstOrNull(),
            draw = winners.size > 1,
            reason = "brisca.reason.points",
        )
    }

    fun botAction(state: BriscaState): BriscaAction? {
        if (state.phase == BriscaPhase.HandEnd) return BriscaAction.NextHand
        if (state.phase != BriscaPhase.Playing || !state.seats[state.turn].isBot) return null
        val card = chooseBotCard(state, state.turn) ?: return null
        return BriscaAction.PlayCard(card.id)
    }

    private fun chooseBotCard(state: BriscaState, seat: Int): SpanishCard? {
        val hand = handOf(state, seat)
        if (hand.isEmpty()) return null
        if (state.trick.isEmpty()) {
            // Al salir, el bot juega su carta más fuerte que no sea triunfo, o el triunfo más bajo.
            val nonTrump = hand.filter { it.suit != state.trumpSuit }
            return (nonTrump.maxByOrNull { power(it) } ?: hand.minByOrNull { power(it) })
        }
        val currentWinner = winningPlay(state.trick, state.trumpSuit)
        val winning = hand.filter { candidate ->
            val test = state.trick + BriscaPlay(seat, candidate)
            winningPlay(test, state.trumpSuit)?.seat == seat
        }
        if (winning.isNotEmpty()) return winning.minByOrNull { power(it) }
        return hand.minByOrNull { points(it) * 10 + power(it) }
    }

    fun sortHand(hand: List<SpanishCard>): List<SpanishCard> =
        hand.sortedWith(compareBy({ it.suit.ordinal }, { POWER[it.rank] ?: 0 }))

    fun winnerName(state: BriscaState): String =
        state.winner?.let { state.seats.getOrNull(it)?.name } ?: ""
}

/** Etiquetas legibles compartidas por las mesas de baraja española. */
object SpanishCardLabel {
    fun rank(rank: Int): String = when (rank) {
        1 -> "A"
        10 -> "S"
        11 -> "C"
        12 -> "R"
        else -> rank.toString()
    }
}
