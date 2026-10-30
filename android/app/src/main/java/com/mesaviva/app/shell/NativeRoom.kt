package com.mesaviva.app.shell

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mesaviva.app.LanRelayServer
import com.mesaviva.app.games.BriscaAction
import com.mesaviva.app.games.BriscaGame
import com.mesaviva.app.games.BriscaPhase
import com.mesaviva.app.games.BriscaState
import com.mesaviva.app.games.DamasChecker
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasMove
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DamasState
import com.mesaviva.app.games.DominoEnd
import com.mesaviva.app.games.DominoGame
import com.mesaviva.app.games.DominoMoveType
import com.mesaviva.app.games.DominoPhase
import com.mesaviva.app.games.DominoState
import com.mesaviva.app.games.DominoTile
import com.mesaviva.app.games.MancalaGame
import com.mesaviva.app.games.MancalaPhase
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.MancalaState
import com.mesaviva.app.games.LudoAction
import com.mesaviva.app.games.LudoGame
import com.mesaviva.app.games.LudoPhase
import com.mesaviva.app.games.LudoState
import com.mesaviva.app.games.TrucoGame
import com.mesaviva.app.games.TrucoPhase
import com.mesaviva.app.games.TrucoState
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class RoomGame(val id: String, val emoji: String, val titleEs: String, val titleEn: String) {
    Damas("damas", "⛀", "Damas", "Checkers"),
    Mancala("mancala", "🫘", "Mancala", "Mancala"),
    Domino("domino", "🁫", "Dominó", "Dominoes"),
    Snakes("serpientes", "🐍", "Serpientes", "Snakes"),
    Brisca("brisca", "🃏", "Brisca", "Brisca"),
    Truco("truco", "🎴", "Truco", "Truco"),
    Ludo("ludo", "🎲", "Ludo", "Ludo");

    fun title(language: String): String = if (language == "en") titleEn else titleEs

    companion object {
        fun fromId(id: String?): RoomGame? = entries.firstOrNull { it.id == id }
    }
}

enum class RoomRole { Host, Guest }

enum class RoomStatus { Idle, Connecting, Hosting, Joined, Reconnecting, Closed, Error }

data class RoomGuest(
    val id: String,
    val name: String,
    val avatar: String,
    val online: Boolean,
    val seat: Int = 1,
)

data class RoomChat(
    val author: String,
    val text: String,
    val mine: Boolean,
    val system: Boolean,
    val time: String,
)

/**
 * Sesión de sala nativa.
 *
 * El anfitrión levanta el relay LAN que ya usa la app y se conecta a él como
 * host; el invitado se conecta por WebSocket y ambos intercambian mensajes
 * JSON. El anfitrión es la autoridad del juego: recibe la jugada del invitado,
 * la valida con el motor Kotlin y difunde el estado completo. Si el invitado
 * se cae, se puede continuar con un bot y al reconectarse recupera el estado.
 */
