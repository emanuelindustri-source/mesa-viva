package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Mahjong para Mesa Viva.
 *
 * Un solo motor sostiene las tres mesas del catálogo:
 *  - [MahjongVariant.American]: 152 piezas con flores y comodines, sin chi.
 *  - [MahjongVariant.Riichi]: 136 piezas, chi, riichi, furiten y yaku.
 *  - [MahjongVariant.Cards]: la edición de naipes del mahjong americano.
 *
 * La partida dura cuatro manos (vuelta del Este). Cada mano reparte un muro
 * nuevo y conserva los puntos. Gana la mano quien complete cuatro melds y un
 * par; en Riichi además necesita al menos un yaku.
 */
enum class MahjongVariant { American, Riichi, Cards }

enum class MahjongSuit { Dots, Bams, Cracks, Wind, Dragon, Flower, Joker }

enum class MahjongPhase { Play, HandEnd, MatchEnd }

enum class MahjongMeldKind { Chi, Pon, Kan }

enum class MahjongClaimKind { Ron, Pon, Chi, Kan }

data class MahjongTile(val id: Int, val suit: MahjongSuit, val rank: Int) {
    val isJoker: Boolean get() = suit == MahjongSuit.Joker
    val isFlower: Boolean get() = suit == MahjongSuit.Flower
    val isHonor: Boolean get() = suit == MahjongSuit.Wind || suit == MahjongSuit.Dragon
    val isTerminal: Boolean get() = suit in setOf(MahjongSuit.Dots, MahjongSuit.Bams, MahjongSuit.Cracks) && rank in setOf(1, 9)

    /** Código estable para historial, pruebas y códecs. */
    val code: String
        get() = when (suit) {
            MahjongSuit.Dots -> "${rank}D"
            MahjongSuit.Bams -> "${rank}B"
            MahjongSuit.Cracks -> "${rank}C"
            MahjongSuit.Wind -> "W$rank"
            MahjongSuit.Dragon -> "G$rank"
            MahjongSuit.Flower -> "F$rank"
            MahjongSuit.Joker -> "J$rank"
        }

    val shortLabel: String
        get() = when (suit) {
            MahjongSuit.Dots -> "$rank●"
            MahjongSuit.Bams -> "$rank❚"
            MahjongSuit.Cracks -> "${rank}萬"
            MahjongSuit.Wind -> listOf("東", "南", "西", "北")[rank - 1]
            MahjongSuit.Dragon -> listOf("中", "發", "白")[rank - 1]
            MahjongSuit.Flower -> "❀"
            MahjongSuit.Joker -> "JOKER"
        }
}

data class MahjongMeld(
    val kind: MahjongMeldKind,
    val tiles: List<MahjongTile>,
    val from: Int,
)

data class MahjongPlayer(
    val seat: Int,
    val hand: List<MahjongTile> = emptyList(),
    val melds: List<MahjongMeld> = emptyList(),
    val discards: List<MahjongTile> = emptyList(),
    val flowers: List<MahjongTile> = emptyList(),
    val riichi: Boolean = false,
    val score: Int = 0,
)

data class MahjongClaim(
    val kind: MahjongClaimKind,
    val seat: Int,
    val tiles: List<MahjongTile>,
)

data class MahjongState(
    val variant: MahjongVariant,
    val players: List<MahjongPlayer>,
    val wall: List<MahjongTile>,
    val turn: Int,
    val dealer: Int,
    val round: Int,
    val phase: MahjongPhase = MahjongPhase.Play,
    val drawn: MahjongTile? = null,
    val lastDiscard: MahjongTile? = null,
    val lastDiscardBy: Int = -1,
    val pending: List<MahjongClaim> = emptyList(),
    val winner: Int? = null,
    val winTile: MahjongTile? = null,
    val winLabel: String = "",
    val history: List<String> = emptyList(),
) {
    val wallCount: Int get() = wall.size
    val current: MahjongPlayer get() = players[turn]
    val humanSeat: Int get() = 0
}

data class MahjongResult(val state: MahjongState, val error: String? = null)

