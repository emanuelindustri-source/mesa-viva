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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.XiangqiGame
import com.mesaviva.app.games.XqPhase
import com.mesaviva.app.games.XqSide
import com.mesaviva.app.games.XqState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val XQ_WOOD = Color(0xFFE3C48D)
private val XQ_LINE = Color(0xFF5A3A1A)
private val XQ_RED = Color(0xFFF4E7D3)
private val XQ_BLACK = Color(0xFF2B2118)

@Composable
fun XiangqiScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(XiangqiGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == XqSide.Red
    val legal = remember(board, humanTurn) {
        if (board.phase == XqPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }
    val checked = remember(board, game) {
        board.phase == XqPhase.Play && game.inCheck(board.board, board.turn)
    }

    fun start() {
        game = XiangqiGame()
        board = game.newMatch()
        selected = null
        setupOpen = false
        resultOpen = false
        historyOpen = false
    }

    fun applyMove(from: Int, to: Int) {
        val res = game.move(board, from, to)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        selected = null
        val captured = res.state.history.lastOrNull()?.contains("x") == true
        NativeAudio.play(
            context,
            when {
                res.state.phase == XqPhase.MatchEnd && res.state.winner == XqSide.Red -> NativeAudio.Sfx.Win
                res.state.phase == XqPhase.MatchEnd -> NativeAudio.Sfx.Lose
                captured -> NativeAudio.Sfx.Capture
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        state.vibrate(context, if (captured) 42 else 22)
        if (res.state.phase == XqPhase.MatchEnd) resultOpen = true
    }

    fun onCell(index: Int) {
        if (board.phase != XqPhase.Play || !humanTurn) return
        if (selected != null) {
            val move = selectedMoves.firstOrNull { it.to == index }
            if (move != null) {
                applyMove(move.from, move.to)
                return
            }
        }
        val piece = board.board.getOrNull(index)
        if (piece?.side == board.turn) {
            selected = if (selected == index) null else index
            state.vibrate(context, 12)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, allHumans, difficulty) {
        if (allHumans || board.phase == XqPhase.MatchEnd || board.turn != XqSide.Black) return@LaunchedEffect
        delay(480)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == XqPhase.Play && board.turn == XqSide.Black) {
            val res = game.move(board, move.from, move.to)
            if (res.error == null) {
                board = res.state
                if (res.state.phase == XqPhase.MatchEnd) resultOpen = true
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("xiangqi.title"),
            subtitle = state.t("xiangqi.subtitle"),
            badge = state.t("xiangqi.badge", mapOf(
                "r" to board.board.count { it?.side == XqSide.Red },
                "b" to board.board.count { it?.side == XqSide.Black },
            )),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == XqPhase.MatchEnd -> xqResultText(state, board, allHumans)
                checked -> state.t("xiangqi.check")
                !humanTurn -> state.t("xiangqi.botTurn")
                else -> state.t("xiangqi.yourTurn")
            },
            alert = checked,
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(XQ_WOOD),
        ) {
            val cell = maxWidth / 9
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 10) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 9) {
                            val index = row * 9 + col
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val target = selectedMoves.firstOrNull { it.to == index }
                            val isLast = board.lastMove?.let { it.from == index || it.to == index } == true
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            isLast -> Color(0x55E0A63A)
                                            row == 4 -> XQ_WOOD.copy(alpha = 0.82f)
                                            else -> XQ_WOOD
                                        },
                                    )
                                    .border(0.4.dp, XQ_LINE.copy(alpha = 0.25f))
                                    .clickable { onCell(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (row == 4 && col == 4) {
                                    Text(
                                        "楚河 · 漢界",
                                        fontSize = 7.sp,
                                        color = XQ_LINE.copy(alpha = 0.55f),
                                    )
                                }
                                if (piece != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(cell * 0.08f)
                                            .background(if (piece.side == XqSide.Red) XQ_RED else XQ_BLACK, CircleShape)
                                            .border(
                                                (cell.value * 0.028f).dp,
                                                if (piece.side == XqSide.Red) Color(0xFF9C2E25) else Color(0xFF15100D),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            XiangqiGame.GLYPHS[piece.side]?.get(piece.kind) ?: "",
                                            fontSize = (cell.value * 0.48f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (piece.side == XqSide.Red) Color(0xFF9C2E25) else Color(0xFFF0D9B5),
                                        )
                                    }
                                }
                                if (target != null) {
                                    if (piece != null) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .border(3.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.85f), CircleShape),
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(cell * 0.18f)
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
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.t("xiangqi.captures", mapOf(
                    "r" to board.board.count { it?.side == XqSide.Red },
                    "b" to board.board.count { it?.side == XqSide.Black },
                )),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("xiangqi.turnSide", mapOf(
                    "s" to if (board.turn == XqSide.Red) state.t("xiangqi.red") else state.t("xiangqi.black"),
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
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == XqPhase.Play) {
                Text(state.t("common.resign"))
            }
        }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("xiangqi.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        state.t("xiangqi.rules"),
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
        pdfTitle = state.t("xiangqi.title"),
        pdfSubtitle = state.t("xiangqi.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == XqPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(xqResultText(state, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("xiangqi.captures", mapOf(
                        "r" to board.board.count { it?.side == XqSide.Red },
                        "b" to board.board.count { it?.side == XqSide.Black },
                    )))
                    Text(state.t("xiangqi.resultReason", mapOf("r" to xqReason(state, board.reason))))
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

private fun xqReason(state: ShellState, key: String?): String = when (key) {
    "xiangqi.reason.checkmate" -> state.t("xiangqi.reason.checkmate")
    "xiangqi.reason.noMoves" -> state.t("xiangqi.reason.noMoves")
    "xiangqi.reason.resign" -> state.t("xiangqi.reason.resign")
    "xiangqi.reason.general" -> state.t("xiangqi.reason.general")
    else -> state.t("common.matchOver")
}

private fun xqResultText(state: ShellState, board: XqState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == XqSide.Red -> state.t("common.youWon")
    board.winner == XqSide.Black && allHumans -> state.t("common.player2Won")
    board.winner == XqSide.Black -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
