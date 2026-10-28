package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Dominó doble 6 para la mesa nativa de Compose.
 *
 * Reglas: 2 jugadores reciben 10 fichas; 3–4 reciben 7. Sale el doble más
 * alto; si no hay dobles, la ficha de más puntos. Si no puedes jugar, robas
 * del pozo; si el pozo está vacío, pasas. La mano termina cuando alguien se
 * queda sin fichas o cuando todos pasan (tranca). En 4 jugadores se puede
 * jugar por parejas (0–2 contra 1–3). Meta configurable, por defecto 100.
 */
enum class DominoEnd { Left, Right }

enum class DominoMoveType { Play, Draw, Pass, NextHand }

enum class DominoPhase { Playing, HandEnd, MatchEnd }

data class DominoTile(val a: Int, val b: Int) {
    val id: String get() = if (a <= b) "$a-$b" else "$b-$a"
    val pips: Int get() = a + b

    fun flipped(): DominoTile = DominoTile(b, a)

    fun fits(value: Int?): Boolean = value != null && (a == value || b == value)

    fun otherThan(value: Int): Int = if (a == value) b else a
}

data class DominoPlayer(
    val name: String,
    val isBot: Boolean,
    val team: Int? = null,
    val hand: List<DominoTile> = emptyList(),
    val score: Int = 0,
)

data class DominoPlay(val playerIndex: Int, val tile: DominoTile, val end: DominoEnd)

data class DominoMove(
    val type: DominoMoveType,
    val tileIndex: Int? = null,
    val end: DominoEnd? = null,
)

data class DominoState(
    val players: List<DominoPlayer>,
    val boneyard: List<DominoTile>,
    val chain: List<DominoTile>,
    val left: Int?,
    val right: Int?,
    val turn: Int,
    val passes: Int,
    val handNo: Int,
    val target: Int,
    val teams: Boolean,
    val phase: DominoPhase,
    val winnerIndices: List<Int> = emptyList(),
    val handWinnerIndex: Int? = null,
    val lastPlay: DominoPlay? = null,
    val messageKey: String = "domino.msgStart",
    val messageParams: Map<String, Any?> = emptyMap(),
) {
    val currentPlayer: DominoPlayer? get() = players.getOrNull(turn)
    val boneyardCount: Int get() = boneyard.size
}

data class DominoResult(val state: DominoState, val error: String? = null)

