package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Truco criollo para dos jugadores con baraja española de 40 cartas.
 *
 * Envido, Real Envido, Truco, Retruco y Vale Cuatro. Cada mano reparte tres
 * cartas y se juega al mejor de tres bazas. El envido se canta antes de la
 * primera carta. Las pardas no suman baza y, si nadie llega a dos, gana el
 * repartidor. La partida se juega a 30 puntos.
 */
enum class TrucoPhase { Envido, Play, HandEnd, MatchEnd }

data class TrucoSeat(
    val name: String,
    val isBot: Boolean,
)

data class TrucoPlay(
    val seat: Int,
    val card: SpanishCard,
)

data class TrucoTrick(
    val plays: List<TrucoPlay>,
    val winner: Int?,
)

data class TrucoChallenge(
    val caller: Int,
    val kind: String,
    val stake: Int,
    val previousStake: Int,
    val awaiting: Int,
)

data class TrucoState(
    val seats: List<TrucoSeat>,
    val scores: List<Int>,
    val hands: List<List<SpanishCard>>,
    val dealer: Int,
    val mano: Int,
    val turn: Int,
    val handNo: Int,
    val target: Int,
    val phase: TrucoPhase = TrucoPhase.Envido,
    val trickLeader: Int = mano,
    val currentTrick: List<TrucoPlay> = emptyList(),
    val tricks: List<TrucoTrick> = emptyList(),
    val trickWins: List<Int> = List(seats.size) { 0 },
    val handStake: Int = 1,
    val trucoLevel: Int = 0,
    val envidoCalled: Boolean = false,
    val envidoBet: Int = 0,
    val envidoWinner: Int? = null,
    val envidoReveal: List<Int>? = null,
    val pending: TrucoChallenge? = null,
    val handWinner: Int? = null,
    val handPoints: Int = 0,
    val lastAction: String? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class TrucoResult(val state: TrucoState, val error: String? = null)

class TrucoGame(
    private val target: Int = 30,
    private val allHumans: Boolean = false,
    seed: Long = Random.nextLong(),
) {

    private val random = Random(seed)
    private val count = 2

    companion object {
        val RANKS = BriscaGame.RANKS
        fun power(card: SpanishCard): Int = BriscaGame.power(card)
        fun label(card: SpanishCard): String = BriscaGame.label(card)

        /** Valor de una carta para el envido: las figuras valen cero. */
        fun envidoValue(rank: Int): Int = if (rank in 10..12) 0 else rank

        fun tanto(hand: List<SpanishCard>): Int {
            if (hand.isEmpty()) return 0
            val values = hand.map { envidoValue(it.rank) }.sortedDescending()
            val counts = values.groupingBy { it }.eachCount()
            val pair = counts.entries.filter { it.value >= 2 && it.key > 0 }.maxByOrNull { it.key }
            return if (pair != null) {
                val third = values.firstOrNull { it != pair.key } ?: 0
                20 + pair.key + third
            } else {
                (values.getOrNull(0) ?: 0) + (values.getOrNull(1) ?: 0)
            }
        }

        fun challengeName(kind: String): String = when (kind) {
            "envido" -> "Envido"
            "realEnvido" -> "Real Envido"
            "envidoEnvigido" -> "Envido Envido"
            "truco" -> "Truco"
            "retruco" -> "Retruco"
            "valeCuatro" -> "Vale Cuatro"
            else -> kind
        }

        fun challengeStake(kind: String): Int = when (kind) {
            "envido" -> 2
            "realEnvido" -> 3
            "envidoEnvigido" -> 4
            "truco" -> 2
            "retruco" -> 3
            "valeCuatro" -> 4
            else -> 1
        }

        fun nextTrucoKind(level: Int): String = when (level) {
            0 -> "truco"
            1 -> "retruco"
            else -> "valeCuatro"
        }
    }

    fun newMatch(): TrucoState {
        val seats = listOf(
            TrucoSeat(if (allHumans) "Jugador 1" else "Tú", isBot = false),
            TrucoSeat(if (allHumans) "Jugador 2" else "El Silbón", isBot = !allHumans),
        )
        val dealer = 1
        val mano = 0
        val deck = shuffle(BriscaGame.deck())
        val hands = List(count) { seat ->
            val cards = deck.drop(seat * 3).take(3)
            gameSort(cards)
        }
        return TrucoState(
            seats = seats,
            scores = List(count) { 0 },
            hands = hands,
            dealer = dealer,
            mano = mano,
            turn = mano,
            handNo = 1,
            target = target,
            trickLeader = mano,
            history = listOf("Mano 1 · reparte ${seats[dealer].name}"),
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

    fun handOf(state: TrucoState, seat: Int): List<SpanishCard> =
        state.hands.getOrElse(seat) { emptyList() }

    fun tantoOf(state: TrucoState, seat: Int): Int = tanto(handOf(state, seat))

    fun maxScore(state: TrucoState): Int = state.scores.maxOrNull() ?: 0

    fun startPlay(state: TrucoState): TrucoResult {
        if (state.phase != TrucoPhase.Envido) return TrucoResult(state, "truco.error.notNow")
        return TrucoResult(
            state.copy(
                phase = TrucoPhase.Play,
                turn = state.mano,
                trickLeader = state.mano,
                lastAction = "Truco.EnvidoDeclined",
                history = (state.history + "Sin envido · se juega la mano").takeLast(180),
            ),
        )
    }

    fun callEnvido(state: TrucoState, seat: Int, kind: String = "envido"): TrucoResult {
        if (state.phase != TrucoPhase.Envido) return TrucoResult(state, "truco.error.notNow")
        if (state.pending != null) return TrucoResult(state, "truco.error.pending")
        if (seat != state.turn) return TrucoResult(state, "truco.error.notYourTurn")
        if (state.envidoCalled) return TrucoResult(state, "truco.error.envidoDone")
        val stake = challengeStake(kind)
        val challenge = TrucoChallenge(
            caller = seat,
            kind = kind,
            stake = stake,
            previousStake = 1,
            awaiting = (seat + 1) % count,
        )
        return TrucoResult(
            state.copy(
                pending = challenge,
                envidoCalled = true,
                envidoBet = stake,
                lastAction = "Truco.Call.$kind",
                history = (state.history + "${state.seats[seat].name} canta ${challengeName(kind)}").takeLast(180),
            ),
        )
    }

    fun callTruco(state: TrucoState, seat: Int): TrucoResult {
        if (state.phase != TrucoPhase.Play) return TrucoResult(state, "truco.error.notNow")
        if (state.pending != null) return TrucoResult(state, "truco.error.pending")
        if (seat != state.turn) return TrucoResult(state, "truco.error.notYourTurn")
        if (state.trucoLevel >= 3) return TrucoResult(state, "truco.error.maxTruco")
        val kind = nextTrucoKind(state.trucoLevel)
        val stake = challengeStake(kind)
        val challenge = TrucoChallenge(
            caller = seat,
            kind = kind,
            stake = stake,
            previousStake = state.handStake,
            awaiting = (seat + 1) % count,
        )
        return TrucoResult(
            state.copy(
                pending = challenge,
                lastAction = "Truco.Call.$kind",
                history = (state.history + "${state.seats[seat].name} canta ${challengeName(kind)}").takeLast(180),
            ),
        )
    }

    fun respond(state: TrucoState, accept: Boolean): TrucoResult {
        val challenge = state.pending ?: return TrucoResult(state, "truco.error.noPending")
        return if (challenge.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
            resolveEnvido(state, challenge, accept)
        } else {
            resolveTruco(state, challenge, accept)
        }
    }

    private fun resolveEnvido(state: TrucoState, challenge: TrucoChallenge, accept: Boolean): TrucoResult {
        val caller = challenge.caller
        val responder = challenge.awaiting
        if (!accept) {
            val scores = state.scores.toMutableList()
            scores[caller] += 1
            val next = state.copy(
                scores = scores,
                pending = null,
                envidoBet = 1,
                envidoWinner = caller,
                envidoReveal = null,
                phase = TrucoPhase.Play,
                turn = state.mano,
                trickLeader = state.mano,
                lastAction = "Truco.EnvidoRejected",
                history = (state.history + "${state.seats[responder].name} no quiere · ${state.seats[caller].name} +1").takeLast(180),
            )
            return TrucoResult(checkTarget(next))
        }
        val tantos = listOf(tantoOf(state, 0), tantoOf(state, 1))
        val winner = when {
            tantos[0] > tantos[1] -> 0
            tantos[1] > tantos[0] -> 1
            else -> state.mano // En empate gana la mano.
        }
        val scores = state.scores.toMutableList()
        scores[winner] += challenge.stake
        val next = state.copy(
            scores = scores,
            pending = null,
            envidoBet = challenge.stake,
            envidoWinner = winner,
            envidoReveal = tantos,
            phase = TrucoPhase.Play,
            turn = state.mano,
            trickLeader = state.mano,
            lastAction = "Truco.EnvidoShown",
            history = (state.history + "Envido ${tantos[0]}–${tantos[1]} · ${state.seats[winner].name} +${challenge.stake}").takeLast(180),
        )
        return TrucoResult(checkTarget(next))
    }

    private fun resolveTruco(state: TrucoState, challenge: TrucoChallenge, accept: Boolean): TrucoResult {
        val caller = challenge.caller
        val responder = challenge.awaiting
        if (!accept) {
            val next = state.copy(
                pending = null,
                turn = caller,
                lastAction = "Truco.Rejected",
                history = (state.history + "${state.seats[responder].name} no quiere · ${state.seats[caller].name} +${challenge.previousStake}").takeLast(180),
            )
            return TrucoResult(awardHand(next, caller, challenge.previousStake, "truco.reason.rejected"))
        }
        val level = when (challenge.kind) {
            "truco" -> 1
            "retruco" -> 2
            else -> 3
        }
        return TrucoResult(
            state.copy(
                pending = null,
                handStake = challenge.stake,
                trucoLevel = level,
                turn = caller,
                lastAction = "Truco.Accepted",
                history = (state.history + "${state.seats[responder].name} quiere · ${challengeName(challenge.kind)} (${challenge.stake})").takeLast(180),
            ),
        )
    }

    fun playCard(state: TrucoState, cardId: String): TrucoResult {
        if (state.phase != TrucoPhase.Play) return TrucoResult(state, "truco.error.notNow")
        if (state.pending != null) return TrucoResult(state, "truco.error.pending")
        val seat = state.turn
        val hand = handOf(state, seat).toMutableList()
        val card = hand.firstOrNull { it.id == cardId }
            ?: return TrucoResult(state, "truco.error.invalidCard")
        hand.remove(card)
        val hands = state.hands.toMutableList()
        hands[seat] = hand
        val play = TrucoPlay(seat, card)
        val current = state.currentTrick + play
        var next = state.copy(
            hands = hands,
            currentTrick = current,
            turn = (seat + 1) % count,
            history = (state.history + "${state.seats[seat].name}: ${label(card)}").takeLast(180),
        )
        if (current.size < count) return TrucoResult(next)

        val a = current[0]
        val b = current[1]
        val comparison = power(a.card).compareTo(power(b.card))
        val trickWinner = when {
            comparison > 0 -> a.seat
            comparison < 0 -> b.seat
            else -> null
        }
        val trick = TrucoTrick(current, trickWinner)
        val tricks = next.tricks + trick
        val wins = next.trickWins.toMutableList()
        if (trickWinner != null) wins[trickWinner] += 1
        val entry = if (trickWinner == null) {
            "Baza parda"
        } else {
            "Baza para ${state.seats[trickWinner].name}"
        }
        next = next.copy(
            tricks = tricks,
            trickWins = wins,
            currentTrick = emptyList(),
            trickLeader = trickWinner ?: a.seat,
            turn = trickWinner ?: a.seat,
            lastAction = entry,
            history = (next.history + entry).takeLast(180),
        )
        val handWinner = when {
            wins.any { it >= 2 } -> wins.indices.first { wins[it] >= 2 }
            tricks.size >= 3 -> {
                val best = wins.maxOrNull() ?: 0
                val leaders = wins.indices.filter { wins[it] == best }
                if (leaders.size == 1 && best > 0) leaders.first() else state.dealer
            }
            else -> null
        }
        if (handWinner != null) {
            return TrucoResult(awardHand(next, handWinner, next.handStake, "truco.reason.tricks"))
        }
        return TrucoResult(next)
    }

    /** El jugador en turno se va al mazo: el rival gana lo que hay en juego. */
    fun fold(state: TrucoState): TrucoResult {
        if (state.phase != TrucoPhase.Envido && state.phase != TrucoPhase.Play) {
            return TrucoResult(state, "truco.error.notNow")
        }
        if (state.pending != null) return TrucoResult(state, "truco.error.pending")
        val loser = state.turn
        val winner = (loser + 1) % count
        val next = state.copy(
            lastAction = "Truco.Fold",
            history = (state.history + "${state.seats[loser].name} se va al mazo").takeLast(180),
        )
        return TrucoResult(awardHand(next, winner, state.handStake, "truco.reason.fold"))
    }

    private fun awardHand(state: TrucoState, winner: Int, points: Int, reason: String): TrucoState {
        val scores = state.scores.toMutableList()
        scores[winner] += points
        val next = state.copy(
            scores = scores,
            handWinner = winner,
            handPoints = points,
            phase = TrucoPhase.HandEnd,
            pending = null,
            reason = reason,
            history = (state.history + "${state.seats[winner].name} gana la mano +$points").takeLast(180),
        )
        return checkTarget(next)
    }

    fun nextHand(state: TrucoState): TrucoResult {
        if (state.phase != TrucoPhase.HandEnd) return TrucoResult(state, "truco.error.notNow")
        val dealer = (state.dealer + 1) % count
        val mano = (dealer + 1) % count
        val deck = shuffle(BriscaGame.deck())
        val hands = List(count) { seat -> gameSort(deck.drop(seat * 3).take(3)) }
        return TrucoResult(
            state.copy(
                hands = hands,
                dealer = dealer,
                mano = mano,
                turn = mano,
                handNo = state.handNo + 1,
                phase = TrucoPhase.Envido,
                trickLeader = mano,
                currentTrick = emptyList(),
                tricks = emptyList(),
                trickWins = List(count) { 0 },
                handStake = 1,
                trucoLevel = 0,
                envidoCalled = false,
                envidoBet = 0,
                envidoWinner = null,
                envidoReveal = null,
                pending = null,
                handWinner = null,
                handPoints = 0,
                lastAction = null,
                reason = null,
                history = (state.history + "Mano ${state.handNo + 1} · reparte ${state.seats[dealer].name}").takeLast(180),
            ),
        )
    }

    fun resign(state: TrucoState): TrucoResult {
        if (state.phase == TrucoPhase.MatchEnd) return TrucoResult(state, "truco.error.matchOver")
        val winner = 1
        return TrucoResult(
            state.copy(
                phase = TrucoPhase.MatchEnd,
                winner = winner,
                reason = "truco.reason.resign",
            ),
        )
    }

    private fun checkTarget(state: TrucoState): TrucoState {
        if (state.phase == TrucoPhase.MatchEnd) return state
        if (maxScore(state) < state.target) return state
        val best = state.scores.maxOrNull() ?: 0
        val winners = state.scores.indices.filter { state.scores[it] == best }
        return state.copy(
            phase = TrucoPhase.MatchEnd,
            winner = winners.firstOrNull(),
            draw = winners.size > 1,
            reason = "truco.reason.points",
        )
    }

    fun gameSort(hand: List<SpanishCard>): List<SpanishCard> =
        hand.sortedWith(compareBy({ BriscaGame.POWER[it.rank] ?: 0 }, { it.suit.ordinal }))

    fun botPlayCard(state: TrucoState, seat: Int): SpanishCard? {
        val hand = handOf(state, seat)
        if (hand.isEmpty()) return null
        val opponentPlay = state.currentTrick.firstOrNull { it.seat != seat }
        if (opponentPlay == null) return hand.maxByOrNull { power(it) }
        val winning = hand.filter { power(it) > power(opponentPlay.card) }
        return winning.minByOrNull { power(it) }
            ?: hand.minByOrNull { power(it) }
    }

    fun botShouldCallTruco(state: TrucoState, seat: Int): Boolean {
        if (state.phase != TrucoPhase.Play || state.trucoLevel >= 3 || state.pending != null) return false
        val hand = handOf(state, seat)
        val high = hand.count { power(it) >= 7 }
        val wins = state.trickWins.getOrElse(seat) { 0 }
        return when {
            state.trucoLevel >= 2 -> high >= 3 || (high >= 2 && wins >= 2)
            state.trucoLevel == 1 -> high >= 2 && wins >= 1
            else -> high >= 2 || (wins >= 1 && high >= 1) || (state.tricks.size >= 2 && wins == 1)
        }
    }

    fun botWantsEnvido(state: TrucoState, seat: Int): Boolean {
        if (state.phase != TrucoPhase.Envido || state.envidoCalled || state.pending != null) return false
        return tantoOf(state, seat) >= 27
    }

    fun botAcceptsEnvido(state: TrucoState, seat: Int, stake: Int): Boolean {
        val tanto = tantoOf(state, seat)
        return when {
            stake >= 4 -> tanto >= 30
            stake >= 3 -> tanto >= 27
            else -> tanto >= 23
        }
    }

    fun botAcceptsTruco(state: TrucoState, seat: Int, stake: Int): Boolean {
        val hand = handOf(state, seat)
        val high = hand.count { power(it) >= 7 }
        val wins = state.trickWins.getOrElse(seat) { 0 }
        val needed = if (stake >= 4) 3 else if (stake >= 3) 2 else 1
        return high + wins >= needed
    }

    fun winnerName(state: TrucoState): String =
        state.winner?.let { state.seats.getOrNull(it)?.name } ?: ""
}
