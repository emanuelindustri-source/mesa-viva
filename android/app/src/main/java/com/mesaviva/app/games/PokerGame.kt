package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Póker Texas Hold'em de práctica para la mesa nativa.
 *
 * Ciegas 10/20, botón rotativo, cuatro calles, subidas con mínimo, all-in,
 * botes laterales, devolución de apuestas no igualadas, showdown con la
 * mejor mano de 5 entre 7 y eliminación por stack. Sin dinero real.
 */
enum class PokerSuit(val symbol: String) {
    Clubs("♣"),
    Diamonds("♦"),
    Hearts("♥"),
    Spades("♠"),
}

data class PokerCard(
    val id: String,
    val suit: PokerSuit,
    val rank: Int,
)

data class PokerSeat(val name: String, val isBot: Boolean)

data class PokerPlayer(
    val seat: Int,
    val chips: Int,
    val hole: List<PokerCard> = emptyList(),
    val folded: Boolean = false,
    val allIn: Boolean = false,
    val bet: Int = 0,
    val totalBet: Int = 0,
    val acted: Boolean = false,
    val out: Boolean = false,
    val lastMove: String? = null,
)

data class PokerHandEval(
    val category: Int,
    val ranks: List<Int>,
    val nameKey: String,
    val cards: List<PokerCard>,
)

data class PokerShowdownRow(
    val seat: Int,
    val hole: List<PokerCard>,
    val handKey: String,
    val best: List<PokerCard>,
    val folded: Boolean,
    val won: Int,
)

data class PokerPotInfo(val amount: Int, val winners: List<Int>)

data class PokerShowdown(
    val rows: List<PokerShowdownRow>,
    val pots: List<PokerPotInfo>,
    val totalPot: Int,
)

data class PokerHandWinner(val seat: Int, val amount: Int, val folded: Boolean)

enum class PokerStreet { Preflop, Flop, Turn, River, Showdown, HandEnd, MatchEnd }

