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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.CaidaAction
import com.mesaviva.app.games.CaidaGame
import com.mesaviva.app.games.CaidaPhase
import com.mesaviva.app.games.CaidaState
import com.mesaviva.app.games.SpanishCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CaidaScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableIntStateOf(4) }
    var target by remember { mutableIntStateOf(100) }
    var teams by remember { mutableStateOf(false) }
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(CaidaGame(4, 100, false, false)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var revealHand by remember { mutableStateOf(false) }
    var effect by remember { mutableIntStateOf(0) }

    val humanTurn = board.seats.getOrNull(board.turn)?.isBot == false
    val humanDealer = board.seats.getOrNull(board.dealer)?.isBot == false
    val canClaim = game.canClaim(board, board.turn)
    val canto = if (canClaim) game.detectCanto(game.handOf(board, board.turn)) else null
    val handSeat = if (allHumans) board.turn else 0
    val showHand = !allHumans || (humanTurn && revealHand)

    LaunchedEffect(board.turn, board.handNo) {
        revealHand = false
    }

    fun start() {
        game = CaidaGame(players, target, teams && players == 4, allHumans)
        board = game.newMatch()
        setupOpen = false
        resultOpen = false
        historyOpen = false
        revealHand = false
        effect = 0
    }

    fun deliver() {
        val res = game.deliver(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        effect++
        state.vibrate(context, 30)
        if (board.phase == CaidaPhase.MatchEnd) resultOpen = true
    }

    fun play(card: SpanishCard) {
        val res = game.playCard(board, card.id)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        effect++
        state.vibrate(context, if (res.state.lastCapture != null) 55 else 22)
        NativeAudio.play(
            context,
            if (res.state.lastCapture != null) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card,
            state.sfx,
        )
        if (board.phase == CaidaPhase.MatchEnd) resultOpen = true
    }

    fun canto() {
        val res = game.claimCanto(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        effect++
        state.vibrate(context, 40)
        NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
        if (board.phase == CaidaPhase.MatchEnd) resultOpen = true
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
    }

    LaunchedEffect(board.turn, board.dealer, board.phase, board.handNo, board.history.size, allHumans) {
        val botTurn = when (board.phase) {
            CaidaPhase.Entrega -> board.seats.getOrNull(board.dealer)?.isBot == true
            CaidaPhase.Play -> board.seats.getOrNull(board.turn)?.isBot == true
            else -> false
        }
        if (!botTurn) return@LaunchedEffect
        delay(720)
        when (val action = withContext(Dispatchers.Default) { game.botAction(board) }) {
            CaidaAction.Deliver -> deliver()
            CaidaAction.Canto -> canto()
            is CaidaAction.PlayCard -> {
                val card = game.handOf(board, board.turn).firstOrNull { it.id == action.cardId }
                if (card != null) play(card)
            }
            CaidaAction.NextHand -> nextHand()
            null -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("caida.title"),
            subtitle = state.t("caida.subtitle"),
            badge = state.t("caida.badge", mapOf("h" to board.handNo, "t" to board.target)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when (board.phase) {
                CaidaPhase.Entrega -> if (humanDealer) {
                    state.t("caida.yourDelivery")
                } else {
                    state.t("caida.waitDelivery", mapOf("p" to (board.seats.getOrNull(board.dealer)?.name ?: "—")))
                }
                CaidaPhase.Play -> when {
                    canClaim -> state.t("caida.cantoAvailable", mapOf("c" to game.cantoName(canto?.key ?: ""), "n" to (canto?.points ?: 0)))
                    humanTurn -> state.t("caida.yourTurn")
                    else -> state.t("caida.waitTurn", mapOf("p" to (board.seats.getOrNull(board.turn)?.name ?: "—")))
                }
                CaidaPhase.HandEnd -> state.t("caida.handEnd")
                CaidaPhase.MatchEnd -> caidaResultText(state, board)
            },
            alert = canClaim || board.phase == CaidaPhase.HandEnd,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            board.seats.forEachIndexed { index, seat ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        index == board.turn && board.phase == CaidaPhase.Play ->
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        seat.team != null -> Color(if (seat.team == 0) 0xFFE0A63A else 0xFF4C7BD9).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    },
                ) {
                    Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == 0) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${board.scores[index]} pts · ${state.t("caida.cardsShort", mapOf("n" to game.handOf(board, index).size))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF1B4A3F), Color(0xFF0E2B26)),
                    ),
                ),
        ) {
            Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardDeckBack(Modifier.size(48.dp, 70.dp), board.deck.size)
                    Text(
                        state.t("caida.deck"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        state.t("caida.table", mapOf("n" to board.table.size)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    val shown = board.table.takeLast(6)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        shown.forEach { card ->
                            SpanishCardFace(
                                card = card,
                                modifier = Modifier.size(43.dp, 64.dp),
                                faceDown = false,
                            )
                        }
                    }
                    if (board.table.size > shown.size) {
                        Text(
                            "+${board.table.size - shown.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    board.lastCapture?.let { capture ->
                        Text(
                            state.t(
                                if (capture.mesa) "caida.lastMesa" else "caida.lastCapture",
                                mapOf("p" to (board.seats.getOrNull(capture.player)?.name ?: ""), "n" to capture.points),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    }
                }
            }
            TableDust(effect, Modifier.fillMaxSize(), color = Color(0xFFE0C28A))
        }

        board.delivery?.let { delivery ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        if (delivery.ok) "✓" else "✗",
                        color = if (delivery.ok) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                    delivery.cards.forEach { card ->
                        SpanishCardFace(card, Modifier.size(28.dp, 42.dp))
                    }
                    Text(
                        state.t("caida.deliveryResult", mapOf(
                            "p" to (board.seats.getOrNull(delivery.toPlayer)?.name ?: ""),
                            "n" to delivery.points,
                        )),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.t("caida.hand"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (allHumans && humanTurn && !revealHand) {
                TextButton(onClick = { revealHand = true }) { Text(state.t("caida.showHand")) }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val hand = game.sortHand(game.handOf(board, handSeat))
            hand.forEach { card ->
                SpanishCardFace(
                    card = card,
                    modifier = Modifier.size(58.dp, 86.dp),
                    faceDown = !showHand,
                    playable = humanTurn && board.phase == CaidaPhase.Play,
                    onClick = if (humanTurn && board.phase == CaidaPhase.Play) {
                        { play(card) }
                    } else {
                        null
                    },
                )
            }
            if (hand.isEmpty()) {
                Text(
                    state.t("caida.noCards"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        if (allHumans && humanTurn && !revealHand) {
            Text(
                state.t("caida.privacy"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            when (board.phase) {
                CaidaPhase.Entrega -> Button(
                    onClick = { deliver() },
                    enabled = humanDealer,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("🎴 ${state.t("caida.deliver")}")
                }
                CaidaPhase.HandEnd -> Button(
                    onClick = { nextHand() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(state.t("caida.nextHand"))
                }
                else -> {
                    if (canClaim && humanTurn) {
                        Button(onClick = { canto() }, modifier = Modifier.weight(1f)) {
                            Text("📣 ${state.t("caida.canto")} +${canto?.points ?: 0}")
                        }
                    } else {
                        OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                            Text(state.t("common.history"))
                        }
                    }
                }
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == CaidaPhase.Play) {
                Text(state.t("common.resign"))
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("caida.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("caida.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4).forEach { value ->
                            FilterChip(
                                selected = players == value,
                                onClick = { players = value; if (value != 4) teams = false },
                                label = { Text("$value") },
                            )
                        }
                    }
                    if (players == 4) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(state.t("caida.teams"), modifier = Modifier.weight(1f))
                            Switch(checked = teams, onCheckedChange = { teams = it })
                        }
                    }
                    Text(state.t("caida.target"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(40, 70, 100).forEach { value ->
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
                        state.t("caida.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = { TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) { Text(state.t("common.close")) } },
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
                    resultOpen = true
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) NativeHistoryDialog(
        state,
        board.history,
        pdfTitle = state.t("caida.title"),
        pdfSubtitle = state.t("caida.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == CaidaPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(caidaResultText(state, board)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.seats.forEachIndexed { index, seat ->
                        Text(
                            "${if (index == 0) state.t("common.you") else seat.name}: ${board.scores[index]} pts",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (board.teams) {
                        Text(
                            state.t("caida.teamScore", mapOf(
                                "a" to game.teamTotal(board, 0),
                                "b" to game.teamTotal(board, 1),
                            )),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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

private fun caidaResultText(state: ShellState, board: CaidaState): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && board.seats.getOrNull(board.winner ?: 0)?.isBot == false ->
        state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
