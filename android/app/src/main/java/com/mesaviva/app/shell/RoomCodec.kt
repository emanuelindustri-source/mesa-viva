package com.mesaviva.app.shell

import com.mesaviva.app.games.BriscaPhase
import com.mesaviva.app.games.BriscaPlay
import com.mesaviva.app.games.BriscaSeat
import com.mesaviva.app.games.BriscaState
import com.mesaviva.app.games.BriscaTrick
import com.mesaviva.app.games.DamasChecker
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasMove
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DamasState
import com.mesaviva.app.games.DominoEnd
import com.mesaviva.app.games.DominoPhase
import com.mesaviva.app.games.DominoPlay
import com.mesaviva.app.games.DominoPlayer
import com.mesaviva.app.games.DominoState
import com.mesaviva.app.games.DominoTile
import com.mesaviva.app.games.MancalaPhase
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.MancalaState
import com.mesaviva.app.games.LudoColor
import com.mesaviva.app.games.LudoMove
import com.mesaviva.app.games.LudoPhase
import com.mesaviva.app.games.LudoPlayer
import com.mesaviva.app.games.LudoState
import com.mesaviva.app.games.LudoToken
import com.mesaviva.app.games.SpanishCard
import com.mesaviva.app.games.SpanishSuit
import com.mesaviva.app.games.TrucoChallenge
import com.mesaviva.app.games.TrucoPhase
import com.mesaviva.app.games.TrucoPlay
import com.mesaviva.app.games.TrucoSeat
import com.mesaviva.app.games.TrucoState
import com.mesaviva.app.games.TrucoTrick
import org.json.JSONArray
import org.json.JSONObject

/**
 * Códec JSON del protocolo de salas nativas.
 *
 * Es puro a propósito: no toca Android ni WebSocket, así que se puede probar
 * en la JVM. El anfitrión valida siempre con los motores Kotlin; esto solo
 * transporta el estado completo.
 */
internal object RoomCodec {

    fun damasToJson(board: DamasState): JSONObject {
        val cells = JSONArray()
        for (piece in board.board) {
            cells.put(
                when {
                    piece == null -> ""
                    piece.color == DamasColor.White && piece.king -> "WK"
                    piece.color == DamasColor.White -> "W"
                    piece.king -> "BK"
                    else -> "B"
                },
            )
        }
        val last = board.lastMove?.let {
            JSONObject()
                .put("from", it.from)
                .put("to", it.to)
                .put("captured", it.captured ?: JSONObject.NULL)
                .put("promotion", it.promotion)
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Damas.id)
            .put("board", cells)
            .put("turn", if (board.turn == DamasColor.White) "white" else "black")
            .put("mustContinueFrom", board.mustContinueFrom ?: JSONObject.NULL)
            .put("phase", if (board.phase == DamasPhase.Play) "play" else "end")
            .put("sinceCapture", board.sinceCapture)
            .put("winner", when (board.winner) {
                DamasColor.White -> "white"
                DamasColor.Black -> "black"
                null -> JSONObject.NULL
            })
            .put("draw", board.draw)
            .put("reason", board.reason ?: JSONObject.NULL)
            .put("history", JSONArray(board.history))
            .put("captures", JSONObject()
                .put("white", board.captures[DamasColor.White] ?: 0)
                .put("black", board.captures[DamasColor.Black] ?: 0))
            .put("lastMove", last)
    }