data class PokerState(
    val seats: List<PokerSeat>,
    val players: List<PokerPlayer>,
    val deck: List<PokerCard>,
    val community: List<PokerCard>,
    val dealer: Int,
    val turn: Int,
    val street: PokerStreet,
    val currentBet: Int,
    val minRaise: Int,
    val smallBlind: Int,
    val bigBlind: Int,
    val handNo: Int,
    val showDown: PokerShowdown? = null,
    val handWinner: PokerHandWinner? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class PokerResult(val state: PokerState, val error: String? = null)

sealed class PokerAction {
    data object Fold : PokerAction()
    data object Check : PokerAction()
    data object Call : PokerAction()
    data class Raise(val amount: Int) : PokerAction()
    data object AllIn : PokerAction()
    data object NextHand : PokerAction()
}

class PokerGame(
    playerCount: Int = 4,
    private val startingChips: Int = 800,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = playerCount.coerceIn(2, 6)

    companion object {
        const val SMALL_BLIND = 10
        const val BIG_BLIND = 20

        val CATEGORY_KEYS = listOf(
            "poker.hand.high",
            "poker.hand.pair",
            "poker.hand.twoPair",
            "poker.hand.trips",
            "poker.hand.straight",
            "poker.hand.flush",
            "poker.hand.fullHouse",
            "poker.hand.quads",
            "poker.hand.straightFlush",
        )

        fun deck(): List<PokerCard> = buildList {
            for (suit in PokerSuit.entries) {
                for (rank in 2..14) add(PokerCard("${suit.name.lowercase()}-$rank", suit, rank))
            }
        }

        fun rankLabel(rank: Int): String = when (rank) {
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            14 -> "A"
            else -> rank.toString()
        }

        fun cardLabel(card: PokerCard): String = "${rankLabel(card.rank)}${card.suit.symbol}"

        fun cardPoints(card: PokerCard): Int = card.rank
    }

    private val botNames = listOf("El Silbón", "La Sayona", "El Hachero", "La Llorona", "El Duende")

    fun newMatch(): PokerState {
        val seats = (0 until count).map { index ->
            PokerSeat(
                name = when {
                    index == 0 -> "Tú"
                    allHumans -> "Jugador ${index + 1}"
                    else -> botNames.getOrElse(index - 1) { "Bot ${index + 1}" }
                },
                isBot = !allHumans && index > 0,
            )
        }
        val players = seats.indices.map { seat ->
            PokerPlayer(seat = seat, chips = max(200, startingChips))
        }
        val base = PokerState(
            seats = seats,
            players = players,
            deck = emptyList(),
            community = emptyList(),
            dealer = 0,
            turn = 0,
            street = PokerStreet.Preflop,
            currentBet = 0,
            minRaise = BIG_BLIND,
            smallBlind = SMALL_BLIND,
            bigBlind = BIG_BLIND,
            handNo = 0,
        )
        return startHand(base)
    }

    fun potTotal(state: PokerState): Int = state.players.sumOf { it.totalBet }

    private fun alive(state: PokerState): List<PokerPlayer> =
        state.players.filter { it.chips > 0 && !it.out }

    private fun livePlayers(state: PokerState): List<PokerPlayer> =
        state.players.filter { !it.folded && !it.out }

    private fun isBettingStreet(street: PokerStreet): Boolean =
        street == PokerStreet.Preflop || street == PokerStreet.Flop ||
            street == PokerStreet.Turn || street == PokerStreet.River

    private fun nextAlive(state: PokerState, from: Int): Int {
        for (step in 1..state.players.size) {
            val index = (from + step) % state.players.size
            val player = state.players[index]
            if (player.chips > 0 && !player.out) return index
        }
        return -1
    }

    private fun nextToAct(state: PokerState, from: Int): Int {
        for (step in 1..state.players.size) {
            val index = (from + step) % state.players.size
            val player = state.players[index]
            if (player.out || player.folded || player.allIn || player.chips <= 0) continue
            if (!player.acted || player.bet < state.currentBet) return index
        }
        return -1
    }

    private fun shuffledDeck(): List<PokerCard> {
        val cards = deck().toMutableList()
        for (i in cards.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val a = cards[i]
            cards[i] = cards[j]
            cards[j] = a
        }
        return cards
    }

    private fun startHand(state: PokerState): PokerState {
        val alivePlayers = alive(state)
        if (alivePlayers.size < 2) return finishMatch(state)
        var players = state.players.map { player ->
            player.copy(
                hole = emptyList(),
                folded = player.chips <= 0,
                allIn = false,
                bet = 0,
                totalBet = 0,
                acted = false,
                out = player.chips <= 0,
                lastMove = null,
            )
        }
        var deck = shuffledDeck()
        var dealer = state.dealer
        if (dealer !in players.indices || players[dealer].chips == 0) {
            val found = nextAlive(state.copy(players = players), dealer)
            dealer = if (found >= 0) found else 0
        }
        var cursor = 0
        for (round in 0 until 2) {
            for (step in 1..players.size) {
                val index = (dealer + step) % players.size
                val player = players[index]
                if (player.chips <= 0) continue
                val card = deck.getOrNull(cursor) ?: break
                cursor++
                players = players.toMutableList().also { list ->
                    list[index] = player.copy(hole = player.hole + card)
                }
            }
        }
        val aliveIndices = players.indices.filter { players[it].chips > 0 }
        val dealerAlive = if (dealer in aliveIndices) dealer else aliveIndices.firstOrNull() ?: 0
        val twoHanded = aliveIndices.size == 2
        val sbIndex = if (twoHanded) dealerAlive else nextAlive(
            state.copy(players = players, dealer = dealerAlive),
            dealerAlive,
        )
        val bbIndex = if (sbIndex >= 0) nextAlive(
            state.copy(players = players, dealer = sbIndex),
            sbIndex,
        ) else -1
        val safeSb = if (sbIndex >= 0) sbIndex else dealerAlive
        val safeBb = if (bbIndex >= 0) bbIndex else nextAlive(
            state.copy(players = players, dealer = safeSb),
            safeSb,
        )
        var nextPlayers = players.toMutableList()
        val sbPaid = postBlind(nextPlayers, safeSb, state.smallBlind)
        val bbPaid = if (safeBb >= 0) postBlind(nextPlayers, safeBb, state.bigBlind) else 0
        players = nextPlayers
        val currentBet = players.maxOfOrNull { it.bet } ?: 0
        val afterBb = players.copyOfWithCurrentBet(currentBet, state, safeBb)
        val first = if (twoHanded) {
            safeSb
        } else {
            val candidate = nextToAct(afterBb, safeBb)
            if (candidate >= 0) candidate else safeSb
        }
        val nextState = state.copy(
            players = players,
            deck = deck.drop(cursor),
            community = emptyList(),
            dealer = dealerAlive,
            turn = if (first >= 0) first else safeSb,
            street = PokerStreet.Preflop,
            currentBet = currentBet,
            minRaise = state.bigBlind,
            handNo = state.handNo + 1,
            showDown = null,
            handWinner = null,
            winner = null,
            draw = false,
            reason = null,
            history = (state.history + "Mano ${state.handNo + 1} · reparte ${state.seats.getOrNull(dealerAlive)?.name ?: ""}").takeLast(200),
        )
        val actual = nextToAct(nextState, nextState.turn - 1)
        return if (actual >= 0) nextState.copy(turn = actual) else nextState
    }

    private fun List<PokerPlayer>.copyOfWithCurrentBet(
        currentBet: Int,
        state: PokerState,
        from: Int,
    ): PokerState = state.copy(players = this, currentBet = currentBet, turn = from)

    private fun postBlind(players: MutableList<PokerPlayer>, index: Int, amount: Int): Int {
        val player = players.getOrNull(index) ?: return 0
        val pay = min(amount, player.chips)
        players[index] = player.copy(
            chips = player.chips - pay,
            bet = player.bet + pay,
            totalBet = player.totalBet + pay,
            allIn = player.chips - pay == 0,
            lastMove = "blind",
        )
        return pay
    }

    fun fold(state: PokerState): PokerResult = act(state, PokerAction.Fold)
    fun check(state: PokerState): PokerResult = act(state, PokerAction.Check)
    fun call(state: PokerState): PokerResult = act(state, PokerAction.Call)
    fun raise(state: PokerState, amount: Int): PokerResult = act(state, PokerAction.Raise(amount))
    fun allIn(state: PokerState): PokerResult = act(state, PokerAction.AllIn)

    fun act(state: PokerState, action: PokerAction): PokerResult {
        if (!isBettingStreet(state.street)) return PokerResult(state, "poker.error.notNow")
        if (action is PokerAction.NextHand) return nextHand(state)
        val index = state.turn
        val current = state.players.getOrNull(index) ?: return PokerResult(state, "poker.error.invalidAction")
        if (current.out || current.folded || current.allIn || current.chips <= 0) {
            return PokerResult(state, "poker.error.invalidAction")
        }
        var players = state.players.toMutableList()
        var player = players[index]
        val toCall = max(0, state.currentBet - player.bet)
        var minRaise = state.minRaise
        var entry = ""

        when (action) {
            PokerAction.Fold -> {
                player = player.copy(folded = true, acted = true, lastMove = "fold")
                entry = "${state.seats[index].name}: se retira"
            }
            PokerAction.Check -> {
                if (toCall > 0) return PokerResult(state, "poker.error.callFirst")
                player = player.copy(acted = true, lastMove = "check")
                entry = "${state.seats[index].name}: pasa"
            }
            PokerAction.Call -> {
                if (toCall <= 0) return PokerResult(state, "poker.error.nothingToCall")
                val pay = min(toCall, player.chips)
                player = player.copy(
                    chips = player.chips - pay,
                    bet = player.bet + pay,
                    totalBet = player.totalBet + pay,
                    acted = true,
                    allIn = player.chips - pay == 0,
                    lastMove = "call",
                )
                entry = "${state.seats[index].name}: iguala $pay"
            }
            is PokerAction.Raise, PokerAction.AllIn -> {
                val maxTarget = player.bet + player.chips
                var target = if (action is PokerAction.Raise) action.amount else maxTarget
                if (target > maxTarget) target = maxTarget
                if (target <= state.currentBet) {
                    if (toCall > 0) {
                        val pay = min(toCall, player.chips)
                        player = player.copy(
                            chips = player.chips - pay,
                            bet = player.bet + pay,
                            totalBet = player.totalBet + pay,
                            acted = true,
                            allIn = player.chips - pay == 0,
                            lastMove = "call",
                        )
                        entry = "${state.seats[index].name}: iguala $pay"
                    } else {
                        return PokerResult(state, "poker.error.minRaise")
                    }
                } else {
                    val previousBet = state.currentBet
                    val raiseSize = target - previousBet
                    val allIn = target >= maxTarget
                    if (raiseSize < state.minRaise && !allIn) return PokerResult(state, "poker.error.minRaise")
                    if (raiseSize >= state.minRaise) minRaise = raiseSize
                    val pay = target - player.bet
                    player = player.copy(
                        chips = player.chips - pay,
                        bet = target,
                        totalBet = player.totalBet + pay,
                        acted = true,
                        allIn = player.chips - pay == 0,
                        lastMove = if (allIn) "allin" else "raise",
                    )
                    for (i in players.indices) {
                        if (i != index) {
                            val other = players[i]
                            if (!other.folded && !other.out && !other.allIn) {
                                players[i] = other.copy(acted = false)
                            }
                        }
                    }
                    entry = "${state.seats[index].name}: ${if (allIn) "all-in" else "sube"} a $target"
                }
            }
            PokerAction.NextHand -> Unit
        }
        players[index] = player
        val currentBet = players.maxOfOrNull { it.bet } ?: 0
        val after = state.copy(
            players = players,
            currentBet = currentBet,
            minRaise = minRaise,
            history = (state.history + entry).takeLast(200),
        )
        return applyAfterAction(after)
    }

    private fun applyAfterAction(state: PokerState): PokerResult {
        val live = livePlayers(state)
        if (live.size == 1) return PokerResult(awardUncontested(state, live.first()))
        val next = nextToAct(state, state.turn)
        if (next >= 0) return PokerResult(state.copy(turn = next))
        return completeStreet(state)
    }

    private fun awardUncontested(state: PokerState, winner: PokerPlayer): PokerState {
        val amount = potTotal(state)
        val players = state.players.map {
            if (it.seat == winner.seat) {
                it.copy(chips = it.chips + amount, bet = 0, totalBet = 0)
            } else {
                it.copy(bet = 0, totalBet = 0)
            }
        }
        val next = state.copy(
            players = players,
            street = PokerStreet.HandEnd,
            turn = -1,
            handWinner = PokerHandWinner(winner.seat, amount, folded = true),
            showDown = null,
            history = (state.history + "${state.seats[winner.seat].name} gana $amount sin showdown").takeLast(200),
        )
        return checkEliminations(next)
    }

    private fun completeStreet(state: PokerState): PokerResult {
        val canAct = state.players.filter { !it.out && !it.folded && !it.allIn && it.chips > 0 }
        if (state.street == PokerStreet.River || canAct.size <= 1) return runOutAndShowdown(state)
        val nextStreet = when (state.street) {
            PokerStreet.Preflop -> PokerStreet.Flop
            PokerStreet.Flop -> PokerStreet.Turn
            PokerStreet.Turn -> PokerStreet.River
            else -> PokerStreet.River
        }
        val reset = state.copy(
            players = state.players.map { it.copy(bet = 0, acted = false) },
            currentBet = 0,
            minRaise = state.bigBlind,
        )
        val dealt = dealCards(reset, if (nextStreet == PokerStreet.Flop) 3 else 1)
        val first = nextToAct(dealt.copy(street = nextStreet, turn = -1), state.dealer)
        if (first < 0) return runOutAndShowdown(dealt.copy(street = nextStreet))
        val entry = when (nextStreet) {
            PokerStreet.Flop -> "Flop: ${dealt.community.drop(0).joinToString(" ") { cardLabel(it) }}"
            PokerStreet.Turn -> "Turn: ${dealt.community.lastOrNull()?.let { cardLabel(it) } ?: ""}"
            else -> "River: ${dealt.community.lastOrNull()?.let { cardLabel(it) } ?: ""}"
        }
        return PokerResult(
            dealt.copy(
                street = nextStreet,
                turn = first,
                history = (state.history + entry).takeLast(200),
            ),
        )
    }

    private fun dealCards(state: PokerState, amount: Int): PokerState {
        val deck = state.deck.toMutableList()
        if (deck.isNotEmpty()) deck.removeAt(0)
        val cards = mutableListOf<PokerCard>()
        repeat(amount) {
            val card = deck.removeFirstOrNull() ?: return@repeat
            cards.add(card)
        }
        return state.copy(deck = deck, community = state.community + cards)
    }

    private fun runOutAndShowdown(state: PokerState): PokerResult {
        var current = state.copy(
            players = state.players.map { it.copy(bet = 0, acted = false) },
            currentBet = 0,
        )
        var safety = 0
        while (current.community.size < 5 && safety < 4) {
            val need = if (current.community.isEmpty()) 3 else 1
            val street = when (current.community.size) {
                0 -> PokerStreet.Flop
                3 -> PokerStreet.Turn
                else -> PokerStreet.River
            }
            current = dealCards(current, need).copy(street = street)
            safety++
        }
        return showdown(current.copy(street = PokerStreet.Showdown))
    }

    private fun refundUncalled(state: PokerState): PokerState {
        val sorted = state.players.map { it.totalBet }.sortedDescending()
        val top = sorted.getOrNull(0) ?: 0
        val second = sorted.firstOrNull { it < top } ?: 0
        if (top <= second) return state
        val topPlayer = state.players.firstOrNull { it.totalBet == top } ?: return state
        val refund = top - second
        return state.copy(
            players = state.players.map {
                if (it.seat == topPlayer.seat) {
                    it.copy(chips = it.chips + refund, totalBet = it.totalBet - refund)
                } else {
                    it
                }
            },
        )
    }

    private fun sidePots(state: PokerState): List<Pair<Int, List<Int>>> {
        val levels = state.players.map { it.totalBet }.filter { it > 0 }.distinct().sorted()
        val pots = mutableListOf<Pair<Int, List<Int>>>()
        var previous = 0
        for (level in levels) {
            val contributors = state.players.count { it.totalBet >= level }
            val amount = (level - previous) * contributors
            val eligible = state.players.filter {
                !it.folded && !it.out && it.totalBet >= level
            }.map { it.seat }
            if (amount > 0) {
                if (eligible.isEmpty() && pots.isNotEmpty()) {
                    val last = pots.removeAt(pots.size - 1)
                    pots.add(last.first + amount to last.second)
                } else if (eligible.isEmpty()) {
                    pots.add(amount to livePlayers(state).map { it.seat })
                } else {
                    pots.add(amount to eligible)
                }
            }
            previous = level
        }
        return pots
    }

    private fun showdown(state: PokerState): PokerResult {
        val refunded = refundUncalled(state)
        val evaluations = mutableMapOf<Int, PokerHandEval>()
        for (player in refunded.players) {
            if (player.folded || player.out) continue
            evaluations[player.seat] = evaluateHand(player.hole + refunded.community)
        }
        val pots = sidePots(refunded)
        val wonBy = mutableMapOf<Int, Int>()
        val potResults = mutableListOf<PokerPotInfo>()
        val history = refunded.history.toMutableList()
        for ((amount, eligible) in pots) {
            val contenders = eligible.mapNotNull { seat -> evaluations[seat]?.let { seat to it } }
            if (contenders.isEmpty()) continue
            var best = contenders.first()
            for (candidate in contenders) {
                if (compareHands(candidate.second, best.second) > 0) best = candidate
            }
            val winners = contenders.filter { compareHands(it.second, best.second) == 0 }.map { it.first }
            val share = amount / winners.size
            var remainder = amount - share * winners.size
            for (seat in winners) {
                val extra = if (remainder > 0) 1 else 0
                remainder -= extra
                val prize = share + extra
                wonBy[seat] = (wonBy[seat] ?: 0) + prize
                history.add("${refunded.seats[seat].name} gana $prize con ${handName(best.second.nameKey)}")
            }
            potResults.add(PokerPotInfo(amount, winners))
        }
        if (wonBy.isEmpty()) {
            val fallback = livePlayers(refunded).firstOrNull()
            if (fallback != null) wonBy[fallback.seat] = potTotal(refunded)
        }
        val totalPot = potTotal(refunded)
        val players = refunded.players.map { player ->
            player.copy(
                chips = player.chips + (wonBy[player.seat] ?: 0),
                bet = 0,
                totalBet = 0,
            )
        }
        val rows = refunded.players.filter { it.hole.isNotEmpty() }.map { player ->
            val hand = evaluations[player.seat]
            PokerShowdownRow(
                seat = player.seat,
                hole = player.hole,
                handKey = hand?.nameKey ?: if (player.folded) "poker.hand.folded" else "poker.hand.high",
                best = hand?.cards ?: emptyList(),
                folded = player.folded,
                won = wonBy[player.seat] ?: 0,
            )
        }
        val winnerSeat = wonBy.keys.firstOrNull()
        val next = refunded.copy(
            players = players,
            street = PokerStreet.HandEnd,
            turn = -1,
            showDown = PokerShowdown(rows, potResults, totalPot),
            handWinner = winnerSeat?.let { PokerHandWinner(it, wonBy[it] ?: 0, false) },
            history = history.takeLast(200),
        )
        return PokerResult(checkEliminations(next))
    }

    private fun checkEliminations(state: PokerState): PokerState {
        val players = state.players.map {
            if (it.chips <= 0 && !it.out) it.copy(out = true, folded = true, allIn = false) else it
        }
        val next = state.copy(players = players)
        return finishMatch(next)
    }

    private fun finishMatch(state: PokerState): PokerState {
        val alive = state.players.filter { it.chips > 0 && !it.out }
        val humans = alive.filter { !state.seats[it.seat].isBot }
        val winners = when {
            alive.size == 1 -> alive
            humans.isEmpty() -> {
                val best = alive.maxOfOrNull { it.chips } ?: 0
                alive.filter { it.chips == best }
            }
            else -> return state
        }
        if (winners.isEmpty()) return state
        return state.copy(
            street = PokerStreet.MatchEnd,
            winner = winners.first().seat,
            draw = winners.size > 1,
            reason = "poker.reason.chips",
            history = (state.history + "Partida terminada: ${winners.joinToString(" · ") { state.seats[it.seat].name }}").takeLast(200),
        )
    }

    fun nextHand(state: PokerState): PokerResult {
        if (state.street != PokerStreet.HandEnd) return PokerResult(state, "poker.error.notNow")
        val reset = state.copy(
            players = state.players.map {
                it.copy(
                    out = it.chips <= 0,
                    folded = it.chips <= 0,
                    bet = 0,
                    totalBet = 0,
                    allIn = false,
                    acted = false,
                    lastMove = null,
                )
            },
        )
        val dealer = nextAlive(reset, state.dealer)
        return PokerResult(startHand(reset.copy(dealer = if (dealer >= 0) dealer else state.dealer)))
    }

    fun legalActions(state: PokerState): List<PokerAction> {
        if (!isBettingStreet(state.street)) return emptyList()
        val player = state.players.getOrNull(state.turn) ?: return emptyList()
        if (player.folded || player.out || player.allIn || player.chips <= 0) return emptyList()
        val toCall = max(0, state.currentBet - player.bet)
        return buildList {
            add(PokerAction.Fold)
            if (toCall == 0) add(PokerAction.Check) else add(PokerAction.Call)
            if (player.chips > 0) add(PokerAction.AllIn)
        }
    }

    fun botAction(state: PokerState, difficulty: String = "normal"): PokerAction? {
        if (!isBettingStreet(state.street)) return null
        val player = state.players.getOrNull(state.turn) ?: return null
        if (!state.seats[player.seat].isBot) return null
        if (player.folded || player.out || player.allIn) return null
        val toCall = max(0, state.currentBet - player.bet)
        val pot = potTotal(state)
        val maxTarget = player.bet + player.chips
        val noise = (random.nextDouble() - 0.5) * when (difficulty) {
            "hard" -> 0.12
            "easy" -> 0.3
            else -> 0.2
        }
        val strength = (botStrength(state, player) + noise).coerceIn(0.0, 1.0)
        fun raiseTarget(fraction: Double): Int {
            val base = max(
                state.currentBet + state.minRaise,
                (floor(((pot + toCall) * fraction) / state.bigBlind).toInt() * state.bigBlind),
            )
            return min(maxTarget, max(state.currentBet + 1, base))
        }
        if (toCall == 0) {
            if (strength > 0.72) {
                val target = raiseTarget(0.55)
                if (target > state.currentBet && target <= maxTarget) return PokerAction.Raise(target)
            }
            if (strength < 0.3 && random.nextDouble() < if (difficulty == "hard") 0.08 else 0.14) {
                val target = raiseTarget(0.35)
                if (target > state.currentBet && target <= maxTarget) return PokerAction.Raise(target)
            }
            return PokerAction.Check
        }
        val allInCall = toCall >= player.chips
        if (allInCall) {
            return if (strength > 0.56 || random.nextDouble() < 0.12) PokerAction.Call else PokerAction.Fold
        }
        val odds = toCall.toDouble() / (pot + toCall)
        if (strength > 0.8 && maxTarget > state.currentBet) return PokerAction.Raise(raiseTarget(0.7))
        if (strength > odds + if (difficulty == "hard") 0.24 else 0.15) return PokerAction.Call
        if (toCall <= state.bigBlind / 2 && strength > 0.35) return PokerAction.Call
        return PokerAction.Fold
    }

    private fun botStrength(state: PokerState, player: PokerPlayer): Double {
        if (state.community.size >= 3) {
            val hand = evaluateHand(player.hole + state.community)
            val high = hand.ranks.firstOrNull() ?: 0
            return (hand.category / 8.0 * 0.86 + (high / 14.0) * 0.14).coerceIn(0.0, 1.0)
        }
        val ranks = player.hole.map { it.rank }.sortedDescending()
        val a = ranks.getOrElse(0) { 0 }
        val b = ranks.getOrElse(1) { 0 }
        var score = ((a + b) / 28.0) * 0.58
        if (a == b) score += 0.28
        if (player.hole.size >= 2 && player.hole[0].suit == player.hole[1].suit) score += 0.045
        if (abs(a - b) <= 1) score += 0.05
        if (a >= 12 && b >= 10) score += 0.07
        if (a == 14) score += 0.02
        return score.coerceIn(0.0, 1.0)
    }

    fun evaluateHand(cards: List<PokerCard>): PokerHandEval = evaluate(cards)

    fun compareHands(a: PokerHandEval, b: PokerHandEval): Int = compare(a, b)

    fun handName(key: String): String = when (key) {
        "poker.hand.high" -> "Carta alta"
        "poker.hand.pair" -> "Pareja"
        "poker.hand.twoPair" -> "Doble pareja"
        "poker.hand.trips" -> "Trío"
        "poker.hand.straight" -> "Escalera"
        "poker.hand.flush" -> "Color"
        "poker.hand.fullHouse" -> "Full"
        "poker.hand.quads" -> "Póker"
        "poker.hand.straightFlush" -> "Escalera de color"
        "poker.hand.folded" -> "Retirado"
        else -> key
    }

    fun winnerName(state: PokerState): String =
        state.winner?.let { state.seats.getOrNull(it)?.name } ?: ""

    private fun evaluate(cards: List<PokerCard>): PokerHandEval {
        val sorted = cards.sortedByDescending { it.rank }
        if (sorted.size < 5) {
            val top = sorted.take(5)
            return PokerHandEval(0, top.map { it.rank }, CATEGORY_KEYS[0], top)
        }
        val counts = sorted.groupingBy { it.rank }.eachCount()
        val groups = counts.entries.sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key },
        )
        val quad = groups.firstOrNull { it.value == 4 }
        val trips = groups.filter { it.value == 3 }.map { it.key }.sortedDescending()
        val pairs = groups.filter { it.value == 2 }.map { it.key }.sortedDescending()
        val bySuit = sorted.groupBy { it.suit }
        val flushCards = bySuit.values.firstOrNull { it.size >= 5 }?.take(5)
        if (flushCards != null) {
            val sfHigh = straightHigh(flushCards.map { it.rank })
            if (sfHigh != null) {
                val ranks = if (sfHigh == 1) listOf(5, 4, 3, 2, 1) else (0..4).map { sfHigh - it }
                return PokerHandEval(8, ranks, CATEGORY_KEYS[8], cardsForRanks(flushCards, ranks))
            }
        }
        if (quad != null) {
            val quadRank = quad.key
            val kicker = sorted.firstOrNull { it.rank != quadRank }?.rank ?: 0
            return PokerHandEval(
                7,
                listOf(quadRank, kicker),
                CATEGORY_KEYS[7],
                cardsForRanks(sorted, listOf(quadRank, quadRank, quadRank, quadRank, kicker)),
            )
        }
        if (trips.isNotEmpty()) {
            val firstTrips = trips[0]
            val pairRank = trips.getOrNull(1) ?: pairs.firstOrNull { it != firstTrips }
            if (pairRank != null) {
                return PokerHandEval(
                    6,
                    listOf(firstTrips, pairRank),
                    CATEGORY_KEYS[6],
                    cardsForRanks(sorted, listOf(firstTrips, firstTrips, firstTrips, pairRank, pairRank)),
                )
            }
        }
        if (flushCards != null) {
            return PokerHandEval(5, flushCards.map { it.rank }, CATEGORY_KEYS[5], flushCards)
        }
        val straight = straightHigh(sorted.map { it.rank })
        if (straight != null) {
            val ranks = if (straight == 1) listOf(5, 4, 3, 2, 1) else (0..4).map { straight - it }
            return PokerHandEval(4, ranks, CATEGORY_KEYS[4], cardsForRanks(sorted, ranks))
        }
        if (trips.isNotEmpty()) {
            val tripRank = trips[0]
            val kickers = groups.filter { it.key != tripRank && it.value < 3 }
                .map { it.key }.sortedDescending().take(2)
            return PokerHandEval(3, listOf(tripRank) + kickers, CATEGORY_KEYS[3], cardsForRanks(sorted, listOf(tripRank, tripRank, tripRank) + kickers))
        }
        if (pairs.size >= 2) {
            val highPair = pairs[0]
            val lowPair = pairs[1]
            val kicker = groups.filter { it.key != highPair && it.key != lowPair }.map { it.key }.sortedDescending().firstOrNull() ?: 0
            return PokerHandEval(2, listOf(highPair, lowPair, kicker), CATEGORY_KEYS[2], cardsForRanks(sorted, listOf(highPair, highPair, lowPair, lowPair, kicker)))
        }
        if (pairs.isNotEmpty()) {
            val pairRank = pairs[0]
            val kickers = groups.filter { it.key != pairRank }.map { it.key }.sortedDescending().take(3)
            return PokerHandEval(1, listOf(pairRank) + kickers, CATEGORY_KEYS[1], cardsForRanks(sorted, listOf(pairRank, pairRank) + kickers))
        }
        val high = sorted.take(5)
        return PokerHandEval(0, high.map { it.rank }, CATEGORY_KEYS[0], high)
    }

    private fun straightHigh(ranks: List<Int>): Int? {
        val unique = ranks.toSortedSet(compareByDescending { it }).toMutableList()
        if (14 in unique) unique.add(1)
        for (i in 0..unique.size - 5) {
            val a = unique[i]
            val b = unique[i + 1]
            val c = unique[i + 2]
            val d = unique[i + 3]
            val e = unique[i + 4]
            if (a - 1 == b && b - 1 == c && c - 1 == d && d - 1 == e) return a
        }
        return null
    }

    private fun cardsForRanks(cards: List<PokerCard>, ranks: List<Int>): List<PokerCard> {
        val pool = cards.sortedByDescending { it.rank }.toMutableList()
        val out = mutableListOf<PokerCard>()
        for (rank in ranks) {
            val index = pool.indexOfFirst { it.rank == rank }
            if (index >= 0) out.add(pool.removeAt(index))
        }
        return out
    }

    private fun compare(a: PokerHandEval, b: PokerHandEval): Int {
        if (a.category != b.category) return a.category - b.category
        val length = max(a.ranks.size, b.ranks.size)
        for (i in 0 until length) {
            val ra = a.ranks.getOrElse(i) { 0 }
            val rb = b.ranks.getOrElse(i) { 0 }
            if (ra != rb) return ra - rb
        }
        return 0
    }
}
