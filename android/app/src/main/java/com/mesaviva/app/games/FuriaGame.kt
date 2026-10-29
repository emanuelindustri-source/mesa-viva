package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Furia de Colores: homenaje original a los juegos de combinar colores.
 *
 * Cuatro palos (Sol, Mar, Selva, Magma), números 0–9, salto, reversa,
 * roba dos, comodín y comodín roba cuatro. Sin marcas ni arte de terceros.
 */
enum class FuriaSuit { Sol, Mar, Selva, Magma, Wild }

enum class FuriaAction { None, Skip, Reverse, Draw2, Wild, Wild4 }

data class FuriaCard(
    val id: String,
    val suit: FuriaSuit,
    val rank: Int,
    val action: FuriaAction = FuriaAction.None,
)

data class FuriaSeat(val name: String, val isBot: Boolean)

enum class FuriaPhase { Play, MatchEnd }

data class FuriaState(
    val seats: List<FuriaSeat>,
    val scores: List<Int>,
    val hands: List<List<FuriaCard>>,
    val deck: List<FuriaCard>,
    val discard: List<FuriaCard>,
    val color: FuriaSuit,
    val direction: Int = 1,
    val turn: Int = 0,
    val phase: FuriaPhase = FuriaPhase.Play,
    val hasDrawn: Boolean = false,
    val drawnId: String? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val lastPlay: Pair<Int, FuriaCard>? = null,
    val lastShout: Int? = null,
    val lastDraw: Pair<Int, Int>? = null,
    val history: List<String> = emptyList(),
)

data class FuriaResult(val state: FuriaState, val error: String? = null)

