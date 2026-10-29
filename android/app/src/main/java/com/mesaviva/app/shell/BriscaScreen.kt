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
import com.mesaviva.app.games.BriscaAction
import com.mesaviva.app.games.BriscaGame
import com.mesaviva.app.games.BriscaPhase
import com.mesaviva.app.games.BriscaState
import com.mesaviva.app.games.SpanishCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun BriscaScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableIntStateOf(2) }
    var target by remember { mutableIntStateOf(100) }
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(BriscaGame(2, 100, false)) }
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
    val myTeam = board.seats.getOrNull(0)?.team

    LaunchedEffect(board.turn, board.handNo) {
        revealHand = false
    }

    fun finish(current: BriscaState) {
        if (current.phase != BriscaPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner?.let { winner ->
            if (current.teams) current.seats[winner].team == myTeam else winner == 0
        } ?: false
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
        game = BriscaGame(players, target, allHumans)
        board = game.newMatch()
        setupOpen = false
        resultOpen = false
        historyOpen = false
        revealHand = false
        effect = 0
        awarded = false
    }

    fun play(card: SpanishCard) {
        val res = game.playCard(board, card.id)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        effect++
        val won = res.state.lastTrick?.winner == 0 || (res.state.teams && res.state.lastTrick?.winner?.let { res.state.seats[it].team == myTeam } == true)
        NativeAudio.play(context, if (won) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card, state.sfx)
        state.vibrate(context, if (won) 44 else 18)
        finish(board)
        if (board.phase == BriscaPhase.MatchEnd) resultOpen = true
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

    LaunchedEffect(board.turn, board.phase, board.handNo, board.history.size, allHumans) {
        if (allHumans || board.phase != BriscaPhase.Playing) return@LaunchedEffect
        if (board.seats.getOrNull(board.turn)?.isBot != true) return@LaunchedEffect
        delay(620)
        when (val action = withContext(Dispatchers.Default) { game.botAction(board) }) {
            is BriscaAction.PlayCard -> {
                val card = game.handOf(board, board.turn).firstOrNull { it.id == action.cardId }
                if (card != null) play(card)
            }
            BriscaAction.NextHand -> nextHand()
            null -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("brisca.title"),
            subtitle = state.t("brisca.subtitle"),
            badge = state.t("brisca.badge", mapOf("h" to board.handNo, "t" to board.target)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == BriscaPhase.HandEnd -> state.t("brisca.handEnd")
                board.phase == BriscaPhase.MatchEnd -> briscaResultText(state, board)
                humanTurn -> state.t("brisca.yourTurn")
                else -> state.t("brisca.waitTurn", mapOf("p" to (board.seats.getOrNull(board.turn)?.name ?: "—")))
            },
            alert = board.phase != BriscaPhase.Playing,
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
                        index == board.turn && board.phase == BriscaPhase.Playing ->
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        seat.team != null -> Color(if (seat.team == 0) 0xFFE0A63A else 0xFF4C7BD9).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    },
                ) {
                    Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (index == 0) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${board.scores[index]} pts · ${game.handOf(board, index).size}",
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
                .height(196.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF1B4A3F), Color(0xFF0E2B26)),
                    ),
                ),
        ) {
            Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CardDeckBack(Modifier.size(48.dp, 70.dp), board.deck.size + if (board.trumpCard != null) 1 else 0)
                    Text(
                        state.t("brisca.deck"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.size(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val trump = board.trumpCard
                    if (trump != null) {
                        SpanishCardFace(trump, Modifier.size(46.dp, 68.dp))
                        Text(
                            state.t("brisca.trump", mapOf("s" to board.trumpSuit.symbol)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    } else {
                        Box(Modifier.size(46.dp, 68.dp).background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)))
                        Text(
                            state.t("brisca.trumpGone"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val shownTrick = if (board.trick.isNotEmpty()) {
                        board.trick
                    } else {
                        board.lastTrick?.plays ?: emptyList()
                    }
                    Text(
                        if (board.trick.isNotEmpty()) state.t("brisca.trickInPlay") else state.t("brisca.lastTrick"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        shownTrick.forEach { play ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SpanishCardFace(
                                    play.card,
                                    Modifier.size(48.dp, 72.dp),
                                    playable = false,
                                )
                                Text(
                                    board.seats.getOrNull(play.seat)?.name ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (board.lastTrick?.winner == play.seat) Color(0xFFE0A63A) else Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                )
                            }
                        }
                        if (shownTrick.isEmpty()) {
                            Text(
                                state.t("brisca.noTrick"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                    board.lastTrick?.let { trick ->
                        Text(
                            state.t(
                                "brisca.trickWon",
                                mapOf(
                                    "p" to (board.seats.getOrNull(trick.winner)?.name ?: ""),
                                    "n" to trick.points,
                                ),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0A63A),
                        )
                    }
                    Text(
                        state.t("brisca.captured", mapOf("n" to board.capturedPoints.getOrElse(0) { 0 })),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
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
                state.t("brisca.hand", mapOf("n" to game.handOf(board, handSeat).size)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (allHumans && humanTurn && !revealHand) {
                TextButton(onClick = { revealHand = true }) { Text(state.t("brisca.showHand")) }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            game.sortHand(game.handOf(board, handSeat)).forEach { card ->
                SpanishCardFace(
                    card = card,
                    modifier = Modifier.size(64.dp, 96.dp),
                    faceDown = !showHand,
                    playable = humanTurn && board.phase == BriscaPhase.Playing,
                    onClick = if (humanTurn && board.phase == BriscaPhase.Playing) {
                        { play(card) }
                    } else {
                        null
                    },
                )
            }
            if (game.handOf(board, handSeat).isEmpty()) {
                Text(
                    state.t("brisca.noCards"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        if (allHumans && humanTurn && !revealHand) {
            Text(
                state.t("brisca.privacy"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (board.phase == BriscaPhase.HandEnd) {
                Button(onClick = { nextHand() }, modifier = Modifier.weight(1f)) {
                    Text(state.t("brisca.nextHand"))
                }
            } else {
                OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                    Text(state.t("common.history"))
                }
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            Button(
                onClick = { resignOpen = true },
                enabled = board.phase == BriscaPhase.Playing,
            ) { Text(state.t("common.resign")) }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("brisca.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("brisca.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4).forEach { value ->
                            FilterChip(
                                selected = players == value,
                                onClick = { players = value },
                                label = { Text(if (value == 4) "4 · ${state.t("brisca.pairs")}" else "2") },
                            )
                        }
                    }
                    Text(state.t("brisca.target"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(50, 100, 120).forEach { value ->
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
                        state.t("brisca.rules"),
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
        pdfTitle = state.t("brisca.title"),
        pdfSubtitle = state.t("brisca.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == BriscaPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(briscaResultText(state, board)) },
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
                            state.t(
                                "brisca.teamScore",
                                mapOf("a" to game.teamTotal(board, 0), "b" to game.teamTotal(board, 1)),
                            ),
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

private fun briscaResultText(state: ShellState, board: BriscaState): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && board.seats.getOrNull(board.winner ?: 0)?.isBot == false ->
        state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
