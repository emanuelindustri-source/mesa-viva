package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Dos juegos de dominó con trenes para la mesa grande.
 *
 * [MexicanTrainGame] usa doble 12, tres rondas, tren propio, tren público y
 * trenes abiertos cuando un jugador no puede jugar. [ChickenfootGame] usa
 * doble 9, tres rondas y obliga a completar tres ramas cada vez que aparece
 * un doble (la "pata de gallina").
 */
enum class TrainGamePhase { Playing, RoundEnd, MatchEnd }

data class TrainPlayer(
    val name: String,
    val isBot: Boolean,
    val hand: List<DominoTile> = emptyList(),
    val score: Int = 0,
)

/* ------------------------------------------------------------------ Mexican Train */

data class MexicanTrainSlot(
    val owner: Int,
    val tiles: List<DominoTile> = emptyList(),
    val open: Boolean = false,
)

data class MexicanTrainState(
    val players: List<TrainPlayer>,
    val boneyard: List<DominoTile>,
    val trains: List<MexicanTrainSlot>,
    val publicTrain: MexicanTrainSlot,
    val hubPip: Int,
    val turn: Int,
    val round: Int,
    val phase: TrainGamePhase = TrainGamePhase.Playing,
    val doubleTrain: Int? = null,
    val doublePip: Int? = null,
    val passes: Int = 0,
    val winner: Int? = null,
    val history: List<String> = emptyList(),
) {
    val current: TrainPlayer get() = players[turn]
    val boneyardCount: Int get() = boneyard.size
}

data class MexicanTrainResult(val state: MexicanTrainState, val error: String? = null)

