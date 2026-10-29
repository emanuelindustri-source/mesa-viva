package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.UrGame
import com.mesaviva.app.games.UrPhase
import com.mesaviva.app.games.UrSide
import com.mesaviva.app.games.UrState
import kotlinx.coroutines.delay

private val UR_WHITE = Color(0xFFF7F1E3)
private val UR_BLACK = Color(0xFF241A13)
private val UR_TRACK = Color(0xFFD7BE92)
private val UR_GOLD = Color(0xFFE0A63A)

@Composable
fun UrScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(UrGame()) }
    var board by remember { mutableStateOf(UrGame.initial()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == UrSide.White
    val legal = remember(board, humanTurn) {
        if (board.phase == UrPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }

    fun finishIfNeeded(current: UrState) {
        if (current.phase != UrPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == UrSide.White
        state.awardMatch(win = won)
        NativeAudio.play(context, if (won) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
    }

    fun start() {
        game = UrGame()
        board = UrGame.initial()
        awarded = false
        setupOpen = false
        resultOpen = false
        historyOpen = false
    }

    fun roll(value: Int = game.botDice()) {
        if (board.phase != UrPhase.Play) return
        val res = game.roll(board, value)
        board = res.state
        NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
        state.vibrate(context, 22)
        if (res.state.phase == UrPhase.MatchEnd) {
            finishIfNeeded(res.state)
            resultOpen = true
        }
    }

    fun move(pieceId: Int) {
        val res = game.move(board, pieceId)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        when {
            res.state.phase == UrPhase.MatchEnd -> {
                NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
                finishIfNeeded(res.state)
                resultOpen = true
            }
            res.state.captured -> NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
            res.state.extraTurn -> NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
            else -> NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        }
        state.vibrate(context, if (res.state.captured) 46 else 20)
    }

    LaunchedEffect(board.turn, board.dice, board.phase, allHumans) {
        if (allHumans || board.phase == UrPhase.MatchEnd || board.turn != UrSide.Black) return@LaunchedEffect
        delay(560)
        if (board.dice !in 1..4) {
            val res = game.roll(board, game.botDice())
            board = res.state
            if (res.state.phase == UrPhase.MatchEnd) {
                finishIfNeeded(res.state)
                resultOpen = true
            }
        } else {
            val choice = game.botChoice(board)
            if (choice != null) {
                move(choice)
            } else {
                board = board.copy(dice = 0, turn = UrGame.other(board.turn))
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("ur.title"),
            subtitle = state.t("ur.subtitle"),
            badge = state.t("ur.score", mapOf("w" to game.finishedOf(board, UrSide.White), "b" to game.finishedOf(board, UrSide.Black))),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == UrPhase.MatchEnd -> resultText(state, board, allHumans)
                board.dice in 1..4 && humanTurn -> state.t("ur.dice", mapOf("n" to board.dice)) + " · " + state.t("ur.choose")
                !humanTurn -> state.t("ur.botTurn")
                else -> state.t("ur.yourTurn")
            },
            alert = board.phase == UrPhase.MatchEnd,
        )
        Spacer(Modifier.height(4.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UrPile(state.t("ur.start"), board.pieces.count { it.side == UrSide.White && it.position < 0 }, UrSide.White, Modifier.weight(1f))
            Text("🎲", fontSize = 26.sp)
            Text(
                if (board.dice > 0) board.dice.toString() else "—",
                style = MaterialTheme.typography.headlineMedium,
                color = UR_GOLD,
            )
            Text("🎲", fontSize = 26.sp)
            UrPile(state.t("ur.start"), board.pieces.count { it.side == UrSide.Black && it.position < 0 }, UrSide.Black, Modifier.weight(1f))
        }

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF6E4A32))
                .border(2.dp, Color(0xFF4A2E1B), RoundedCornerShape(14.dp))
                .padding(6.dp),
        ) {
            val cell = maxWidth / 5
            Column(Modifier.fillMaxWidth()) {
                val startPieces = board.pieces.filter { it.side == board.turn && it.position < 0 }
                val entryCell = board.dice - 1
                for (row in 0 until 4) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 5) {
                            val number = if (row % 2 == 0) row * 5 + col else row * 5 + (4 - col)
                            val piecesHere = board.pieces.filter { it.position == number }
                            val candidates = piecesHere.filter { it.id in legal } +
                                if (number == entryCell) startPieces.filter { it.id in legal } else emptyList()
                            val isLegal = candidates.isNotEmpty()
                            val rosette = number in UrGame.ROSETTES
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.86f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isLegal) Color(0x66E0A63A) else UR_TRACK)
                                    .border(
                                        if (rosette) 3.dp else 1.dp,
                                        if (rosette) UR_GOLD else Color(0xFF8A6A45),
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        if (!isLegal) return@clickable
                                        move(candidates.first().id)
                                    }
                                    .semantics {
                                        contentDescription = buildString {
                                            append("Casilla $number")
                                            if (rosette) append(", roseta")
                                            piecesHere.forEach {
                                                append(", ")
                                                append(if (it.side == UrSide.White) "blanca" else "negra")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    number.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF5A3A22),
                                    modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                                )
                                if (rosette) {
                                    Text("✦", color = Color(0xFF8A6A45), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                    piecesHere.take(4).forEach { piece ->
                                        Box(
                                            Modifier
                                                .size(if (cell.value > 60) 13.dp else 9.dp)
                                                .clip(CircleShape)
                                                .background(if (piece.side == UrSide.White) UR_WHITE else UR_BLACK)
                                                .border(1.dp, Color(0xFF8A6A45), CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "${state.t("reversi.white")}: ${state.t("ur.finish")} ${game.finishedOf(board, UrSide.White)}/4 · " +
                "${state.t("reversi.black")}: ${state.t("ur.finish")} ${game.finishedOf(board, UrSide.Black)}/4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { roll() },
                enabled = humanTurn && board.phase == UrPhase.Play && board.dice !in 1..4,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("ur.roll")) }
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(0.8f)) {
                Text(state.t("common.history"))
            }
            OutlinedButton(onClick = { resignOpen = true }, enabled = board.phase == UrPhase.Play) {
                Text(state.t("common.resign"))
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
        Spacer(Modifier.height(4.dp))
        Text(
            state.t("ur.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("ur.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(state.t("ur.rules"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = { TextButton(onClick = { setupOpen = false }) { Text(state.t("common.close")) } },
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
                    finishIfNeeded(board)
                    resultOpen = true
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("ur.title"),
            pdfSubtitle = state.t("ur.subtitle"),
            stats = listOf(
                state.t("ur.finish") to "${game.finishedOf(board, UrSide.White)} / 4",
                "⚔" to board.history.count { it.contains("⚔") }.toString(),
                "🎲" to board.history.count { it.contains("🎲") }.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen && board.phase == UrPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(resultText(state, board, allHumans)) },
            text = { Text(state.t("ur.rules"), style = MaterialTheme.typography.bodySmall) },
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

@Composable
private fun UrPile(label: String, count: Int, side: UrSide, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count.coerceAtMost(4)) {
                Box(
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(if (side == UrSide.White) UR_WHITE else UR_BLACK)
                        .border(1.dp, Color(0xFF8A6A45), CircleShape),
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun resultText(state: ShellState, board: UrState, allHumans: Boolean): String = when {
    board.winner == UrSide.White -> state.t("ur.won")
    board.winner == UrSide.Black && allHumans -> state.t("common.player2Won")
    board.winner == UrSide.Black -> state.t("ur.botWon")
    else -> state.t("common.matchOver")
}
