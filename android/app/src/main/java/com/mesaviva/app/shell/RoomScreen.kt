package com.mesaviva.app.shell

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DamasState
import com.mesaviva.app.games.BriscaGame
import com.mesaviva.app.games.BriscaPhase
import com.mesaviva.app.games.BriscaState
import com.mesaviva.app.games.DominoEnd
import com.mesaviva.app.games.DominoPhase
import com.mesaviva.app.games.DominoState
import com.mesaviva.app.games.MancalaGame
import com.mesaviva.app.games.MancalaPhase
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.MancalaState
import com.mesaviva.app.games.LudoGame
import com.mesaviva.app.games.LudoPhase
import com.mesaviva.app.games.LudoState
import com.mesaviva.app.games.SpanishCard
import com.mesaviva.app.games.TrucoGame
import com.mesaviva.app.games.TrucoPhase
import com.mesaviva.app.games.TrucoState
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(state: ShellState, onOpenWeb: (String) -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var session by remember { mutableStateOf<NativeRoomController?>(null) }
    var joinHint by remember { mutableStateOf("") }
    var pendingLink by remember { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            val controller = NativeRoomController(context.applicationContext, state, RoomRole.Guest)
            if (controller.joinLink(contents)) {
                session = controller
                joinHint = ""
            } else {
                joinHint = controller.hint.ifBlank { state.t("room.errorInvalidLink") }
                controller.close()
            }
        }
    }

    LaunchedEffect(state.roomLink) {
        val link = state.roomLink ?: return@LaunchedEffect
        state.roomLink = null
        pendingLink = link
        tab = 0
    }

    LaunchedEffect(pendingLink) {
        val link = pendingLink ?: return@LaunchedEffect
        pendingLink = null
        val controller = NativeRoomController(context.applicationContext, state, RoomRole.Guest)
        if (controller.joinLink(link)) {
            session = controller
            joinHint = ""
        } else {
            joinHint = controller.hint.ifBlank { state.t("room.errorInvalidLink") }
            controller.close()
        }
    }

    DisposableEffect(session) {
        val current = session
        onDispose { current?.close() }
    }

    val active = session
    if (active != null && active.inGame) {
        NativeRoomGame(state, active) {
            active.close()
            session = null
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(state.t("room.title"), style = MaterialTheme.typography.headlineMedium)
            Text(
                state.t("room.subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("📶 ${state.t("room.nativeTab")}") },
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("🌐 ${state.t("room.webTab")}") },
                )
            }
        }

        if (tab == 0) {
            NativeRoomLobby(
                state = state,
                session = session,
                joinHint = joinHint,
                onHost = { game, relay ->
                    val controller = NativeRoomController(context.applicationContext, state, RoomRole.Host)
                    controller.host(game, relay)
                    session = controller
                    joinHint = ""
                },
                onJoin = { url, code ->
                    val controller = NativeRoomController(context.applicationContext, state, RoomRole.Guest)
                    if (controller.join(url, code)) {
                        session = controller
                        joinHint = ""
                    } else {
                        joinHint = controller.hint.ifBlank { state.t("room.errorConnect") }
                        controller.close()
                    }
                },
                onScan = {
                    val options = ScanOptions()
                        .setOrientationLocked(false)
                        .setPrompt(state.t("room.scanPrompt"))
                        .setBeepEnabled(false)
                    runCatching { scanLauncher.launch(options) }
                        .onFailure { joinHint = state.t("room.scannerMissing") }
                },
                onClose = {
                    session?.close()
                    session = null
                },
                onSelectGame = { session?.selectGame(it) },
            )
        } else {
            WebRoomTab(state, onOpenWeb)
        }
    }
}

