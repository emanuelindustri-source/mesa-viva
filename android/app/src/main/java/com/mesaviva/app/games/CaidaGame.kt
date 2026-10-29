package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Caída venezolana para 2–4 jugadores.
 *
 * Entrega de cuatro cartas, cantos, caídas por rango, mesa limpia, parejas,
 * conteo de cartas capturadas y meta configurable. El bot entrega, canta y
 * elige la carta que más le conviene sin ver las manos rivales.
 */
enum class SpanishSuit(val symbol: String) {
    Oros("◉"),
    Copas("♡"),
    Espadas("⚔"),
    Bastos("♣"),
}

data class SpanishCard(
    val id: String,
    val suit: SpanishSuit,
    val rank: Int,
)

data class CaidaSeat(
    val name: String,
    val isBot: Boolean,
    val team: Int? = null,
)

data class CaidaDelivery(
    val cards: List<SpanishCard>,
    val ok: Boolean,
    val order: List<Int>,
    val points: Int,
    val toPlayer: Int,
)

data class CaidaCapture(
    val player: Int,
    val cards: List<SpanishCard>,
    val points: Int,
    val mesa: Boolean,
    val rank: Int,
)

data class CaidaCanto(
    val player: Int,
    val key: String,
    val points: Int,
)

data class CaidaHandRow(
    val player: Int,
    val name: String,
    val cards: Int,
    val points: Int,
    val score: Int,
)

enum class CaidaPhase { Entrega, Play, HandEnd, MatchEnd }

