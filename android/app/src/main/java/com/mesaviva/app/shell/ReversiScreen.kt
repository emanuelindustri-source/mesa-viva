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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.ReversiColor
import com.mesaviva.app.games.ReversiGame
import com.mesaviva.app.games.ReversiPhase
import com.mesaviva.app.games.ReversiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ReversiScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(ReversiGame()) }
    var board by remember { mutableStateOf(ReversiGame.initial()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == ReversiColor.White
    val legal = remember(board, humanTurn) {
        if (board.phase == ReversiPhase.Play && humanTurn) game.legalMoves(board) else emptyMap()
    }

    fun finishIfNeeded(current: ReversiState) {
        if (current.phase != ReversiPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == ReversiColor.White
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
        game = ReversiGame()
        board = ReversiGame.initial()
        awarded = false
        setupOpen = false
        resultOpen = false
        historyOpen = false
    }

    fun apply(index: Int) {
        val res = game.move(board, index)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        val captured = res.state.lastFlips.isNotEmpty()
        NativeAudio.play(context, if (captured) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card, state.sfx)
        state.vibrate(context, if (captured) 38 else 18)
        finishIfNeeded(res.state)
        if (res.state.phase == ReversiPhase.MatchEnd) resultOpen = true
    }

    LaunchedEffect(board.turn, board.phase, allHumans, difficulty) {
        if (allHumans || board.phase == ReversiPhase.MatchEnd || board.turn != ReversiColor.Black) return@LaunchedEffect
        delay(420)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == ReversiPhase.Play && board.turn == ReversiColor.Black) {
            apply(move)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("reversi.title"),
            subtitle = state.t("reversi.subtitle"),
            badge = state.t("reversi.scoreBadge", mapOf("w" to board.whiteCount, "b" to board.blackCount)),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == ReversiPhase.MatchEnd -> resultText(state, board, allHumans)
                board.passed -> state.t("reversi.passed")
                !humanTurn -> state.t("reversi.botTurn")
                else -> state.t("reversi.yourTurn")
            },
            alert = board.phase == ReversiPhase.MatchEnd,
        )
        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF1B4D3E), RoundedCornerShape(12.dp))
                .background(Color(0xFF1F6A58)),
        ) {
            val cell = maxWidth / 8
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 8) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 8) {
                            val index = row * 8 + col
                            val value = board.board[index]
                            val isLegal = index in legal
                            val isLast = board.lastMove == index || index in board.lastFlips
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.4.dp, Color(0xFF0E3B34))
                                    .background(
                                        when {
                                            isLast -> Color(0x66E0A63A)
                                            else -> Color(0xFF1F6A58)
                                        },
                                    )
                                    .clickable { if (isLegal) apply(index) }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(ReversiGame.square(index))
                                            when (value) {
                                                1 -> append(", ficha blanca")
                                                2 -> append(", ficha negra")
                                                else -> append(", vacía")
                                            }
                                            if (isLegal) append(", jugada legal")
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (value != 0) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(cell * 0.07f)
                                            .background(
                                                if (value == 1) Color(0xFFF7F1E3) else Color(0xFF17130E),
                                                CircleShape,
                                            )
                                            .border(
                                                (cell.value * 0.012f).dp,
                                                if (value == 1) Color(0xFF8A6A45) else Color(0xFF000000),
                                                CircleShape,
                                            ),
                                    )
                                } else if (isLegal) {
                                    Box(
                                        Modifier
                                            .size(cell * 0.18f)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0A63A).copy(alpha = 0.75f)),
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
            Text("● ${board.whiteCount}   ○ ${board.blackCount}", style = MaterialTheme.typography.labelSmall)
            Text(
                state.t("reversi.scoreBadge", mapOf("w" to board.whiteCount, "b" to board.blackCount)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NativeMoveLog(state, board.history) { historyOpen = true }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == ReversiPhase.Play) {
                Text(state.t("common.resign"))
            }
        }
    }

    if (setupOpen) {
        ReversiSetupDialog(state, allHumans, difficulty, { allHumans = it }, { difficulty = it }, {
            if (board.lastMove != null) setupOpen = false
        }, { start() })
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
            pdfTitle = state.t("reversi.title"),
            pdfSubtitle = state.t("reversi.subtitle"),
            stats = listOf(
                state.t("reversi.white") to board.whiteCount.toString(),
                state.t("reversi.black") to board.blackCount.toString(),
                "⚔" to board.history.count { it.contains("+") }.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen && board.phase == ReversiPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(resultText(state, board, allHumans)) },
            text = {
                Text("● ${board.whiteCount}   ○ ${board.blackCount}", style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun ReversiSetupDialog(
    state: ShellState,
    allHumans: Boolean,
    difficulty: String,
    onAllHumans: (Boolean) -> Unit,
    onDifficulty: (String) -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.t("reversi.setup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                    Switch(checked = allHumans, onCheckedChange = onAllHumans)
                }
                if (!allHumans) {
                    Text(state.t("common.difficulty"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("easy", "normal", "hard").forEach { value ->
                            FilterChip(
                                selected = difficulty == value,
                                onClick = { onDifficulty(value) },
                                label = { Text(state.t("common.$value")) },
                            )
                        }
                    }
                }
                Text(state.t("reversi.rules"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onStart) { Text(state.t("common.start")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("common.close")) } },
    )
}

private fun resultText(state: ShellState, board: ReversiState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == ReversiColor.White -> state.t("reversi.won")
    board.winner == ReversiColor.Black && allHumans -> state.t("common.player2Won")
    board.winner == ReversiColor.Black -> state.t("reversi.botWon")
    else -> state.t("common.matchOver")
}