@Composable
private fun NativeRoomLobby(
    state: ShellState,
    session: NativeRoomController?,
    joinHint: String,
    onHost: (RoomGame, String) -> Unit,
    onJoin: (String, String) -> Unit,
    onScan: () -> Unit,
    onClose: () -> Unit,
    onSelectGame: (RoomGame) -> Unit,
) {
    if (session == null) {
        var game by remember { mutableStateOf(RoomGame.Damas) }
        var relay by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📶  ${state.t("room.nativeBadge")}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.t("room.nativeDesc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.t("room.createNative"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (game == RoomGame.Snakes || game == RoomGame.Domino || game == RoomGame.Brisca || game == RoomGame.Ludo) {
                                state.t("room.snakesPlayers")
                            } else {
                                state.t("room.twoPlayers")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(RoomGame.entries) { option ->
                                FilterChip(
                                    selected = game == option,
                                    onClick = { game = option },
                                    label = { Text("${option.emoji} ${option.title(state.language)}") },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = relay,
                            onValueChange = { relay = it.take(160) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(state.t("room.relayOptional")) },
                            placeholder = { Text("wss://tu-relay.example") },
                        )
                        Text(
                            state.t("room.relayHint"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { onHost(game, relay) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("🎲 ${state.t("room.createNative")}") }
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.t("room.joinNative"), style = MaterialTheme.typography.titleMedium)
                        Text(state.t("room.joinHelp"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                            Text("📷 ${state.t("room.scanNative")}")
                        }
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(state.t("room.wsLabel")) },
                            placeholder = { Text("ws://192.168.1.20:9001") },
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { value ->
                                code = value.uppercase().filter { it.isLetterOrDigit() }.take(6)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(state.t("room.code")) },
                        )
                        Button(
                            onClick = { onJoin(url, code) },
                            enabled = url.isNotBlank() && code.trim().length in 4..6,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(state.t("room.joinButton")) }
                        if (joinHint.isNotBlank()) {
                            Text(joinHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            state.t("room.deepLinkNote"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        return
    }

    if (session.isHost) {
        HostLobby(state, session, onClose, onSelectGame)
    } else {
        GuestLobby(state, session, onClose)
    }
}

@Composable
private fun HostLobby(
    state: ShellState,
    session: NativeRoomController,
    onClose: () -> Unit,
    onSelectGame: (RoomGame) -> Unit,
) {
    val qr = remember(session.qrText) {
        if (session.qrText.isBlank()) null else generateQrBitmap(session.qrText)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(state.t("room.youAreHost"), style = MaterialTheme.typography.titleMedium)
                    RoomStatusPill(state, session.status)
                    if (session.code.isNotBlank()) {
                        Text(
                            session.code,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(state.t("room.yourCode"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (qr != null) {
                        Image(
                            bitmap = qr,
                            contentDescription = state.t("room.qr"),
                            modifier = Modifier.size(190.dp),
                        )
                    }
                    if (session.shareUrl.isNotBlank()) {
                        Text(
                            session.shareUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        state.t("room.nativeNote"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("room.games"), style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(RoomGame.entries) { option ->
                            FilterChip(
                                selected = session.selectedGame == option,
                                onClick = { onSelectGame(option) },
                                label = { Text("${option.emoji} ${option.title(state.language)}") },
                            )
                        }
                    }
                    if (session.selectedGame == RoomGame.Domino) {
                        Text(state.t("room.dominoPlayers"), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4).forEach { count ->
                                FilterChip(
                                    selected = session.dominoPlayers == count,
                                    onClick = { session.chooseDominoPlayers(count) },
                                    label = { Text(count.toString()) },
                                )
                            }
                        }
                    }
                    if (session.selectedGame == RoomGame.Brisca) {
                        Text(state.t("room.briscaPlayers"), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4).forEach { count ->
                                FilterChip(
                                    selected = session.briscaPlayers == count,
                                    onClick = { session.chooseBriscaPlayers(count) },
                                    label = { Text(if (count == 4) "4 · ${state.t("brisca.pairs")}" else "2") },
                                )
                            }
                        }
                    }
                    if (session.selectedGame == RoomGame.Ludo) {
                        Text(state.t("room.ludoPlayers"), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4).forEach { count ->
                                FilterChip(
                                    selected = session.ludoPlayers == count,
                                    onClick = { session.chooseLudoPlayers(count) },
                                    label = { Text(count.toString()) },
                                )
                            }
                        }
                    }
                    Text(state.t("room.guestsTitle"), style = MaterialTheme.typography.titleMedium)
                    if (session.guests.isEmpty()) {
                        Text(state.t("room.noneYet"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        session.guests.forEach { guest ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("👤", fontSize = 18.sp)
                                Text(guest.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                if (guest.seat in 1..3) {
                                    Text(
                                        state.t("room.seat", mapOf("n" to guest.seat)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    state.t(if (guest.online) "room.onlineLabel" else "room.offlineLabel"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (guest.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (session.hint.isNotBlank()) {
                        Text(session.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (!session.inGame) {
                        Button(
                            onClick = { session.startMatch() },
                            enabled = session.canStart,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("▶  ${state.t("room.startNative")}") }
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text(state.t("room.closeRoom"))
                    }
                }
            }
        }
        item {
            RoomChatPanel(state, session.chat, session::sendChat)
        }
    }
}

@Composable
private fun GuestLobby(
    state: ShellState,
    session: NativeRoomController,
    onLeave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (session.hostName.isBlank()) state.t("room.waitingHost")
                        else state.t("room.youAreGuestOf", mapOf("name" to session.hostName)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    RoomStatusPill(state, session.status)
                    Text(
                        state.t("room.waitingStart"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (session.error != null) {
                        Text(session.error!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (session.hint.isNotBlank()) {
                        Text(session.hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                        Text(state.t("room.leaveRoom"))
                    }
                }
            }
        }
        item {
            RoomChatPanel(state, session.chat, session::sendChat)
        }
    }
}

@Composable
private fun RoomStatusPill(state: ShellState, status: RoomStatus) {
    val label = when (status) {
        RoomStatus.Idle -> state.t("room.connectingStatus")
        RoomStatus.Connecting -> state.t("room.connectingStatus")
        RoomStatus.Hosting -> state.t("room.connectedStatus")
        RoomStatus.Joined -> state.t("room.connectedStatus")
        RoomStatus.Reconnecting -> state.t("room.reconnectingStatus")
        RoomStatus.Closed -> state.t("room.roomClosed")
        RoomStatus.Error -> state.t("room.offlineLabel")
    }
    val color = when (status) {
        RoomStatus.Hosting, RoomStatus.Joined -> MaterialTheme.colorScheme.primary
        RoomStatus.Error, RoomStatus.Closed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            "● $label",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun RoomChatPanel(
    state: ShellState,
    messages: List<RoomChat>,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💬  ${state.t("room.chatTitle")}", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().height(170.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(messages) { message ->
                    if (message.system) {
                        Text(
                            "${message.time} · ${message.text}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "${message.time} · ${message.author}: ${message.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (message.mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(200) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(state.t("room.chatHint")) },
                )
                Button(
                    onClick = {
                        onSend(text)
                        text = ""
                    },
                    enabled = text.isNotBlank(),
                ) { Text(state.t("room.send")) }
            }
        }
    }
}

/* --------------------------------------------------------------- mesa nativa */

@Composable
private fun NativeRoomGame(
    state: ShellState,
    session: NativeRoomController,
    onExit: () -> Unit,
) {
    var chatOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var exitOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var resultShown by remember { mutableStateOf(false) }

    val game = session.activeGame
    val damas = session.damasState
    val mancala = session.mancalaState
    val snakes = session.snakesState
    val domino = session.dominoState
    val brisca = session.briscaState
    val truco = session.trucoState
    val ludo = session.ludoState
    val finished = (game == RoomGame.Damas && damas?.phase == DamasPhase.MatchEnd) ||
        (game == RoomGame.Mancala && mancala?.phase == MancalaPhase.MatchEnd) ||
        (game == RoomGame.Domino && domino?.phase == DominoPhase.MatchEnd) ||
        (game == RoomGame.Brisca && brisca?.phase == BriscaPhase.MatchEnd) ||
        (game == RoomGame.Truco && truco?.phase == TrucoPhase.MatchEnd) ||
        (game == RoomGame.Ludo && ludo?.phase == LudoPhase.MatchEnd) ||
        (game == RoomGame.Snakes && snakes?.finished == true)

    LaunchedEffect(finished) {
        if (finished && !resultShown) {
            resultShown = true
            resultOpen = true
        } else if (!finished) {
            resultShown = false
        }
    }

    val myColor = when {
        session.isHost -> DamasColor.White
        session.mySeat == 1 -> DamasColor.Black
        else -> null
    }
    val mySide = when {
        session.isHost -> MancalaSide.White
        session.mySeat == 1 -> MancalaSide.Black
        else -> null
    }
    val opponentName = if (session.isHost) {
        session.guests.firstOrNull()?.name ?: state.t("room.guestFallback")
    } else {
        session.hostName.ifBlank { state.t("room.hostFallback") }
    }
    val myName = if (session.isHost) {
        state.name.ifBlank { state.t("room.hostFallback") }
    } else {
        state.name.ifBlank { state.t("room.guestFallback") }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { exitOpen = true }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    game?.title(state.language) ?: state.t("room.title"),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val transport = when {
                    session.isHost && session.onlineRelay -> state.t("room.transportOnline")
                    session.isHost -> state.t("room.transportLan")
                    session.onlineRelay -> state.t("room.transportOnline")
                    else -> state.t("room.transportGuest")
                }
                Text(
                    "$myName · $transport",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { chatOpen = true }) { Text("💬") }
        }

        val banner = when {
            finished -> state.t("common.matchOver")
            game == RoomGame.Damas && damas != null -> {
                when {
                    myColor == null -> state.t("room.spectating")
                    damas.turn == myColor -> state.t("room.yourTurn")
                    else -> state.t("room.theirTurn", mapOf("name" to opponentName))
                }
            }
            game == RoomGame.Mancala && mancala != null -> {
                when {
                    mySide == null -> state.t("room.spectating")
                    mancala.turn == mySide -> state.t("room.yourTurn")
                    else -> state.t("room.theirTurn", mapOf("name" to opponentName))
                }
            }
            game == RoomGame.Domino && domino != null -> {
                val myDominoSeat = session.mySeat
                if (myDominoSeat !in domino.players.indices) {
                    state.t("room.spectating")
                } else {
                    val name = domino.players.getOrNull(domino.turn)?.name ?: opponentName
                    if (domino.turn == myDominoSeat) state.t("room.yourTurn")
                    else state.t("room.theirTurn", mapOf("name" to name))
                }
            }
            game == RoomGame.Snakes && snakes != null -> {
                val name = snakes.names.getOrElse(snakes.turn) { opponentName }
                if (snakes.turn == session.mySeat) state.t("room.yourTurn")
                else state.t("room.theirTurn", mapOf("name" to name))
            }
            game == RoomGame.Brisca && brisca != null -> {
                val name = brisca.seats.getOrNull(brisca.turn)?.name ?: opponentName
                if (brisca.turn == session.mySeat) state.t("room.yourTurn")
                else state.t("room.theirTurn", mapOf("name" to name))
            }
            game == RoomGame.Truco && truco != null -> {
                val pending = truco.pending
                when {
                    pending != null && pending.awaiting == session.mySeat -> state.t(
                        "truco.pendingBanner",
                        mapOf(
                            "p" to (truco.seats.getOrNull(pending.caller)?.name ?: opponentName),
                            "c" to TrucoGame.challengeName(pending.kind),
                            "n" to pending.stake,
                        ),
                    )
                    truco.turn == session.mySeat -> state.t("room.yourTurn")
                    else -> state.t("room.theirTurn", mapOf("name" to (truco.seats.getOrNull(truco.turn)?.name ?: opponentName)))
                }
            }
            game == RoomGame.Ludo && ludo != null -> {
                val player = ludo.players.getOrNull(ludo.turn)
                if (player?.isBot == true) state.t("room.botPlaying", mapOf("name" to player.name))
                else if (ludo.turn == session.mySeat) state.t("room.yourTurn")
                else state.t("room.theirTurn", mapOf("name" to (player?.name ?: opponentName)))
            }
            else -> state.t("room.syncState")
        }
        NativeTurnBanner(text = banner, alert = false)

        if (session.isHost && session.hasBotSeat && session.inGame && game != RoomGame.Snakes) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.t("room.botActive"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    if (game != RoomGame.Brisca && game != RoomGame.Truco) {
                        TextButton(onClick = { session.hostToggleBot() }) { Text(state.t("room.botOff")) }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        when (game) {
            RoomGame.Damas -> {
                if (damas == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomDamasBoard(state, session, damas, myColor)
                    NativeMoveLog(state, damas.history) { }
                }
            }
            RoomGame.Mancala -> {
                if (mancala == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomMancalaBoard(state, session, mancala, mySide)
                }
            }
            RoomGame.Domino -> {
                if (domino == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomDominoBoard(state, session, domino)
                }
            }
            RoomGame.Snakes -> {
                if (snakes == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomSnakesBoard(state, session, snakes)
                }
            }
            RoomGame.Brisca -> {
                if (brisca == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomBriscaBoard(state, session, brisca)
                }
            }
            RoomGame.Truco -> {
                if (truco == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomTrucoBoard(state, session, truco)
                }
            }
            RoomGame.Ludo -> {
                if (ludo == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(state.t("room.syncState"), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    RoomLudoBoard(state, session, ludo)
                }
            }
            null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(state.t("room.syncState"))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { session.returnToLobby() }, modifier = Modifier.weight(1f)) {
                Text(state.t("room.backToLobby"))
            }
            val canResign = !finished && when (game) {
                RoomGame.Snakes -> session.mySeat in 0..3
                RoomGame.Domino -> session.mySeat in 0 until (session.dominoState?.players?.size ?: 0)
                RoomGame.Brisca -> session.mySeat in 0 until (session.briscaState?.seats?.size ?: 0)
                RoomGame.Truco -> session.mySeat in 0..1
                RoomGame.Ludo -> session.mySeat in 0 until (session.ludoState?.players?.size ?: 0)
                null -> false
                else -> session.isHost || session.mySeat == 1
            }
            Button(
                onClick = { resignOpen = true },
                enabled = canResign,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("common.resign")) }
            if (session.isHost && session.botSeat && game != RoomGame.Snakes && game != RoomGame.Brisca && game != RoomGame.Truco) {
                OutlinedButton(onClick = { session.hostToggleBot() }) { Text("🤖") }
            }
        }
    }

    LaunchedEffect(session.damasState?.turn, session.botSeat, session.damasState?.history?.size) {
        val board = session.damasState ?: return@LaunchedEffect
        if (session.isHost && session.botSeat && board.turn == DamasColor.Black && board.phase == DamasPhase.Play) {
            delay(620)
            session.hostBotDamasMove()
        }
    }

    LaunchedEffect(session.mancalaState?.turn, session.botSeat, session.mancalaState?.pits) {
        val board = session.mancalaState ?: return@LaunchedEffect
        if (session.isHost && session.botSeat && board.turn == MancalaSide.Black && board.phase == MancalaPhase.Play) {
            delay(620)
            session.hostBotMancalaMove()
        }
    }

    LaunchedEffect(
        session.dominoState?.turn,
        session.dominoState?.phase,
        session.dominoState?.handNo,
        session.dominoState?.players?.map { it.hand.size },
    ) {
        val board = session.dominoState ?: return@LaunchedEffect
        val player = board.players.getOrNull(board.turn) ?: return@LaunchedEffect
        if (session.isHost && board.phase == DominoPhase.Playing && player.isBot) {
            delay(680)
            session.hostBotDominoMove()
        }
    }

    LaunchedEffect(
        session.snakesState?.turn,
        session.snakesState?.positions,
        session.snakesState?.bots,
        session.snakesState?.finished,
    ) {
        val board = session.snakesState ?: return@LaunchedEffect
        if (session.isHost && !board.finished && board.bots.getOrElse(board.turn) { false }) {
            delay(760)
            session.hostBotSnakesRoll()
        }
    }

    LaunchedEffect(
        session.briscaState?.turn,
        session.briscaState?.phase,
        session.briscaState?.handNo,
        session.briscaState?.history?.size,
        session.briscaState?.seats?.map { it.isBot },
    ) {
        val board = session.briscaState ?: return@LaunchedEffect
        val seat = board.seats.getOrNull(board.turn) ?: return@LaunchedEffect
        if (session.isHost && board.phase == BriscaPhase.Playing && seat.isBot) {
            delay(700)
            session.hostBotBriscaAction()
        }
    }

    LaunchedEffect(
        session.trucoState?.turn,
        session.trucoState?.phase,
        session.trucoState?.pending,
        session.trucoState?.history?.size,
        session.trucoState?.seats?.map { it.isBot },
        session.trucoState?.handNo,
    ) {
        val board = session.trucoState ?: return@LaunchedEffect
        val pending = board.pending
        val botTurn = when {
            pending != null -> board.seats.getOrNull(pending.awaiting)?.isBot == true
            board.phase == TrucoPhase.Envido || board.phase == TrucoPhase.Play ->
                board.seats.getOrNull(board.turn)?.isBot == true
            else -> false
        }
        if (session.isHost && botTurn) {
            delay(760)
            session.hostBotTrucoAction()
        }
    }

    LaunchedEffect(
        session.ludoState?.turn,
        session.ludoState?.phase,
        session.ludoState?.dice,
        session.ludoState?.lastMove,
    ) {
        val board = session.ludoState ?: return@LaunchedEffect
        if (!session.isHost || board.phase == LudoPhase.MatchEnd) return@LaunchedEffect
        val player = board.players.getOrNull(board.turn) ?: return@LaunchedEffect
        if (!player.isBot) return@LaunchedEffect
        delay(700)
        session.hostBotLudoAction()
    }

    if (chatOpen) {
        AlertDialog(
            onDismissRequest = { chatOpen = false },
            title = { Text(state.t("room.chatTitle")) },
            text = { RoomChatPanel(state, session.chat, session::sendChat) },
            confirmButton = {
                TextButton(onClick = { chatOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }

    if (resignOpen) {
        AlertDialog(
            onDismissRequest = { resignOpen = false },
            title = { Text(state.t("common.resign")) },
            text = { Text(state.t("common.resignConfirm")) },
            confirmButton = {
                TextButton(onClick = {
                    resignOpen = false
                    if (session.isHost) session.hostResign() else session.guestResign()
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) }
            },
        )
    }

    if (exitOpen) {
        AlertDialog(
            onDismissRequest = { exitOpen = false },
            title = { Text(state.t("room.leaveRoom")) },
            text = { Text(state.t("room.leaveConfirm")) },
            confirmButton = {
                TextButton(onClick = {
                    exitOpen = false
                    onExit()
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { exitOpen = false }) { Text(state.t("common.cancel")) }
            },
        )
    }

    if (resultOpen && finished) {
        val draw = (game == RoomGame.Damas && damas?.draw == true) ||
            (game == RoomGame.Mancala && mancala?.whiteStore == mancala?.blackStore) ||
            (game == RoomGame.Domino && domino?.winnerIndices?.isEmpty() == true) ||
            (game == RoomGame.Brisca && brisca?.draw == true) ||
            (game == RoomGame.Truco && truco?.draw == true) ||
            (game == RoomGame.Ludo && ludo?.winnerIndex == null)
        val won = when (game) {
            RoomGame.Damas -> myColor != null && damas?.winner == myColor
            RoomGame.Mancala -> mySide != null && mancala?.winner == mySide
            RoomGame.Domino -> domino?.winnerIndices?.contains(session.mySeat) == true
            RoomGame.Snakes -> snakes?.winner == session.mySeat
            RoomGame.Brisca -> {
                val board = brisca
                board?.winner?.let { seat ->
                    if (board.teams) board.seats.getOrNull(seat)?.team == board.seats.getOrNull(session.mySeat)?.team
                    else seat == session.mySeat
                } == true
            }
            RoomGame.Truco -> truco?.winner == session.mySeat
            RoomGame.Ludo -> ludo?.winnerIndex == session.mySeat
            null -> false
        }
        val spectator = game != RoomGame.Snakes && !session.isHost && session.mySeat != 1
        val winnerName = when (game) {
            RoomGame.Snakes -> snakes?.names?.getOrNull(snakes.winner ?: -1) ?: opponentName
            RoomGame.Brisca -> brisca?.winner?.let { brisca.seats.getOrNull(it)?.name } ?: opponentName
            RoomGame.Truco -> truco?.winner?.let { truco.seats.getOrNull(it)?.name } ?: opponentName
            RoomGame.Ludo -> ludo?.winnerIndex?.let { ludo.players.getOrNull(it)?.name } ?: opponentName
            else -> opponentName
        }
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = {
                Text(
                    when {
                        spectator -> state.t("common.matchOver")
                        draw -> state.t("room.drawMatch")
                        won -> state.t("room.matchWon")
                        else -> state.t("room.matchLost", mapOf("name" to winnerName))
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (game == RoomGame.Damas && damas != null) {
                        Text(state.t("damas.whitePieces", mapOf("n" to damas.board.count { it?.color == DamasColor.White })))
                        Text(state.t("damas.blackPieces", mapOf("n" to damas.board.count { it?.color == DamasColor.Black })))
                    }
                    if (game == RoomGame.Mancala && mancala != null) {
                        Text("${state.t("mancala.you")}: ${mancala.whiteStore} · ${opponentName}: ${mancala.blackStore}")
                    }
                    if (game == RoomGame.Domino && domino != null) {
                        domino.players.forEachIndexed { index, player ->
                            Text("${player.name}: ${state.t("domino.score", mapOf("n" to player.score))}")
                        }
                    }
                    if (game == RoomGame.Snakes && snakes != null) {
                        snakes.names.forEachIndexed { index, name ->
                            Text("$name: ${snakes.positions.getOrElse(index) { 0 }}")
                        }
                    }
                    Text(
                        state.t("replay.note"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                if (session.isHost) {
                    TextButton(onClick = {
                        resultOpen = false
                        session.startMatch()
                    }) { Text(state.t("room.rematch")) }
                } else {
                    TextButton(onClick = {
                        resultOpen = false
                        session.returnToLobby()
                    }) { Text(state.t("room.backToLobby")) }
                }
            },
            dismissButton = {
                TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }
}

@Composable
private fun RoomDamasBoard(
    state: ShellState,
    session: NativeRoomController,
    board: DamasState,
    myColor: DamasColor?,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val myTurn = myColor != null && board.phase == DamasPhase.Play && board.turn == myColor
    val legal = remember(board) {
        if (board.phase == DamasPhase.Play) session.damasLegalMoves() else emptyList()
    }
    val targets = remember(selected, legal, myTurn) {
        if (selected == null || !myTurn) emptyList() else legal.filter { it.from == selected }
    }

    LaunchedEffect(board.mustContinueFrom, board.history.size) {
        selected = board.mustContinueFrom
    }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
    ) {
        val cell = maxWidth / 8
        val flip = myColor == DamasColor.Black
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 8) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 8) {
                        val index = if (flip) 63 - (row * 8 + col) else row * 8 + col
                        val dark = DamasGame.isDark(index)
                        val piece = board.board[index]
                        val isSelected = selected == index
                        val isTarget = targets.any { it.to == index }
                        val isLast = board.lastMove?.let { it.from == index || it.to == index } == true
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                        isLast -> Color(0x55E0A63A)
                                        dark -> Color(0xFF6E4A32)
                                        else -> Color(0xFFE9D8B4)
                                    },
                                )
                                .clickable {
                                    if (!myTurn) return@clickable
                                    if (selected != null) {
                                        val move = targets.firstOrNull { it.to == index }
                                        if (move != null) {
                                            if (session.isHost) session.hostDamasMove(move.from, move.to)
                                            else session.guestDamasMove(move.from, move.to)
                                            selected = null
                                            return@clickable
                                        }
                                    }
                                    if (piece?.color == myColor) {
                                        selected = if (selected == index) null else index
                                    }
                                }
                                .semantics {
                                    contentDescription = buildString {
                                        append(DamasGame.square(index))
                                        if (piece != null) {
                                            append(", ")
                                            append(
                                                if (piece.color == DamasColor.White) state.t("damas.white")
                                                else state.t("damas.black"),
                                            )
                                            if (piece.king) append(" ♛")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (piece != null) {
                                val white = piece.color == DamasColor.White
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(cell * 0.10f)
                                        .background(if (white) Color(0xFFFFF6E2) else Color(0xFF241A13), CircleShape)
                                        .border(
                                            (cell.value * 0.025f).dp,
                                            if (white) Color(0xFF8A6A45) else Color(0xFF0B0806),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (piece.king) "♛" else "",
                                        fontSize = (cell.value * 0.38f).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (white) Color(0xFF3A2817) else Color(0xFFE9C46A),
                                    )
                                }
                            }
                            if (isTarget) {
                                Box(
                                    Modifier
                                        .size(cell * 0.22f)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "● ${board.board.count { it?.color == DamasColor.White }} · ○ ${board.board.count { it?.color == DamasColor.Black }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            state.t(
                "damas.kings",
                mapOf(
                    "w" to board.board.count { it?.color == DamasColor.White && it.king },
                    "b" to board.board.count { it?.color == DamasColor.Black && it.king },
                ),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RoomDominoBoard(
    state: ShellState,
    session: NativeRoomController,
    board: DominoState,
) {
    val mySeat = session.mySeat
    val canPlay = mySeat in board.players.indices
    val myTurn = canPlay && board.turn == mySeat && board.phase == DominoPhase.Playing
    val myHand = board.players.getOrNull(mySeat)?.hand.orEmpty()
    var selected by remember(mySeat, board.handNo) { mutableStateOf<Int?>(null) }
    val selectedEnds = selected?.let { session.dominoEndsFor(it) } ?: emptyList()

    fun play(index: Int, end: DominoEnd) {
        if (session.isHost) session.hostDominoPlay(index, end) else session.guestDominoPlay(index, end)
        selected = null
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            board.players.forEachIndexed { index, player ->
                val active = index == board.turn && board.phase == DominoPhase.Playing
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    },
                ) {
                    Column(
                        Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.t("domino.score", mapOf("n" to player.score)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${board.handCounts.getOrElse(index) { player.hand.size }} 🁢",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (active) {
                            Text("●", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                board.left?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                itemsIndexed(board.chain) { _, tile ->
                    DominoTileView(tile = tile, compact = true)
                }
            }
            Text(
                board.right?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        Text(
            state.t("domino.hand", mapOf("n" to board.handNo)) + " · " +
                state.t("domino.boneyard", mapOf("n" to board.boneyard.size)) + " · " +
                state.t("domino.target", mapOf("n" to board.target)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (board.phase == DominoPhase.HandEnd) {
            Text(
                state.t("room.handClosed") + " " + state.t("domino.msgHandEnd", mapOf("points" to "")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(state.t("room.yourHand"), style = MaterialTheme.typography.labelMedium)
        if (!canPlay) {
            Text(state.t("room.spectating"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(myHand) { index, tile ->
                    val ends = session.dominoEndsFor(index)
                    DominoTileView(
                        tile = tile,
                        compact = false,
                        selected = selected == index,
                        playable = myTurn && ends.isNotEmpty() && !session.dominoPending,
                        onClick = {
                            if (myTurn && ends.isNotEmpty()) {
                                if (ends.size == 1) play(index, ends.first()) else selected = index
                            }
                        },
                    )
                }
            }
        }

        if (selected != null && selectedEnds.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { play(selected!!, DominoEnd.Left) },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("domino.left")) }
                OutlinedButton(
                    onClick = { play(selected!!, DominoEnd.Right) },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("domino.right")) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (session.isHost) session.hostDominoDraw() else session.guestDominoDraw()
                },
                enabled = myTurn && !session.dominoHasPlayable() &&
                    board.boneyard.isNotEmpty() && !session.dominoPending,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("domino.draw")) }
            OutlinedButton(
                onClick = {
                    if (session.isHost) session.hostDominoPass() else session.guestDominoPass()
                },
                enabled = myTurn && !session.dominoHasPlayable() &&
                    board.boneyard.isEmpty() && !session.dominoPending,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("domino.pass")) }
            if (board.phase == DominoPhase.HandEnd) {
                Button(
                    onClick = { if (session.isHost) session.hostDominoNextHand() },
                    enabled = session.isHost,
                    modifier = Modifier.weight(1.2f),
                ) {
                    Text(if (session.isHost) state.t("domino.nextHand") else state.t("room.waitNextHand"))
                }
            }
        }

        if (session.dominoPending) {
            Text(
                state.t("room.syncState"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SNAKES_ROOM_COLORS = listOf(
    Color(0xFFE0A63A),
    Color(0xFF7FD8B4),
    Color(0xFFE57373),
    Color(0xFF8AB4F8),
)

@Composable
private fun RoomSnakesBoard(
    state: ShellState,
    session: NativeRoomController,
    board: SnakesRoomState,
) {
    val myTurn = board.turn == session.mySeat && !board.finished
    val currentName = board.names.getOrElse(board.turn) { "—" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            board.names.forEachIndexed { index, name ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (board.turn == index && !board.finished) {
                        SNAKES_ROOM_COLORS[index % 4].copy(alpha = 0.26f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
                    },
                ) {
                    Column(
                        Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SNAKES_ROOM_COLORS[index % 4]),
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            board.positions.getOrElse(index) { 0 }.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            val cell = maxWidth / 10
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 10) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 10) {
                            val rank = 9 - row
                            val number = rank * 10 + if (rank % 2 == 0) col + 1 else 10 - col
                            val here = board.positions.indices.filter { board.positions[it] == number }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if ((row + col) % 2 == 0) Color(0xFFE9D8B4)
                                        else Color(0xFFD7BE92),
                                    )
                                    .border(0.3.dp, Color(0xFF8A6A45).copy(alpha = 0.35f))
                                    .semantics {
                                        contentDescription = buildString {
                                            append("Casilla $number")
                                            here.forEach { index ->
                                                append(", ")
                                                append(board.names.getOrElse(index) { "" })
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        number.toString(),
                                        fontSize = (cell.value * 0.13f).coerceIn(7f, 11f).sp,
                                        color = Color(0xFF5A3A22),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                        here.take(4).forEach { index ->
                                            Box(
                                                Modifier
                                                    .size((cell.value * 0.12f).coerceIn(4f, 8f).dp)
                                                    .clip(CircleShape)
                                                    .background(SNAKES_ROOM_COLORS[index % 4]),
                                            )
                                        }
                                    }
                                }
                                SnakesRoomRules.LADDERS[number]?.let { destination ->
                                    Text(
                                        "↑$destination",
                                        fontSize = 7.sp,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    )
                                }
                                SnakesRoomRules.SNAKES[number]?.let { destination ->
                                    Text(
                                        "↓$destination",
                                        fontSize = 7.sp,
                                        color = Color(0xFFB71C1C),
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (board.dice > 0) "🎲 ${board.dice}" else "🎲",
                style = MaterialTheme.typography.titleLarge,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        board.finished -> state.t(
                            "snakes.winner",
                            mapOf("name" to (board.names.getOrNull(board.winner ?: -1) ?: currentName)),
                        )
                        myTurn -> state.t("snakes.yourTurn")
                        else -> state.t("snakes.botTurn", mapOf("name" to currentName))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val event = when (board.lastEvent) {
                    "ladder" -> state.t(
                        "snakes.ladder",
                        mapOf("name" to (board.names.getOrNull(board.lastSeat ?: -1) ?: ""), "n" to board.lastTo),
                    )
                    "snake" -> state.t(
                        "snakes.snake",
                        mapOf("name" to (board.names.getOrNull(board.lastSeat ?: -1) ?: ""), "n" to board.lastTo),
                    )
                    "needExact" -> state.t("snakes.needExact")
                    "win" -> state.t(
                        "snakes.winner",
                        mapOf("name" to (board.names.getOrNull(board.winner ?: -1) ?: "")),
                    )
                    "resign" -> state.t("common.matchOver")
                    "start" -> state.t("snakes.rules")
                    else -> if (board.lastSeat != null) {
                        "${board.names.getOrNull(board.lastSeat) ?: ""}: ${board.lastFrom} → ${board.lastTo}"
                    } else {
                        state.t("snakes.rules")
                    }
                }
                Text(
                    event,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = {
                    if (session.isHost) session.hostSnakesRoll() else session.guestSnakesRoll()
                },
                enabled = myTurn && !board.finished,
            ) {
                Text(state.t("snakes.roll"))
            }
        }
    }
}

@Composable
private fun RoomMancalaBoard(
    state: ShellState,
    session: NativeRoomController,
    board: MancalaState,
    mySide: MancalaSide?,
) {
    val viewSide = mySide ?: MancalaSide.Black
    val otherSide = if (viewSide == MancalaSide.Black) MancalaSide.White else MancalaSide.Black
    val myTurn = mySide != null && board.phase == MancalaPhase.Play && board.turn == mySide
    val playable = remember(board, mySide) {
        if (mySide == null || board.phase != MancalaPhase.Play || board.turn != mySide) emptyList()
        else MancalaGame.sidePits(mySide).filter { board.pits.getOrElse(it) { 0 } > 0 }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF7B4E2E))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            (if (mySide == null) state.t("room.spectating") else state.t("mancala.you")) +
                ": " + board.pits[MancalaGame.store(viewSide)],
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFE0A3),
        )
        Row(
            Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StorePit(state.t("mancala.player2"), board.pits[MancalaGame.store(otherSide)], Modifier.weight(0.7f))
            Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (pit in MancalaGame.sidePits(otherSide)) {
                        MancalaPit(
                            count = board.pits[pit],
                            enabled = false,
                            label = "${state.t("mancala.player2")} ${pit + 1}",
                            modifier = Modifier.weight(1f),
                        ) { }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (pit in MancalaGame.sidePits(viewSide)) {
                        MancalaPit(
                            count = board.pits[pit],
                            enabled = myTurn && pit in playable,
                            label = "${state.t("mancala.you")} ${pit + 1}",
                            modifier = Modifier.weight(1f),
                        ) { if (session.isHost) session.hostMancalaSow(pit) else session.guestMancalaSow(pit) }
                    }
                }
            }
            StorePit(
                if (mySide == null) state.t("room.spectating") else state.t("mancala.you"),
                board.pits[MancalaGame.store(viewSide)],
                Modifier.weight(0.7f),
            )
        }
        Text(
            state.t("mancala.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFEADBC8),
        )
        if (board.lastCapture != null) {
            Text(
                state.t("mancala.capture", mapOf("n" to board.lastCapture)),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFE0A3),
            )
        }
    }
}

@Composable
private fun StorePit(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MancalaPit(
    count: Int,
    enabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            )
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                CircleShape,
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "$label: $count" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* --------------------------------------------------------------- sala web */

@Composable
private fun WebRoomTab(state: ShellState, onOpenWeb: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("caida") }
    val qr = remember { generateQrBitmap("https://emanuelindustri-source.github.io/mesa-viva/play/#/room") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌐  ${state.t("room.lan")}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.t("room.lanDesc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenWeb("/room") }, modifier = Modifier.weight(1f)) {
                            Text(state.t("room.create"))
                        }
                        OutlinedButton(onClick = { onOpenWeb("/room?scan=1") }, modifier = Modifier.weight(1f)) {
                            Text(state.t("room.scan"))
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("room.join"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { value ->
                            code = value.uppercase().filter { it.isLetterOrDigit() }.take(6)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(state.t("room.code")) },
                    )
                    Button(
                        onClick = {
                            val clean = code.trim().uppercase()
                            if (clean.length in 4..6) onOpenWeb("/room?code=$clean")
                        },
                        enabled = code.trim().length in 4..6,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(state.t("room.joinButton"))
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(state.t("room.qr"), style = MaterialTheme.typography.titleMedium)
                    if (qr != null) {
                        Image(
                            bitmap = qr,
                            contentDescription = state.t("room.qr"),
                            modifier = Modifier.size(190.dp),
                        )
                    } else {
                        Text("QR", fontSize = 42.sp)
                    }
                    Text(
                        state.t("room.qrDesc"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Text(state.t("room.round"), style = MaterialTheme.typography.titleMedium)
        }
        item {
            Text(
                state.t("room.games"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (rowIndex in 0 until (PLAYABLE_GAMES.size + 1) / 2) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (column in 0..1) {
                            val index = rowIndex * 2 + column
                            val game = PLAYABLE_GAMES.getOrNull(index)
                            if (game != null) {
                                FilterChip(
                                    selected = selectedGame == game.id,
                                    onClick = { selectedGame = game.id },
                                    label = {
                                        Text(
                                            "${game.emoji} ${game.name}",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onOpenWeb("/setup/$selectedGame") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("▶  ${state.t("room.propose")}")
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(state.festival.accent).copy(alpha = 0.10f)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    state.t("room.note"),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* ------------------------------------------------------ mesas de baraja en sala */

@Composable
private fun RoomBriscaBoard(
    state: ShellState,
    session: NativeRoomController,
    board: BriscaState,
) {
    val mySeat = session.mySeat.coerceIn(0, board.seats.lastIndex)
    val myTurn = board.turn == mySeat && board.phase == BriscaPhase.Playing && board.seats.getOrNull(mySeat)?.isBot == false
    var historyOpen by remember { mutableStateOf(false) }
    val hand = board.hands.getOrElse(mySeat) { emptyList() }
        .sortedWith(compareBy({ it.suit.ordinal }, { BriscaGame.POWER[it.rank] ?: 0 }))

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            board.seats.forEachIndexed { index, seat ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        index == board.turn && board.phase == BriscaPhase.Playing ->
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        seat.team != null -> Color(if (seat.team == 0) 0xFFE0A63A else 0xFF4C7BD9).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    },
                ) {
                    Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == mySeat) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${board.scores.getOrElse(index) { 0 }} · ${board.hands.getOrElse(index) { emptyList() }.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1B4A3F), Color(0xFF0E2B26)))),
        ) {
            Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardDeckBack(Modifier.size(42.dp, 62.dp), board.deck.size + if (board.trumpCard != null) 1 else 0)
                    Text(state.t("brisca.deck"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
                Spacer(Modifier.width(6.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    board.trumpCard?.let {
                        SpanishCardFace(it, Modifier.size(40.dp, 60.dp), playable = false)
                        Text(
                            board.trumpSuit.symbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val shown = if (board.trick.isNotEmpty()) board.trick else board.lastTrick?.plays ?: emptyList()
                    Text(
                        if (board.trick.isNotEmpty()) state.t("brisca.trickInPlay") else state.t("brisca.lastTrick"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        shown.forEach { play ->
                            SpanishCardFace(play.card, Modifier.size(38.dp, 57.dp), playable = false)
                        }
                    }
                    board.lastTrick?.let { trick ->
                        Text(
                            state.t("brisca.trickWon", mapOf("p" to (board.seats.getOrNull(trick.winner)?.name ?: ""), "n" to trick.points)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    }
                }
            }
        }

        Text(
            state.t("brisca.hand", mapOf("n" to hand.size)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            hand.forEach { card ->
                SpanishCardFace(
                    card = card,
                    modifier = Modifier.size(48.dp, 72.dp),
                    playable = myTurn,
                    onClick = if (myTurn) {
                        {
                            if (session.isHost) session.hostBriscaPlay(card.id) else session.guestBriscaPlay(card.id)
                        }
                    } else {
                        null
                    },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (board.phase == BriscaPhase.HandEnd) {
                Button(
                    onClick = {
                        if (session.isHost) session.hostBriscaNextHand() else session.guestBriscaNextHand()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("brisca.nextHand")) }
            }
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
        }
        Column(Modifier.fillMaxWidth()) {
            NativeMoveLog(state, board.history) { historyOpen = true }
        }
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("brisca.title"),
            pdfSubtitle = state.t("brisca.subtitle"),
        ) { historyOpen = false }
    }
}

@Composable
private fun RoomTrucoBoard(
    state: ShellState,
    session: NativeRoomController,
    board: TrucoState,
) {
    val mySeat = session.mySeat.coerceIn(0, board.seats.lastIndex)
    val myTurn = board.turn == mySeat && board.pending == null &&
        (board.phase == TrucoPhase.Envido || board.phase == TrucoPhase.Play)
    val myHand = board.hands.getOrElse(mySeat) { emptyList() }
    val hand = myHand.sortedWith(compareByDescending<SpanishCard> { BriscaGame.POWER[it.rank] ?: 0 }.thenBy { it.suit.ordinal })
    var historyOpen by remember { mutableStateOf(false) }
    val pending = board.pending
    val iMustRespond = pending != null && pending.awaiting == mySeat

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            board.seats.forEachIndexed { index, seat ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (index == board.turn && board.phase == TrucoPhase.Play) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    },
                ) {
                    Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == mySeat) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${board.scores.getOrElse(index) { 0 }} · ${board.hands.getOrElse(index) { emptyList() }.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE0A63A).copy(alpha = 0.16f)) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.t("truco.stake"), style = MaterialTheme.typography.labelSmall)
                    Text("${board.handStake}", fontWeight = FontWeight.Bold, color = Color(0xFFE0A63A))
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF2A1B3D), Color(0xFF120E1E)))),
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (board.envidoReveal != null) {
                    Text(
                        state.t("truco.envidoShown", mapOf("a" to (board.envidoReveal.getOrNull(0) ?: 0), "b" to (board.envidoReveal.getOrNull(1) ?: 0))),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE0A63A),
                    )
                }
                Text(state.t("truco.currentTrick"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (board.currentTrick.isEmpty()) {
                        Box(Modifier.size(46.dp, 68.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)))
                        Box(Modifier.size(46.dp, 68.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)))
                    } else {
                        board.currentTrick.forEach { play ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SpanishCardFace(play.card, Modifier.size(46.dp, 68.dp), playable = false)
                                Text(
                                    board.seats.getOrNull(play.seat)?.name ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    board.tricks.takeLast(2).forEach { trick ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            trick.plays.forEach { play ->
                                SpanishCardFace(play.card, Modifier.size(32.dp, 48.dp), playable = false)
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                state.t("truco.hand", mapOf("n" to hand.size)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("truco.tanto", mapOf("n" to TrucoGame.tanto(myHand))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            hand.forEach { card ->
                SpanishCardFace(
                    card = card,
                    modifier = Modifier.size(48.dp, 72.dp),
                    playable = myTurn && board.phase == TrucoPhase.Play,
                    onClick = if (myTurn && board.phase == TrucoPhase.Play) {
                        {
                            if (session.isHost) session.hostTrucoPlay(card.id) else session.guestTrucoPlay(card.id)
                        }
                    } else {
                        null
                    },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (board.phase == TrucoPhase.Envido && myTurn) {
                OutlinedButton(
                    onClick = {
                        if (session.isHost) session.hostTrucoCallEnvido("envido") else session.guestTrucoEnvido("envido")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Envido 2") }
                OutlinedButton(
                    onClick = {
                        if (session.isHost) session.hostTrucoCallEnvido("realEnvido") else session.guestTrucoEnvido("realEnvido")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Real 3") }
                Button(
                    onClick = {
                        if (session.isHost) session.hostTrucoStartPlay() else session.guestTrucoStartPlay()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("truco.playNoEnvido")) }
            } else if (board.phase == TrucoPhase.Play && myTurn) {
                if (board.trucoLevel < 3) {
                    Button(
                        onClick = {
                            if (session.isHost) session.hostTrucoCallTruco() else session.guestTrucoCall()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            when (board.trucoLevel) {
                                0 -> "Truco 2"
                                1 -> "Retruco 3"
                                else -> "Vale 4"
                            },
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (session.isHost) session.hostTrucoFold() else session.guestTrucoFold()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("truco.fold")) }
            }
            if (board.phase == TrucoPhase.HandEnd) {
                Button(
                    onClick = {
                        if (session.isHost) session.hostTrucoNextHand() else session.guestTrucoNextHand()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("truco.nextHand")) }
            }
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (iMustRespond) {
        val challenge = pending ?: return
        AlertDialog(
            onDismissRequest = { },
            title = { Text(state.t("truco.challengeTitle")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        state.t(
                            "truco.challengeBody",
                            mapOf(
                                "p" to (board.seats.getOrNull(challenge.caller)?.name ?: ""),
                                "c" to TrucoGame.challengeName(challenge.kind),
                                "n" to challenge.stake,
                            ),
                        ),
                    )
                    if (challenge.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
                        Text(
                            state.t("truco.yourTanto", mapOf("n" to TrucoGame.tanto(myHand))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (session.isHost) session.hostTrucoRespond(true) else session.guestTrucoRespond(true)
                }) { Text(state.t("truco.accept")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (session.isHost) session.hostTrucoRespond(false) else session.guestTrucoRespond(false)
                }) { Text(state.t("truco.reject")) }
            },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("truco.title"),
            pdfSubtitle = state.t("truco.subtitle"),
        ) { historyOpen = false }
    }
}

@Composable
private fun RoomLudoBoard(
    state: ShellState,
    session: NativeRoomController,
    board: LudoState,
) {
    val ludo = remember(board.players.size) { LudoGame(board.players.size, allHumans = true) }
    val mySeat = session.mySeat.coerceIn(0, board.players.lastIndex)
    val myPlayer = board.players.getOrNull(mySeat)
    val myTurn = board.phase != LudoPhase.MatchEnd && board.turn == mySeat && myPlayer?.isBot == false
    val legal = if (board.phase == LudoPhase.Move && myTurn) {
        ludo.legalMoves(board).map { it.tokenId }.toSet()
    } else {
        emptySet()
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            val cell = maxWidth / 15
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 15) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 15) {
                            val zone = ludoZone(col, row)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(zone.first)
                                    .border(0.4.dp, Color.Black.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (zone.second) {
                                    Text("★", fontSize = (cell.value * 0.35f).sp, color = Color(0x66FFFFFF))
                                }
                            }
                        }
                    }
                }
            }

            val byCell = board.tokens.groupBy { LudoGame.tokenCoord(it) }
            for ((coord, tokens) in byCell) {
                tokens.forEachIndexed { index, token ->
                    val tokenSize = cell * 0.62f
                    val baseX = cell * coord.first + cell * 0.19f + (index * 2).dp
                    val baseY = cell * coord.second + cell * 0.19f + (index * 2).dp
                    val x by animateDpAsState(baseX, label = "room-ludo-x-${token.id}")
                    val y by animateDpAsState(baseY, label = "room-ludo-y-${token.id}")
                    Box(
                        Modifier
                            .offset(x = x, y = y)
                            .size(tokenSize)
                            .clip(CircleShape)
                            .background(LUDO_COLORS[token.color] ?: Color.Gray)
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                            .semantics {
                                contentDescription = "Ficha ${token.slot + 1}, ${token.color.name}, casilla ${token.progress}"
                            }
                            .then(
                                if (token.id in legal) {
                                    Modifier.clickable {
                                        if (session.isHost) session.hostLudoMove(token.id) else session.guestLudoMove(token.id)
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${token.slot + 1}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            board.players.forEachIndexed { index, player ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = LUDO_COLORS[player.color]?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == mySeat) state.t("common.you") else player.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${ludo.finishedCount(board, index)}/4 · ${player.captures}⚔",
                            style = MaterialTheme.typography.labelSmall,
                            color = LUDO_COLORS[player.color] ?: MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Button(
            onClick = { if (session.isHost) session.hostLudoRoll() else session.guestLudoRoll() },
            enabled = myTurn && board.phase == LudoPhase.Roll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🎲 ${state.t("ludo.roll")}${board.lastRoll?.let { " · $it" } ?: ""}")
        }

        Text(
            when {
                board.phase == LudoPhase.MatchEnd -> state.t("ludo.winner", mapOf("p" to (board.players.getOrNull(board.winnerIndex ?: 0)?.name ?: "")))
                board.turn != mySeat -> state.t("room.theirTurn", mapOf("name" to (board.players.getOrNull(board.turn)?.name ?: "")))
                board.phase == LudoPhase.Roll -> state.t("ludo.yourRoll")
                else -> state.t("ludo.movePrompt")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