data class CaidaState(
    val seats: List<CaidaSeat>,
    val scores: List<Int>,
    val hands: List<List<SpanishCard>>,
    val deck: List<SpanishCard>,
    val table: List<SpanishCard>,
    val captured: List<List<SpanishCard>>,
    val claimedHands: List<Set<Int>>,
    val dealer: Int,
    val turn: Int,
    val handNo: Int,
    val target: Int,
    val teams: Boolean,
    val phase: CaidaPhase = CaidaPhase.Entrega,
    val delivery: CaidaDelivery? = null,
    val lastCapture: CaidaCapture? = null,
    val lastCanto: CaidaCanto? = null,
    val lastPlayed: Pair<Int, SpanishCard>? = null,
    val lastCapturer: Int? = null,
    val handStartScores: List<Int> = scores,
    val playedThisHand: List<Boolean>,
    val handSummary: List<CaidaHandRow>? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class CaidaResult(val state: CaidaState, val error: String? = null)

class CaidaGame(
    playerCount: Int = 4,
    private val target: Int = 100,
    private val teams: Boolean = false,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = playerCount.coerceIn(2, 4)
    private val useTeams = teams && count == 4

    companion object {
        val RANKS = listOf(1, 2, 3, 4, 5, 6, 7, 10, 11, 12)
        val SUITS = SpanishSuit.entries

        val CANTO_NAMES = mapOf(
            "ronda" to "canto.ronda",
            "trivilin" to "canto.trivilin",
            "patrulla" to "canto.patrulla",
            "vigia" to "canto.vigia",
            "registro" to "canto.registro",
            "registrico" to "canto.registrico",
            "casaChica" to "canto.casaChica",
            "casaGrande" to "canto.casaGrande",
        )

        fun spanishDeck(): List<SpanishCard> = buildList {
            for (suit in SUITS) {
                for (rank in RANKS) add(SpanishCard("${suit.name.lowercase()}-$rank", suit, rank))
            }
        }

        fun rankPoints(rank: Int): Int = if (rank in 10..12) rank - 8 else 1

        fun cardLabel(card: SpanishCard): String = "${rankLabel(card.rank)}${card.suit.symbol}"

        fun rankLabel(rank: Int): String = when (rank) {
            10 -> "S"
            11 -> "C"
            12 -> "R"
            else -> rank.toString()
        }

        fun suitOrder(suit: SpanishSuit): Int = when (suit) {
            SpanishSuit.Oros -> 0
            SpanishSuit.Copas -> 1
            SpanishSuit.Espadas -> 2
            SpanishSuit.Bastos -> 3
        }

        fun cantoLabel(key: String): String = CANTO_NAMES[key] ?: "canto.ronda"
    }

    private val botNames = listOf("El Silbón", "La Sayona", "El Hachero")

    fun newMatch(): CaidaState {
        val seats = (0 until count).map { index ->
            CaidaSeat(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> botNames.getOrElse(index - 1) { "Bot ${index + 1}" }
                },
                isBot = !allHumans && index > 0,
                team = if (useTeams) index % 2 else null,
            )
        }
        return CaidaState(
            seats = seats,
            scores = List(count) { 0 },
            hands = List(count) { emptyList() },
            deck = shuffledDeck(),
            table = emptyList(),
            captured = List(count) { emptyList() },
            claimedHands = List(count) { emptySet() },
            dealer = 0,
            turn = 0,
            handNo = 1,
            target = target,
            teams = useTeams,
            handStartScores = List(count) { 0 },
            playedThisHand = List(count) { false },
        )
    }

    private fun shuffledDeck(): List<SpanishCard> {
        val cards = spanishDeck().toMutableList()
        for (i in cards.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val a = cards[i]
            cards[i] = cards[j]
            cards[j] = a
        }
        return cards
    }

    fun handOf(state: CaidaState, seat: Int): List<SpanishCard> =
        state.hands.getOrElse(seat) { emptyList() }

    fun scoreOf(state: CaidaState, seat: Int): Int = state.scores.getOrElse(seat) { 0 }

    fun teamTotal(state: CaidaState, team: Int): Int =
        state.seats.indices.filter { state.seats[it].team == team }.sumOf { scoreOf(state, it) }

    fun maxScore(state: CaidaState): Int = if (state.teams) {
        state.seats.mapNotNull { it.team }.distinct().maxOfOrNull { teamTotal(state, it) } ?: 0
    } else {
        state.scores.maxOrNull() ?: 0
    }

    fun detectCanto(hand: List<SpanishCard>): Canto? {
        if (hand.size < 3) return null
        val ranks = hand.map { it.rank }.sorted()
        val counts = ranks.groupingBy { it }.eachCount()
        val triple = counts.entries.firstOrNull { it.value == 3 }
        val pair = counts.entries.firstOrNull { it.value == 2 }
        fun has(rank: Int) = counts.containsKey(rank)

        if (triple != null) {
            return Canto("trivilin", 3, triple.key)
        }
        if (has(1) && (counts[11] ?: 0) == 2) return Canto("casaChica", 5)
        if (has(1) && (counts[12] ?: 0) == 2) return Canto("casaGrande", 6)
        if (has(1) && has(10) && has(11)) return Canto("registrico", 5)
        if (has(1) && has(11) && has(12)) return Canto("registro", 4)

        if (pair != null) {
            val pairRank = pair.key
            val third = ranks.firstOrNull { it != pairRank } ?: 0
            val adjacent = third == pairRank - 1 || third == pairRank + 1
            if (adjacent) return Canto("vigia", 4, pairRank)
            return Canto("ronda", minOf(2, rankPoints(pairRank)), pairRank)
        }

        val unique = counts.keys.sorted()
        if (unique.size == 3) {
            val a = unique[0]
            val b = unique[1]
            val c = unique[2]
            val consecutive = (b == a + 1 && c == b + 1) ||
                (a == 1 && b == 2 && c == 3) ||
                (a == 5 && b == 6 && c == 7) ||
                (a == 10 && b == 11 && c == 12)
            if (consecutive) return Canto("patrulla", 3)
        }
        return null
    }

    fun canClaim(state: CaidaState, seat: Int): Boolean {
        if (state.phase != CaidaPhase.Play || state.turn != seat) return false
        if (state.playedThisHand.getOrElse(seat) { false }) return false
        if (state.handNo in state.claimedHands.getOrElse(seat) { emptySet() }) return false
        return detectCanto(handOf(state, seat)) != null
    }

    fun deliver(state: CaidaState): CaidaResult {
        if (state.phase != CaidaPhase.Entrega) return CaidaResult(state, "caida.error.notNow")
        val deck = state.deck.toMutableList()
        val cards = deck.take(4)
        if (cards.size < 4) return CaidaResult(state, "caida.error.deck")
        repeat(4) { deck.removeAt(0) }
        val ranks = cards.map { it.rank }
        val asc = ranks == listOf(1, 2, 3, 4)
        val desc = ranks == listOf(4, 3, 2, 1)
        val ok = asc || desc
        val points = if (ok) 2 else 1
        val recipient = if (ok) state.dealer else (state.dealer + 1) % count
        val scores = state.scores.toMutableList()
        scores[recipient] += points
        val hands = MutableList(count) { mutableListOf<SpanishCard>() }
        var cursor = (state.dealer + 1) % count
        for (round in 0 until 3) {
            for (i in 0 until count) {
                val seat = (cursor + i) % count
                val card = deck.removeFirstOrNull() ?: break
                hands[seat].add(card)
            }
        }
        val delivery = CaidaDelivery(cards, ok, ranks, points, recipient)
        val entry = if (ok) {
            "Entrega ${ranks.joinToString("·")} ✓ · ${state.seats[recipient].name} +$points"
        } else {
            "Entrega ${ranks.joinToString("·")} ✗ · ${state.seats[recipient].name} +$points"
        }
        var next = state.copy(
            deck = deck,
            scores = scores,
            hands = hands.map { it.toList() },
            delivery = delivery,
            phase = CaidaPhase.Play,
            turn = cursor,
            lastCapture = null,
            lastCanto = null,
            lastPlayed = null,
            lastCapturer = null,
            history = (state.history + entry).takeLast(160),
        )
        next = checkTarget(next)
        return CaidaResult(next)
    }

    fun playCard(state: CaidaState, cardId: String): CaidaResult {
        if (state.phase != CaidaPhase.Play) return CaidaResult(state, "caida.error.notNow")
        val seat = state.turn
        val hand = handOf(state, seat).toMutableList()
        val card = hand.firstOrNull { it.id == cardId } ?: return CaidaResult(state, "caida.error.invalidCard")
        hand.remove(card)
        val tableBefore = state.table
        var matchIndex = -1
        for (i in tableBefore.indices.reversed()) {
            if (tableBefore[i].rank == card.rank) {
                matchIndex = i
                break
            }
        }
        val tableAfter = tableBefore + card
        var table = tableAfter
        val captured = state.captured.map { it.toMutableList() }.toMutableList()
        var points = 0
        var mesa = false
        var captureCards: List<SpanishCard> = emptyList()
        if (matchIndex >= 0) {
            captureCards = tableAfter.subList(matchIndex, tableAfter.size).toList()
            table = tableAfter.subList(0, matchIndex).toList()
            points = rankPoints(card.rank)
            mesa = matchIndex == 0
            captured[seat].addAll(captureCards)
            if (mesa) points += 5
        }
        val scores = state.scores.toMutableList()
        if (points > 0) scores[seat] += points
        val deck = state.deck.toMutableList()
        if (deck.isNotEmpty() && hand.size < 3) {
            hand.add(deck.removeAt(0))
        }
        val sortedHand = sortHand(hand)
        val hands = state.hands.toMutableList()
        hands[seat] = sortedHand
        val entry = buildString {
            append(state.seats[seat].name)
            append(": ")
            append(cardLabel(card))
            if (points > 0) {
                append(" · ")
                append(if (mesa) "¡mesa! " else "caída ")
                append("+")
                append(points)
            }
        }
        var next = state.copy(
            hands = hands,
            deck = deck,
            table = table,
            captured = captured.map { it.toList() },
            scores = scores,
            turn = (state.turn + 1) % count,
            playedThisHand = state.playedThisHand.mapIndexed { index, value -> if (index == seat) true else value },
            lastPlayed = seat to card,
            lastCapturer = if (captureCards.isNotEmpty()) seat else state.lastCapturer,
            lastCapture = if (captureCards.isNotEmpty()) {
                CaidaCapture(seat, captureCards, points, mesa, card.rank)
            } else {
                null
            },
            history = (state.history + entry).takeLast(160),
        )
        if (next.hands.all { it.isEmpty() }) next = endHand(next)
        return CaidaResult(checkTarget(next))
    }

    fun claimCanto(state: CaidaState): CaidaResult {
        if (state.phase != CaidaPhase.Play) return CaidaResult(state, "caida.error.notNow")
        val seat = state.turn
        if (!canClaim(state, seat)) return CaidaResult(state, "caida.error.noCanto")
        val canto = detectCanto(handOf(state, seat)) ?: return CaidaResult(state, "caida.error.noCanto")
        val scores = state.scores.toMutableList()
        scores[seat] += canto.points
        val claimed = state.claimedHands.toMutableList()
        claimed[seat] = claimed[seat] + state.handNo
        val entry = "${state.seats[seat].name}: ${cantoName(canto.key)} +${canto.points}"
        val next = state.copy(
            scores = scores,
            claimedHands = claimed,
            lastCanto = CaidaCanto(seat, canto.key, canto.points),
            history = (state.history + entry).takeLast(160),
        )
        return CaidaResult(checkTarget(next))
    }

    fun nextHand(state: CaidaState): CaidaResult {
        if (state.phase != CaidaPhase.HandEnd) return CaidaResult(state, "caida.error.notNow")
        val dealer = (state.dealer + 1) % count
        val next = state.copy(
            deck = shuffledDeck(),
            table = emptyList(),
            hands = List(count) { emptyList() },
            captured = List(count) { emptyList() },
            claimedHands = List(count) { emptySet() },
            dealer = dealer,
            turn = (dealer + 1) % count,
            handNo = state.handNo + 1,
            phase = CaidaPhase.Entrega,
            delivery = null,
            lastCapture = null,
            lastCanto = null,
            lastPlayed = null,
            lastCapturer = null,
            handStartScores = state.scores,
            playedThisHand = List(count) { false },
            handSummary = null,
            history = (state.history + "Mano ${state.handNo + 1} · reparte ${state.seats[dealer].name}").takeLast(160),
        )
        return CaidaResult(next)
    }

    fun resign(state: CaidaState): CaidaResult {
        if (state.phase == CaidaPhase.MatchEnd) return CaidaResult(state, "caida.error.matchOver")
        val winner = (state.turn + 1) % count
        return CaidaResult(
            state.copy(
                phase = CaidaPhase.MatchEnd,
                winner = winner,
                reason = "caida.reason.resign",
            ),
        )
    }

    private fun endHand(state: CaidaState): CaidaState {
        val captured = state.captured.map { it.toMutableList() }.toMutableList()
        var table = state.table
        if (table.isNotEmpty() && state.lastCapturer != null) {
            captured[state.lastCapturer].addAll(table)
            table = emptyList()
        }
        val scores = state.scores.toMutableList()
        for (seat in 0 until count) {
            val cards = captured[seat].size
            val points = cards / 10
            if (points > 0) scores[seat] += points
        }
        val summary = state.seats.indices.map { seat ->
            CaidaHandRow(
                player = seat,
                name = state.seats[seat].name,
                cards = captured[seat].size,
                points = scores[seat] - state.handStartScores[seat],
                score = scores[seat],
            )
        }
        val next = state.copy(
            captured = captured.map { it.toList() },
            table = table,
            scores = scores,
            phase = CaidaPhase.HandEnd,
            handSummary = summary,
            lastCapture = null,
            lastPlayed = null,
            delivery = null,
            lastCanto = null,
            lastCapturer = null,
            history = (state.history + "Fin de mano · cartas contadas").takeLast(160),
        )
        val checked = checkTarget(next)
        if (checked.phase == CaidaPhase.MatchEnd) {
            return checked
        }
        return next
    }

    private fun checkTarget(state: CaidaState): CaidaState {
        if (state.phase == CaidaPhase.MatchEnd) return state
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
            phase = CaidaPhase.MatchEnd,
            winner = winners.firstOrNull(),
            draw = winners.size > 1,
            reason = "caida.reason.points",
        )
    }

    fun botAction(state: CaidaState): CaidaAction? {
        val seat = state.seats.getOrNull(state.turn) ?: return null
        if (state.phase == CaidaPhase.Entrega) {
            val dealer = state.seats.getOrNull(state.dealer) ?: return null
            return if (dealer.isBot) CaidaAction.Deliver else null
        }
        if (state.phase == CaidaPhase.HandEnd) return CaidaAction.NextHand
        if (state.phase != CaidaPhase.Play || !seat.isBot) return null
        if (canClaim(state, state.turn)) return CaidaAction.Canto
        val card = chooseBotCard(state, state.turn) ?: return null
        return CaidaAction.PlayCard(card.id)
    }

    private fun chooseBotCard(state: CaidaState, seat: Int): SpanishCard? {
        val hand = handOf(state, seat)
        if (hand.isEmpty()) return null
        val table = state.table
        val scored = hand.map { card ->
            var matchIndex = -1
            for (i in table.indices.reversed()) {
                if (table[i].rank == card.rank) {
                    matchIndex = i
                    break
                }
            }
            val capture = matchIndex >= 0
            val points = if (capture) rankPoints(card.rank) + if (matchIndex == 0) 5 else 0 else 0
            val risk = if (capture) 0 else rankPoints(card.rank) * 2
            card to (points * 10 - risk + if (card.rank <= 7) 2 else 0)
        }
        return scored.maxByOrNull { it.second }?.first ?: hand.first()
    }

    fun sortHand(hand: List<SpanishCard>): List<SpanishCard> =
        hand.sortedWith(compareBy({ suitOrder(it.suit) }, { RANKS.indexOf(it.rank) }))

    fun cantoName(key: String): String = when (key) {
        "ronda" -> "Ronda"
        "trivilin" -> "Trivilín"
        "patrulla" -> "Patrulla"
        "vigia" -> "Vigía"
        "registro" -> "Registro"
        "registrico" -> "Registrico"
        "casaChica" -> "Casa chica"
        "casaGrande" -> "Casa grande"
        else -> key
    }

    fun winnerName(state: CaidaState): String =
        state.winner?.let { state.seats.getOrNull(it)?.name } ?: ""
}

data class Canto(val key: String, val points: Int, val rank: Int? = null)

sealed class CaidaAction {
    data object Deliver : CaidaAction()
    data object Canto : CaidaAction()
    data object NextHand : CaidaAction()
    data class PlayCard(val cardId: String) : CaidaAction()
}