class FuriaGame(
    playerCount: Int = 4,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = playerCount.coerceIn(2, 4)

    companion object {
        val COLORS = listOf(FuriaSuit.Sol, FuriaSuit.Mar, FuriaSuit.Selva, FuriaSuit.Magma)
        val ACTION_POINTS = 20
        val WILD_POINTS = 50

        fun deck(): List<FuriaCard> = buildList {
            for (suit in COLORS) {
                add(FuriaCard("${suit.name.lowercase()}-0", suit, 0))
                for (rank in 1..9) {
                    add(FuriaCard("${suit.name.lowercase()}-${rank}a", suit, rank))
                    add(FuriaCard("${suit.name.lowercase()}-${rank}b", suit, rank))
                }
                for (action in listOf(FuriaAction.Skip, FuriaAction.Reverse, FuriaAction.Draw2)) {
                    add(FuriaCard("${suit.name.lowercase()}-${action.name.lowercase()}a", suit, -1, action))
                    add(FuriaCard("${suit.name.lowercase()}-${action.name.lowercase()}b", suit, -1, action))
                }
            }
            for (i in 0 until 4) {
                add(FuriaCard("wild-$i", FuriaSuit.Wild, -1, FuriaAction.Wild))
                add(FuriaCard("wild4-$i", FuriaSuit.Wild, -1, FuriaAction.Wild4))
            }
        }

        fun colorName(suit: FuriaSuit): String = when (suit) {
            FuriaSuit.Sol -> "Sol"
            FuriaSuit.Mar -> "Mar"
            FuriaSuit.Selva -> "Selva"
            FuriaSuit.Magma -> "Magma"
            FuriaSuit.Wild -> "Comodín"
        }

        fun cardLabel(card: FuriaCard): String = when (card.action) {
            FuriaAction.Skip -> "Salto ${colorName(card.suit)}"
            FuriaAction.Reverse -> "Reversa ${colorName(card.suit)}"
            FuriaAction.Draw2 -> "Roba 2 ${colorName(card.suit)}"
            FuriaAction.Wild -> "Comodín"
            FuriaAction.Wild4 -> "Comodín roba 4"
            FuriaAction.None -> "${card.rank} ${colorName(card.suit)}"
        }

        fun cardPoints(card: FuriaCard): Int = when (card.action) {
            FuriaAction.Wild, FuriaAction.Wild4 -> WILD_POINTS
            FuriaAction.Skip, FuriaAction.Reverse, FuriaAction.Draw2 -> ACTION_POINTS
            FuriaAction.None -> card.rank
        }

        fun isWild(card: FuriaCard): Boolean =
            card.action == FuriaAction.Wild || card.action == FuriaAction.Wild4
    }

    private val botNames = listOf("El Silbón", "La Sayona", "El Hachero")

    fun newMatch(): FuriaState {
        val seats = (0 until count).map { index ->
            FuriaSeat(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> botNames.getOrElse(index - 1) { "Bot ${index + 1}" }
                },
                isBot = !allHumans && index > 0,
            )
        }
        var deck = shuffled(deck())
        val hands = MutableList(count) { mutableListOf<FuriaCard>() }
        for (round in 0 until 7) {
            for (seat in 0 until count) {
                deck.firstOrNull()?.let {
                    hands[seat].add(it)
                    deck = deck.drop(1)
                }
            }
        }
        var first = deck.firstOrNull()
        while (first != null && isWild(first)) {
            deck = deck.drop(1) + first
            first = deck.firstOrNull()
        }
        deck = if (first != null) deck.drop(1) else deck
        val discard = first?.let { listOf(it) } ?: emptyList()
        val color = first?.suit?.takeIf { it != FuriaSuit.Wild } ?: mostCommonColor(hands[0])
        return FuriaState(
            seats = seats,
            scores = List(count) { 0 },
            hands = hands.map { sortHand(it) },
            deck = deck,
            discard = discard,
            color = color,
            turn = 0,
        )
    }

    private fun shuffled(cards: List<FuriaCard>): List<FuriaCard> {
        val out = cards.toMutableList()
        for (i in out.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val a = out[i]
            out[i] = out[j]
            out[j] = a
        }
        return out
    }

    fun sortHand(hand: List<FuriaCard>): List<FuriaCard> = hand.sortedWith(
        compareBy({ COLORS.indexOf(it.suit) }, { if (it.action == FuriaAction.None) 0 else 1 }, { it.rank }),
    )

    fun topCard(state: FuriaState): FuriaCard? = state.discard.lastOrNull()

    fun canPlayOn(card: FuriaCard, state: FuriaState): Boolean {
        val top = topCard(state) ?: return true
        if (isWild(card)) return true
        if (card.suit == state.color) return true
        if (top.rank >= 0 && card.rank == top.rank) return true
        if (top.action != FuriaAction.None && card.action == top.action) return true
        return false
    }

    private fun mostCommonColor(hand: List<FuriaCard>): FuriaSuit {
        val counts = COLORS.associateWith { suit -> hand.count { it.suit == suit } }
        return counts.maxByOrNull { it.value }?.key ?: FuriaSuit.Sol
    }

    private fun normalizeDeck(state: FuriaState): FuriaState {
        if (state.deck.isNotEmpty()) return state
        val top = topCard(state) ?: return state
        val rest = state.discard.dropLast(1)
        if (rest.isEmpty()) return state
        return state.copy(deck = shuffled(rest), discard = listOf(top))
    }

    private fun drawTo(state: FuriaState, seat: Int, amount: Int): FuriaState {
        var next = normalizeDeck(state)
        var deck = next.deck.toMutableList()
        val hand = (next.hands.getOrElse(seat) { emptyList() }).toMutableList()
        repeat(amount) {
            if (deck.isEmpty()) {
                val top = topCard(next)
                val rest = next.discard.dropLast(1)
                if (rest.isEmpty()) return@repeat
                deck = shuffled(rest).toMutableList()
                next = next.copy(discard = top?.let { listOf(it) } ?: emptyList())
            }
            deck.firstOrNull()?.let {
                hand.add(it)
                deck.removeAt(0)
            }
        }
        val hands = next.hands.toMutableList()
        hands[seat] = sortHand(hand)
        return next.copy(deck = deck, hands = hands)
    }

    private fun nextTurnIndex(state: FuriaState, steps: Int = 1): Int {
        val n = state.seats.size
        return ((state.turn + state.direction * steps) % n + n) % n
    }

    private fun nextPlayer(state: FuriaState): Int = nextTurnIndex(state, 1)

    fun draw(state: FuriaState): FuriaResult {
        if (state.phase == FuriaPhase.MatchEnd) return FuriaResult(state, "furia.error.matchOver")
        if (state.hasDrawn) return FuriaResult(state, "furia.error.alreadyDrawn")
        val before = state.hands.getOrElse(state.turn) { emptyList() }.map { it.id }.toSet()
        var next = drawTo(state, state.turn, 1)
        val card = next.hands.getOrElse(state.turn) { emptyList() }.firstOrNull { it.id !in before }
        val entry = "${state.seats[state.turn].name}: robó 1"
        next = next.copy(
            lastDraw = state.turn to 1,
            history = (state.history + entry).takeLast(120),
        )
        if (card == null) {
            next = next.copy(turn = nextTurnIndex(next), hasDrawn = false, drawnId = null)
            return FuriaResult(next)
        }
        return if (canPlayOn(card, next)) {
            FuriaResult(next.copy(hasDrawn = true, drawnId = card.id))
        } else {
            FuriaResult(next.copy(turn = nextTurnIndex(next), hasDrawn = false, drawnId = null))
        }
    }

    fun pass(state: FuriaState): FuriaResult {
        if (state.phase == FuriaPhase.MatchEnd) return FuriaResult(state, "furia.error.matchOver")
        if (!state.hasDrawn) return FuriaResult(state, "furia.error.invalidAction")
        val entry = "${state.seats[state.turn].name}: pasó"
        return FuriaResult(
            state.copy(
                turn = nextTurnIndex(state),
                hasDrawn = false,
                drawnId = null,
                history = (state.history + entry).takeLast(120),
            ),
        )
    }

    fun play(state: FuriaState, cardId: String, chosenColor: FuriaSuit? = null): FuriaResult {
        if (state.phase == FuriaPhase.MatchEnd) return FuriaResult(state, "furia.error.matchOver")
        val seat = state.turn
        val hand = state.hands.getOrElse(seat) { emptyList() }
        val card = hand.firstOrNull { it.id == cardId } ?: return FuriaResult(state, "furia.error.invalidCard")
        if (state.hasDrawn && state.drawnId != cardId) return FuriaResult(state, "furia.error.onlyDrawn")
        if (!canPlayOn(card, state)) return FuriaResult(state, "furia.error.cardDoesNotMatch")
        var color = card.suit
        if (isWild(card)) {
            val requested = chosenColor
            if (requested == null || requested == FuriaSuit.Wild) {
                return FuriaResult(state, "furia.error.chooseColor")
            }
            color = requested
        }
        val hands = state.hands.toMutableList()
        hands[seat] = sortHand(hand.filter { it.id != cardId })
        val discard = state.discard + card
        var next = state.copy(
            hands = hands,
            discard = discard,
            color = color,
            hasDrawn = false,
            drawnId = null,
            lastPlay = seat to card,
            lastShout = if (hands[seat].size == 1) seat else null,
            history = (state.history + "${state.seats[seat].name}: ${cardLabel(card)}").takeLast(120),
        )

        if (hands[seat].isEmpty()) {
            val gained = state.seats.indices.filter { it != seat }.sumOf { other ->
                hands[other].sumOf { cardPoints(it) }
            }
            val scores = state.scores.toMutableList()
            scores[seat] += gained
            return FuriaResult(
                next.copy(
                    phase = FuriaPhase.MatchEnd,
                    winner = seat,
                    scores = scores,
                    reason = "furia.reason.lastCard",
                    history = (next.history + "¡${state.seats[seat].name} ganó la partida!").takeLast(120),
                ),
            )
        }

        if (card.action == FuriaAction.Reverse) {
            next = next.copy(direction = -state.direction)
        }

        val opponent = nextPlayer(next)
        when (card.action) {
            FuriaAction.Draw2 -> next = drawTo(next, opponent, 2)
            FuriaAction.Wild4 -> next = drawTo(next, opponent, 4)
            else -> Unit
        }

        var steps = 1
        if (card.action == FuriaAction.Skip || card.action == FuriaAction.Draw2 || card.action == FuriaAction.Wild4) {
            steps = 2
        }
        if (card.action == FuriaAction.Reverse && state.seats.size == 2) steps = 2
        next = normalizeDeck(next.copy(turn = nextTurnIndex(next, steps)))
        return FuriaResult(next)
    }

    fun resign(state: FuriaState): FuriaResult {
        if (state.phase == FuriaPhase.MatchEnd) return FuriaResult(state, "furia.error.matchOver")
        val winner = (state.turn + 1) % count
        return FuriaResult(
            state.copy(
                phase = FuriaPhase.MatchEnd,
                winner = winner,
                reason = "furia.reason.resign",
            ),
        )
    }

    fun botAction(state: FuriaState): FuriaBotAction? {
        if (state.phase == FuriaPhase.MatchEnd) return null
        val seat = state.seats.getOrNull(state.turn) ?: return null
        if (!seat.isBot) return null
        val hand = state.hands.getOrElse(state.turn) { emptyList() }
        if (state.hasDrawn) {
            val drawn = hand.firstOrNull { it.id == state.drawnId }
            return if (drawn != null && canPlayOn(drawn, state)) {
                FuriaBotAction.PlayCard(drawn.id, mostCommonColor(hand))
            } else {
                FuriaBotAction.Pass
            }
        }
        val playable = hand.filter { canPlayOn(it, state) }
        if (playable.isEmpty()) return FuriaBotAction.Draw
        val chosen = playable.sortedWith(
            compareByDescending<FuriaCard> { if (it.action != FuriaAction.None) 100 else it.rank },
        ).first()
        return FuriaBotAction.PlayCard(chosen.id, mostCommonColor(hand))
    }

    fun winnerName(state: FuriaState): String =
        state.winner?.let { state.seats.getOrNull(it)?.name } ?: ""
}

sealed class FuriaBotAction {
    data object Draw : FuriaBotAction()
    data object Pass : FuriaBotAction()
    data class PlayCard(val cardId: String, val color: FuriaSuit? = null) : FuriaBotAction()
}