class MexicanTrainGame(
    private val playerCount: Int,
    seed: Long = Random.nextLong(),
) {
    private val random = Random(seed)

    companion object {
        const val ROUNDS = 3
        fun handSize(players: Int): Int = when {
            players <= 4 -> 12
            players <= 6 -> 10
            else -> 8
        }
    }

    fun newMatch(): MexicanTrainState {
        val players = (0 until playerCount).map { TrainPlayer("Tren ${it + 1}", it != 0) }
        return deal(players, round = 1, startPip = 12)
    }

    private fun deal(players: List<TrainPlayer>, round: Int, startPip: Int): MexicanTrainState {
        var deck = DominoGame.doubleSixDeck(12).filterNot { it.a == startPip && it.b == startPip }.shuffled(random)
        val handSize = handSize(playerCount)
        val dealt = players.map { player ->
            val hand = deck.take(handSize)
            deck = deck.drop(handSize)
            player.copy(hand = hand)
        }
        return MexicanTrainState(
            players = dealt,
            boneyard = deck,
            trains = dealt.indices.map { MexicanTrainSlot(owner = it) },
            publicTrain = MexicanTrainSlot(owner = -1, open = true),
            hubPip = startPip,
            turn = 0,
            round = round,
            history = listOf("Ronda $round · sale el doble $startPip"),
        )
    }

    fun openEnd(state: MexicanTrainState, target: Int): Int {
        val slot = if (target < 0) state.publicTrain else state.trains[target]
        return slot.tiles.lastOrNull()?.b ?: state.hubPip
    }

    fun canPlayOn(state: MexicanTrainState, target: Int): Boolean =
        target == -1 || target == state.turn || state.trains[target].open

    fun playableTargets(state: MexicanTrainState, tile: DominoTile): List<Int> {
        if (state.phase != TrainGamePhase.Playing) return emptyList()
        val targets = mutableListOf<Int>()
        if (state.doubleTrain != null) {
            val target = state.doubleTrain!!
            if (canPlayOn(state, target) && tile.fits(openEnd(state, target))) targets += target
            return targets
        }
        if (canPlayOn(state, state.turn) && tile.fits(openEnd(state, state.turn))) targets += state.turn
        if (tile.fits(openEnd(state, -1))) targets += -1
        state.trains.indices.forEach { index ->
            if (index != state.turn && state.trains[index].open && tile.fits(openEnd(state, index))) targets += index
        }
        return targets.distinct()
    }

    fun play(state: MexicanTrainState, tileIndex: Int, target: Int): MexicanTrainResult {
        if (state.phase != TrainGamePhase.Playing) return MexicanTrainResult(state, "train.error.over")
        val player = state.players[state.turn]
        val tile = player.hand.getOrNull(tileIndex) ?: return MexicanTrainResult(state, "train.error.tile")
        val targets = playableTargets(state, tile)
        if (target !in targets) return MexicanTrainResult(state, "train.error.move")
        val end = openEnd(state, target)
        val oriented = if (tile.a == end) tile else tile.flipped()
        val players = state.players.toMutableList()
        players[state.turn] = player.copy(hand = player.hand.filterIndexed { index, _ -> index != tileIndex })
        val trains = state.trains.toMutableList()
        var publicTrain = state.publicTrain
        if (target == -1) publicTrain = publicTrain.copy(tiles = publicTrain.tiles + oriented)
        else trains[target] = trains[target].copy(tiles = trains[target].tiles + oriented)
        val isDouble = oriented.a == oriented.b
        val base = state.copy(
            players = players.toList(),
            trains = trains.toList(),
            publicTrain = publicTrain,
            doubleTrain = if (isDouble) target else null,
            doublePip = if (isDouble) oriented.a else null,
            passes = 0,
            history = (state.history + "${player.name} juega ${oriented.a}-${oriented.b} en ${trainLabel(target)}").takeLast(200),
        )
        if (players[state.turn].hand.isEmpty()) return MexicanTrainResult(endRound(base, state.turn))
        if (isDouble) {
            return MexicanTrainResult(base)
        }
        return MexicanTrainResult(advance(base))
    }

    fun draw(state: MexicanTrainState): MexicanTrainResult {
        if (state.phase != TrainGamePhase.Playing) return MexicanTrainResult(state, "train.error.over")
        if (state.boneyard.isEmpty()) return MexicanTrainResult(state, "train.error.empty")
        val tile = state.boneyard.first()
        val players = state.players.toMutableList()
        players[state.turn] = players[state.turn].copy(hand = players[state.turn].hand + tile)
        return MexicanTrainResult(
            state.copy(
                players = players.toList(),
                boneyard = state.boneyard.drop(1),
                history = (state.history + "${state.players[state.turn].name} roba del pozo").takeLast(200),
            ),
        )
    }

    fun pass(state: MexicanTrainState): MexicanTrainResult {
        if (state.phase != TrainGamePhase.Playing) return MexicanTrainResult(state, "train.error.over")
        val trains = state.trains.toMutableList()
        trains[state.turn] = trains[state.turn].copy(open = true)
        val cleared = state.copy(
            trains = trains.toList(),
            doubleTrain = null,
            doublePip = null,
            passes = state.passes + 1,
            history = (state.history + "${state.players[state.turn].name} pasa y abre su tren").takeLast(200),
        )
        if (cleared.passes >= playerCount) return MexicanTrainResult(endRound(cleared, null))
        return MexicanTrainResult(advance(cleared))
    }

    private fun advance(state: MexicanTrainState): MexicanTrainState =
        state.copy(turn = (state.turn + 1) % playerCount)

    private fun endRound(state: MexicanTrainState, winner: Int?): MexicanTrainState {
        val players = state.players.mapIndexed { index, player ->
            val pips = if (index == winner) 0 else player.hand.sumOf { it.pips }
            player.copy(score = player.score + pips)
        }
        return state.copy(
            players = players,
            phase = TrainGamePhase.RoundEnd,
            winner = winner,
            history = (state.history + "Fin de ronda ${state.round}").takeLast(200),
        )
    }

    fun nextRound(state: MexicanTrainState): MexicanTrainState {
        if (state.phase != TrainGamePhase.RoundEnd) return state
        if (state.round >= ROUNDS) {
            val best = state.players.indices.minByOrNull { state.players[it].score }
            return state.copy(phase = TrainGamePhase.MatchEnd, winner = best)
        }
        val carried = state.players.map { it.copy(hand = emptyList()) }
        return deal(carried, state.round + 1, (state.hubPip - 1).coerceAtLeast(0))
    }

    fun trainLabel(target: Int): String = if (target == -1) "tren público" else "tren ${target + 1}"

    fun bestMove(state: MexicanTrainState): Triple<Int, Int, Int>? {
        val player = state.players[state.turn]
        val options = player.hand.mapIndexedNotNull { index, tile ->
            val targets = playableTargets(state, tile)
            if (targets.isEmpty()) null else Triple(index, targets.first(), tile.pips)
        }
        return options.maxByOrNull { it.third }
    }

    fun hasPlay(state: MexicanTrainState): Boolean =
        state.players[state.turn].hand.any { playableTargets(state, it).isNotEmpty() }

    fun botStep(state: MexicanTrainState): MexicanTrainState? {
        if (state.phase != TrainGamePhase.Playing) return null
        val move = bestMove(state)
        if (move != null) return play(state, move.first, move.second).state
        if (state.boneyard.isNotEmpty()) {
            val drawn = draw(state).state
            if (hasPlay(drawn)) {
                val after = bestMove(drawn)
                if (after != null) return play(drawn, after.first, after.second).state
            }
            return pass(drawn).state
        }
        return pass(state).state
    }
}

