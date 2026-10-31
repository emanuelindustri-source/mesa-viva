package com.mesaviva.app.shell

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.JanggiGame
import com.mesaviva.app.games.JanggiMove
import com.mesaviva.app.games.JanggiPhase
import com.mesaviva.app.games.JanggiSide
import com.mesaviva.app.games.JanggiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun JanggiScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(JanggiGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == JanggiSide.Cho
    val legal = remember(board, humanTurn) {
        if (board.phase == JanggiPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }
    val targets = remember(selectedMoves) { selectedMoves.map { it.to }.toSet() }

    fun start() {
        game = JanggiGame()
        board = game.newMatch()
        selected = null
        setupOpen = false
        resultOpen = false
        historyOpen = false
        awarded = false
    }

    fun finish(current: JanggiState) {
        if (current.phase != JanggiPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == JanggiSide.Cho
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
        resultOpen = true
    }

    fun apply(move: JanggiMove) {
        val result = game.move(board, move)
        if (result.error != null) {
            selected = null
            return
        }
        val captured = board.board.getOrNull(move.to) != null
        board = result.state
        selected = null
        NativeAudio.play(
            context,
            when {
                result.state.phase == JanggiPhase.MatchEnd && result.state.winner == JanggiSide.Cho -> NativeAudio.Sfx.Win
                result.state.phase == JanggiPhase.MatchEnd -> NativeAudio.Sfx.Lose
                captured -> NativeAudio.Sfx.Capture
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        state.vibrate(context, if (captured) 42 else 18)
        finish(result.state)
    }

    fun tap(index: Int) {
        if (!humanTurn || board.phase != JanggiPhase.Play) return
        if (selected != null) {
            val move = selectedMoves.firstOrNull { it.to == index }
            if (move != null) {
                apply(move)
                return
            }
        }
        val piece = board.board.getOrNull(index)
        if (piece != null && piece.side == board.turn) {
            selected = if (selected == index) null else index
            state.vibrate(context, 12)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, allHumans, difficulty) {
        if (allHumans || board.phase != JanggiPhase.Play || board.turn != JanggiSide.Han) return@LaunchedEffect
        delay(540)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) } ?: return@LaunchedEffect
        val result = game.move(board, move)
        if (result.error == null) {
            board = result.state
            NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
            finish(result.state)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("janggi.title"),
            subtitle = state.t("janggi.subtitle"),
            badge = if (board.check != null) state.t("janggi.check") else state.t("janggi.badge"),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == JanggiPhase.MatchEnd -> janggiResultText(state, board, allHumans)
                board.check == JanggiSide.Cho -> state.t("janggi.yourCheck")
                board.check == JanggiSide.Han -> state.t("janggi.botCheck")
                !humanTurn -> state.t("janggi.botTurn")
                allHumans -> state.t("janggi.turnPlayer", mapOf("p" to if (board.turn == JanggiSide.Cho) 1 else 2))
                else -> state.t("janggi.yourTurn")
            },
            alert = board.check != null || board.phase == JanggiPhase.MatchEnd,
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, Color(0xFF6E4A32), RoundedCornerShape(10.dp))
                .background(Color(0xFFE9D8B4)),
        ) {
            val cellW = maxWidth / 9
            val cellH = maxHeight / 10
            Canvas(Modifier.fillMaxSize()) {
                for (col in 0..9) {
                    val x = size.width * col / 9f
                    drawLine(Color(0xFF6E4A32), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1.2f)
                }
                for (row in 0..10) {
                    val y = size.height * row / 10f
                    drawLine(Color(0xFF6E4A32), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1.2f)
                }
            }
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 10) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 9) {
                            val index = row * 9 + col
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val isTarget = index in targets
                            val isLast = board.lastMove?.to == index || board.lastMove?.from == index
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(1.dp)
                                    .background(
                                        when {
                                            isSelected -> Color(0x66E0A63A)
                                            isTarget -> Color(0x554FA36B)
                                            isLast -> Color(0x44E0A63A)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(50),
                                    )
                                    .clickable { tap(index) }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(JanggiGame.coord(index))
                                            if (piece == null) append(", vacía")
                                            else append(", ${JanggiGame.GLYPHS[piece.kind]} ${game.sideName(piece.side)}")
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece != null) {
                                    Text(
                                        JanggiGame.GLYPHS[piece.kind] ?: "?",
                                        color = if (piece.side == JanggiSide.Cho) Color(0xFF8B2E23) else Color(0xFF1F2933),
                                        fontSize = (cellW.value * 0.48f).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .size(cellW * 0.82f)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFFFFF3D6))
                                            .border(1.dp, Color(0xFF6E4A32), RoundedCornerShape(50))
                                            .padding(top = (cellH.value * 0.10f).dp),
                                    )
                                } else if (isTarget) {
                                    Text("·", color = Color(0xFF2E7D52), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { setupOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.newMatch"))
            }
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            Button(
                onClick = { resignOpen = true },
                enabled = board.phase == JanggiPhase.Play,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("common.resign")) }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("janggi.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("janggi.difficulty"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("easy", "normal", "hard").forEach { value ->
                            FilterChip(
                                selected = difficulty == value,
                                onClick = { difficulty = value },
                                label = { Text(value) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("janggi.rules"),
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
        pdfTitle = state.t("janggi.title"),
        pdfSubtitle = state.t("janggi.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == JanggiPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(janggiResultText(state, board, allHumans)) },
            text = { Text(state.t("janggi.resultBody", mapOf("n" to board.history.size)), style = MaterialTheme.typography.bodySmall) },
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

private fun janggiResultText(state: ShellState, board: JanggiState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == JanggiSide.Cho -> state.t("common.youWon")
    board.winner != null && allHumans -> state.t("common.playerNWon", mapOf("n" to if (board.winner == JanggiSide.Cho) 1 else 2))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
