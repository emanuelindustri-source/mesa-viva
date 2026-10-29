package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesaviva.app.games.SpanishCard
import com.mesaviva.app.games.TrucoChallenge
import com.mesaviva.app.games.TrucoGame
import com.mesaviva.app.games.TrucoPhase
import com.mesaviva.app.games.TrucoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun TrucoScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var target by remember { mutableIntStateOf(30) }
    var game by remember { mutableStateOf(TrucoGame(30, false)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var revealHand by remember { mutableStateOf(false) }
    var effect by remember { mutableIntStateOf(0) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = board.seats.getOrNull(board.turn)?.isBot == false
    val handSeat = if (allHumans) board.turn else 0
    val showHand = !allHumans || (humanTurn && revealHand)
    val humanResponds = board.pending != null && (allHumans || board.pending?.awaiting == 0)

    LaunchedEffect(board.turn, board.handNo) {
        revealHand = false
    }

    fun finish(current: TrucoState) {
        if (current.phase != TrucoPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == 0
        state.awardMatch(win = won, draw = current.draw)
        NativeAudio.play(
            context,
            when {
                current.draw -> NativeAudio.Sfx.Draw
                won -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
    }

    fun start() {
        game = TrucoGame(target, allHumans)
        board = game.newMatch()
        setupOpen = false
        resultOpen = false
        historyOpen = false
        revealHand = false
        effect = 0
        awarded = false
    }

    fun apply(result: com.mesaviva.app.games.TrucoResult, sound: NativeAudio.Sfx = NativeAudio.Sfx.Card) {
        if (result.error != null) {
            Toast.makeText(context, state.t(result.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = result.state
        effect++
        NativeAudio.play(context, sound, state.sfx)
        state.vibrate(context, 24)
        finish(board)
        if (board.phase == TrucoPhase.MatchEnd) resultOpen = true
    }

    fun play(card: SpanishCard) {
        apply(game.playCard(board, card.id), NativeAudio.Sfx.Card)
    }

    fun callEnvido(kind: String) {
        apply(game.callEnvido(board, handSeat, kind), NativeAudio.Sfx.Promote)
    }

    fun callTruco() {
        apply(game.callTruco(board, handSeat), NativeAudio.Sfx.Promote)
    }

    fun respond(accept: Boolean) {
        val stake = board.pending?.stake ?: 0
        apply(
            game.respond(board, accept),
            if (accept) NativeAudio.Sfx.Promote else NativeAudio.Sfx.Capture,
        )
        if (accept && stake > 0) state.vibrate(context, 42)
    }

    fun fold() {
        apply(game.fold(board), NativeAudio.Sfx.Lose)
    }

    fun nextHand() {
        val res = game.nextHand(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        revealHand = false
        effect = 0
        awarded = false
    }

    LaunchedEffect(
        board.phase,
        board.turn,
        board.pending,
        board.tricks.size,
        board.currentTrick.size,
        board.trucoLevel,
        board.handNo,
        allHumans,
    ) {
        if (allHumans) return@LaunchedEffect
        val pending = board.pending
        if (pending != null && pending.awaiting == 1) {
            delay(780)
            val accept = withContext(Dispatchers.Default) {
                if (pending.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
                    game.botAcceptsEnvido(board, 1, pending.stake)
                } else {
                    game.botAcceptsTruco(board, 1, pending.stake)
                }
            }
            respond(accept)
            return@LaunchedEffect
        }
        if (board.phase == TrucoPhase.Envido && board.turn == 1) {
            delay(760)
            val wants = withContext(Dispatchers.Default) { game.botWantsEnvido(board, 1) }
            if (wants) callEnvido("envido") else apply(game.startPlay(board), NativeAudio.Sfx.Tap)
            return@LaunchedEffect
        }
        if (board.phase == TrucoPhase.Play && board.turn == 1 && pending == null) {
            delay(680)
            val shouldCall = withContext(Dispatchers.Default) { game.botShouldCallTruco(board, 1) }
            if (shouldCall) {
                callTruco()
            } else {
                val card = withContext(Dispatchers.Default) { game.botPlayCard(board, 1) }
                if (card != null) play(card)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("truco.title"),
            subtitle = state.t("truco.subtitle"),
            badge = state.t("truco.badge", mapOf("h" to board.handNo, "t" to board.target)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.pending != null -> state.t(
                    "truco.pendingBanner",
                    mapOf(
                        "p" to (board.seats.getOrNull(board.pending?.caller ?: 0)?.name ?: ""),
                        "c" to TrucoGame.challengeName(board.pending?.kind ?: ""),
                        "n" to (board.pending?.stake ?: 0),
                    ),
                )
                board.phase == TrucoPhase.HandEnd -> state.t("truco.handEnd")
                board.phase == TrucoPhase.MatchEnd -> trucoResultText(state, board)
                board.phase == TrucoPhase.Envido -> state.t("truco.envidoPhase")
                humanTurn -> state.t("truco.yourTurn")
                else -> state.t("truco.waitTurn", mapOf("p" to (board.seats.getOrNull(board.turn)?.name ?: "—")))
            },
            alert = board.pending != null || board.phase != TrucoPhase.Play,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            board.seats.forEachIndexed { index, seat ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    color = if (index == board.turn && board.phase == TrucoPhase.Play) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                ) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == 0) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                        )
                        Text(
                            "${board.scores[index]} ${state.t("truco.pointsShort")} · ${board.trickWins[index]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFFE0A63A).copy(alpha = 0.16f),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.t("truco.stake"), style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${board.handStake}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0A63A),
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF2A1B3D), Color(0xFF120E1E)),
                    ),
                ),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        state.t("truco.table"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    val envidoReveal = board.envidoReveal
                    if (envidoReveal != null) {
                        Text(
                            state.t(
                                "truco.envidoShown",
                                mapOf(
                                    "a" to (envidoReveal.getOrNull(0) ?: 0),
                                    "b" to (envidoReveal.getOrNull(1) ?: 0),
                                ),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    } else if (board.envidoWinner != null) {
                        Text(
                            state.t("truco.envidoRejectedShort", mapOf("n" to board.envidoBet)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            state.t("truco.currentTrick"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (board.currentTrick.isEmpty()) {
                                Box(Modifier.size(52.dp, 78.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)))
                                Box(Modifier.size(52.dp, 78.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)))
                            } else {
                                board.currentTrick.forEach { p ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SpanishCardFace(p.card, Modifier.size(52.dp, 78.dp), playable = false)
                                        Text(
                                            board.seats.getOrNull(p.seat)?.name ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    board.tricks.takeLast(2).forEachIndexed { index, trick ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                state.t("truco.trickN", mapOf("n" to (board.tricks.size - minOf(2, board.tricks.size) + index + 1))),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                trick.plays.forEach { p ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SpanishCardFace(p.card, Modifier.size(42.dp, 63.dp), playable = false)
                                        Text(
                                            board.seats.getOrNull(p.seat)?.name ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (trick.winner == p.seat) Color(0xFFE0A63A) else Color.White.copy(alpha = 0.5f),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TableDust(effect, Modifier.fillMaxSize(), color = Color(0xFFE0C28A))
        }

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.t("truco.hand", mapOf("n" to game.handOf(board, handSeat).size)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("truco.tanto", mapOf("n" to game.tantoOf(board, handSeat))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            game.gameSort(game.handOf(board, handSeat)).reversed().forEach { card ->
                SpanishCardFace(
                    card = card,
                    modifier = Modifier.size(64.dp, 96.dp),
                    faceDown = !showHand,
                    playable = humanTurn && board.phase == TrucoPhase.Play && board.pending == null,
                    onClick = if (humanTurn && board.phase == TrucoPhase.Play && board.pending == null) {
                        { play(card) }
                    } else {
                        null
                    },
                )
            }
            if (game.handOf(board, handSeat).isEmpty()) {
                Text(
                    state.t("truco.noCards"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        if (allHumans && humanTurn && !revealHand) {
            TextButton(onClick = { revealHand = true }) { Text(state.t("truco.showHand")) }
        }

        Column(Modifier.fillMaxWidth().padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (board.phase == TrucoPhase.Envido && humanTurn && board.pending == null) {
                    OutlinedButton(onClick = { callEnvido("envido") }, modifier = Modifier.weight(1f)) {
                        Text("Envido 2")
                    }
                    OutlinedButton(onClick = { callEnvido("realEnvido") }, modifier = Modifier.weight(1f)) {
                        Text("Real 3")
                    }
                    Button(onClick = { apply(game.startPlay(board), NativeAudio.Sfx.Tap) }, modifier = Modifier.weight(1f)) {
                        Text(state.t("truco.playNoEnvido"))
                    }
                } else if (board.phase == TrucoPhase.Play && humanTurn && board.pending == null) {
                    if (board.trucoLevel < 3) {
                        Button(onClick = { callTruco() }, modifier = Modifier.weight(1f)) {
                            Text(
                                when (board.trucoLevel) {
                                    0 -> "Truco 2"
                                    1 -> "Retruco 3"
                                    else -> "Vale 4"
                                },
                            )
                        }
                    }
                    OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                        Text(state.t("common.history"))
                    }
                } else {
                    OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                        Text(state.t("common.history"))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Button(
                    onClick = { fold() },
                    enabled = (board.phase == TrucoPhase.Envido || board.phase == TrucoPhase.Play) &&
                        humanTurn && board.pending == null,
                    modifier = Modifier.weight(1f),
                ) { Text(state.t("truco.fold")) }
                OutlinedButton(onClick = { setupOpen = true }, modifier = Modifier.weight(1f)) {
                    Text(state.t("common.newMatch"))
                }
                OutlinedButton(onClick = { resignOpen = true }, enabled = board.phase != TrucoPhase.MatchEnd) {
                    Text(state.t("common.resign"))
                }
            }
        }

        if (board.phase == TrucoPhase.HandEnd) {
            Button(onClick = { nextHand() }, modifier = Modifier.fillMaxWidth().padding(top = 3.dp)) {
                Text(state.t("truco.nextHand"))
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("truco.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("truco.target"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30).forEach { value ->
                            FilterChip(
                                selected = target == value,
                                onClick = { target = value },
                                label = { Text("$value") },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("truco.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = {
                TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) {
                    Text(state.t("common.close"))
                }
            },
        )
    }

    if (humanResponds) {
        val pending = board.pending ?: return
        AlertDialog(
            onDismissRequest = { },
            title = { Text(state.t("truco.challengeTitle")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        state.t(
                            "truco.challengeBody",
                            mapOf(
                                "p" to (board.seats.getOrNull(pending.caller)?.name ?: ""),
                                "c" to TrucoGame.challengeName(pending.kind),
                                "n" to pending.stake,
                            ),
                        ),
                    )
                    if (pending.kind in setOf("envido", "realEnvido", "envidoEnvigido")) {
                        Text(
                            state.t("truco.yourTanto", mapOf("n" to game.tantoOf(board, pending.awaiting))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { respond(true) }) { Text(state.t("truco.accept")) }
            },
            dismissButton = {
                TextButton(onClick = { respond(false) }) { Text(state.t("truco.reject")) }
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
                    board = game.resign(board).state
                    awarded = true
                    resultOpen = true
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) NativeHistoryDialog(
        state,
        board.history,
        pdfTitle = state.t("truco.title"),
        pdfSubtitle = state.t("truco.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == TrucoPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(trucoResultText(state, board)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.seats.forEachIndexed { index, seat ->
                        Text(
                            "${if (index == 0) state.t("common.you") else seat.name}: ${board.scores[index]} ${state.t("truco.pointsShort")}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        state.t("truco.handNoSummary", mapOf("n" to board.handNo)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    resultOpen = false
                    setupOpen = true
                }) { Text(state.t("common.newMatch")) }
            },
            dismissButton = { TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) } },
        )
    }
}

private fun trucoResultText(state: ShellState, board: TrucoState): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && board.seats.getOrNull(board.winner ?: 0)?.isBot == false ->
        state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