class DominoGame(
    private val playerCount: Int,
    private val teams: Boolean,
    private val target: Int = 100,
    seed: Long = Random.nextLong(),
) {
    private val random = Random(seed)

    companion object {
        val BOT_NAMES = listOf("El Silbón", "La Sayona", "El Hachero", "La Llorona")
        val BOT_EMOJI = listOf("🎩", "👻", "🪓", "🕯️")

        fun doubleSixDeck(maxPip: Int = 6): List<DominoTile> {
            val tiles = mutableListOf<DominoTile>()
            for (a in 0..maxPip) {
                for (b in a..maxPip) tiles.add(DominoTile(a, b))
            }
            return tiles
        }

        fun handPips(hand: List<DominoTile>): Int = hand.sumOf { it.pips }

        fun teamPips(players: List<DominoPlayer>, team: Int): Int =
            players.filter { it.team == team }.sumOf { handPips(it.hand) }

        fun teamScore(players: List<DominoPlayer>, team: Int): Int =
            players.filter { it.team == team }.maxOfOrNull { it.score } ?: 0
    }

    fun newMatch(): DominoState {
        val players = (0 until playerCount.coerceIn(2, 4)).map { index ->
            DominoPlayer(
                name = if (index == 0) "Tú" else BOT_NAMES[(index - 1) % BOT_NAMES.size],
                isBot = index > 0,
                team = if (teams && playerCount == 4) index % 2 else null,
            )
        }
        val base = DominoState(
            players = players,
            boneyard = emptyList(),
            chain = emptyList(),
            left = null,
            right = null,
            turn = 0,
            passes = 0,
            handNo = 0,
            target = target.coerceAtLeast(30),
            teams = teams && playerCount == 4,
            phase = DominoPhase.Playing,
        )
        return dealHand(base)
    }

    fun dealHand(state: DominoState): DominoState {
        val deck = doubleSixDeck().shuffled(random).toMutableList()
        val handSize = if (state.players.size <= 2) 10 else 7
        val hands = MutableList(state.players.size) { mutableListOf<DominoTile>() }
        repeat(handSize) {
            for (index in state.players.indices) {
                val tile = deck.removeFirstOrNull() ?: return@repeat
                hands[index].add(tile)
            }
        }

        var starter = 0
        var bestTile: DominoTile? = null
        for (double in 6 downTo 0) {
            for (index in hands.indices) {
                val tile = hands[index].firstOrNull { it.a == double && it.b == double }
                if (tile != null) {
                    starter = index
                    bestTile = tile
                    break
                }
            }
            if (bestTile != null) break
        }
        if (bestTile == null) {
            var best = -1
            for (index in hands.indices) {
                for (tile in hands[index]) {
                    val value = tile.pips * 10 + maxOf(tile.a, tile.b)
                    if (value > best) {
                        best = value
                        bestTile = tile
                        starter = index
                    }
                }
            }
        }
        val starterTile = bestTile
        if (starterTile != null) hands[starter].removeAll { it.id == starterTile.id }
        val first = bestTile ?: DominoTile(0, 0)
        val nextPlayers = state.players.mapIndexed { index, player ->
            player.copy(hand = hands[index].toList())
        }
        return state.copy(
            players = nextPlayers,
            boneyard = deck.toList(),
            chain = if (bestTile != null) listOf(first) else emptyList(),
            left = if (bestTile != null) first.a else null,
            right = if (bestTile != null) first.b else null,
            turn = if (bestTile != null) (starter + 1) % state.players.size else 0,
            passes = 0,
            handNo = state.handNo + 1,
            phase = DominoPhase.Playing,
            winnerIndices = emptyList(),
            handWinnerIndex = null,
            lastPlay = bestTile?.let { DominoPlay(starter, it, DominoEnd.Right) },
            messageKey = "domino.msgStart",
            messageParams = mapOf("n" to state.handNo + 1),
        )
    }

    fun playableEnds(state: DominoState, tile: DominoTile): List<DominoEnd> {
        val ends = mutableListOf<DominoEnd>()
        if (tile.fits(state.left)) ends.add(DominoEnd.Left)
        if (tile.fits(state.right)) ends.add(DominoEnd.Right)
        return ends
    }

    fun hasPlayable(state: DominoState, playerIndex: Int): Boolean {
        val hand = state.players.getOrNull(playerIndex)?.hand ?: return false
        return hand.any { playableEnds(state, it).isNotEmpty() }
    }

    fun play(state: DominoState, playerIndex: Int, tileIndex: Int, end: DominoEnd): DominoResult {
        if (state.phase != DominoPhase.Playing) return DominoResult(state, "domino.error.matchOver")
        if (state.turn != playerIndex) return DominoResult(state, "domino.error.notYourTurn")
        val player = state.players.getOrNull(playerIndex) ?: return DominoResult(state, "domino.error.invalid")
        val tile = player.hand.getOrNull(tileIndex) ?: return DominoResult(state, "domino.error.invalid")
        val touching = if (end == DominoEnd.Left) state.left else state.right
        if (touching == null || !tile.fits(touching)) return DominoResult(state, "domino.error.doesNotFit")

        val placed: DominoTile
        val other: Int
        if (end == DominoEnd.Left) {
            placed = if (tile.a == touching) tile.flipped() else tile
            other = if (tile.a == touching) tile.b else tile.a
        } else {
            placed = if (tile.b == touching) tile.flipped() else tile
            other = if (tile.b == touching) tile.a else tile.b
        }
        val newChain = if (end == DominoEnd.Left) listOf(placed) + state.chain else state.chain + placed
        val newHand = player.hand.filterIndexed { index, _ -> index != tileIndex }
        val updatedPlayers = state.players.mapIndexed { index, current ->
            if (index == playerIndex) current.copy(hand = newHand) else current
        }
        val next = state.copy(
            players = updatedPlayers,
            chain = newChain,
            left = if (end == DominoEnd.Left) other else state.left,
            right = if (end == DominoEnd.Right) other else state.right,
            passes = 0,
            lastPlay = DominoPlay(playerIndex, placed, end),
            messageKey = "domino.msgPlayed",
            messageParams = mapOf("p" to player.name, "a" to placed.a, "b" to placed.b),
        )
        if (newHand.isEmpty()) {
            return DominoResult(endHand(next, playerIndex))
        }
        return DominoResult(next.copy(turn = (state.turn + 1) % state.players.size))
    }

    fun draw(state: DominoState, playerIndex: Int): DominoResult {
        if (state.phase != DominoPhase.Playing) return DominoResult(state, "domino.error.matchOver")
        if (state.turn != playerIndex) return DominoResult(state, "domino.error.notYourTurn")
        if (hasPlayable(state, playerIndex)) return DominoResult(state, "domino.error.mustPlay")
        val tile = state.boneyard.firstOrNull() ?: return DominoResult(state, "domino.error.boneyardEmpty")
        val boneyard = state.boneyard.drop(1)
        val player = state.players[playerIndex]
        val players = state.players.mapIndexed { index, current ->
            if (index == playerIndex) current.copy(hand = current.hand + tile) else current
        }
        val playable = playableEnds(state, tile).isNotEmpty()
        if (playable) {
            return DominoResult(
                state.copy(
                    players = players,
                    boneyard = boneyard,
                    messageKey = "domino.msgDrewPlayable",
                    messageParams = mapOf("p" to player.name, "a" to tile.a, "b" to tile.b),
                ),
            )
        }
        val passes = state.passes + 1
        val advanced = state.copy(
            players = players,
            boneyard = boneyard,
            passes = passes,
            turn = (state.turn + 1) % state.players.size,
            messageKey = "domino.msgDrewPassed",
            messageParams = mapOf("p" to player.name),
        )
        return if (passes >= state.players.size) {
            DominoResult(endHand(advanced.copy(boneyard = emptyList()), null))
        } else {
            DominoResult(advanced)
        }
    }

    fun pass(state: DominoState, playerIndex: Int): DominoResult {
        if (state.phase != DominoPhase.Playing) return DominoResult(state, "domino.error.matchOver")
        if (state.turn != playerIndex) return DominoResult(state, "domino.error.notYourTurn")
        if (hasPlayable(state, playerIndex)) return DominoResult(state, "domino.error.mustPlay")
        if (state.boneyard.isNotEmpty()) return DominoResult(state, "domino.error.mustDraw")
        val passes = state.passes + 1
        val advanced = state.copy(
            passes = passes,
            turn = (state.turn + 1) % state.players.size,
            messageKey = "domino.msgPassed",
            messageParams = mapOf("p" to state.players[playerIndex].name),
        )
        return if (passes >= state.players.size) {
            DominoResult(endHand(advanced, null))
        } else {
            DominoResult(advanced)
        }
    }

    fun nextHand(state: DominoState): DominoState {
        if (state.phase != DominoPhase.HandEnd) return state
        return dealHand(state)
    }

    fun botMove(state: DominoState): DominoMove? {
        if (state.phase != DominoPhase.Playing) return null
        val index = state.turn
        val player = state.players.getOrNull(index) ?: return null
        if (!player.isBot) return null
        val options = player.hand.mapIndexedNotNull { tileIndex, tile ->
            playableEnds(state, tile).map { end -> Triple(tileIndex, tile, end) }
        }.flatten()
        if (options.isEmpty()) {
            return if (state.boneyard.isNotEmpty()) DominoMove(DominoMoveType.Draw) else DominoMove(DominoMoveType.Pass)
        }
        val scored = options.map { (tileIndex, tile, end) ->
            val touching = if (end == DominoEnd.Left) state.left else state.right
            val other = if (touching != null) tile.otherThan(touching) else 0
            val future = player.hand.count { it.id != tile.id && (it.a == other || it.b == other) }
            val score = tile.pips * 2 + future + random.nextDouble() * 0.5
            Triple(tileIndex, end, score)
        }
        val chosen = scored.maxByOrNull { it.third } ?: return null
        return DominoMove(DominoMoveType.Play, chosen.first, chosen.second)
    }

    private fun endHand(state: DominoState, winnerIndex: Int?): DominoState {
        val winners: List<Int>
        val points: Int
        val reasonKey: String
        if (winnerIndex != null) {
            val winner = state.players[winnerIndex]
            winners = if (state.teams) {
                state.players.indices.filter { state.players[it].team == winner.team }
            } else {
                listOf(winnerIndex)
            }
            val losers = state.players.indices.filter { it !in winners }
            points = losers.sumOf { handPips(state.players[it].hand) }
            reasonKey = "domino.reason.out"
        } else {
            if (state.teams) {
                val totals = (0..1).associateWith { team -> teamPips(state.players, team) }
                val min = totals.values.minOrNull() ?: 0
                val winningTeams = totals.filterValues { it == min }.keys
                if (winningTeams.size != 1) {
                    return state.copy(
                        phase = DominoPhase.MatchEnd,
                        winnerIndices = emptyList(),
                        messageKey = "domino.msgDraw",
                        messageParams = emptyMap(),
                    )
                }
                val team = winningTeams.first()
                winners = state.players.indices.filter { state.players[it].team == team }
                points = state.players.indices.filter { it !in winners }.sumOf { handPips(state.players[it].hand) }
            } else {
                val totals = state.players.map { handPips(it.hand) }
                val min = totals.minOrNull() ?: 0
                val winning = totals.indices.filter { totals[it] == min }
                if (winning.size != 1) {
                    return state.copy(
                        phase = DominoPhase.MatchEnd,
                        winnerIndices = emptyList(),
                        messageKey = "domino.msgDraw",
                        messageParams = emptyMap(),
                    )
                }
                winners = winning
                points = totals.filterIndexed { index, _ -> index !in winners }.sum()
            }
            reasonKey = "domino.reason.blocked"
        }

        val scores = state.players.mapIndexed { index, player ->
            if (index in winners) player.copy(score = player.score + points) else player
        }
        val withScores = state.copy(players = scores)
        val best = scores.maxOfOrNull { it.score } ?: 0
        val matchWinners = scores.indices.filter { scores[it].score == best }
        return if (best >= state.target) {
            withScores.copy(
                phase = DominoPhase.MatchEnd,
                winnerIndices = matchWinners,
                handWinnerIndex = winnerIndex,
                messageKey = "domino.msgMatchWin",
                messageParams = mapOf(
                    "p" to matchWinners.joinToString(" · ") { scores[it].name },
                    "points" to points,
                ),
            )
        } else {
            withScores.copy(
                phase = DominoPhase.HandEnd,
                handWinnerIndex = winnerIndex,
                messageKey = "domino.msgHandEnd",
                messageParams = mapOf("points" to points),
            )
        }
    }

    fun scoreForDisplay(state: DominoState, playerIndex: Int): Int {
        val player = state.players[playerIndex]
        return if (state.teams && player.team != null) {
            teamScore(state.players, player.team)
        } else {
            player.score
        }
    }

    fun reasonLabelKey(state: DominoState): String =
        if (state.handWinnerIndex != null) "domino.reason.out" else "domino.reason.blocked"
}
