package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.GoGame
import com.mesaviva.app.games.GoPhase
import com.mesaviva.app.games.GoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val GO_WOOD = Color(0xFFDDBB77)
private val GO_LINE = Color(0xFF3B2A16)
private val GO_BLACK = Color(0xFF1A1712)
private val GO_WHITE = Color(0xFFF7F1E1)

@Composable
fun GoScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var boardSize by remember { mutableStateOf(9) }
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(GoGame()) }
    var board by remember { mutableStateOf(game.newMatch(9)) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == 1
    val legal = remember(board, humanTurn) {
        if (board.phase == GoPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val score = remember(board) { GoGame.scoreBoard(board) }

    fun start(size: Int) {
        boardSize = if (size == 13) 13 else 9
        game = GoGame()
        board = game.newMatch(boardSize)
        setupOpen = false
        resultOpen = false
        historyOpen = false
    }

    fun playAt(index: Int) {
        val res = game.play(board, index)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        val captured = res.state.history.lastOrNull()?.contains("×") == true
        NativeAudio.play(
            context,
            if (captured) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card,
            state.sfx,
        )
        state.vibrate(context, if (captured) 45 else 20)
    }

    fun pass() {
        if (board.phase != GoPhase.Play || !humanTurn) return
        board = game.pass(board).state
        state.vibrate(context, 18)
        if (board.phase == GoPhase.MatchEnd) resultOpen = true
    }

    LaunchedEffect(board.turn, board.phase, board.moveNo, board.passes, allHumans, difficulty) {
        if (allHumans || board.phase == GoPhase.MatchEnd || board.turn != 2) return@LaunchedEffect
        delay(520)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == GoPhase.Play && board.turn == 2) {
            board = game.play(board, move).state
            if (board.phase == GoPhase.MatchEnd) resultOpen = true
        } else if (move == null && board.phase == GoPhase.Play && board.turn == 2) {
            board = game.pass(board).state
            if (board.phase == GoPhase.MatchEnd) resultOpen = true
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("go.title"),
            subtitle = state.t("go.subtitle"),
            badge = state.t("go.badge", mapOf("n" to board.size)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == GoPhase.MatchEnd -> goResultText(state, board, allHumans)
                board.passes == 1 -> state.t("go.passWarning")
                !humanTurn -> state.t("go.botTurn")
                else -> state.t("go.yourTurn")
            },
            alert = board.passes == 1,
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(GO_WOOD),
        ) {
            Canvas(
                Modifier.fillMaxSize()
                    .semantics {
                        contentDescription = "Tablero de Go ${board.size} por ${board.size}. " +
                            if (board.turn == 1) "Turno de negras." else "Turno de blancas."
                    }
                    .pointerInput(board.size, board.turn, board.phase, humanTurn) {
                    detectTapGestures { tap ->
                        if (board.phase != GoPhase.Play || !humanTurn) return@detectTapGestures
                        val margin = size.width * 0.065f
                        val step = (size.width - margin * 2f) / (board.size - 1)
                        val col = ((tap.x - margin) / step).roundToInt().coerceIn(0, board.size - 1)
                        val row = ((tap.y - margin) / step).roundToInt().coerceIn(0, board.size - 1)
                        val index = row * board.size + col
                        if (index in legal) playAt(index)
                    }
                },
            ) {
                val margin = size.width * 0.065f
                val step = (size.width - margin * 2f) / (board.size - 1)
                val max = margin + step * (board.size - 1)
                for (i in 0 until board.size) {
                    val position = margin + step * i
                    drawLine(GO_LINE, Offset(margin, position), Offset(max, position), 1.4f)
                    drawLine(GO_LINE, Offset(position, margin), Offset(position, max), 1.4f)
                }
                val stars = if (board.size == 9) {
                    listOf(2 to 2, 6 to 2, 4 to 4, 2 to 6, 6 to 6)
                } else {
                    listOf(3 to 3, 9 to 3, 6 to 6, 3 to 9, 9 to 9)
                }
                for ((col, row) in stars) {
                    drawCircle(GO_LINE, step * 0.09f, Offset(margin + col * step, margin + row * step))
                }
                for (index in board.board.indices) {
                    val value = board.board[index]
                    if (value == 0) continue
                    val col = index % board.size
                    val row = index / board.size
                    val center = Offset(margin + col * step, margin + row * step)
                    drawCircle(
                        color = if (value == 1) GO_BLACK else GO_WHITE,
                        radius = step * 0.46f,
                        center = center,
                    )
                    drawCircle(
                        color = if (value == 1) Color(0xFF0B0907) else Color(0xFF8F8068),
                        radius = step * 0.46f,
                        center = center,
                        style = Stroke(width = 1.5f),
                    )
                    if (value == 2) {
                        drawCircle(
                            color = Color(0x66FFFFFF),
                            radius = step * 0.16f,
                            center = Offset(center.x - step * 0.14f, center.y - step * 0.14f),
                        )
                    }
                }
                board.lastMove?.let { index ->
                    val col = index % board.size
                    val row = index / board.size
                    drawCircle(
                        color = Color(0xFFE0A63A),
                        radius = step * 0.10f,
                        center = Offset(margin + col * step, margin + row * step),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.t("go.captures", mapOf("b" to board.capturesBlack, "w" to board.capturesWhite)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("go.scoreLive", mapOf(
                    "b" to formatScore(score.black),
                    "w" to formatScore(score.white),
                )),
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
            OutlinedButton(onClick = { pass() }, enabled = board.phase == GoPhase.Play && humanTurn) {
                Text(state.t("go.pass"))
            }
            Button(onClick = { resignOpen = true }, enabled = board.phase == GoPhase.Play) {
                Text(state.t("common.resign"))
            }
        }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("go.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("go.chooseSize"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(9, 13).forEach { size ->
                            FilterChip(
                                selected = boardSize == size,
                                onClick = { boardSize = size },
                                label = { Text("$size×$size") },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    if (!allHumans) {
                        Text(state.t("common.difficulty"), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("easy", "normal", "hard").forEach { value ->
                                FilterChip(
                                    selected = difficulty == value,
                                    onClick = { difficulty = value },
                                    label = { Text(state.t("common.$value")) },
                                )
                            }
                        }
                    }
                    Text(
                        state.t("go.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start(boardSize) }) { Text(state.t("common.start")) } },
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
        pdfTitle = state.t("go.title"),
        pdfSubtitle = state.t("go.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == GoPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(goResultText(state, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("go.finalScore", mapOf(
                        "b" to formatScore(board.blackScore),
                        "w" to formatScore(board.whiteScore),
                    )))
                    Text(state.t("go.captures", mapOf("b" to board.capturesBlack, "w" to board.capturesWhite)))
                    Text(state.t("go.komi", mapOf("n" to GoGame.KOMI)))
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

private fun formatScore(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

private fun goResultText(state: ShellState, board: GoState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 1 -> state.t("common.youWon")
    board.winner == 2 && allHumans -> state.t("common.player2Won")
    board.winner == 2 -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
