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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesaviva.app.games.FuriaBotAction
import com.mesaviva.app.games.FuriaCard
import com.mesaviva.app.games.FuriaGame
import com.mesaviva.app.games.FuriaPhase
import com.mesaviva.app.games.FuriaState
import com.mesaviva.app.games.FuriaSuit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun FuriaScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableIntStateOf(4) }
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(FuriaGame(4)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var pendingWild by remember { mutableStateOf<FuriaCard?>(null) }
    var revealHand by remember { mutableStateOf(false) }
    var effect by remember { mutableIntStateOf(0) }

    val humanTurn = board.seats.getOrNull(board.turn)?.isBot == false
    val handSeat = if (allHumans) board.turn else 0
    val showHand = !allHumans || (humanTurn && revealHand)
    val lastShout = board.lastShout
    val anyLastCard = board.hands.any { it.size == 1 }

    LaunchedEffect(board.turn) {
        revealHand = false
    }

    fun start() {
        game = FuriaGame(players, allHumans)
        board = game.newMatch()
        setupOpen = false
        resultOpen = false
        historyOpen = false
        pendingWild = null
        effect = 0
    }

    fun play(card: FuriaCard, chosen: FuriaSuit? = null) {
        val res = game.play(board, card.id, chosen)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        pendingWild = null
        effect++
        state.vibrate(context, if (res.state.phase == FuriaPhase.MatchEnd) 70 else 22)
        if (board.phase == FuriaPhase.MatchEnd) resultOpen = true
    }

    fun draw() {
        val res = game.draw(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        effect++
        state.vibrate(context, 18)
    }

    fun pass() {
        val res = game.pass(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
    }

    fun onCard(card: FuriaCard) {
        if (!humanTurn || board.phase != FuriaPhase.Play) return
        if (!game.canPlayOn(card, board)) {
            Toast.makeText(context, state.t("furia.error.cardDoesNotMatch"), Toast.LENGTH_SHORT).show()
            return
        }
        if (board.hasDrawn && board.drawnId != card.id) {
            Toast.makeText(context, state.t("furia.error.onlyDrawn"), Toast.LENGTH_SHORT).show()
            return
        }
        if (FuriaGame.isWild(card)) {
            pendingWild = card
        } else {
            play(card)
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, board.hasDrawn, allHumans) {
        val seat = board.seats.getOrNull(board.turn) ?: return@LaunchedEffect
        if (!seat.isBot || board.phase == FuriaPhase.MatchEnd) return@LaunchedEffect
        delay(620)
        when (val action = withContext(Dispatchers.Default) { game.botAction(board) }) {
            FuriaBotAction.Draw -> draw()
            FuriaBotAction.Pass -> pass()
            is FuriaBotAction.PlayCard -> {
                val card = game.sortHand(board.hands.getOrElse(board.turn) { emptyList() })
                    .firstOrNull { it.id == action.cardId }
                if (card != null) play(card, action.color)
            }
            null -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("furia.title"),
            subtitle = state.t("furia.subtitle"),
            badge = state.t("furia.badge", mapOf("n" to board.hands.getOrElse(handSeat) { emptyList() }.size)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == FuriaPhase.MatchEnd -> furiaResultText(state, board)
                anyLastCard -> state.t("furia.lastCardBanner")
                humanTurn && board.hasDrawn -> state.t("furia.drawn")
                humanTurn -> state.t("furia.yourTurn")
                else -> state.t("furia.waitTurn", mapOf("p" to (board.seats.getOrNull(board.turn)?.name ?: "—")))
            },
            alert = anyLastCard,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            board.seats.forEachIndexed { index, seat ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (index == board.turn) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
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
                            "${board.scores[index]} pts · ${state.t("furia.cardsShort", mapOf("n" to board.hands[index].size))}",
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
                    Brush.verticalGradient(listOf(Color(0xFF2B2433), Color(0xFF12101A))),
                ),
        ) {
            Row(
                Modifier.fillMaxSize().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardDeckBack(Modifier.size(48.dp, 70.dp), board.deck.size)
                    Text(
                        state.t("furia.deck"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                game.topCard(board)?.let { top ->
                    FuriaCardFace(top, Modifier.size(64.dp, 96.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(furiaColor(board.color)),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            state.t("furia.color.${board.color.name.lowercase()}"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        if (board.direction > 0) "↻ ${state.t("furia.clockwise")}" else "↺ ${state.t("furia.counterClockwise")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    board.lastPlay?.let { (player, card) ->
                        Text(
                            state.t("furia.lastPlay", mapOf(
                                "p" to (board.seats.getOrNull(player)?.name ?: ""),
                                "c" to FuriaGame.cardLabel(card),
                            )),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 2,
                        )
                    }
                    board.lastDraw?.let { (player, amount) ->
                        Text(
                            state.t("furia.lastDraw", mapOf(
                                "p" to (board.seats.getOrNull(player)?.name ?: ""),
                                "n" to amount,
                            )),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }
            TableDust(effect, Modifier.fillMaxSize(), color = Color(0xFFE0A63A))
        }

        Spacer(Modifier.height(5.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.t("furia.yourHand"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (allHumans && humanTurn && !revealHand) {
                    TextButton(onClick = { revealHand = true }) {
                        Text(state.t("furia.showHand"), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                state.t("furia.direction"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            game.sortHand(board.hands.getOrElse(handSeat) { emptyList() }).forEach { card ->
                val playable = humanTurn && game.canPlayOn(card, board) &&
                    (!board.hasDrawn || board.drawnId == card.id)
                FuriaCardFace(
                    card = card,
                    modifier = Modifier.size(50.dp, 74.dp),
                    faceDown = !showHand,
                    playable = playable,
                    onClick = if (humanTurn) {
                        { onCard(card) }
                    } else {
                        null
                    },
                )
            }
            if (board.hands.getOrElse(handSeat) { emptyList() }.isEmpty()) {
                Text(
                    state.t("furia.noCards"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (board.phase == FuriaPhase.Play && humanTurn) {
                if (!board.hasDrawn) {
                    Button(onClick = { draw() }, modifier = Modifier.weight(1f)) {
                        Text("🂠 ${state.t("furia.draw")}")
                    }
                } else {
                    OutlinedButton(onClick = { pass() }, modifier = Modifier.weight(1f)) {
                        Text(state.t("furia.pass"))
                    }
                }
            } else {
                OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                    Text(state.t("common.history"))
                }
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == FuriaPhase.Play) {
                Text(state.t("common.resign"))
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }

        if (allHumans && humanTurn && !revealHand) {
            Text(
                state.t("furia.privacy"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (lastShout != null) {
            Text(
                state.t("furia.shout", mapOf("p" to (board.seats.getOrNull(lastShout)?.name ?: ""))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (pendingWild != null) {
        AlertDialog(
            onDismissRequest = { pendingWild = null },
            title = { Text(state.t("furia.chooseColor")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FuriaGame.COLORS.forEach { suit ->
                        Button(
                            onClick = {
                                val card = pendingWild ?: return@Button
                                play(card, suit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(furiaColor(suit)),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(state.t("furia.color.${suit.name.lowercase()}"))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pendingWild = null }) { Text(state.t("common.cancel")) } },
        )
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("furia.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("furia.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4).forEach { value ->
                            FilterChip(
                                selected = players == value,
                                onClick = { players = value },
                                label = { Text("$value") },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("furia.rules"),
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

    if (historyOpen) NativeHistoryDialog(state, board.history) { historyOpen = false }

    if (resultOpen && board.phase == FuriaPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(furiaResultText(state, board)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.seats.forEachIndexed { index, seat ->
                        Text(
                            "${if (index == 0) state.t("common.you") else seat.name}: ${board.scores[index]} pts",
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

private fun furiaResultText(state: ShellState, board: FuriaState): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && board.seats.getOrNull(board.winner ?: 0)?.isBot == false ->
        state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
