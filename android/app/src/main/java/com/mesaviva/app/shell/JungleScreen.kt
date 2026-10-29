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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.JungleGame
import com.mesaviva.app.games.JunglePhase
import com.mesaviva.app.games.JungleSide
import com.mesaviva.app.games.JungleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val JUNGLE_GREEN = Color(0xFF527A4A)
private val JUNGLE_ALT = Color(0xFF5F8A55)
private val JUNGLE_WATER = Color(0xFF3E7FA3)
private val JUNGLE_TRAP = Color(0xFFB9853C)
private val JUNGLE_RED_DEN = Color(0xFF8F3B32)
private val JUNGLE_BLACK_DEN = Color(0xFF352B3A)

@Composable
fun JungleScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(JungleGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == JungleSide.Red
    val legal = remember(board, humanTurn) {
        if (board.phase == JunglePhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }

    fun start() {
        game = JungleGame()
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
                res.state.phase == JunglePhase.MatchEnd && res.state.winner == JungleSide.Red -> NativeAudio.Sfx.Win
                res.state.phase == JunglePhase.MatchEnd -> NativeAudio.Sfx.Lose
                captured -> NativeAudio.Sfx.Capture
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        state.vibrate(context, if (captured) 42 else 22)
        if (res.state.phase == JunglePhase.MatchEnd) resultOpen = true
    }

    fun onCell(index: Int) {
        if (board.phase != JunglePhase.Play || !humanTurn) return
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
        if (allHumans || board.phase == JunglePhase.MatchEnd || board.turn != JungleSide.Black) return@LaunchedEffect
        delay(480)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == JunglePhase.Play && board.turn == JungleSide.Black) {
            val res = game.move(board, move.from, move.to)
            if (res.error == null) {
                board = res.state
                if (res.state.phase == JunglePhase.MatchEnd) resultOpen = true
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("jungle.title"),
            subtitle = state.t("jungle.subtitle"),
            badge = state.t("jungle.badge", mapOf(
                "r" to board.board.count { it?.side == JungleSide.Red },
                "b" to board.board.count { it?.side == JungleSide.Black },
            )),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == JunglePhase.MatchEnd -> jungleResultText(state, board, allHumans)
                !humanTurn -> state.t("jungle.botTurn")
                else -> state.t("jungle.yourTurn")
            },
            alert = board.phase == JunglePhase.MatchEnd,
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(7f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            val cellW = maxWidth / JungleGame.COLS
            val cellH = maxHeight / JungleGame.ROWS
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until JungleGame.ROWS) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until JungleGame.COLS) {
                            val index = row * JungleGame.COLS + col
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val target = selectedMoves.firstOrNull { it.to == index }
                            val isLast = board.lastMove?.let { it.from == index || it.to == index } == true
                            val water = JungleGame.isWater(index)
                            val trap = JungleGame.isTrap(index)
                            val den = when (index) {
                                JungleGame.RED_DEN -> JUNGLE_RED_DEN
                                JungleGame.BLACK_DEN -> JUNGLE_BLACK_DEN
                                else -> null
                            }
                            val base = when {
                                den != null -> den
                                water -> JUNGLE_WATER
                                trap -> JUNGLE_TRAP
                                (row + col) % 2 == 0 -> JUNGLE_GREEN
                                else -> JUNGLE_ALT
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            isLast -> Color(0x55E0A63A)
                                            else -> base
                                        },
                                    )
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.14f))
                                    .clickable { onCell(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (water && piece == null) {
                                    Text("≈", color = Color(0x66FFFFFF), fontSize = 15.sp)
                                }
                                if (den != null && piece == null) {
                                    Text("★", color = Color(0x88FFFFFF), fontSize = 15.sp)
                                }
                                if (trap && piece == null) {
                                    Text("▲", color = Color(0x66FFFFFF), fontSize = 9.sp)
                                }
                                if (piece != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(cellW * 0.06f)
                                            .background(
                                                if (piece.side == JungleSide.Red) Color(0x55F2C94C) else Color(0x553D2A4A),
                                                CircleShape,
                                            )
                                            .border(
                                                1.5.dp,
                                                if (piece.side == JungleSide.Red) Color(0xFFE8B04A) else Color(0xFF221A27),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            JungleGame.ANIMALS.getOrElse(piece.rank) { "" },
                                            fontSize = (cellH.value * 0.42f).sp,
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
                                                .size(cellW * 0.20f)
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
                state.t("jungle.captures", mapOf("r" to board.capturesRed, "b" to board.capturesBlack)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("jungle.turnSide", mapOf(
                    "s" to if (board.turn == JungleSide.Red) state.t("jungle.red") else state.t("jungle.black"),
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
            Button(onClick = { resignOpen = true }, enabled = board.phase == JunglePhase.Play) {
                Text(state.t("common.resign"))
            }
        }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("jungle.setup")) },
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
                        state.t("jungle.rules"),
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
        pdfTitle = state.t("jungle.title"),
        pdfSubtitle = state.t("jungle.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == JunglePhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(jungleResultText(state, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("jungle.captures", mapOf("r" to board.capturesRed, "b" to board.capturesBlack)))
                    Text(state.t("jungle.piecesLeft", mapOf(
                        "r" to board.board.count { it?.side == JungleSide.Red },
                        "b" to board.board.count { it?.side == JungleSide.Black },
                    )))
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

private fun jungleResultText(state: ShellState, board: JungleState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == JungleSide.Red -> state.t("common.youWon")
    board.winner == JungleSide.Black && allHumans -> state.t("common.player2Won")
    board.winner == JungleSide.Black -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