/* ------------------------------------------------------------------ Chickenfoot */

data class ChickenBranch(val tiles: List<DominoTile> = emptyList()) {
    fun openEnd(hubPip: Int): Int = tiles.lastOrNull()?.b ?: hubPip
}

data class ChickenFoot(
    val pip: Int,
    val branches: List<List<DominoTile>> = List(3) { emptyList() },
)

data class ChickenState(
    val players: List<TrainPlayer>,
    val boneyard: List<DominoTile>,
    val hubPip: Int,
    val branches: List<ChickenBranch>,
    val foot: ChickenFoot?,
    val turn: Int,
    val round: Int,
    val phase: TrainGamePhase = TrainGamePhase.Playing,
    val passes: Int = 0,
    val winner: Int? = null,
    val history: List<String> = emptyList(),
) {
    val current: TrainPlayer get() = players[turn]
    val boneyardCount: Int get() = boneyard.size
}

data class ChickenResult(val state: ChickenState, val error: String? = null)

class ChickenfootGame(
    private val playerCount: Int,
    seed: Long = Random.nextLong(),
) {
    private val random = Random(seed)

    companion object {
        const val ROUNDS = 3
        val START_PIPS = listOf(9, 8, 7)
        fun handSize(players: Int): Int = when {
            players <= 3 -> 9
            players <= 5 -> 7
            else -> 5
        }
    }

    fun newMatch(): ChickenState {
        val players = (0 until playerCount).map { TrainPlayer("Pata ${it + 1}", it != 0) }
        return deal(players, round = 1)
    }

    private fun deal(players: List<TrainPlayer>, round: Int): ChickenState {
        val startPip = START_PIPS[(round - 1).coerceIn(0, START_PIPS.lastIndex)]
        var deck = DominoGame.doubleSixDeck(9).filterNot { it.a == startPip && it.b == startPip }.shuffled(random)
        val handSize = handSize(playerCount)
        val dealt = players.map { player ->
            val hand = deck.take(handSize)
            deck = deck.drop(handSize)
            player.copy(hand = hand)
        }
        return ChickenState(
            players = dealt,
            boneyard = deck,
            hubPip = startPip,
            branches = emptyList(),
            foot = ChickenFoot(startPip),
            turn = 0,
            round = round,
            history = listOf("Ronda $round · pata del $startPip"),
        )
    }

    fun playableTargets(state: ChickenState, tile: DominoTile): List<Int> {
        if (state.phase != TrainGamePhase.Playing) return emptyList()
        val foot = state.foot
        if (foot != null) {
            if (!tile.fits(foot.pip)) return emptyList()
            return foot.branches.indices.filter { foot.branches[it].isEmpty() }
        }
        return state.branches.indices.filter { tile.fits(state.branches[it].openEnd(state.hubPip)) }
    }

    fun play(state: ChickenState, tileIndex: Int, target: Int): ChickenResult {
        if (state.phase != TrainGamePhase.Playing) return ChickenResult(state, "chicken.error.over")
        val player = state.players[state.turn]
        val tile = player.hand.getOrNull(tileIndex) ?: return ChickenResult(state, "chicken.error.tile")
        val targets = playableTargets(state, tile)
        if (target !in targets) return ChickenResult(state, "chicken.error.move")
        val players = state.players.toMutableList()
        players[state.turn] = player.copy(hand = player.hand.filterIndexed { index, _ -> index != tileIndex })
        val foot = state.foot
        var branches = state.branches
        var nextFoot = foot
        if (foot != null) {
            val end = foot.pip
            val oriented = if (tile.a == end) tile else tile.flipped()
            val footBranches = foot.branches.toMutableList()
            footBranches[target] = footBranches[target] + oriented
            nextFoot = foot.copy(branches = footBranches)
            if (nextFoot.branches.all { it.isNotEmpty() }) {
                branches = branches + nextFoot.branches.map { ChickenBranch(it) }
                nextFoot = null
            }
        } else {
            val end = state.branches[target].openEnd(state.hubPip)
            val oriented = if (tile.a == end) tile else tile.flipped()
            branches = branches.toMutableList()
            branches[target] = ChickenBranch(branches[target].tiles + oriented)
            if (oriented.a == oriented.b) nextFoot = ChickenFoot(oriented.a)
        }
        val base = state.copy(
            players = players.toList(),
            branches = branches,
            foot = nextFoot,
            passes = 0,
            history = (state.history + "${player.name} juega ${tile.a}-${tile.b}").takeLast(200),
        )
        if (players[state.turn].hand.isEmpty()) return ChickenResult(endRound(base, state.turn))
        if (nextFoot != null) return ChickenResult(base)
        return ChickenResult(advance(base))
    }

    fun draw(state: ChickenState): ChickenResult {
        if (state.phase != TrainGamePhase.Playing) return ChickenResult(state, "chicken.error.over")
        if (state.boneyard.isEmpty()) return ChickenResult(state, "chicken.error.empty")
        val players = state.players.toMutableList()
        players[state.turn] = players[state.turn].copy(hand = players[state.turn].hand + state.boneyard.first())
        return ChickenResult(
            state.copy(
                players = players.toList(),
                boneyard = state.boneyard.drop(1),
                history = (state.history + "${state.players[state.turn].name} roba").takeLast(200),
            ),
        )
    }

    fun pass(state: ChickenState): ChickenResult {
        if (state.phase != TrainGamePhase.Playing) return ChickenResult(state, "chicken.error.over")
        val cleared = state.copy(
            passes = state.passes + 1,
            history = (state.history + "${state.players[state.turn].name} pasa").takeLast(200),
        )
        if (cleared.passes >= playerCount) return ChickenResult(endRound(cleared, null))
        return ChickenResult(advance(cleared))
    }

    private fun advance(state: ChickenState): ChickenState =
        state.copy(turn = (state.turn + 1) % playerCount)

    private fun endRound(state: ChickenState, winner: Int?): ChickenState {
        val players = state.players.mapIndexed { index, player ->
            player.copy(score = player.score + if (index == winner) 0 else player.hand.sumOf { it.pips })
        }
        return state.copy(
            players = players,
            phase = TrainGamePhase.RoundEnd,
            winner = winner,
            history = (state.history + "Fin de ronda ${state.round}").takeLast(200),
        )
    }

    fun nextRound(state: ChickenState): ChickenState {
        if (state.phase != TrainGamePhase.RoundEnd) return state
        if (state.round >= ROUNDS) {
            val best = state.players.indices.minByOrNull { state.players[it].score }
            return state.copy(phase = TrainGamePhase.MatchEnd, winner = best)
        }
        val carried = state.players.map { it.copy(hand = emptyList()) }
        return deal(carried, state.round + 1)
    }

    fun bestMove(state: ChickenState): Pair<Int, Int>? {
        val player = state.players[state.turn]
        return player.hand.mapIndexedNotNull { index, tile ->
            val targets = playableTargets(state, tile)
            if (targets.isEmpty()) null else (index to targets.first()) to tile.pips
        }.maxByOrNull { it.second }?.first
    }

    fun hasPlay(state: ChickenState): Boolean =
        state.players[state.turn].hand.any { playableTargets(state, it).isNotEmpty() }

    fun botStep(state: ChickenState): ChickenState? {
        if (state.phase != TrainGamePhase.Playing) return null
        val move = bestMove(state)
        if (move != null) return play(state, move.first, move.second).state
        if (state.boneyard.isNotEmpty()) {
            val drawn = draw(state).state
            val after = bestMove(drawn)
            if (after != null) return play(drawn, after.first, after.second).state
            return pass(drawn).state
        }
        return pass(state).state
    }
}