class MahjongGame(
    private val variant: MahjongVariant,
    seed: Long = Random.nextLong(),
) {
    private val random = Random(seed)

    companion object {
        const val HANDS = 4
        const val SEATS = 4
        val BOT_NAMES = listOf("La Sayona", "El Silbón", "Doña Bárbara", "El Carrao")
        val SEAT_NAMES = listOf("Este", "Sur", "Oeste", "Norte")
        val WINDS = listOf("Este", "Sur", "Oeste", "Norte")

        fun standardTypes(): List<MahjongTile> {
            val types = mutableListOf<MahjongTile>()
            var id = 0
            for (suit in listOf(MahjongSuit.Dots, MahjongSuit.Bams, MahjongSuit.Cracks)) {
                for (rank in 1..9) types += MahjongTile(id++, suit, rank)
            }
            for (rank in 1..4) types += MahjongTile(id++, MahjongSuit.Wind, rank)
            for (rank in 1..3) types += MahjongTile(id++, MahjongSuit.Dragon, rank)
            return types
        }

        fun tileName(tile: MahjongTile): String = when (tile.suit) {
            MahjongSuit.Dots -> "${tile.rank} de círculos"
            MahjongSuit.Bams -> "${tile.rank} de bambú"
            MahjongSuit.Cracks -> "${tile.rank} de caracteres"
            MahjongSuit.Wind -> listOf("Este", "Sur", "Oeste", "Norte")[tile.rank - 1]
            MahjongSuit.Dragon -> listOf("Dragón rojo", "Dragón verde", "Dragón blanco")[tile.rank - 1]
            MahjongSuit.Flower -> "Flor ${tile.rank}"
            MahjongSuit.Joker -> "Comodín"
        }
    }

    private fun buildWall(): List<MahjongTile> {
        val tiles = mutableListOf<MahjongTile>()
        var id = 0
        for (suit in listOf(MahjongSuit.Dots, MahjongSuit.Bams, MahjongSuit.Cracks)) {
            for (rank in 1..9) repeat(4) { tiles += MahjongTile(id++, suit, rank) }
        }
        for (rank in 1..4) repeat(4) { tiles += MahjongTile(id++, MahjongSuit.Wind, rank) }
        for (rank in 1..3) repeat(4) { tiles += MahjongTile(id++, MahjongSuit.Dragon, rank) }
        if (variant != MahjongVariant.Riichi) {
            for (rank in 1..8) tiles += MahjongTile(id++, MahjongSuit.Flower, rank)
            for (rank in 1..8) tiles += MahjongTile(id++, MahjongSuit.Joker, rank)
        }
        return tiles.shuffled(random)
    }

    fun newMatch(): MahjongState {
        val players = (0 until SEATS).map { MahjongPlayer(seat = it, score = 250) }
        return deal(players, dealer = 0, round = 1)
    }

    private fun deal(players: List<MahjongPlayer>, dealer: Int, round: Int): MahjongState {
        var wall = buildWall()
        val dealt = players.toMutableList()
        for (seat in 0 until SEATS) {
            val hand = mutableListOf<MahjongTile>()
            val flowers = mutableListOf<MahjongTile>()
            repeat(13) {
                var (tile, rest) = drawOne(wall)
                wall = rest
                while (tile != null && tile.isFlower) {
                    flowers += tile
                    val replacement = drawOne(wall)
                    tile = replacement.first
                    wall = replacement.second
                }
                if (tile != null) hand += tile
            }
            dealt[seat] = dealt[seat].copy(hand = hand, flowers = flowers)
        }
        val (opening, rest) = drawOne(wall)
        wall = rest
        var openingTile = opening
        while (openingTile != null && openingTile.isFlower) {
            dealt[dealer] = dealt[dealer].copy(flowers = dealt[dealer].flowers + openingTile)
            val (next, rest2) = drawOne(wall)
            wall = rest2
            openingTile = next
        }
        if (openingTile != null) dealt[dealer] = dealt[dealer].copy(hand = dealt[dealer].hand + openingTile)
        return MahjongState(
            variant = variant,
            players = dealt.toList(),
            wall = wall,
            turn = dealer,
            dealer = dealer,
            round = round,
            drawn = openingTile,
            history = listOf("Mano ${round} · reparte ${SEAT_NAMES[dealer]}"),
        )
    }

    private fun drawOne(wall: List<MahjongTile>): Pair<MahjongTile?, List<MahjongTile>> {
        if (wall.isEmpty()) return null to wall
        return wall.first() to wall.drop(1)
    }

    /** Roba una pieza saltando flores (bonus) y comodines no aplican. */
    private fun drawFor(state: MahjongState, seat: Int): MahjongState {
        var wall = state.wall
        val players = state.players.toMutableList()
        var player = players[seat]
        var tile: MahjongTile? = null
        while (wall.isNotEmpty()) {
            val next = wall.first()
            wall = wall.drop(1)
            if (next.isFlower) {
                player = player.copy(flowers = player.flowers + next)
                continue
            }
            tile = next
            break
        }
        if (tile == null) {
            players[seat] = player
            return state.copy(
                players = players.toList(),
                wall = wall,
                phase = MahjongPhase.HandEnd,
                winner = null,
                winLabel = "empate",
                history = (state.history + "El muro se agotó: mano empatada").takeLast(160),
            )
        }
        players[seat] = player.copy(hand = player.hand + tile)
        return state.copy(players = players.toList(), wall = wall, drawn = tile)
    }

    private fun advance(state: MahjongState): MahjongState {
        if (state.phase != MahjongPhase.Play) return state
        val next = (state.turn + 1) % SEATS
        val rotated = state.copy(turn = next, drawn = null, lastDiscard = null, lastDiscardBy = -1, pending = emptyList())
        return drawFor(rotated, next)
    }

    fun legalDiscards(state: MahjongState): List<MahjongTile> =
        if (state.phase == MahjongPhase.Play && state.pending.isEmpty()) state.players[state.turn].hand else emptyList()

    fun discard(state: MahjongState, tileId: Int): MahjongResult {
        if (state.phase != MahjongPhase.Play) return MahjongResult(state, "mahjong.error.over")
        if (state.pending.isNotEmpty()) return MahjongResult(state, "mahjong.error.pending")
        val player = state.players[state.turn]
        val tile = player.hand.firstOrNull { it.id == tileId }
            ?: return MahjongResult(state, "mahjong.error.tile")
        val players = state.players.toMutableList()
        players[state.turn] = player.copy(
            hand = player.hand.filterNot { it.id == tileId },
            discards = player.discards + tile,
        )
        val base = state.copy(
            players = players.toList(),
            drawn = null,
            lastDiscard = tile,
            lastDiscardBy = state.turn,
            history = (state.history + "${SEAT_NAMES[state.turn]} descarta ${tile.code}").takeLast(160),
        )
        val claims = claimsFor(base, state.turn, tile)
        if (claims.isEmpty()) return MahjongResult(advance(base))
        return MahjongResult(base.copy(pending = claims))
    }

    fun claimsFor(state: MahjongState, from: Int, tile: MahjongTile): List<MahjongClaim> {
        val claims = mutableListOf<MahjongClaim>()
        for (seat in 0 until SEATS) {
            if (seat == from) continue
            val player = state.players[seat]
            val yakuOk = variant != MahjongVariant.Riichi ||
                yakuFor(player, tile, tsumo = false, state = state).isNotEmpty()
            if (canWinHand(player.hand + tile, player.melds, variant) && yakuOk &&
                !isFuriten(player.hand, player.discards, player.melds, variant)
            ) {
                claims += MahjongClaim(MahjongClaimKind.Ron, seat, listOf(tile))
            }
            if (!player.riichi) {
                val copies = player.hand.count { it.suit == tile.suit && it.rank == tile.rank }
                if (copies >= 3) claims += MahjongClaim(MahjongClaimKind.Kan, seat, player.hand.filter { it.suit == tile.suit && it.rank == tile.rank })
                if (copies >= 2) claims += MahjongClaim(MahjongClaimKind.Pon, seat, player.hand.filter { it.suit == tile.suit && it.rank == tile.rank }.take(2))
            }
            if (variant == MahjongVariant.Riichi && seat == (from + 1) % SEATS && !player.riichi && tile.suit in setOf(MahjongSuit.Dots, MahjongSuit.Bams, MahjongSuit.Cracks)) {
                val first = player.hand.firstOrNull { it.suit == tile.suit && it.rank == tile.rank - 1 }
                val second = player.hand.firstOrNull { it.suit == tile.suit && it.rank == tile.rank - 2 }
                if (first != null && second != null) claims += MahjongClaim(MahjongClaimKind.Chi, seat, listOf(first, second))
                val up = player.hand.firstOrNull { it.suit == tile.suit && it.rank == tile.rank + 1 }
                val up2 = player.hand.firstOrNull { it.suit == tile.suit && it.rank == tile.rank + 2 }
                if (up != null && up2 != null) claims += MahjongClaim(MahjongClaimKind.Chi, seat, listOf(up, up2))
                if (first != null && up != null) claims += MahjongClaim(MahjongClaimKind.Chi, seat, listOf(first, up))
            }
        }
        return claims.sortedBy {
            when (it.kind) {
                MahjongClaimKind.Ron -> 0
                MahjongClaimKind.Kan -> 1
                MahjongClaimKind.Pon -> 2
                MahjongClaimKind.Chi -> 3
            }
        }
    }

    fun pass(state: MahjongState, seat: Int? = null): MahjongResult {
        if (state.pending.isEmpty()) return MahjongResult(state, "mahjong.error.pending")
        if (seat == null) {
            return MahjongResult(advance(state.copy(pending = emptyList())))
        }
        val remaining = state.pending.filterNot { it.seat == seat }
        if (remaining.isNotEmpty()) return MahjongResult(state.copy(pending = remaining))
        return MahjongResult(advance(state.copy(pending = emptyList())))
    }

    fun claim(state: MahjongState, claim: MahjongClaim): MahjongResult {
        if (state.phase != MahjongPhase.Play) return MahjongResult(state, "mahjong.error.over")
        if (claim !in state.pending) return MahjongResult(state, "mahjong.error.pending")
        val tile = state.lastDiscard ?: return MahjongResult(state, "mahjong.error.pending")
        val players = state.players.toMutableList()
        val player = players[claim.seat]
        when (claim.kind) {
            MahjongClaimKind.Ron -> return MahjongResult(settle(state, claim.seat, tile, tsumo = false))
            MahjongClaimKind.Pon, MahjongClaimKind.Chi -> {
                val used = claim.tiles.map { it.id }.toSet()
                val meld = MahjongMeld(
                    kind = if (claim.kind == MahjongClaimKind.Pon) MahjongMeldKind.Pon else MahjongMeldKind.Chi,
                    tiles = claim.tiles + tile,
                    from = state.lastDiscardBy,
                )
                players[claim.seat] = player.copy(
                    hand = player.hand.filterNot { it.id in used },
                    melds = player.melds + meld,
                )
                players[state.lastDiscardBy] = players[state.lastDiscardBy].copy(
                    discards = players[state.lastDiscardBy].discards.dropLast(1),
                )
                val label = if (claim.kind == MahjongClaimKind.Pon) "pon" else "chi"
                return MahjongResult(
                    state.copy(
                        players = players.toList(),
                        turn = claim.seat,
                        drawn = null,
                        pending = emptyList(),
                        lastDiscard = null,
                        lastDiscardBy = -1,
                        history = (state.history + "${SEAT_NAMES[claim.seat]} $label ${tile.code}").takeLast(160),
                    ),
                )
            }
            MahjongClaimKind.Kan -> {
                val used = claim.tiles.map { it.id }.toSet()
                val meld = MahjongMeld(MahjongMeldKind.Kan, claim.tiles + tile, state.lastDiscardBy)
                players[claim.seat] = player.copy(
                    hand = player.hand.filterNot { it.id in used },
                    melds = player.melds + meld,
                )
                players[state.lastDiscardBy] = players[state.lastDiscardBy].copy(
                    discards = players[state.lastDiscardBy].discards.dropLast(1),
                )
                val claimed = state.copy(
                    players = players.toList(),
                    turn = claim.seat,
                    drawn = null,
                    pending = emptyList(),
                    lastDiscard = null,
                    lastDiscardBy = -1,
                    history = (state.history + "${SEAT_NAMES[claim.seat]} kan ${tile.code}").takeLast(160),
                )
                return MahjongResult(drawFor(claimed, claim.seat))
            }
        }
    }

    fun declareRiichi(state: MahjongState, tileId: Int): MahjongResult {
        if (variant != MahjongVariant.Riichi) return MahjongResult(state, "mahjong.error.variant")
        if (state.phase != MahjongPhase.Play || state.pending.isNotEmpty()) return MahjongResult(state, "mahjong.error.pending")
        val player = state.players[state.turn]
        if (player.melds.isNotEmpty() || player.riichi) return MahjongResult(state, "mahjong.error.riichi")
        if (player.score < 10) return MahjongResult(state, "mahjong.error.points")
        val tile = player.hand.firstOrNull { it.id == tileId } ?: return MahjongResult(state, "mahjong.error.tile")
        val rest = player.hand.filterNot { it.id == tileId }
        if (!isTenpai(rest, player.melds, variant)) return MahjongResult(state, "mahjong.error.notTenpai")
        val players = state.players.toMutableList()
        players[state.turn] = player.copy(riichi = true, score = player.score - 10)
        val ready = state.copy(players = players.toList(), history = (state.history + "${SEAT_NAMES[state.turn]} declara riichi").takeLast(160))
        return discard(ready, tileId)
    }

    fun declareTsumo(state: MahjongState): MahjongResult {
        if (state.phase != MahjongPhase.Play) return MahjongResult(state, "mahjong.error.over")
        val player = state.players[state.turn]
        val tile = state.drawn ?: return MahjongResult(state, "mahjong.error.noDraw")
        if (!canWinHand(player.hand, player.melds, variant)) return MahjongResult(state, "mahjong.error.noWin")
        if (variant == MahjongVariant.Riichi && yakuFor(player, tile, tsumo = true, state = state).isEmpty()) {
            return MahjongResult(state, "mahjong.error.noYaku")
        }
        return MahjongResult(settle(state, state.turn, tile, tsumo = true))
    }

    private fun settle(state: MahjongState, winnerSeat: Int, tile: MahjongTile, tsumo: Boolean): MahjongState {
        val winner = state.players[winnerSeat]
        val yaku = if (variant == MahjongVariant.Riichi) yakuFor(winner, tile, tsumo, state) else listOf("mano estándar")
        var han = yaku.size + if (tsumo) 1 else 0
        if (winner.riichi) han += 1
        val bonus = winner.flowers.size * 2
        val points = if (variant == MahjongVariant.Riichi) {
            (8 * (1 shl han.coerceAtMost(6))) + bonus
        } else {
            40 + han * 10 + bonus
        }
        val players = state.players.toMutableList()
        // La mesa es de suma cero: cada rival paga la mano y el ganador cobra.
        val payment = points.coerceAtLeast(1)
        for (index in players.indices) {
            if (index == winnerSeat) {
                players[index] = players[index].copy(score = players[index].score + payment * (SEATS - 1))
            } else {
                players[index] = players[index].copy(score = players[index].score - payment)
            }
        }
        val label = if (variant == MahjongVariant.Riichi) yaku.joinToString(" · ") else "4 melds + par"
        return state.copy(
            players = players.toList(),
            phase = MahjongPhase.HandEnd,
            winner = winnerSeat,
            winTile = tile,
            winLabel = label,
            pending = emptyList(),
            history = (state.history + "${SEAT_NAMES[winnerSeat]} gana con ${tile.code} · $label (+$points)").takeLast(160),
        )
    }

    fun nextHand(state: MahjongState): MahjongState {
        if (state.phase != MahjongPhase.HandEnd) return state
        if (state.round >= HANDS) {
            return state.copy(
                phase = MahjongPhase.MatchEnd,
                history = (state.history + "Partida terminada · gana ${SEAT_NAMES[state.players.indices.maxByOrNull { state.players[it].score } ?: 0]}").takeLast(160),
            )
        }
        val nextDealer = if (state.winner == state.dealer) state.dealer else (state.dealer + 1) % SEATS
        val carried = state.players.map { it.copy(hand = emptyList(), melds = emptyList(), discards = emptyList(), flowers = emptyList(), riichi = false) }
        return deal(carried, nextDealer, state.round + 1)
    }

    fun matchWinner(state: MahjongState): Int? =
        if (state.players.isEmpty()) null else state.players.indices.maxByOrNull { state.players[it].score }

    /** Jugada del bot: descarta la pieza menos útil. */
    fun botDiscard(state: MahjongState): Int? {
        val player = state.players[state.turn]
        if (player.hand.isEmpty()) return null
        val scored = player.hand.map { tile ->
            val copies = player.hand.count { it.suit == tile.suit && it.rank == tile.rank }
            val neighbors = player.hand.count {
                it.suit == tile.suit && kotlin.math.abs(it.rank - tile.rank) in 1..2
            }
            val value = copies * 4 + neighbors * 2 + when {
                tile.isHonor -> 1
                tile.rank in setOf(1, 9) -> 0
                else -> 2
            }
            tile.id to value
        }
        return scored.minByOrNull { it.second }?.first
    }

    fun botClaim(state: MahjongState): MahjongClaim? {
        if (state.pending.isEmpty()) return null
        return state.pending
            .filter { it.seat != 0 }
            .minByOrNull {
                when (it.kind) {
                    MahjongClaimKind.Ron -> 0
                    MahjongClaimKind.Kan -> 1
                    MahjongClaimKind.Pon -> 2
                    MahjongClaimKind.Chi -> 3
                }
            }
    }

    fun humanClaims(state: MahjongState): List<MahjongClaim> =
        state.pending.filter { it.seat == 0 }

    fun isTenpai(hand: List<MahjongTile>, melds: List<MahjongMeld>, variant: MahjongVariant): Boolean {
        val types = standardTypes()
        return types.any { type ->
            canWinHand(hand + MahjongTile(-1, type.suit, type.rank), melds, variant)
        }
    }

    fun isFuriten(hand: List<MahjongTile>, discards: List<MahjongTile>, melds: List<MahjongMeld>, variant: MahjongVariant): Boolean =
        discards.any { tile -> canWinHand(hand + tile, melds, variant) }

    fun canWinHand(hand: List<MahjongTile>, melds: List<MahjongMeld>, variant: MahjongVariant): Boolean {
        val concealed = hand.filterNot { it.isFlower }
        val needs = 4 - melds.size
        if (needs < 0) return false
        if (concealed.size != needs * 3 + 2) return false
        val allowSequences = variant == MahjongVariant.Riichi
        val allowJokers = variant != MahjongVariant.Riichi
        if (!allowJokers && concealed.any { it.isJoker }) return false
        if (variant == MahjongVariant.Riichi && isSevenPairs(concealed)) return true
        val sorted = concealed.sortedWith(compareBy({ it.suit.ordinal }, { it.rank }))
        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                val a = sorted[i]
                val b = sorted[j]
                val same = a.suit == b.suit && a.rank == b.rank
                val wildPair = allowJokers && (a.isJoker || b.isJoker)
                if (same || wildPair) {
                    val rest = sorted.toMutableList().also { it.removeAt(j); it.removeAt(i) }
                    if (formSets(rest, needs, allowSequences, allowJokers)) return true
                }
            }
        }
        return false
    }

    private fun isSevenPairs(tiles: List<MahjongTile>): Boolean {
        if (tiles.size != 14) return false
        val counts = tiles.groupingBy { it.suit to it.rank }.eachCount()
        return counts.values.count { it == 2 } == 7 && counts.values.all { it == 2 }
    }

    private fun formSets(tiles: MutableList<MahjongTile>, need: Int, sequences: Boolean, jokers: Boolean): Boolean {
        if (need == 0) return tiles.isEmpty()
        if (tiles.isEmpty()) return false
        // Tripletas con la primera pieza.
        val first = tiles[0]
        if (first.isJoker) {
            // Comodín: trío con dos piezas iguales o con otro comodín.
            for (i in 1 until tiles.size) {
                for (j in i + 1 until tiles.size) {
                    val a = tiles[i]
                    val b = tiles[j]
                    if ((a.suit == b.suit && a.rank == b.rank) || (a.isJoker && b.isJoker)) {
                        val rest = tiles.toMutableList().also { it.removeAt(j); it.removeAt(i); it.removeAt(0) }
                        if (formSets(rest, need - 1, sequences, jokers)) return true
                    }
                }
            }
            if (sequences) {
                for (i in 1 until tiles.size) {
                    for (j in i + 1 until tiles.size) {
                        val a = tiles[i]
                        val b = tiles[j]
                        if (a.suit == b.suit && !a.isHonor && kotlin.math.abs(a.rank - b.rank) <= 2 && a.rank != b.rank) {
                            val rest = tiles.toMutableList().also { it.removeAt(j); it.removeAt(i); it.removeAt(0) }
                            if (formSets(rest, need - 1, sequences, jokers)) return true
                        }
                    }
                }
            }
            return false
        }
        val same = tiles.indices.filter { tiles[it].suit == first.suit && tiles[it].rank == first.rank }
        if (same.size >= 3) {
            val rest = tiles.toMutableList().also { list -> same.take(3).sortedDescending().forEach { list.removeAt(it) } }
            if (formSets(rest, need - 1, sequences, jokers)) return true
        }
        if (jokers) {
            val jokersIdx = tiles.indices.filter { tiles[it].isJoker }
            if (same.size >= 2 && jokersIdx.isNotEmpty()) {
                val remove = (same.take(2) + jokersIdx.first()).sortedDescending()
                val rest = tiles.toMutableList().also { list -> remove.forEach { list.removeAt(it) } }
                if (formSets(rest, need - 1, sequences, jokers)) return true
            }
            if (same.size >= 1 && jokersIdx.size >= 2) {
                val remove = (same.take(1) + jokersIdx.take(2)).sortedDescending()
                val rest = tiles.toMutableList().also { list -> remove.forEach { list.removeAt(it) } }
                if (formSets(rest, need - 1, sequences, jokers)) return true
            }
        }
        if (sequences && !first.isHonor) {
            val second = tiles.firstOrNull { it.suit == first.suit && it.rank == first.rank + 1 && !it.isJoker }
            val third = tiles.firstOrNull { it.suit == first.suit && it.rank == first.rank + 2 && !it.isJoker }
            if (second != null && third != null) {
                val rest = tiles.toMutableList().also {
                    it.remove(third); it.remove(second); it.remove(first)
                }
                if (formSets(rest, need - 1, sequences, jokers)) return true
            }
            if (jokers) {
                val jokerIdx = tiles.indexOfFirst { it.isJoker }
                if (jokerIdx > 0 && second != null) {
                    val rest = tiles.toMutableList().also {
                        it.removeAt(jokerIdx); it.remove(second); it.remove(first)
                    }
                    if (formSets(rest, need - 1, sequences, jokers)) return true
                }
                if (jokerIdx > 0 && third != null) {
                    val rest = tiles.toMutableList().also {
                        it.removeAt(jokerIdx); it.remove(third); it.remove(first)
                    }
                    if (formSets(rest, need - 1, sequences, jokers)) return true
                }
            }
        }
        return false
    }

    fun yakuFor(player: MahjongPlayer, winTile: MahjongTile, tsumo: Boolean, state: MahjongState): List<String> {
        val yaku = mutableListOf<String>()
        val hasWinTile = player.hand.any { it.id == winTile.id }
        val concealed = if (hasWinTile) player.hand else player.hand + winTile
        val all = concealed + player.melds.flatMap { it.tiles }
        if (player.riichi) yaku += "riichi"
        if (tsumo) yaku += "tsumo"
        if (player.melds.isEmpty() && isSevenPairs(concealed)) yaku += "chiitoitsu"
        if (all.none { it.isTerminal || it.isHonor } && all.isNotEmpty()) yaku += "tanyao"
        if (all.all { it.suit == all.first().suit }) yaku += "chinitsu"
        else {
            val suits = all.filter { it.suit in setOf(MahjongSuit.Dots, MahjongSuit.Bams, MahjongSuit.Cracks) }.map { it.suit }.distinct()
            if (suits.size == 1 && all.any { it.isHonor }) yaku += "honitsu"
        }
        if (player.melds.isNotEmpty() && player.melds.all { it.kind != MahjongMeldKind.Chi }) yaku += "toitoi"
        val dragonTriplet = all.groupBy { it.suit to it.rank }.any { (key, tiles) -> key.first == MahjongSuit.Dragon && tiles.size >= 3 }
        if (dragonTriplet) yaku += "yakuhai"
        val seatWind = state.dealer
        val windTriplet = all.groupBy { it.suit to it.rank }.any { (key, tiles) ->
            key.first == MahjongSuit.Wind && tiles.size >= 3 && (key.second == 1 || key.second == ((player.seat - seatWind + 4) % 4) + 1)
        }
        if (windTriplet) yaku += "viento"
        val allSequences = player.melds.all { it.kind == MahjongMeldKind.Chi } && concealed.size >= 2
        if (allSequences && player.melds.isNotEmpty()) yaku += "pinfu"
        if (winTile.isHonor || winTile.rank in setOf(1, 9)) {
            // Las esperas de borde o pareja reducen el pinfu; no se suman aquí.
        }
        return yaku.distinct()
    }
}