    fun damasFromJson(json: JSONObject): DamasState? {
        val cells = json.optJSONArray("board") ?: return null
        if (cells.length() != DamasGame.SIZE * DamasGame.SIZE) return null
        val board = MutableList<DamasChecker?>(cells.length()) { null }
        for (index in 0 until cells.length()) {
            board[index] = when (cells.optString(index)) {
                "W" -> DamasChecker(DamasColor.White)
                "WK" -> DamasChecker(DamasColor.White, king = true)
                "B" -> DamasChecker(DamasColor.Black)
                "BK" -> DamasChecker(DamasColor.Black, king = true)
                else -> null
            }
        }
        val last = json.optJSONObject("lastMove")?.let {
            DamasMove(
                from = it.optInt("from"),
                to = it.optInt("to"),
                captured = if (it.isNull("captured")) null else it.optInt("captured"),
                promotion = it.optBoolean("promotion"),
            )
        }
        val captures = json.optJSONObject("captures")
        val history = json.optJSONArray("history")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
        } ?: emptyList()
        return DamasState(
            board = board,
            turn = if (json.optString("turn") == "black") DamasColor.Black else DamasColor.White,
            mustContinueFrom = if (json.isNull("mustContinueFrom")) null else json.optInt("mustContinueFrom"),
            phase = if (json.optString("phase") == "end") DamasPhase.MatchEnd else DamasPhase.Play,
            sinceCapture = json.optInt("sinceCapture"),
            lastMove = last,
            winner = when (json.optString("winner")) {
                "white" -> DamasColor.White
                "black" -> DamasColor.Black
                else -> null
            },
            draw = json.optBoolean("draw"),
            reason = if (json.isNull("reason")) null else json.optString("reason"),
            history = history,
            captures = mapOf(
                DamasColor.White to (captures?.optInt("white") ?: 0),
                DamasColor.Black to (captures?.optInt("black") ?: 0),
            ),
        )
    }

    fun mancalaToJson(board: MancalaState): JSONObject = JSONObject()
        .put("k", "state")
        .put("game", RoomGame.Mancala.id)
        .put("pits", JSONArray(board.pits))
        .put("turn", if (board.turn == MancalaSide.White) "white" else "black")
        .put("phase", if (board.phase == MancalaPhase.Play) "play" else "end")
        .put("lastPit", board.lastPit ?: JSONObject.NULL)
        .put("lastCapture", board.lastCapture ?: JSONObject.NULL)
        .put("winner", when (board.winner) {
            MancalaSide.White -> "white"
            MancalaSide.Black -> "black"
            null -> JSONObject.NULL
        })

    fun snakesToJson(board: SnakesRoomState): JSONObject = JSONObject()
        .put("k", "state")
        .put("game", RoomGame.Snakes.id)
        .put("names", JSONArray(board.names))
        .put("bots", JSONArray(board.bots))
        .put("positions", JSONArray(board.positions))
        .put("turn", board.turn)
        .put("dice", board.dice)
        .put("winner", board.winner ?: JSONObject.NULL)
        .put("lastSeat", board.lastSeat ?: JSONObject.NULL)
        .put("lastFrom", board.lastFrom)
        .put("lastTo", board.lastTo)
        .put("lastEvent", board.lastEvent)

    fun snakesFromJson(json: JSONObject): SnakesRoomState? {
        val namesJson = json.optJSONArray("names") ?: return null
        val botsJson = json.optJSONArray("bots") ?: return null
        val positionsJson = json.optJSONArray("positions") ?: return null
        if (namesJson.length() !in 2..4) return null
        if (positionsJson.length() != namesJson.length()) return null
        val names = (0 until namesJson.length()).map { namesJson.optString(it) }
        val bots = (0 until namesJson.length()).map { botsJson.optBoolean(it) }
        val positions = (0 until positionsJson.length()).map { positionsJson.optInt(it) }
        if (positions.any { it !in 0..100 }) return null
        val winner = if (json.isNull("winner")) null else json.optInt("winner").takeIf { it in names.indices }
        return SnakesRoomState(
            names = names,
            bots = bots,
            positions = positions,
            turn = json.optInt("turn").coerceIn(0, names.lastIndex),
            dice = json.optInt("dice").coerceIn(0, 6),
            winner = winner,
            lastSeat = if (json.isNull("lastSeat")) null else json.optInt("lastSeat").takeIf { it in names.indices },
            lastFrom = json.optInt("lastFrom").coerceIn(0, 100),
            lastTo = json.optInt("lastTo").coerceIn(0, 100),
            lastEvent = json.optString("lastEvent", "move"),
        )
    }

    fun dominoToJson(board: DominoState, viewerSeat: Int): JSONObject {
        val players = JSONArray()
        board.players.forEachIndexed { index, player ->
            val ownsHand = index == viewerSeat
            players.put(
                JSONObject()
                    .put("name", player.name)
                    .put("bot", player.isBot)
                    .put("team", player.team ?: JSONObject.NULL)
                    .put("score", player.score)
                    .put("handCount", board.handCounts.getOrElse(index) { player.hand.size })
                    .put("hand", if (ownsHand) JSONArray(player.hand.map { it.id }) else JSONArray()),
            )
        }
        val chain = JSONArray()
        board.chain.forEach { chain.put(it.id) }
        val lastPlay = board.lastPlay?.let {
            JSONObject()
                .put("player", it.playerIndex)
                .put("tile", it.tile.id)
                .put("end", it.end.name.lowercase())
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Domino.id)
            .put("viewer", viewerSeat)
            .put("players", players)
            .put("boneyard", board.boneyard.size)
            .put("chain", chain)
            .put("left", board.left ?: JSONObject.NULL)
            .put("right", board.right ?: JSONObject.NULL)
            .put("turn", board.turn)
            .put("passes", board.passes)
            .put("handNo", board.handNo)
            .put("target", board.target)
            .put("teams", board.teams)
            .put("phase", when (board.phase) {
                DominoPhase.Playing -> "playing"
                DominoPhase.HandEnd -> "handend"
                DominoPhase.MatchEnd -> "matchend"
            })
            .put("winnerIndices", JSONArray(board.winnerIndices))
            .put("handWinnerIndex", board.handWinnerIndex ?: JSONObject.NULL)
            .put("lastPlay", lastPlay)
    }

    fun dominoFromJson(json: JSONObject): DominoState? {
        val playersJson = json.optJSONArray("players") ?: return null
        if (playersJson.length() !in 2..4) return null
        val viewer = json.optInt("viewer", -1)
        val players = mutableListOf<DominoPlayer>()
        val handCounts = mutableListOf<Int>()
        for (index in 0 until playersJson.length()) {
            val entry = playersJson.optJSONObject(index) ?: return null
            val hand = if (index == viewer) {
                val array = entry.optJSONArray("hand") ?: JSONArray()
                (0 until array.length()).mapNotNull { tileFromId(array.optString(it)) }
            } else {
                emptyList()
            }
            players.add(
                DominoPlayer(
                    name = entry.optString("name", "Jugador ${index + 1}"),
                    isBot = entry.optBoolean("bot"),
                    team = if (entry.isNull("team")) null else entry.optInt("team"),
                    hand = hand,
                    score = entry.optInt("score"),
                ),
            )
            handCounts.add(entry.optInt("handCount", hand.size).coerceAtLeast(0))
        }
        val chainJson = json.optJSONArray("chain") ?: JSONArray()
        val chain = (0 until chainJson.length()).mapNotNull { tileFromId(chainJson.optString(it)) }
        val boneyard = List(json.optInt("boneyard").coerceAtLeast(0)) { DominoTile(6, 6) }
        val winnersJson = json.optJSONArray("winnerIndices") ?: JSONArray()
        val winners = (0 until winnersJson.length())
            .map { winnersJson.optInt(it) }
            .filter { it in players.indices }
        val lastPlay = json.optJSONObject("lastPlay")?.let { entry ->
            val index = entry.optInt("player", -1)
            val tile = tileFromId(entry.optString("tile")) ?: return@let null
            if (index !in players.indices) return@let null
            DominoPlay(
                playerIndex = index,
                tile = tile,
                end = if (entry.optString("end") == "left") DominoEnd.Left else DominoEnd.Right,
            )
        }
        return DominoState(
            players = players,
            boneyard = boneyard,
            chain = chain,
            left = if (json.isNull("left")) null else json.optInt("left"),
            right = if (json.isNull("right")) null else json.optInt("right"),
            turn = json.optInt("turn").coerceIn(0, players.lastIndex),
            passes = json.optInt("passes").coerceAtLeast(0),
            handNo = json.optInt("handNo").coerceAtLeast(1),
            target = json.optInt("target", 100).coerceAtLeast(30),
            teams = json.optBoolean("teams"),
            phase = when (json.optString("phase")) {
                "handend" -> DominoPhase.HandEnd
                "matchend" -> DominoPhase.MatchEnd
                else -> DominoPhase.Playing
            },
            winnerIndices = winners,
            handWinnerIndex = if (json.isNull("handWinnerIndex")) null else json.optInt("handWinnerIndex"),
            lastPlay = lastPlay,
            handCounts = handCounts,
        )
    }

    private fun tileFromId(raw: String): DominoTile? {
        val parts = raw.split("-")
        if (parts.size != 2) return null
        val a = parts[0].toIntOrNull() ?: return null
        val b = parts[1].toIntOrNull() ?: return null
        if (a !in 0..6 || b !in 0..6) return null
        return DominoTile(a, b)
    }

    fun mancalaFromJson(json: JSONObject): MancalaState? {
        val pits = json.optJSONArray("pits") ?: return null
        if (pits.length() != 14) return null
        val values = (0 until 14).map { pits.optInt(it) }
        if (values.any { it < 0 }) return null
        return MancalaState(
            pits = values,
            turn = if (json.optString("turn") == "black") MancalaSide.Black else MancalaSide.White,
            phase = if (json.optString("phase") == "end") MancalaPhase.MatchEnd else MancalaPhase.Play,
            lastPit = if (json.isNull("lastPit")) null else json.optInt("lastPit"),
            lastCapture = if (json.isNull("lastCapture")) null else json.optInt("lastCapture"),
            winner = when (json.optString("winner")) {
                "white" -> MancalaSide.White
                "black" -> MancalaSide.Black
                else -> null
            },
        )
    }

    /* --------------------------------------------------------- baraja española */

    private fun cardToJson(card: SpanishCard): JSONObject = JSONObject()
        .put("id", card.id)
        .put("suit", card.suit.name)
        .put("rank", card.rank)

    private fun cardFromJson(json: JSONObject?): SpanishCard? {
        if (json == null) return null
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val suit = runCatching { SpanishSuit.valueOf(json.optString("suit")) }.getOrNull() ?: return null
        val rank = json.optInt("rank", 0)
        if (rank !in 1..12) return null
        return SpanishCard(id, suit, rank)
    }

    private fun cardsToJson(cards: List<SpanishCard>): JSONArray = JSONArray().apply {
        cards.forEach { put(cardToJson(it)) }
    }

    /**
     * Cartas ocultas: no viajan los rangos ni los palos reales. Solo se envía
     * la cantidad para que la mesa dibuje los dorsos del rival.
     */
    private fun hiddenCards(prefix: String, count: Int): JSONArray = JSONArray().apply {
        repeat(count.coerceAtLeast(0)) { index ->
            put(cardToJson(SpanishCard("$prefix-$index", SpanishSuit.Oros, 2)))
        }
    }

    fun briscaToJson(board: BriscaState, viewerSeat: Int): JSONObject {
        val seats = JSONArray()
        board.seats.forEachIndexed { index, seat ->
            val own = index == viewerSeat
            seats.put(
                JSONObject()
                    .put("name", seat.name)
                    .put("bot", seat.isBot)
                    .put("team", seat.team ?: JSONObject.NULL)
                    .put("handCount", board.hands.getOrElse(index) { emptyList() }.size)
                    .put("hand", if (own) cardsToJson(board.hands.getOrElse(index) { emptyList() }) else hiddenCards("h$index", board.hands.getOrElse(index) { emptyList() }.size)),
            )
        }
        val trick = JSONArray()
        board.trick.forEach { play ->
            trick.put(JSONObject().put("seat", play.seat).put("card", cardToJson(play.card)))
        }
        val lastTrick = board.lastTrick?.let { last ->
            JSONObject()
                .put("winner", last.winner)
                .put("points", last.points)
                .put("plays", JSONArray().apply {
                    last.plays.forEach { play ->
                        put(JSONObject().put("seat", play.seat).put("card", cardToJson(play.card)))
                    }
                })
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Brisca.id)
            .put("viewer", viewerSeat)
            .put("seats", seats)
            .put("scores", JSONArray(board.scores))
            .put("deck", hiddenCards("deck", board.deck.size))
            .put("trumpCard", board.trumpCard?.let { cardToJson(it) } ?: JSONObject.NULL)
            .put("trumpSuit", board.trumpSuit.name)
            .put("leader", board.leader)
            .put("turn", board.turn)
            .put("dealer", board.dealer)
            .put("handNo", board.handNo)
            .put("target", board.target)
            .put("teams", board.teams)
            .put("phase", when (board.phase) {
                BriscaPhase.Playing -> "playing"
                BriscaPhase.HandEnd -> "handend"
                BriscaPhase.MatchEnd -> "matchend"
            })
            .put("trick", trick)
            .put("lastTrick", lastTrick)
            .put("capturedPoints", JSONArray(board.capturedPoints))
            .put("lastPlay", board.lastPlay?.let {
                JSONObject().put("seat", it.seat).put("card", cardToJson(it.card))
            } ?: JSONObject.NULL)
            .put("winner", board.winner ?: JSONObject.NULL)
            .put("draw", board.draw)
            .put("reason", board.reason ?: JSONObject.NULL)
            .put("history", JSONArray(board.history))
    }

    fun briscaFromJson(json: JSONObject): BriscaState? {
        val seatsJson = json.optJSONArray("seats") ?: return null
        if (seatsJson.length() !in 2..4) return null
        val seats = mutableListOf<BriscaSeat>()
        val hands = mutableListOf<List<SpanishCard>>()
        for (index in 0 until seatsJson.length()) {
            val entry = seatsJson.optJSONObject(index) ?: return null
            seats.add(
                BriscaSeat(
                    name = entry.optString("name", "Jugador ${index + 1}"),
                    isBot = entry.optBoolean("bot"),
                    team = if (entry.isNull("team")) null else entry.optInt("team"),
                ),
            )
            val handJson = entry.optJSONArray("hand") ?: JSONArray()
            hands.add((0 until handJson.length()).mapNotNull { cardFromJson(handJson.optJSONObject(it)) })
        }
        val scoresJson = json.optJSONArray("scores") ?: JSONArray()
        val scores = (0 until seats.size).map { scoresJson.optInt(it, 0) }
        val capturedJson = json.optJSONArray("capturedPoints") ?: JSONArray()
        val captured = (0 until seats.size).map { capturedJson.optInt(it, 0) }
        val deckJson = json.optJSONArray("deck") ?: JSONArray()
        val deck = (0 until deckJson.length()).mapNotNull { cardFromJson(deckJson.optJSONObject(it)) }
        val trickJson = json.optJSONArray("trick") ?: JSONArray()
        val trick = (0 until trickJson.length()).mapNotNull { entry ->
            val play = trickJson.optJSONObject(entry) ?: return@mapNotNull null
            val card = cardFromJson(play.optJSONObject("card")) ?: return@mapNotNull null
            BriscaPlay(play.optInt("seat"), card)
        }
        val lastTrick = json.optJSONObject("lastTrick")?.let { last ->
            val playsJson = last.optJSONArray("plays") ?: JSONArray()
            val plays = (0 until playsJson.length()).mapNotNull { entry ->
                val play = playsJson.optJSONObject(entry) ?: return@mapNotNull null
                val card = cardFromJson(play.optJSONObject("card")) ?: return@mapNotNull null
                BriscaPlay(play.optInt("seat"), card)
            }
            BriscaTrick(plays, last.optInt("winner"), last.optInt("points"))
        }
        val lastPlay = json.optJSONObject("lastPlay")?.let {
            val card = cardFromJson(it.optJSONObject("card")) ?: return@let null
            BriscaPlay(it.optInt("seat"), card)
        }
        val historyJson = json.optJSONArray("history") ?: JSONArray()
        val history = (0 until historyJson.length()).map { historyJson.optString(it) }
        return BriscaState(
            seats = seats,
            scores = scores,
            hands = hands,
            deck = deck,
            trumpCard = cardFromJson(json.optJSONObject("trumpCard")),
            trumpSuit = runCatching { SpanishSuit.valueOf(json.optString("trumpSuit")) }.getOrDefault(SpanishSuit.Oros),
            leader = json.optInt("leader").coerceIn(0, seats.lastIndex),
            turn = json.optInt("turn").coerceIn(0, seats.lastIndex),
            dealer = json.optInt("dealer").coerceIn(0, seats.lastIndex),
            handNo = json.optInt("handNo", 1),
            target = json.optInt("target", 100),
            teams = json.optBoolean("teams"),
            phase = when (json.optString("phase")) {
                "handend" -> BriscaPhase.HandEnd
                "matchend" -> BriscaPhase.MatchEnd
                else -> BriscaPhase.Playing
            },
            trick = trick,
            lastTrick = lastTrick,
            capturedPoints = captured,
            lastPlay = lastPlay,
            winner = if (json.isNull("winner")) null else json.optInt("winner"),
            draw = json.optBoolean("draw"),
            reason = if (json.isNull("reason")) null else json.optString("reason"),
            history = history,
        )
    }

    fun trucoToJson(board: TrucoState, viewerSeat: Int): JSONObject {
        val seats = JSONArray()
        board.seats.forEachIndexed { index, seat ->
            val own = index == viewerSeat
            seats.put(
                JSONObject()
                    .put("name", seat.name)
                    .put("bot", seat.isBot)
                    .put("handCount", board.hands.getOrElse(index) { emptyList() }.size)
                    .put("hand", if (own) cardsToJson(board.hands.getOrElse(index) { emptyList() }) else hiddenCards("th$index", board.hands.getOrElse(index) { emptyList() }.size)),
            )
        }
        val current = JSONArray()
        board.currentTrick.forEach { play ->
            current.put(JSONObject().put("seat", play.seat).put("card", cardToJson(play.card)))
        }
        val tricks = JSONArray()
        board.tricks.forEach { trick ->
            tricks.put(
                JSONObject()
                    .put("winner", trick.winner ?: JSONObject.NULL)
                    .put("plays", JSONArray().apply {
                        trick.plays.forEach { play ->
                            put(JSONObject().put("seat", play.seat).put("card", cardToJson(play.card)))
                        }
                    }),
            )
        }
        val pending = board.pending?.let {
            JSONObject()
                .put("caller", it.caller)
                .put("kind", it.kind)
                .put("stake", it.stake)
                .put("previousStake", it.previousStake)
                .put("awaiting", it.awaiting)
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Truco.id)
            .put("viewer", viewerSeat)
            .put("seats", seats)
            .put("scores", JSONArray(board.scores))
            .put("dealer", board.dealer)
            .put("mano", board.mano)
            .put("turn", board.turn)
            .put("handNo", board.handNo)
            .put("target", board.target)
            .put("phase", when (board.phase) {
                TrucoPhase.Envido -> "envido"
                TrucoPhase.Play -> "play"
                TrucoPhase.HandEnd -> "handend"
                TrucoPhase.MatchEnd -> "matchend"
            })
            .put("trickLeader", board.trickLeader)
            .put("currentTrick", current)
            .put("tricks", tricks)
            .put("trickWins", JSONArray(board.trickWins))
            .put("handStake", board.handStake)
            .put("trucoLevel", board.trucoLevel)
            .put("envidoCalled", board.envidoCalled)
            .put("envidoBet", board.envidoBet)
            .put("envidoWinner", board.envidoWinner ?: JSONObject.NULL)
            .put("envidoReveal", board.envidoReveal?.let { JSONArray(it) } ?: JSONObject.NULL)
            .put("pending", pending)
            .put("handWinner", board.handWinner ?: JSONObject.NULL)
            .put("handPoints", board.handPoints)
            .put("lastAction", board.lastAction ?: JSONObject.NULL)
            .put("winner", board.winner ?: JSONObject.NULL)
            .put("draw", board.draw)
            .put("reason", board.reason ?: JSONObject.NULL)
            .put("history", JSONArray(board.history))
    }

    fun trucoFromJson(json: JSONObject): TrucoState? {
        val seatsJson = json.optJSONArray("seats") ?: return null
        if (seatsJson.length() != 2) return null
        val seats = mutableListOf<TrucoSeat>()
        val hands = mutableListOf<List<SpanishCard>>()
        for (index in 0 until 2) {
            val entry = seatsJson.optJSONObject(index) ?: return null
            seats.add(TrucoSeat(entry.optString("name", "Jugador ${index + 1}"), entry.optBoolean("bot")))
            val handJson = entry.optJSONArray("hand") ?: JSONArray()
            hands.add((0 until handJson.length()).mapNotNull { cardFromJson(handJson.optJSONObject(it)) })
        }
        val scoresJson = json.optJSONArray("scores") ?: JSONArray()
        val scores = (0 until 2).map { scoresJson.optInt(it, 0) }
        val currentJson = json.optJSONArray("currentTrick") ?: JSONArray()
        val current = (0 until currentJson.length()).mapNotNull { index ->
            val play = currentJson.optJSONObject(index) ?: return@mapNotNull null
            val card = cardFromJson(play.optJSONObject("card")) ?: return@mapNotNull null
            TrucoPlay(play.optInt("seat"), card)
        }
        val tricksJson = json.optJSONArray("tricks") ?: JSONArray()
        val tricks = (0 until tricksJson.length()).mapNotNull { index ->
            val trick = tricksJson.optJSONObject(index) ?: return@mapNotNull null
            val playsJson = trick.optJSONArray("plays") ?: JSONArray()
            val plays = (0 until playsJson.length()).mapNotNull { entryIndex ->
                val play = playsJson.optJSONObject(entryIndex) ?: return@mapNotNull null
                val card = cardFromJson(play.optJSONObject("card")) ?: return@mapNotNull null
                TrucoPlay(play.optInt("seat"), card)
            }
            TrucoTrick(plays, if (trick.isNull("winner")) null else trick.optInt("winner"))
        }
        val winsJson = json.optJSONArray("trickWins") ?: JSONArray()
        val wins = (0 until 2).map { winsJson.optInt(it, 0) }
        val revealJson = json.optJSONArray("envidoReveal")
        val reveal = revealJson?.let { array -> (0 until array.length()).map { array.optInt(it) } }
        val pending = json.optJSONObject("pending")?.let {
            TrucoChallenge(
                caller = it.optInt("caller"),
                kind = it.optString("kind", "envido"),
                stake = it.optInt("stake", 1),
                previousStake = it.optInt("previousStake", 1),
                awaiting = it.optInt("awaiting").coerceIn(0, 1),
            )
        }
        val historyJson = json.optJSONArray("history") ?: JSONArray()
        val history = (0 until historyJson.length()).map { historyJson.optString(it) }
        return TrucoState(
            seats = seats,
            scores = scores,
            hands = hands,
            dealer = json.optInt("dealer").coerceIn(0, 1),
            mano = json.optInt("mano").coerceIn(0, 1),
            turn = json.optInt("turn").coerceIn(0, 1),
            handNo = json.optInt("handNo", 1),
            target = json.optInt("target", 30),
            phase = when (json.optString("phase")) {
                "envido" -> TrucoPhase.Envido
                "handend" -> TrucoPhase.HandEnd
                "matchend" -> TrucoPhase.MatchEnd
                else -> TrucoPhase.Play
            },
            trickLeader = json.optInt("trickLeader").coerceIn(0, 1),
            currentTrick = current,
            tricks = tricks,
            trickWins = wins,
            handStake = json.optInt("handStake", 1),
            trucoLevel = json.optInt("trucoLevel", 0),
            envidoCalled = json.optBoolean("envidoCalled"),
            envidoBet = json.optInt("envidoBet", 0),
            envidoWinner = if (json.isNull("envidoWinner")) null else json.optInt("envidoWinner"),
            envidoReveal = reveal,
            pending = pending,
            handWinner = if (json.isNull("handWinner")) null else json.optInt("handWinner"),
            handPoints = json.optInt("handPoints", 0),
            lastAction = if (json.isNull("lastAction")) null else json.optString("lastAction"),
            winner = if (json.isNull("winner")) null else json.optInt("winner"),
            draw = json.optBoolean("draw"),
            reason = if (json.isNull("reason")) null else json.optString("reason"),
            history = history,
        )
    }

    /* --------------------------------------------------------------- ludo */

    fun ludoToJson(board: LudoState): JSONObject {
        val players = JSONArray()
        board.players.forEach { player ->
            players.put(
                JSONObject()
                    .put("name", player.name)
                    .put("bot", player.isBot)
                    .put("color", player.color.name)
                    .put("captures", player.captures),
            )
        }
        val tokens = JSONArray()
        board.tokens.forEach { token ->
            tokens.put(
                JSONObject()
                    .put("id", token.id)
                    .put("color", token.color.name)
                    .put("slot", token.slot)
                    .put("progress", token.progress),
            )
        }
        val lastMove = board.lastMove?.let {
            JSONObject().put("token", it.tokenId).put("from", it.from).put("to", it.to)
        } ?: JSONObject.NULL
        return JSONObject()
            .put("k", "state")
            .put("game", RoomGame.Ludo.id)
            .put("players", players)
            .put("tokens", tokens)
            .put("turn", board.turn)
            .put("phase", when (board.phase) {
                LudoPhase.Roll -> "roll"
                LudoPhase.Move -> "move"
                LudoPhase.MatchEnd -> "matchend"
            })
            .put("dice", board.dice ?: JSONObject.NULL)
            .put("lastRoll", board.lastRoll ?: JSONObject.NULL)
            .put("sixStreak", board.sixStreak)
            .put("lastMove", lastMove)
            .put("winnerIndex", board.winnerIndex ?: JSONObject.NULL)
    }

    fun ludoFromJson(json: JSONObject): LudoState? {
        val playersJson = json.optJSONArray("players") ?: return null
        if (playersJson.length() !in 2..4) return null
        val players = mutableListOf<LudoPlayer>()
        for (index in 0 until playersJson.length()) {
            val entry = playersJson.optJSONObject(index) ?: return null
            val color = runCatching { LudoColor.valueOf(entry.optString("color")) }.getOrNull() ?: return null
            players.add(
                LudoPlayer(
                    name = entry.optString("name", "Jugador ${index + 1}"),
                    isBot = entry.optBoolean("bot"),
                    color = color,
                    captures = entry.optInt("captures", 0),
                ),
            )
        }
        val tokensJson = json.optJSONArray("tokens") ?: return null
        val tokens = mutableListOf<LudoToken>()
        for (index in 0 until tokensJson.length()) {
            val entry = tokensJson.optJSONObject(index) ?: return null
            val color = runCatching { LudoColor.valueOf(entry.optString("color")) }.getOrNull() ?: return null
            val progress = entry.optInt("progress", -1)
            if (progress < -1 || progress > 57) return null
            tokens.add(
                LudoToken(
                    id = entry.optString("id").takeIf { it.isNotBlank() } ?: return null,
                    color = color,
                    slot = entry.optInt("slot").coerceIn(0, 3),
                    progress = progress,
                ),
            )
        }
        if (tokens.size != players.size * 4) return null
        val lastMoveJson = json.optJSONObject("lastMove")
        val lastMove = lastMoveJson?.let {
            val tokenId = it.optString("token").takeIf { value -> value.isNotBlank() } ?: return@let null
            LudoMove(tokenId, it.optInt("from", -1), it.optInt("to", -1))
        }
        return LudoState(
            players = players,
            tokens = tokens,
            turn = json.optInt("turn").coerceIn(0, players.lastIndex),
            phase = when (json.optString("phase")) {
                "move" -> LudoPhase.Move
                "matchend" -> LudoPhase.MatchEnd
                else -> LudoPhase.Roll
            },
            dice = if (json.isNull("dice")) null else json.optInt("dice").takeIf { it in 1..6 },
            lastRoll = if (json.isNull("lastRoll")) null else json.optInt("lastRoll").takeIf { it in 1..6 },
            sixStreak = json.optInt("sixStreak", 0),
            lastMove = lastMove,
            winnerIndex = if (json.isNull("winnerIndex")) null else json.optInt("winnerIndex").takeIf { it in players.indices },
        )
    }
}