class NativeRoomController(
    private val context: Context,
    private val state: ShellState,
    val role: RoomRole,
) {

    var status by mutableStateOf(RoomStatus.Idle)
        private set

    var code by mutableStateOf("")
        private set

    var shareUrl by mutableStateOf("")
        private set

    var qrText by mutableStateOf("")
        private set

    var hostName by mutableStateOf("")
        private set

    var guests by mutableStateOf<List<RoomGuest>>(emptyList())
        private set

    var chat by mutableStateOf<List<RoomChat>>(emptyList())
        private set

    var selectedGame by mutableStateOf(RoomGame.Damas)
        private set

    var activeGame by mutableStateOf<RoomGame?>(null)
        private set

    var damasState by mutableStateOf<DamasState?>(null)
        private set

    var mancalaState by mutableStateOf<MancalaState?>(null)
        private set

    var snakesState by mutableStateOf<SnakesRoomState?>(null)
        private set

    var dominoState by mutableStateOf<DominoState?>(null)
        private set

    var briscaState by mutableStateOf<BriscaState?>(null)
        private set

    var trucoState by mutableStateOf<TrucoState?>(null)
        private set

    var ludoState by mutableStateOf<LudoState?>(null)
        private set

    var briscaPlayers by mutableStateOf(2)
        private set

    var ludoPlayers by mutableStateOf(4)
        private set

    var dominoPlayers by mutableStateOf(4)
        private set

    var dominoPending by mutableStateOf(false)
        private set

    var mySeat by mutableStateOf(if (role == RoomRole.Host) 0 else 1)
        private set

    var hint by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var botSeat by mutableStateOf(false)
        private set

    var onlineRelay by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private val damas = DamasGame()
    private val mancala = MancalaGame()
    private var domino = DominoGame(4, false, 100)
    private var brisca = BriscaGame(2, 100, true)
    private var truco = TrucoGame(30, true)
    private var ludo = LudoGame(4, true)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    private var relay: LanRelayServer? = null
    private var client: WebSocketClient? = null
    private var guestId: String? = null
    private var snakesRandom = kotlin.random.Random(System.nanoTime())
    private var pendingUrl: String? = null
    private var pendingCode: String? = null
    private var reconnectAttempts = 0
    private var hostAttempts = 0
    private var closedByUser = false
    private var awarded = false

    val isHost: Boolean get() = role == RoomRole.Host
    val isGuest: Boolean get() = role == RoomRole.Guest
    val onlineGuests: List<RoomGuest> get() = guests.filter { it.online }
    val hasGuest: Boolean get() = guests.any { it.online }
    val inGame: Boolean get() = activeGame != null
    val hasBotSeat: Boolean get() = when (activeGame) {
        RoomGame.Brisca -> briscaState?.seats?.any { it.isBot } == true
        RoomGame.Truco -> trucoState?.seats?.any { it.isBot } == true
        RoomGame.Ludo -> ludoState?.players?.any { it.isBot } == true
        else -> botSeat
    }
    val canStart: Boolean
        get() = isHost && status == RoomStatus.Hosting && when (selectedGame) {
            RoomGame.Snakes -> guests.any { it.online && it.seat in 1..3 }
            RoomGame.Domino -> guests.any { it.online && it.seat in 1 until dominoPlayers }
            RoomGame.Brisca -> guests.any { it.online && it.seat in 1 until briscaPlayers }
            RoomGame.Truco -> guests.any { it.online && it.seat == 1 }
            RoomGame.Ludo -> guests.any { it.online && it.seat in 1 until ludoPlayers }
            else -> guests.any { it.online && it.seat == 1 }
        }

    private fun myName(): String = state.name.ifBlank {
        if (isHost) state.t("room.hostFallback") else state.t("room.guestFallback")
    }

    /* ------------------------------------------------------------- anfitrión */

    fun host(game: RoomGame, relayUrl: String? = null) {
        if (!isHost || status != RoomStatus.Idle) return
        selectedGame = game
        hostName = myName()
        code = newCode()
        status = RoomStatus.Connecting
        if (!relayUrl.isNullOrBlank()) {
            val remote = normalizeWsUrl(relayUrl)
            if (remote == null) {
                status = RoomStatus.Error
                error = state.t("room.errorConnect")
                appendSystem(state.t("room.errorConnect"))
                return
            }
            shareUrl = remote
            onlineRelay = true
            appendSystem(state.t("room.connectingRoom"))
            connectHost(remote)
            return
        }
        appendSystem(state.t("room.opening"))
        val thread = Thread {
            val server = try {
                LanRelayServer()
            } catch (_: Exception) {
                null
            }
            val url = server?.start()
            main.post {
                if (url == null) {
                    status = RoomStatus.Error
                    error = state.t("room.errorNoLan")
                    appendSystem(state.t("room.errorNoLan"))
                    return@post
                }
                relay = server
                shareUrl = url
                connectHost(url)
            }
        }
        thread.isDaemon = true
        thread.start()
    }

    private fun connectHost(url: String) {
        if (closedByUser) return
        val ws = try {
            object : WebSocketClient(URI(url)) {
                override fun onOpen(handshake: ServerHandshake) {
                    sendRaw(JSONObject().put("t", "host").put("code", code))
                }

                override fun onMessage(message: String) {
                    val json = parse(message) ?: return
                    main.post { handleHostMessage(json) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    main.post {
                        if (!closedByUser && status != RoomStatus.Closed) scheduleHostReconnect(url)
                    }
                }

                override fun onError(ex: Exception) {
                    // El cierre y la reconexión se gestionan en onClose.
                }
            }
        } catch (_: Exception) {
            status = RoomStatus.Error
            error = state.t("room.errorConnect")
            return
        }
        client = ws
        try {
            ws.connect()
        } catch (_: Exception) {
            status = RoomStatus.Error
            error = state.t("room.errorConnect")
        }
    }

    private fun scheduleHostReconnect(url: String) {
        if (closedByUser) return
        if (hostAttempts >= 4) {
            status = RoomStatus.Error
            error = state.t("room.errorReconnect")
            return
        }
        hostAttempts += 1
        status = RoomStatus.Reconnecting
        appendSystem(state.t("room.rehosting"))
        main.postDelayed({
            if (!closedByUser) connectHost(url)
        }, 1600L * hostAttempts)
    }

    private fun handleHostMessage(msg: JSONObject) {
        when (msg.optString("t")) {
            "hosted" -> {
                hostAttempts = 0
                if (status != RoomStatus.Hosting) {
                    status = RoomStatus.Hosting
                    qrText = "mesaviva://join?ws=${Uri.encode(shareUrl)}&code=$code"
                    appendSystem(state.t("room.roomOpen", mapOf("code" to code)))
                }
            }
            "guest" -> {
                val id = msg.optString("guestId")
                if (id.isBlank()) return
                val name = msg.optString("name", state.t("room.guestFallback")).take(24)
                val avatar = msg.optString("avatar", "tucan")
                val seat = nextSeat()
                guests = guests.filterNot { it.id == id } + RoomGuest(id, name, avatar, online = true, seat = seat)
                appendSystem(state.t("room.guestJoined", mapOf("name" to name)))
                NativeAudio.play(context, NativeAudio.Sfx.Chat, state.sfx)
                sendPayloadTo(id, JSONObject()
                    .put("k", "welcome")
                    .put("host", hostName)
                    .put("game", selectedGame.id)
                    .put("seat", seat))
                if (inGame) {
                    when (activeGame) {
                        RoomGame.Snakes -> {
                            syncSnakesSeats()
                            broadcastState()
                        }
                        RoomGame.Domino -> {
                            syncDominoSeats()
                            broadcastState()
                        }
                        RoomGame.Brisca -> {
                            syncBriscaSeats()
                            broadcastState()
                        }
                        RoomGame.Truco -> {
                            syncTrucoSeats()
                            broadcastState()
                        }
                        RoomGame.Ludo -> {
                            syncLudoSeats()
                            broadcastState()
                        }
                        else -> {
                            if (seat == 1) botSeat = false
                            sendStateTo(id)
                        }
                    }
                    sendPayloadTo(id, JSONObject().put("k", "seat").put("seat", seat))
                }
            }
            "guestRejoined" -> {
                val id = msg.optString("guestId")
                val guest = guests.firstOrNull { it.id == id }
                guests = guests.map { if (it.id == id) it.copy(online = true) else it }
                if (guest?.seat == 1) botSeat = false
                if (inGame && activeGame == RoomGame.Snakes) {
                    syncSnakesSeats()
                    broadcastState()
                }
                if (inGame && activeGame == RoomGame.Domino) {
                    syncDominoSeats()
                    broadcastState()
                }
                if (inGame && activeGame == RoomGame.Brisca) {
                    syncBriscaSeats()
                    broadcastState()
                }
                if (inGame && activeGame == RoomGame.Truco) {
                    syncTrucoSeats()
                    broadcastState()
                }
                if (inGame && activeGame == RoomGame.Ludo) {
                    syncLudoSeats()
                    broadcastState()
                }
                appendSystem(state.t("room.guestRejoined"))
                NativeAudio.play(context, NativeAudio.Sfx.Chat, state.sfx)
                if (inGame) {
                    sendPayloadTo(id, JSONObject().put("k", "seat").put("seat", guest?.seat ?: 1))
                    if (activeGame !in setOf(RoomGame.Snakes, RoomGame.Domino, RoomGame.Brisca, RoomGame.Truco, RoomGame.Ludo)) sendStateTo(id)
                }
            }
            "guestLeft" -> {
                val id = msg.optString("guestId")
                val permanent = msg.optBoolean("permanent", false)
                val guest = guests.firstOrNull { it.id == id }
                if (permanent) {
                    guests = guests.filterNot { it.id == id }
                } else {
                    guests = guests.map { if (it.id == id) it.copy(online = false) else it }
                }
                if (inGame) {
                    if (activeGame == RoomGame.Snakes) {
                        syncSnakesSeats()
                        broadcastState()
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    } else if (activeGame == RoomGame.Domino) {
                        syncDominoSeats()
                        broadcastState()
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    } else if (activeGame == RoomGame.Brisca) {
                        syncBriscaSeats()
                        broadcastState()
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    } else if (activeGame == RoomGame.Truco) {
                        syncTrucoSeats()
                        broadcastState()
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    } else if (activeGame == RoomGame.Ludo) {
                        syncLudoSeats()
                        broadcastState()
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    } else if (guest?.seat == 1) {
                        botSeat = true
                        appendSystem(state.t("room.guestLeftBot", mapOf("name" to (guest?.name ?: state.t("room.guestFallback")))))
                    }
                } else {
                    appendSystem(state.t("room.guestLeft"))
                }
            }
            "message" -> {
                val payload = msg.optJSONObject("payload") ?: return
                handleHostPayload(msg.optString("from"), payload)
            }
            "error" -> {
                val key = msg.optString("key")
                hint = when (key) {
                    "sala.codeTaken" -> state.t("room.codeTaken")
                    "sala.invalid" -> state.t("room.roomGone")
                    else -> state.t("room.errorGeneric")
                }
            }
            "closed" -> {
                status = RoomStatus.Closed
                error = state.t("room.hostClosed")
            }
        }
    }

    private fun handleHostPayload(from: String?, payload: JSONObject) {
        val guestName = guests.firstOrNull { it.id == from }?.name ?: state.t("room.guestFallback")
        when (payload.optString("k")) {
            "chat" -> {
                val text = payload.optString("text").trim().take(200)
                if (text.isNotEmpty()) {
                    appendChat(guestName, text, mine = false, system = false)
                    NativeAudio.play(context, NativeAudio.Sfx.Chat, state.sfx)
                }
            }
            "move" -> hostGuestDamasMove(from, payload)
            "sow" -> hostGuestMancalaSow(from, payload)
            "resign" -> hostGuestResign(from)
            "snakesRoll" -> hostGuestSnakesRoll(from)
            "dominoPlay" -> hostGuestDominoPlay(from, payload)
            "dominoDraw" -> hostGuestDominoDraw(from)
            "dominoPass" -> hostGuestDominoPass(from)
            "dominoNextHand" -> if (activeGame == RoomGame.Domino) hostDominoNextHand()
            "briscaPlay" -> hostGuestBriscaPlay(from, payload)
            "briscaNextHand" -> hostGuestBriscaNextHand(from)
            "trucoPlay" -> hostGuestTrucoPlay(from, payload)
            "trucoEnvido" -> hostGuestTrucoEnvido(from, payload)
            "trucoStart" -> hostGuestTrucoStart(from)
            "trucoCall" -> hostGuestTrucoCall(from)
            "trucoRespond" -> hostGuestTrucoRespond(from, payload)
            "trucoFold" -> hostGuestTrucoFold(from)
            "trucoNextHand" -> hostGuestTrucoNextHand(from)
            "ludoRoll" -> hostGuestLudoRoll(from)
            "ludoMove" -> hostGuestLudoMove(from, payload)
            "rematch" -> startMatch()
        }
    }

    fun startMatch() {
        if (!isHost) return
        if (!canStart) {
            hint = state.t("room.needGuest")
            return
        }
        activeGame = selectedGame
        awarded = false
        botSeat = false
        hint = ""
        when (selectedGame) {
            RoomGame.Damas -> damasState = damas.newMatch()
            RoomGame.Mancala -> mancalaState = mancala.newMatch()
            RoomGame.Domino -> {
                domino = DominoGame(dominoPlayers.coerceIn(2, 4), false, 100, System.nanoTime())
                val base = domino.newMatch()
                val names = MutableList(base.players.size) { index ->
                    if (index == 0) hostName.ifBlank { state.t("room.hostFallback") } else botName(index)
                }
                val bots = MutableList(base.players.size) { it != 0 }
                for (guest in guests) {
                    if (guest.seat in 1 until base.players.size) {
                        names[guest.seat] = guest.name
                        bots[guest.seat] = !guest.online
                    }
                }
                dominoState = base.copy(
                    players = base.players.mapIndexed { index, player ->
                        player.copy(name = names[index], isBot = bots[index])
                    },
                    handCounts = base.players.map { it.hand.size },
                )
            }
            RoomGame.Brisca -> {
                brisca = BriscaGame(briscaPlayers.coerceIn(2, 4), 100, true, System.nanoTime())
                val base = brisca.newMatch()
                val names = MutableList(base.seats.size) { index ->
                    if (index == 0) hostName.ifBlank { state.t("room.hostFallback") } else botName(index)
                }
                val bots = MutableList(base.seats.size) { it != 0 }
                for (guest in guests) {
                    if (guest.seat in 1 until base.seats.size) {
                        names[guest.seat] = guest.name
                        bots[guest.seat] = !guest.online
                    }
                }
                briscaState = base.copy(
                    seats = base.seats.mapIndexed { index, seat ->
                        seat.copy(name = names[index], isBot = bots[index])
                    },
                )
            }
            RoomGame.Truco -> {
                truco = TrucoGame(30, true, System.nanoTime())
                val base = truco.newMatch()
                val guest = guests.firstOrNull { it.seat == 1 && it.online }
                val names = listOf(
                    hostName.ifBlank { state.t("room.hostFallback") },
                    guest?.name ?: botName(1),
                )
                val bots = listOf(false, guest == null)
                trucoState = base.copy(
                    seats = base.seats.mapIndexed { index, seat ->
                        seat.copy(name = names[index], isBot = bots[index])
                    },
                )
            }
            RoomGame.Ludo -> {
                ludo = LudoGame(ludoPlayers.coerceIn(2, 4), true, System.nanoTime())
                val base = ludo.newMatch()
                val names = MutableList(base.players.size) { index ->
                    if (index == 0) hostName.ifBlank { state.t("room.hostFallback") } else botName(index)
                }
                val bots = MutableList(base.players.size) { it != 0 }
                for (guest in guests) {
                    if (guest.seat in 1 until base.players.size) {
                        names[guest.seat] = guest.name
                        bots[guest.seat] = !guest.online
                    }
                }
                ludoState = base.copy(
                    players = base.players.mapIndexed { index, player ->
                        player.copy(name = names[index], isBot = bots[index])
                    },
                )
            }
            RoomGame.Snakes -> {
                snakesRandom = kotlin.random.Random(System.nanoTime())
                val names = MutableList(4) { index ->
                    if (index == 0) hostName.ifBlank { state.t("room.hostFallback") } else botName(index)
                }
                val bots = MutableList(4) { it != 0 }
                for (guest in guests) {
                    if (guest.seat in 1..3) {
                        names[guest.seat] = guest.name
                        bots[guest.seat] = !guest.online
                    }
                }
                snakesState = SnakesRoomRules.newMatch(names, bots)
            }
        }
        appendSystem(state.t("room.matchStarted", mapOf("game" to selectedGame.title(state.language))))
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        for (guest in guests) {
            if (!guest.online) continue
            sendPayloadTo(
                guest.id,
                JSONObject()
                    .put("k", "start")
                    .put("game", selectedGame.id)
                    .put("seat", guest.seat),
            )
        }
        broadcastState()
    }

    fun hostDamasMove(from: Int, to: Int) {
        if (!isHost || activeGame != RoomGame.Damas) return
        val current = damasState ?: return
        if (current.turn != DamasColor.White) {
            hint = state.t("room.notYourTurn")
            return
        }
        val result = damas.move(current, from, to)
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        damasState = result.state
        hint = ""
        playDamasSfx(current, result.state)
        state.vibrate(context, if (result.state.lastMove?.captured != null) 42 else 18)
        finishDamas(result.state, hostWon = result.state.winner == DamasColor.White)
        broadcastState()
    }

    fun hostMancalaSow(pit: Int) {
        if (!isHost || activeGame != RoomGame.Mancala) return
        val current = mancalaState ?: return
        if (current.turn != MancalaSide.White) {
            hint = state.t("room.notYourTurn")
            return
        }
        val result = mancala.sow(current, pit)
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        mancalaState = result.state
        hint = ""
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        state.vibrate(context, 20)
        finishMancala(result.state, hostWon = result.state.winner == MancalaSide.White)
        broadcastState()
    }

    /* ---------------------------------------------------------------- cartas */

    fun hostBriscaPlay(cardId: String) {
        if (!isHost || activeGame != RoomGame.Brisca) return
        val board = briscaState ?: return
        if (board.seats.getOrNull(board.turn)?.isBot != false) {
            hint = state.t("room.notYourTurn")
            return
        }
        if (board.turn != 0) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyBrisca(brisca.playCard(board, cardId))
    }

    fun hostBriscaNextHand() {
        if (!isHost || activeGame != RoomGame.Brisca) return
        val board = briscaState ?: return
        applyBrisca(brisca.nextHand(board))
    }

    fun hostTrucoPlay(cardId: String) {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != 0 || board.pending != null) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyTruco(truco.playCard(board, cardId))
    }

    fun hostTrucoCallEnvido(kind: String = "envido") {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != 0 || board.pending != null) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyTruco(truco.callEnvido(board, 0, kind))
    }

    fun hostTrucoStartPlay() {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != 0 || board.pending != null || board.phase != TrucoPhase.Envido) return
        applyTruco(truco.startPlay(board))
    }

    fun hostTrucoCallTruco() {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != 0 || board.pending != null) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyTruco(truco.callTruco(board, 0))
    }

    fun hostTrucoRespond(accept: Boolean) {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        val pending = board.pending ?: return
        if (pending.awaiting != 0) return
        applyTruco(truco.respond(board, accept))
    }

    fun hostTrucoFold() {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != 0 || board.pending != null) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyTruco(truco.fold(board))
    }

    fun hostTrucoNextHand() {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        applyTruco(truco.nextHand(board))
    }

    fun hostLudoRoll() {
        if (!isHost || activeGame != RoomGame.Ludo) return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Roll || board.turn != 0 || board.players.getOrNull(0)?.isBot != false) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyLudo(ludo.roll(board))
    }

    fun hostLudoMove(tokenId: String) {
        if (!isHost || activeGame != RoomGame.Ludo) return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Move || board.turn != 0 || board.players.getOrNull(0)?.isBot != false) {
            hint = state.t("room.notYourTurn")
            return
        }
        applyLudo(ludo.move(board, tokenId))
    }

    fun hostBotLudoAction() {
        if (!isHost || activeGame != RoomGame.Ludo) return
        val board = ludoState ?: return
        if (board.phase == LudoPhase.MatchEnd) return
        val player = board.players.getOrNull(board.turn) ?: return
        if (!player.isBot) return
        when (val action = ludo.botAction(board)) {
            LudoAction.Roll -> applyLudo(ludo.roll(board))
            is LudoAction.Move -> applyLudo(ludo.move(board, action.tokenId))
            null -> Unit
        }
    }

    private fun applyLudo(result: com.mesaviva.app.games.LudoResult) {
        val previous = ludoState
        if (result.error != null && result.error !in setOf("ludo.error.noMoves", "ludo.error.threeSixes")) {
            hint = state.t(result.error)
            return
        }
        ludoState = result.state
        hint = ""
        val capturesBefore = previous?.players?.sumOf { it.captures } ?: 0
        val capturesAfter = result.state.players.sumOf { it.captures }
        when {
            capturesAfter > capturesBefore -> {
                NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
                state.vibrate(context, 55)
            }
            result.state.lastRoll != previous?.lastRoll -> {
                NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
                state.vibrate(context, 26)
            }
            result.state.lastMove != previous?.lastMove -> {
                NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                state.vibrate(context, 24)
            }
        }
        finishLudo(result.state)
        broadcastState()
    }

    private fun finishLudo(board: LudoState) {
        if (board.phase != LudoPhase.MatchEnd || awarded) return
        awarded = true
        val won = board.winnerIndex == 0
        state.awardMatch(win = won)
        NativeAudio.play(context, if (won) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
    }

    private fun syncLudoSeats() {
        val board = ludoState ?: return
        ludoState = board.copy(
            players = board.players.mapIndexed { index, player ->
                val guest = guests.firstOrNull { it.seat == index }
                player.copy(
                    name = when {
                        index == 0 -> hostName.ifBlank { state.t("room.hostFallback") }
                        guest != null -> guest.name
                        else -> player.name
                    },
                    isBot = if (index == 0) false else guest?.online != true,
                )
            },
        )
    }

    private fun applyBrisca(result: com.mesaviva.app.games.BriscaResult) {
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        val previous = briscaState
        briscaState = result.state
        hint = ""
        if (result.state.lastTrick != null && result.state.lastTrick != previous?.lastTrick) {
            val hostWon = result.state.lastTrick?.winner == 0 ||
                (result.state.teams && result.state.lastTrick?.winner?.let { result.state.seats.getOrNull(it)?.team == result.state.seats.getOrNull(0)?.team } == true)
            NativeAudio.play(context, if (hostWon) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card, state.sfx)
            state.vibrate(context, if (hostWon) 42 else 18)
        } else {
            NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
            state.vibrate(context, 18)
        }
        finishBrisca(result.state)
        broadcastState()
    }

    private fun finishBrisca(board: BriscaState) {
        if (board.phase != BriscaPhase.MatchEnd || awarded) return
        awarded = true
        val hostTeam = board.seats.getOrNull(0)?.team
        val won = board.winner?.let { seat ->
            if (board.teams) board.seats.getOrNull(seat)?.team == hostTeam else seat == 0
        } ?: false
        state.awardMatch(win = won, draw = board.draw)
        NativeAudio.play(
            context,
            when {
                board.draw -> NativeAudio.Sfx.Draw
                won -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
    }

    private fun applyTruco(result: com.mesaviva.app.games.TrucoResult) {
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        val previous = trucoState
        trucoState = result.state
        hint = ""
        val pendingResolved = previous?.pending != null && result.state.pending == null
        NativeAudio.play(
            context,
            if (pendingResolved) NativeAudio.Sfx.Promote else NativeAudio.Sfx.Card,
            state.sfx,
        )
        state.vibrate(context, if (result.state.phase == TrucoPhase.MatchEnd) 55 else 20)
        finishTruco(result.state)
        broadcastState()
    }

    private fun finishTruco(board: TrucoState) {
        if (board.phase != TrucoPhase.MatchEnd || awarded) return
        awarded = true
        val won = board.winner == 0
        state.awardMatch(win = won, draw = board.draw)
        NativeAudio.play(
            context,
            when {
                board.draw -> NativeAudio.Sfx.Draw
                won -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
    }

    fun hostBotBriscaAction() {
        if (!isHost || activeGame != RoomGame.Brisca) return
        val board = briscaState ?: return
        val seat = board.seats.getOrNull(board.turn) ?: return
        if (!seat.isBot) return
        when (val action = brisca.botAction(board)) {
            is BriscaAction.PlayCard -> applyBrisca(brisca.playCard(board, action.cardId))
            BriscaAction.NextHand -> applyBrisca(brisca.nextHand(board))
            null -> Unit
        }
    }

    fun hostBotTrucoAction() {
        if (!isHost || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        val pending = board.pending
        if (pending != null) {
            if (board.seats.getOrNull(pending.awaiting)?.isBot != true) return
            val accept = if (pending.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
                truco.botAcceptsEnvido(board, pending.awaiting, pending.stake)
            } else {
                truco.botAcceptsTruco(board, pending.awaiting, pending.stake)
            }
            applyTruco(truco.respond(board, accept))
            return
        }
        val seat = board.seats.getOrNull(board.turn) ?: return
        if (!seat.isBot) return
        when (board.phase) {
            TrucoPhase.Envido -> {
                if (truco.botWantsEnvido(board, board.turn)) {
                    applyTruco(truco.callEnvido(board, board.turn, "envido"))
                } else {
                    applyTruco(truco.startPlay(board))
                }
            }
            TrucoPhase.Play -> {
                if (truco.botShouldCallTruco(board, board.turn)) {
                    applyTruco(truco.callTruco(board, board.turn))
                } else {
                    val card = truco.botPlayCard(board, board.turn)
                    if (card != null) applyTruco(truco.playCard(board, card.id))
                }
            }
            else -> Unit
        }
    }

    private fun syncBriscaSeats() {
        val board = briscaState ?: return
        briscaState = board.copy(
            seats = board.seats.mapIndexed { index, seat ->
                val guest = guests.firstOrNull { it.seat == index }
                seat.copy(
                    name = when {
                        index == 0 -> hostName.ifBlank { state.t("room.hostFallback") }
                        guest != null -> guest.name
                        else -> seat.name
                    },
                    isBot = if (index == 0) false else guest?.online != true,
                )
            },
        )
    }

    private fun syncTrucoSeats() {
        val board = trucoState ?: return
        val guest = guests.firstOrNull { it.seat == 1 }
        trucoState = board.copy(
            seats = listOf(
                board.seats[0].copy(name = hostName.ifBlank { state.t("room.hostFallback") }, isBot = false),
                board.seats[1].copy(name = guest?.name ?: board.seats[1].name, isBot = guest?.online != true),
            ),
        )
    }

    fun hostBotDamasMove() {
        if (!isHost || !botSeat || activeGame != RoomGame.Damas) return
        val current = damasState ?: return
        if (current.phase != DamasPhase.Play || current.turn != DamasColor.Black) return
        val move = damas.botMove(current, "normal") ?: return
        val result = damas.move(current, move.from, move.to)
        if (result.error == null) {
            damasState = result.state
            playDamasSfx(current, result.state)
            finishDamas(result.state, hostWon = result.state.winner == DamasColor.White)
            broadcastState()
        }
    }

    fun hostBotMancalaMove() {
        if (!isHost || !botSeat || activeGame != RoomGame.Mancala) return
        val current = mancalaState ?: return
        if (current.phase != MancalaPhase.Play || current.turn != MancalaSide.Black) return
        val pit = mancala.botPit(current, "normal") ?: return
        val result = mancala.sow(current, pit)
        if (result.error == null) {
            mancalaState = result.state
            NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
            finishMancala(result.state, hostWon = result.state.winner == MancalaSide.White)
            broadcastState()
        }
    }

    fun hostResign() {
        if (!isHost) return
        when (activeGame) {
            RoomGame.Damas -> {
                val current = damasState ?: return
                if (current.phase == DamasPhase.MatchEnd) return
                damasState = current.copy(
                    phase = DamasPhase.MatchEnd,
                    winner = DamasColor.Black,
                    reason = "damas.reason.resign",
                )
                finishDamas(damasState!!, hostWon = false)
                broadcastState()
            }
            RoomGame.Mancala -> {
                val current = mancalaState ?: return
                if (current.phase == MancalaPhase.MatchEnd) return
                mancalaState = current.copy(
                    phase = MancalaPhase.MatchEnd,
                    winner = MancalaSide.Black,
                )
                finishMancala(mancalaState!!, hostWon = false)
                broadcastState()
            }
            RoomGame.Snakes -> {
                val current = snakesState ?: return
                if (current.finished) return
                snakesState = current.copy(winner = 1, lastEvent = "resign")
                finishSnakes(snakesState!!)
                broadcastState()
            }
            RoomGame.Brisca -> {
                val current = briscaState ?: return
                if (current.phase == BriscaPhase.MatchEnd) return
                briscaState = current.copy(
                    phase = BriscaPhase.MatchEnd,
                    winner = 1,
                    reason = "brisca.reason.resign",
                )
                finishBrisca(briscaState!!)
                broadcastState()
            }
            RoomGame.Truco -> {
                val current = trucoState ?: return
                if (current.phase == TrucoPhase.MatchEnd) return
                trucoState = current.copy(
                    phase = TrucoPhase.MatchEnd,
                    winner = 1,
                    reason = "truco.reason.resign",
                )
                finishTruco(trucoState!!)
                broadcastState()
            }
            RoomGame.Ludo -> {
                val current = ludoState ?: return
                if (current.phase == LudoPhase.MatchEnd) return
                ludoState = current.copy(phase = LudoPhase.MatchEnd, winnerIndex = 1)
                finishLudo(ludoState!!)
                broadcastState()
            }
            RoomGame.Domino -> {
                val current = dominoState ?: return
                if (current.phase == DominoPhase.MatchEnd) return
                val rival = (1 until current.players.size).firstOrNull() ?: 0
                dominoState = current.copy(
                    phase = DominoPhase.MatchEnd,
                    winnerIndices = listOf(rival),
                )
                finishDomino(dominoState!!)
                broadcastState()
            }
            null -> Unit
        }
    }

    fun chooseDominoPlayers(count: Int) {
        if (isHost && !inGame) dominoPlayers = count.coerceIn(2, 4)
    }

    fun chooseBriscaPlayers(count: Int) {
        if (isHost && !inGame) briscaPlayers = if (count >= 4) 4 else 2
    }

    fun chooseLudoPlayers(count: Int) {
        if (isHost && !inGame) ludoPlayers = count.coerceIn(2, 4)
    }

    fun dominoEndsFor(tileIndex: Int): List<DominoEnd> {
        val board = dominoState ?: return emptyList()
        val seat = if (isHost) 0 else mySeat
        val hand = board.players.getOrNull(seat)?.hand ?: return emptyList()
        val tile = hand.getOrNull(tileIndex) ?: return emptyList()
        val ends = mutableListOf<DominoEnd>()
        if (tile.fits(board.left)) ends.add(DominoEnd.Left)
        if (tile.fits(board.right)) ends.add(DominoEnd.Right)
        return ends
    }

    fun dominoHasPlayable(): Boolean {
        val board = dominoState ?: return false
        val seat = if (isHost) 0 else mySeat
        val hand = board.players.getOrNull(seat)?.hand ?: return false
        return hand.any { tile ->
            tile.fits(board.left) || tile.fits(board.right)
        }
    }

    fun hostDominoPlay(tileIndex: Int, end: DominoEnd) {
        if (!isHost || activeGame != RoomGame.Domino) return
        val current = dominoState ?: return
        applyHostDomino(domino.play(current, 0, tileIndex, end))
    }

    fun hostDominoDraw() {
        if (!isHost || activeGame != RoomGame.Domino) return
        val current = dominoState ?: return
        applyHostDomino(domino.draw(current, 0))
    }

    fun hostDominoPass() {
        if (!isHost || activeGame != RoomGame.Domino) return
        val current = dominoState ?: return
        applyHostDomino(domino.pass(current, 0))
    }

    fun hostDominoNextHand() {
        if (!isHost || activeGame != RoomGame.Domino) return
        val current = dominoState ?: return
        if (current.phase != DominoPhase.HandEnd) return
        val next = domino.nextHand(current)
        dominoState = next.copy(handCounts = next.players.map { it.hand.size })
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        broadcastState()
    }

    fun hostBotDominoMove() {
        if (!isHost || activeGame != RoomGame.Domino) return
        val current = dominoState ?: return
        if (current.phase == DominoPhase.HandEnd) {
            hostDominoNextHand()
            return
        }
        if (current.phase != DominoPhase.Playing) return
        val player = current.players.getOrNull(current.turn) ?: return
        if (!player.isBot) return
        when (val move = domino.botMove(current)) {
            null -> Unit
            else -> when (move.type) {
                DominoMoveType.Play -> {
                    val tile = move.tileIndex ?: return
                    val end = move.end ?: return
                    applyHostDomino(domino.play(current, current.turn, tile, end))
                }
                DominoMoveType.Draw -> applyHostDomino(domino.draw(current, current.turn))
                DominoMoveType.Pass -> applyHostDomino(domino.pass(current, current.turn))
                DominoMoveType.NextHand -> hostDominoNextHand()
            }
        }
    }

    fun guestDominoPlay(tileIndex: Int, end: DominoEnd) {
        if (!isGuest || activeGame != RoomGame.Domino || dominoPending) return
        val board = dominoState ?: return
        if (mySeat !in 0 until board.players.size || board.turn != mySeat) return
        if (board.phase != DominoPhase.Playing) return
        dominoPending = true
        sendPayload(JSONObject().put("k", "dominoPlay").put("tile", tileIndex).put("end", end.name.lowercase()))
    }

    fun guestDominoDraw() {
        if (!isGuest || activeGame != RoomGame.Domino || dominoPending) return
        val board = dominoState ?: return
        if (mySeat !in 0 until board.players.size || board.turn != mySeat) return
        if (board.phase != DominoPhase.Playing) return
        dominoPending = true
        sendPayload(JSONObject().put("k", "dominoDraw"))
    }

    fun guestDominoPass() {
        if (!isGuest || activeGame != RoomGame.Domino || dominoPending) return
        val board = dominoState ?: return
        if (mySeat !in 0 until board.players.size || board.turn != mySeat) return
        if (board.phase != DominoPhase.Playing) return
        dominoPending = true
        sendPayload(JSONObject().put("k", "dominoPass"))
    }

    private fun applyHostDomino(result: com.mesaviva.app.games.DominoResult) {
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        dominoState = result.state.copy(handCounts = result.state.players.map { it.hand.size })
        hint = ""
        val board = dominoState ?: return
        when {
            board.phase == DominoPhase.MatchEnd -> finishDomino(board)
            board.phase == DominoPhase.HandEnd -> NativeAudio.play(
                context,
                if (board.winnerIndices.isEmpty()) NativeAudio.Sfx.Draw else NativeAudio.Sfx.Promote,
                state.sfx,
            )
            else -> NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        }
        state.vibrate(context, if (board.phase == DominoPhase.MatchEnd) 70 else 22)
        broadcastState()
    }

    private fun finishDomino(board: DominoState) {
        if (board.phase != DominoPhase.MatchEnd || awarded) return
        awarded = true
        val won = 0 in board.winnerIndices
        state.awardMatch(win = won, draw = board.winnerIndices.isEmpty())
        NativeAudio.play(
            context,
            when {
                board.winnerIndices.isEmpty() -> NativeAudio.Sfx.Draw
                won -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
    }

    private fun hostGuestDominoPlay(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Domino) return
        val guest = guests.firstOrNull { it.id == fromId } ?: return
        val current = dominoState ?: return
        if (guest.seat !in 1 until current.players.size) return
        if (current.turn != guest.seat || current.phase != DominoPhase.Playing) return
        val tileIndex = payload.optInt("tile", -1)
        val end = if (payload.optString("end") == "left") DominoEnd.Left else DominoEnd.Right
        val result = domino.play(current, guest.seat, tileIndex, end)
        if (result.error != null) {
            sendStateTo(guest.id)
            return
        }
        applyHostDomino(result)
    }

    private fun hostGuestDominoDraw(fromId: String?) {
        if (activeGame != RoomGame.Domino) return
        val guest = guests.firstOrNull { it.id == fromId } ?: return
        val current = dominoState ?: return
        if (guest.seat !in 1 until current.players.size) return
        if (current.turn != guest.seat || current.phase != DominoPhase.Playing) return
        val result = domino.draw(current, guest.seat)
        if (result.error != null) {
            sendStateTo(guest.id)
            return
        }
        applyHostDomino(result)
    }

    private fun hostGuestDominoPass(fromId: String?) {
        if (activeGame != RoomGame.Domino) return
        val guest = guests.firstOrNull { it.id == fromId } ?: return
        val current = dominoState ?: return
        if (guest.seat !in 1 until current.players.size) return
        if (current.turn != guest.seat || current.phase != DominoPhase.Playing) return
        val result = domino.pass(current, guest.seat)
        if (result.error != null) {
            sendStateTo(guest.id)
            return
        }
        applyHostDomino(result)
    }

    private fun syncDominoSeats() {
        val current = dominoState ?: return
        val players = current.players.mapIndexed { index, player ->
            when {
                index == 0 -> player.copy(name = hostName.ifBlank { state.t("room.hostFallback") }, isBot = false)
                else -> {
                    val guest = guests.firstOrNull { it.seat == index }
                    if (guest != null) {
                        player.copy(name = guest.name, isBot = !guest.online)
                    } else {
                        player.copy(name = botName(index), isBot = true)
                    }
                }
            }
        }
        dominoState = current.copy(
            players = players,
            handCounts = current.handCounts.ifEmpty { players.map { it.hand.size } },
        )
    }

    fun hostToggleBot() {
        if (!isHost || !inGame || activeGame in setOf(RoomGame.Snakes, RoomGame.Domino)) return
        botSeat = !botSeat
        hint = state.t(if (botSeat) "room.botActive" else "room.botOff")
    }

    private fun hostGuestDamasMove(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Damas || botSeat) return
        if (guests.firstOrNull { it.id == fromId }?.seat != 1) return
        val current = damasState ?: return
        if (current.turn != DamasColor.Black) return
        val from = payload.optInt("from", -1)
        val to = payload.optInt("to", -1)
        if (from !in 0..63 || to !in 0..63) return
        val result = damas.move(current, from, to)
        if (result.error != null) {
            sendStateTo(guests.firstOrNull { it.online }?.id ?: return)
            return
        }
        damasState = result.state
        playDamasSfx(current, result.state)
        finishDamas(result.state, hostWon = result.state.winner == DamasColor.White)
        broadcastState()
    }

    private fun hostGuestMancalaSow(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Mancala || botSeat) return
        if (guests.firstOrNull { it.id == fromId }?.seat != 1) return
        val current = mancalaState ?: return
        if (current.turn != MancalaSide.Black) return
        val pit = payload.optInt("pit", -1)
        val result = mancala.sow(current, pit)
        if (result.error != null) {
            sendStateTo(guests.firstOrNull { it.online }?.id ?: return)
            return
        }
        mancalaState = result.state
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        finishMancala(result.state, hostWon = result.state.winner == MancalaSide.White)
        broadcastState()
    }

    private fun hostGuestBriscaPlay(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Brisca) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = briscaState ?: return
        if (board.turn != seat || board.seats.getOrNull(seat)?.isBot != false) return
        val cardId = payload.optString("card").takeIf { it.isNotBlank() } ?: return
        applyBrisca(brisca.playCard(board, cardId))
    }

    private fun hostGuestBriscaNextHand(fromId: String?) {
        if (activeGame != RoomGame.Brisca) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = briscaState ?: return
        if (seat !in board.seats.indices || board.phase != BriscaPhase.HandEnd) return
        applyBrisca(brisca.nextHand(board))
    }

    private fun hostGuestTrucoPlay(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (board.seats.getOrNull(seat)?.isBot != false) return
        if (board.turn != seat || board.pending != null || board.phase != TrucoPhase.Play) return
        val cardId = payload.optString("card").takeIf { it.isNotBlank() } ?: return
        applyTruco(truco.playCard(board, cardId))
    }

    private fun hostGuestTrucoEnvido(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (board.seats.getOrNull(seat)?.isBot != false) return
        if (board.turn != seat || board.pending != null || board.phase != TrucoPhase.Envido) return
        val kind = payload.optString("kind", "envido").takeIf { it in setOf("envido", "realEnvido") } ?: "envido"
        applyTruco(truco.callEnvido(board, seat, kind))
    }

    private fun hostGuestTrucoStart(fromId: String?) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (board.seats.getOrNull(seat)?.isBot != false) return
        if (board.turn != seat || board.pending != null || board.phase != TrucoPhase.Envido) return
        applyTruco(truco.startPlay(board))
    }

    private fun hostGuestTrucoCall(fromId: String?) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (board.seats.getOrNull(seat)?.isBot != false) return
        if (board.turn != seat || board.pending != null || board.phase != TrucoPhase.Play) return
        applyTruco(truco.callTruco(board, seat))
    }

    private fun hostGuestTrucoRespond(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        val pending = board.pending ?: return
        if (pending.awaiting != seat) return
        applyTruco(truco.respond(board, payload.optBoolean("accept", false)))
    }

    private fun hostGuestTrucoFold(fromId: String?) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (board.turn != seat || board.pending != null) return
        applyTruco(truco.fold(board))
    }

    private fun hostGuestTrucoNextHand(fromId: String?) {
        if (activeGame != RoomGame.Truco) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = trucoState ?: return
        if (seat !in board.seats.indices || board.phase != TrucoPhase.HandEnd) return
        applyTruco(truco.nextHand(board))
    }

    private fun hostGuestLudoRoll(fromId: String?) {
        if (activeGame != RoomGame.Ludo) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Roll || board.turn != seat || board.players.getOrNull(seat)?.isBot != false) return
        applyLudo(ludo.roll(board))
    }

    private fun hostGuestLudoMove(fromId: String?, payload: JSONObject) {
        if (activeGame != RoomGame.Ludo) return
        val seat = guests.firstOrNull { it.id == fromId }?.seat ?: return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Move || board.turn != seat || board.players.getOrNull(seat)?.isBot != false) return
        val tokenId = payload.optString("token").takeIf { it.isNotBlank() } ?: return
        applyLudo(ludo.move(board, tokenId))
    }

    private fun hostGuestResign(fromId: String?) {
        val guest = guests.firstOrNull { it.id == fromId } ?: return
        when (activeGame) {
            RoomGame.Snakes -> {
                if (guest.seat !in 1..3) return
                val current = snakesState ?: return
                if (current.finished) return
                snakesState = current.copy(winner = 0, lastEvent = "resign")
                finishSnakes(snakesState!!)
                broadcastState()
            }
            RoomGame.Damas -> {
                if (guest.seat != 1) return
                val current = damasState ?: return
                if (current.phase == DamasPhase.MatchEnd) return
                damasState = current.copy(
                    phase = DamasPhase.MatchEnd,
                    winner = DamasColor.White,
                    reason = "damas.reason.resign",
                )
                finishDamas(damasState!!, hostWon = true)
                broadcastState()
            }
            RoomGame.Mancala -> {
                if (guest.seat != 1) return
                val current = mancalaState ?: return
                if (current.phase == MancalaPhase.MatchEnd) return
                mancalaState = current.copy(
                    phase = MancalaPhase.MatchEnd,
                    winner = MancalaSide.White,
                )
                finishMancala(mancalaState!!, hostWon = true)
                broadcastState()
            }
            RoomGame.Domino -> {
                val current = dominoState ?: return
                if (guest.seat !in 1 until current.players.size) return
                if (current.phase == DominoPhase.MatchEnd) return
                dominoState = current.copy(
                    phase = DominoPhase.MatchEnd,
                    winnerIndices = listOf(0),
                )
                finishDomino(dominoState!!)
                broadcastState()
            }
            RoomGame.Brisca -> {
                val current = briscaState ?: return
                if (guest.seat !in current.seats.indices) return
                if (current.phase == BriscaPhase.MatchEnd) return
                briscaState = current.copy(
                    phase = BriscaPhase.MatchEnd,
                    winner = 0,
                    reason = "brisca.reason.resign",
                )
                finishBrisca(briscaState!!)
                broadcastState()
            }
            RoomGame.Truco -> {
                val current = trucoState ?: return
                if (guest.seat !in current.seats.indices) return
                if (current.phase == TrucoPhase.MatchEnd) return
                trucoState = current.copy(
                    phase = TrucoPhase.MatchEnd,
                    winner = 0,
                    reason = "truco.reason.resign",
                )
                finishTruco(trucoState!!)
                broadcastState()
            }
            RoomGame.Ludo -> {
                val current = ludoState ?: return
                if (guest.seat !in current.players.indices) return
                if (current.phase == LudoPhase.MatchEnd) return
                ludoState = current.copy(phase = LudoPhase.MatchEnd, winnerIndex = 0)
                finishLudo(ludoState!!)
                broadcastState()
            }
            null -> Unit
        }
    }

    fun hostSnakesRoll() {
        if (!isHost || activeGame != RoomGame.Snakes) return
        if (snakesState?.turn != 0) return
        rollSnakes(0)
    }

    fun hostBotSnakesRoll() {
        if (!isHost || activeGame != RoomGame.Snakes) return
        val current = snakesState ?: return
        if (current.finished) return
        if (current.bots.getOrElse(current.turn) { false }) rollSnakes(current.turn)
    }

    fun guestSnakesRoll() {
        if (!isGuest || activeGame != RoomGame.Snakes) return
        val current = snakesState ?: return
        if (current.finished || current.turn != mySeat) return
        sendPayload(JSONObject().put("k", "snakesRoll"))
    }

    private fun hostGuestSnakesRoll(fromId: String?) {
        if (activeGame != RoomGame.Snakes) return
        val guest = guests.firstOrNull { it.id == fromId } ?: return
        if (guest.seat !in 1..3) return
        if (snakesState?.turn != guest.seat) return
        rollSnakes(guest.seat)
    }

    private fun rollSnakes(seat: Int) {
        val current = snakesState ?: return
        if (current.finished || current.turn != seat) return
        val dice = snakesRandom.nextInt(1, 7)
        val next = SnakesRoomRules.roll(current, seat, dice)
        if (next == current) return
        snakesState = next
        NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
        state.vibrate(context, if (next.lastEvent == "snake") 55 else 24)
        when (next.lastEvent) {
            "ladder" -> NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
            "snake" -> NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
            "win" -> finishSnakes(next)
            else -> Unit
        }
        broadcastState()
    }

    private fun finishSnakes(board: SnakesRoomState) {
        val winner = board.winner ?: return
        if (awarded) return
        awarded = true
        if (isHost) {
            state.awardMatch(win = winner == 0)
            NativeAudio.play(context, if (winner == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
        } else {
            state.awardMatch(win = winner == mySeat)
            NativeAudio.play(context, if (winner == mySeat) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
        }
    }

    private fun syncSnakesSeats() {
        val current = snakesState ?: return
        val names = MutableList(4) { index ->
            if (index == 0) hostName.ifBlank { state.t("room.hostFallback") } else botName(index)
        }
        val bots = MutableList(4) { it != 0 }
        for (guest in guests) {
            if (guest.seat in 1..3) {
                names[guest.seat] = guest.name
                bots[guest.seat] = !guest.online
            }
        }
        snakesState = current.copy(names = names, bots = bots)
    }

    private fun botName(seat: Int): String = when (seat) {
        1 -> "Bot Sayona"
        2 -> "Bot Silbón"
        else -> "Bot Hacha"
    }

    private fun nextSeat(): Int {
        val used = guests.map { it.seat }.toSet()
        return (1..3).firstOrNull { it !in used } ?: 0
    }

    private fun finishDamas(board: DamasState, hostWon: Boolean) {
        if (board.phase != DamasPhase.MatchEnd) return
        if (isHost) {
            if (awarded) return
            awarded = true
            state.awardMatch(win = hostWon, draw = board.draw)
        }
        when {
            board.draw -> NativeAudio.play(context, NativeAudio.Sfx.Draw, state.sfx)
            hostWon -> NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
            else -> NativeAudio.play(context, NativeAudio.Sfx.Lose, state.sfx)
        }
    }

    private fun finishMancala(board: MancalaState, hostWon: Boolean) {
        if (board.phase != MancalaPhase.MatchEnd) return
        if (isHost) {
            if (awarded) return
            awarded = true
            state.awardMatch(win = hostWon, draw = board.whiteStore == board.blackStore)
        }
        when {
            board.whiteStore == board.blackStore -> NativeAudio.play(context, NativeAudio.Sfx.Draw, state.sfx)
            hostWon -> NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
            else -> NativeAudio.play(context, NativeAudio.Sfx.Lose, state.sfx)
        }
    }

    private fun playDamasSfx(previous: DamasState, next: DamasState) {
        val move = next.lastMove ?: return
        if (next.phase == DamasPhase.MatchEnd) return
        when {
            move.promotion -> NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
            move.captured != null -> NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
            else -> NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        }
    }

    private fun broadcastState() {
        if (!isHost) return
        if (activeGame == RoomGame.Domino) {
            val board = dominoState ?: return
            for (guest in guests) {
                if (guest.online) {
                    sendPayloadTo(guest.id, RoomCodec.dominoToJson(board, guest.seat))
                }
            }
            return
        }
        if (activeGame == RoomGame.Brisca) {
            val board = briscaState ?: return
            for (guest in guests) {
                if (guest.online) {
                    sendPayloadTo(guest.id, RoomCodec.briscaToJson(board, guest.seat))
                }
            }
            return
        }
        if (activeGame == RoomGame.Truco) {
            val board = trucoState ?: return
            for (guest in guests) {
                if (guest.online) {
                    sendPayloadTo(guest.id, RoomCodec.trucoToJson(board, guest.seat))
                }
            }
            return
        }
        val payload = stateJson() ?: return
        sendPayloadToGuests(payload)
    }

    private fun sendStateTo(guest: String) {
        if (!isHost || guest.isBlank()) return
        val seat = guests.firstOrNull { it.id == guest }?.seat ?: 1
        stateJson(seat)?.let { sendPayloadTo(guest, it) }
    }

    private fun stateJson(viewerSeat: Int = 0): JSONObject? = when (activeGame) {
        RoomGame.Damas -> damasState?.let { damasJson(it) }
        RoomGame.Mancala -> mancalaState?.let { mancalaJson(it) }
        RoomGame.Domino -> dominoState?.let { RoomCodec.dominoToJson(it, viewerSeat) }
        RoomGame.Snakes -> snakesState?.let { RoomCodec.snakesToJson(it) }
        RoomGame.Brisca -> briscaState?.let { RoomCodec.briscaToJson(it, viewerSeat) }
        RoomGame.Truco -> trucoState?.let { RoomCodec.trucoToJson(it, viewerSeat) }
        RoomGame.Ludo -> ludoState?.let { RoomCodec.ludoToJson(it) }
        null -> null
    }

    /* --------------------------------------------------------------- invitado */

    fun joinLink(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        if (text.startsWith("mesaviva://", ignoreCase = true)) {
            val uri = try {
                Uri.parse(text)
            } catch (_: Exception) {
                return false
            }
            val ws = uri.getQueryParameter("ws")
            val roomCode = uri.getQueryParameter("code")
            if (ws.isNullOrBlank() || roomCode.isNullOrBlank()) {
                hint = state.t("room.errorInvalidLink")
                return false
            }
            return join(ws, roomCode)
        }
        return false
    }

    fun join(wsUrl: String, roomCode: String): Boolean {
        if (!isGuest || status != RoomStatus.Idle) return false
        val url = normalizeWsUrl(wsUrl)
        if (url == null) {
            hint = state.t("room.errorConnect")
            return false
        }
        val cleanCode = roomCode.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6)
        if (cleanCode.length < 4) {
            hint = state.t("room.invalidCode")
            return false
        }
        pendingUrl = url
        pendingCode = cleanCode
        onlineRelay = !isLocalRelay(url)
        reconnectAttempts = 0
        status = RoomStatus.Connecting
        appendSystem(state.t("room.connectingRoom"))
        connectGuest(pendingUrl!!, cleanCode)
        return true
    }

    private fun connectGuest(url: String, roomCode: String) {
        if (closedByUser) return
        val ws = try {
            object : WebSocketClient(URI(url)) {
                override fun onOpen(handshake: ServerHandshake) {
                    val message = JSONObject()
                        .put("t", if (guestId != null) "rejoin" else "join")
                        .put("code", roomCode)
                        .put("name", myName())
                        .put("avatar", state.avatar)
                    if (guestId != null) message.put("guestId", guestId)
                    sendRaw(message)
                }

                override fun onMessage(message: String) {
                    val json = parse(message) ?: return
                    main.post { handleGuestMessage(json) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    main.post {
                        if (!closedByUser && status != RoomStatus.Closed) scheduleGuestReconnect()
                    }
                }

                override fun onError(ex: Exception) {
                    // La reconexión se gestiona en onClose.
                }
            }
        } catch (_: Exception) {
            status = RoomStatus.Error
            error = state.t("room.errorConnect")
            return
        }
        client = ws
        try {
            ws.connect()
        } catch (_: Exception) {
            scheduleGuestReconnect()
        }
    }

    private fun scheduleGuestReconnect() {
        if (closedByUser) return
        if (reconnectAttempts >= 6 || pendingUrl == null || pendingCode == null) {
            status = RoomStatus.Error
            error = state.t("room.errorReconnect")
            appendSystem(state.t("room.errorReconnect"))
            return
        }
        status = RoomStatus.Reconnecting
        reconnectAttempts += 1
        appendSystem(state.t("room.reconnecting", mapOf("n" to reconnectAttempts)))
        main.postDelayed({
            if (!closedByUser && status == RoomStatus.Reconnecting) {
                pendingUrl?.let { url -> pendingCode?.let { c -> connectGuest(url, c) } }
            }
        }, 1200L * reconnectAttempts)
    }

    private fun handleGuestMessage(msg: JSONObject) {
        when (msg.optString("t")) {
            "joined" -> {
                val id = msg.optString("guestId")
                if (id.isNotBlank()) guestId = id
                reconnectAttempts = 0
                status = RoomStatus.Joined
                if (chat.none { it.text == state.t("room.connectedRoom") }) {
                    appendSystem(state.t("room.connectedRoom"))
                }
                sendPayload(JSONObject().put("k", "ready"))
            }
            "message" -> msg.optJSONObject("payload")?.let { handleGuestPayload(it) }
            "closed" -> {
                status = RoomStatus.Closed
                error = state.t("room.hostClosed")
                appendSystem(state.t("room.hostClosed"))
            }
            "error" -> {
                val key = msg.optString("key")
                if (key == "sala.rejoinExpired") {
                    guestId = null
                    val message = JSONObject()
                        .put("t", "join")
                        .put("code", pendingCode ?: "")
                        .put("name", myName())
                        .put("avatar", state.avatar)
                    sendRaw(message)
                } else {
                    hint = when (key) {
                        "sala.invalid" -> state.t("room.roomGone")
                        "sala.full" -> state.t("room.full")
                        "sala.invalidCode" -> state.t("room.invalidCode")
                        else -> state.t("room.errorGeneric")
                    }
                    status = RoomStatus.Error
                    error = hint
                }
            }
        }
    }

    private fun handleGuestPayload(payload: JSONObject) {
        when (payload.optString("k")) {
            "welcome" -> {
                hostName = payload.optString("host", state.t("room.hostFallback"))
                mySeat = payload.optInt("seat", mySeat).coerceIn(0, 3)
                appendSystem(state.t("room.welcomeHost", mapOf("name" to hostName)))
            }
            "seat" -> {
                mySeat = payload.optInt("seat", mySeat).coerceIn(0, 3)
            }
            "start" -> {
                activeGame = RoomGame.fromId(payload.optString("game"))
                mySeat = payload.optInt("seat", mySeat).coerceIn(0, 3)
                damasState = null
                mancalaState = null
                snakesState = null
                dominoState = null
                briscaState = null
                trucoState = null
                ludoState = null
                dominoPending = false
                awarded = false
                appendSystem(state.t("room.youAreInMatch"))
                NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
            }
            "state" -> applyGuestState(payload)
            "chat" -> {
                val text = payload.optString("text").trim().take(200)
                if (text.isNotEmpty()) {
                    appendChat(payload.optString("name", hostName), text, mine = false, system = false)
                    NativeAudio.play(context, NativeAudio.Sfx.Chat, state.sfx)
                }
            }
        }
    }

    private fun applyGuestState(payload: JSONObject) {
        when (payload.optString("game")) {
            RoomGame.Damas.id -> {
                val parsed = damasFromJson(payload) ?: return
                val previous = damasState
                damasState = parsed
                activeGame = RoomGame.Damas
                if (parsed.history.size > (previous?.history?.size ?: 0)) {
                    playDamasSfx(previous ?: parsed, parsed)
                }
                if (parsed.phase == DamasPhase.MatchEnd && !awarded && mySeat == 1) {
                    awarded = true
                    val won = parsed.winner == DamasColor.Black
                    state.awardMatch(win = won, draw = parsed.draw)
                    when {
                        parsed.draw -> NativeAudio.play(context, NativeAudio.Sfx.Draw, state.sfx)
                        won -> NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
                        else -> NativeAudio.play(context, NativeAudio.Sfx.Lose, state.sfx)
                    }
                }
            }
            RoomGame.Mancala.id -> {
                val parsed = mancalaFromJson(payload) ?: return
                val previous = mancalaState
                mancalaState = parsed
                activeGame = RoomGame.Mancala
                if (previous != null && parsed.pits != previous.pits) {
                    NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                }
                if (parsed.phase == MancalaPhase.MatchEnd && !awarded && mySeat == 1) {
                    awarded = true
                    val draw = parsed.whiteStore == parsed.blackStore
                    val won = parsed.winner == MancalaSide.Black
                    state.awardMatch(win = won, draw = draw)
                    when {
                        draw -> NativeAudio.play(context, NativeAudio.Sfx.Draw, state.sfx)
                        won -> NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
                        else -> NativeAudio.play(context, NativeAudio.Sfx.Lose, state.sfx)
                    }
                }
            }
            RoomGame.Domino.id -> {
                val parsed = RoomCodec.dominoFromJson(payload) ?: return
                val previous = dominoState
                dominoState = parsed
                activeGame = RoomGame.Domino
                dominoPending = false
                if (parsed.phase == DominoPhase.MatchEnd && !awarded) {
                    awarded = true
                    val won = mySeat in parsed.winnerIndices
                    state.awardMatch(win = won, draw = parsed.winnerIndices.isEmpty())
                    NativeAudio.play(
                        context,
                        when {
                            parsed.winnerIndices.isEmpty() -> NativeAudio.Sfx.Draw
                            won -> NativeAudio.Sfx.Win
                            else -> NativeAudio.Sfx.Lose
                        },
                        state.sfx,
                    )
                } else if (parsed.phase == DominoPhase.HandEnd) {
                    NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
                } else if (previous == null || parsed.chain.size > previous.chain.size) {
                    NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                }
            }
            RoomGame.Snakes.id -> {
                val parsed = RoomCodec.snakesFromJson(payload) ?: return
                val previous = snakesState
                snakesState = parsed
                activeGame = RoomGame.Snakes
                if (previous?.dice != parsed.dice && parsed.dice > 0) {
                    NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
                    when (parsed.lastEvent) {
                        "ladder" -> NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
                        "snake" -> NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
                        else -> Unit
                    }
                }
                if (parsed.finished) finishSnakes(parsed)
            }
            RoomGame.Brisca.id -> {
                val parsed = RoomCodec.briscaFromJson(payload) ?: return
                val previous = briscaState
                briscaState = parsed
                activeGame = RoomGame.Brisca
                if (parsed.lastTrick != null && parsed.lastTrick != previous?.lastTrick) {
                    val won = parsed.lastTrick?.winner == mySeat
                    NativeAudio.play(context, if (won) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card, state.sfx)
                    state.vibrate(context, if (won) 40 else 16)
                } else if (previous != null && parsed.history.size > previous.history.size) {
                    NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                }
                if (parsed.phase == BriscaPhase.MatchEnd && !awarded) {
                    awarded = true
                    val hostTeam = parsed.seats.getOrNull(0)?.team
                    val won = parsed.winner?.let { seat ->
                        if (parsed.teams) parsed.seats.getOrNull(seat)?.team == hostTeam else seat == mySeat
                    } ?: false
                    state.awardMatch(win = won, draw = parsed.draw)
                    NativeAudio.play(
                        context,
                        when {
                            parsed.draw -> NativeAudio.Sfx.Draw
                            won -> NativeAudio.Sfx.Win
                            else -> NativeAudio.Sfx.Lose
                        },
                        state.sfx,
                    )
                }
            }
            RoomGame.Truco.id -> {
                val parsed = RoomCodec.trucoFromJson(payload) ?: return
                val previous = trucoState
                trucoState = parsed
                activeGame = RoomGame.Truco
                if (parsed.phase == TrucoPhase.MatchEnd && !awarded) {
                    awarded = true
                    val won = parsed.winner == mySeat
                    state.awardMatch(win = won, draw = parsed.draw)
                    NativeAudio.play(
                        context,
                        when {
                            parsed.draw -> NativeAudio.Sfx.Draw
                            won -> NativeAudio.Sfx.Win
                            else -> NativeAudio.Sfx.Lose
                        },
                        state.sfx,
                    )
                } else if (previous != null && parsed.history.size > previous.history.size) {
                    NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                }
            }
            RoomGame.Ludo.id -> {
                val parsed = RoomCodec.ludoFromJson(payload) ?: return
                val previous = ludoState
                ludoState = parsed
                activeGame = RoomGame.Ludo
                if (previous?.lastRoll != parsed.lastRoll) {
                    NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
                } else if (previous?.lastMove != parsed.lastMove) {
                    NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
                }
                if (parsed.phase == LudoPhase.MatchEnd && !awarded) {
                    awarded = true
                    val won = parsed.winnerIndex == mySeat
                    state.awardMatch(win = won)
                    NativeAudio.play(context, if (won) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
                }
            }
        }
    }

    fun guestDamasMove(from: Int, to: Int) {
        if (!isGuest || activeGame != RoomGame.Damas || mySeat != 1) return
        val current = damasState ?: return
        if (current.turn != DamasColor.Black) {
            hint = state.t("room.notYourTurn")
            return
        }
        val result = damas.move(current, from, to)
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        damasState = result.state
        hint = ""
        playDamasSfx(current, result.state)
        state.vibrate(context, if (result.state.lastMove?.captured != null) 42 else 18)
        sendPayload(JSONObject().put("k", "move").put("from", from).put("to", to))
    }

    fun guestMancalaSow(pit: Int) {
        if (!isGuest || activeGame != RoomGame.Mancala || mySeat != 1) return
        val current = mancalaState ?: return
        if (current.turn != MancalaSide.Black) {
            hint = state.t("room.notYourTurn")
            return
        }
        val result = mancala.sow(current, pit)
        if (result.error != null) {
            hint = state.t(result.error)
            return
        }
        mancalaState = result.state
        hint = ""
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        state.vibrate(context, 20)
        sendPayload(JSONObject().put("k", "sow").put("pit", pit))
    }

    fun guestResign() {
        if (!isGuest) return
        if (activeGame in setOf(RoomGame.Damas, RoomGame.Mancala) && mySeat != 1) return
        if (activeGame == RoomGame.Brisca && mySeat !in (briscaState?.seats?.indices ?: emptyList())) return
        if (activeGame == RoomGame.Truco && mySeat !in (trucoState?.seats?.indices ?: emptyList())) return
        if (activeGame == RoomGame.Ludo && mySeat !in (ludoState?.players?.indices ?: emptyList())) return
        sendPayload(JSONObject().put("k", "resign"))
    }

    fun guestBriscaPlay(cardId: String) {
        if (!isGuest || activeGame != RoomGame.Brisca) return
        val board = briscaState ?: return
        if (board.turn != mySeat || board.phase != BriscaPhase.Playing) {
            hint = state.t("room.notYourTurn")
            return
        }
        if (board.hands.getOrElse(mySeat) { emptyList() }.none { it.id == cardId }) return
        sendPayload(JSONObject().put("k", "briscaPlay").put("card", cardId))
    }

    fun guestBriscaNextHand() {
        if (!isGuest || activeGame != RoomGame.Brisca) return
        if (briscaState?.phase != BriscaPhase.HandEnd) return
        sendPayload(JSONObject().put("k", "briscaNextHand"))
    }

    fun guestTrucoPlay(cardId: String) {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != mySeat || board.pending != null || board.phase != TrucoPhase.Play) {
            hint = state.t("room.notYourTurn")
            return
        }
        if (board.hands.getOrElse(mySeat) { emptyList() }.none { it.id == cardId }) return
        sendPayload(JSONObject().put("k", "trucoPlay").put("card", cardId))
    }

    fun guestTrucoEnvido(kind: String = "envido") {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != mySeat || board.pending != null || board.phase != TrucoPhase.Envido) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "trucoEnvido").put("kind", kind))
    }

    fun guestTrucoStartPlay() {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != mySeat || board.pending != null || board.phase != TrucoPhase.Envido) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "trucoStart"))
    }

    fun guestTrucoCall() {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != mySeat || board.pending != null || board.phase != TrucoPhase.Play) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "trucoCall"))
    }

    fun guestTrucoRespond(accept: Boolean) {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.pending?.awaiting != mySeat) return
        sendPayload(JSONObject().put("k", "trucoRespond").put("accept", accept))
    }

    fun guestTrucoFold() {
        if (!isGuest || activeGame != RoomGame.Truco) return
        val board = trucoState ?: return
        if (board.turn != mySeat || board.pending != null) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "trucoFold"))
    }

    fun guestTrucoNextHand() {
        if (!isGuest || activeGame != RoomGame.Truco) return
        if (trucoState?.phase != TrucoPhase.HandEnd) return
        sendPayload(JSONObject().put("k", "trucoNextHand"))
    }

    fun guestLudoRoll() {
        if (!isGuest || activeGame != RoomGame.Ludo) return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Roll || board.turn != mySeat || board.players.getOrNull(mySeat)?.isBot != false) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "ludoRoll"))
    }

    fun guestLudoMove(tokenId: String) {
        if (!isGuest || activeGame != RoomGame.Ludo) return
        val board = ludoState ?: return
        if (board.phase != LudoPhase.Move || board.turn != mySeat || board.players.getOrNull(mySeat)?.isBot != false) {
            hint = state.t("room.notYourTurn")
            return
        }
        sendPayload(JSONObject().put("k", "ludoMove").put("token", tokenId))
    }

    fun sendChat(text: String) {
        val clean = text.trim().take(200)
        if (clean.isEmpty()) return
        appendChat(myName(), clean, mine = true, system = false)
        NativeAudio.play(context, NativeAudio.Sfx.Chat, state.sfx)
        val payload = JSONObject()
            .put("k", "chat")
            .put("name", myName())
            .put("text", clean)
        if (isHost) sendPayloadToGuests(payload) else sendPayload(payload)
    }

    /* --------------------------------------------------------------- protocolo */

    private fun sendPayload(payload: JSONObject) {
        if (isGuest) {
            sendRaw(JSONObject().put("t", "send").put("payload", payload))
        }
    }

    private fun sendPayloadTo(guest: String, payload: JSONObject) {
        if (!isHost) return
        sendRaw(JSONObject().put("t", "send").put("to", guest).put("payload", payload))
    }

    private fun sendPayloadToGuests(payload: JSONObject) {
        if (!isHost) return
        for (guest in guests) {
            if (guest.online) sendPayloadTo(guest.id, payload)
        }
    }

    private fun sendRaw(json: JSONObject) {
        val socket = client ?: return
        runCatching {
            if (socket.isOpen) socket.send(json.toString())
        }
    }

    private fun parse(raw: String?): JSONObject? {
        if (raw.isNullOrBlank() || raw.length > 64 * 1024) return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        closedByUser = true
        runCatching { client?.close() }
        client = null
        relay?.stop()
        relay = null
        status = RoomStatus.Closed
    }

    fun clearHint() {
        hint = ""
    }

    fun damasLegalMoves(): List<DamasMove> {
        val board = damasState ?: return emptyList()
        if (board.phase != DamasPhase.Play) return emptyList()
        return damas.legalMoves(board)
    }

    fun returnToLobby() {
        activeGame = null
        damasState = null
        mancalaState = null
        snakesState = null
        dominoState = null
        dominoPending = false
        awarded = false
        botSeat = false
    }

    fun selectGame(game: RoomGame) {
        if (isHost && !inGame) selectedGame = game
    }

    private fun damasJson(board: DamasState): JSONObject = RoomCodec.damasToJson(board)

    private fun damasFromJson(json: JSONObject): DamasState? = RoomCodec.damasFromJson(json)

    private fun mancalaJson(board: MancalaState): JSONObject = RoomCodec.mancalaToJson(board)

    private fun mancalaFromJson(json: JSONObject): MancalaState? = RoomCodec.mancalaFromJson(json)


    /* ------------------------------------------------------------------- util */

    private fun appendChat(author: String, text: String, mine: Boolean, system: Boolean) {
        val timestamp = LocalTime.now().format(timeFormat)
        chat = (chat + RoomChat(author, text, mine, system, timestamp)).takeLast(120)
    }

    private fun appendSystem(text: String) {
        if (text.isBlank()) return
        val timestamp = LocalTime.now().format(timeFormat)
        chat = (chat + RoomChat("", text, mine = false, system = true, time = timestamp)).takeLast(120)
    }

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5).map { alphabet.random() }.joinToString("")
    }

    private fun isLocalRelay(url: String): Boolean {
        val host = runCatching { URI(url).host }.getOrNull() ?: return false
        if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2") return true
        if (host.startsWith("10.") || host.startsWith("192.168.")) return true
        if (host.startsWith("172.")) {
            val second = host.split(".").getOrNull(1)?.toIntOrNull() ?: 0
            if (second in 16..31) return true
        }
        return false
    }

    private fun normalizeWsUrl(raw: String): String? {
        var url = raw.trim().trimEnd('/')
        if (url.isEmpty()) return null
        if (url.startsWith("http://")) url = "ws://" + url.removePrefix("http://")
        else if (url.startsWith("https://")) url = "wss://" + url.removePrefix("https://")
        else if (!url.startsWith("ws://") && !url.startsWith("wss://")) url = "ws://$url"
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) return null
        return runCatching { URI(url) }.getOrNull()?.toString()
    }
}
