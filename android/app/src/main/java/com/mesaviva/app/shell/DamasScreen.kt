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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.DamasColor
import com.mesaviva.app.games.DamasGame
import com.mesaviva.app.games.DamasMove
import com.mesaviva.app.games.DamasPhase
import com.mesaviva.app.games.DamasState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val DAMAS_LIGHT = Color(0xFFE9D8B4)
private val DAMAS_DARK = Color(0xFF6E4A32)
private val DAMAS_WHITE = Color(0xFFFFF6E2)
private val DAMAS_BLACK = Color(0xFF241A13)

@Composable
fun DamasScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(DamasGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var replayOpen by remember { mutableStateOf(false) }
    var seed by remember { mutableStateOf(System.nanoTime()) }
    var moves by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == DamasColor.White
    val legal = remember(board, humanTurn) {
        if (board.phase == DamasPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }

    fun finishIfNeeded(current: DamasState) {
        if (current.phase != DamasPhase.MatchEnd || awarded) return
        awarded = true
        val win = current.winner == DamasColor.White
        state.awardMatch(win = win, draw = current.draw)
        NativeAudio.play(
            context,
            when {
                current.draw -> NativeAudio.Sfx.Draw
                win -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
    }

    fun start() {
        seed = System.nanoTime()
        game = DamasGame(seed)
        board = game.newMatch()
        selected = null
        moves = emptyList()
        awarded = false
        setupOpen = false
        resultOpen = false
        historyOpen = false
        replayOpen = false
    }

    fun applyMove(from: Int, to: Int) {
        val res = game.move(board, from, to)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        moves = moves + (from to to)
        selected = res.state.mustContinueFrom
        state.vibrate(context, if (res.state.lastMove?.captured != null) 42 else 22)
        val move = res.state.lastMove
        NativeAudio.play(
            context,
            when {
                move?.promotion == true -> NativeAudio.Sfx.Promote
                move?.captured != null -> NativeAudio.Sfx.Capture
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        finishIfNeeded(res.state)
        if (res.state.phase == DamasPhase.MatchEnd) resultOpen = true
    }

    fun onCell(index: Int) {
        if (board.phase != DamasPhase.Play || !humanTurn) return
        if (selected != null) {
            val move = selectedMoves.firstOrNull { it.to == index }
            if (move != null) {
                applyMove(move.from, move.to)
                return
            }
        }
        val piece = board.board.getOrNull(index)
        if (piece?.color == board.turn) {
            selected = if (selected == index) null else index
            state.vibrate(context, 12)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, board.mustContinueFrom, allHumans, difficulty) {
        if (allHumans || board.phase == DamasPhase.MatchEnd || board.turn != DamasColor.Black) return@LaunchedEffect
        delay(480)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == DamasPhase.Play && board.turn == DamasColor.Black) {
            val res = game.move(board, move.from, move.to)
            if (res.error == null) {
                board = res.state
                moves = moves + (move.from to move.to)
                selected = null
                val done = res.state.lastMove
                NativeAudio.play(
                    context,
                    when {
                        done?.promotion == true -> NativeAudio.Sfx.Promote
                        done?.captured != null -> NativeAudio.Sfx.Capture
                        else -> NativeAudio.Sfx.Card
                    },
                    state.sfx,
                )
                finishIfNeeded(res.state)
                if (res.state.phase == DamasPhase.MatchEnd) resultOpen = true
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("damas.title"),
            subtitle = state.t("damas.subtitle"),
            badge = if (board.phase == DamasPhase.Play) {
                state.t("damas.scoreBadge", mapOf(
                    "w" to game.piecesOf(board, DamasColor.White),
                    "b" to game.piecesOf(board, DamasColor.Black),
                ))
            } else {
                ""
            },
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == DamasPhase.MatchEnd -> damasResultText(state, game, board, allHumans)
                board.mustContinueFrom != null -> state.t("damas.chain")
                !humanTurn -> state.t("damas.botTurn")
                else -> state.t("damas.yourTurn")
            },
            alert = board.mustContinueFrom != null,
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            val cell = maxWidth / 8
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 8) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 8) {
                            val index = row * 8 + col
                            val dark = DamasGame.isDark(index)
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val isTarget = selectedMoves.any { it.to == index }
                            val isCapture = piece != null && isTarget
                            val isLast = board.lastMove?.let { it.from == index || it.to == index } == true
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            isLast -> Color(0x55E0A63A)
                                            dark -> DAMAS_DARK
                                            else -> DAMAS_LIGHT
                                        },
                                    )
                                    .clickable { onCell(index) }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(DamasGame.square(index))
                                            if (piece != null) {
                                                append(", ")
                                                append(if (piece.color == DamasColor.White) state.t("damas.white") else state.t("damas.black"))
                                                if (piece.king) append(" ♛")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece != null) {
                                    val white = piece.color == DamasColor.White
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(cell * 0.10f)
                                            .background(if (white) DAMAS_WHITE else DAMAS_BLACK, CircleShape)
                                            .border(
                                                (cell.value * 0.025f).dp,
                                                if (white) Color(0xFF8A6A45) else Color(0xFF0B0806),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            if (piece.king) "♛" else "",
                                            fontSize = (cell.value * 0.38f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (white) Color(0xFF3A2817) else Color(0xFFE9C46A),
                                        )
                                    }
                                }
                                if (isTarget) {
                                    if (isCapture) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(cell * 0.04f)
                                                .border(3.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.85f), CircleShape),
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(cell * 0.22f)
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
                "● ${game.piecesOf(board, DamasColor.White)}  ·  ○ ${game.piecesOf(board, DamasColor.Black)}  ·  ⚔ ${board.captures[DamasColor.White] ?: 0}/${board.captures[DamasColor.Black] ?: 0}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("damas.kings", mapOf(
                    "w" to game.kingsOf(board, DamasColor.White),
                    "b" to game.kingsOf(board, DamasColor.Black),
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
            Button(
                onClick = { resignOpen = true },
                enabled = board.phase == DamasPhase.Play,
            ) { Text(state.t("common.resign")) }
        }
    }

    if (setupOpen) {
        DamasSetupDialog(
            state = state,
            allHumans = allHumans,
            difficulty = difficulty,
            onAllHumans = { allHumans = it },
            onDifficulty = { difficulty = it },
            onDismiss = { if (board.history.isNotEmpty()) setupOpen = false },
            onStart = { start() },
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
            dismissButton = {
                TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) }
            },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("damas.title"),
            pdfSubtitle = state.t("damas.subtitle"),
            stats = listOf(
                state.t("damas.white") to game.piecesOf(board, DamasColor.White).toString(),
                state.t("damas.black") to game.piecesOf(board, DamasColor.Black).toString(),
                state.t("damas.capturesStat") to "${board.captures[DamasColor.White] ?: 0} / ${board.captures[DamasColor.Black] ?: 0}",
                state.t("damas.kingsStat") to "${game.kingsOf(board, DamasColor.White)} / ${game.kingsOf(board, DamasColor.Black)}",
            ),
            onReplay = if (moves.isNotEmpty()) {
                { replayOpen = true }
            } else {
                null
            },
        ) { historyOpen = false }
    }

    if (replayOpen) {
        DamasReplayDialog(state, seed, moves) { replayOpen = false }
    }

    if (resultOpen && board.phase == DamasPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(damasResultText(state, game, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("damas.whitePieces", mapOf("n" to game.piecesOf(board, DamasColor.White))))
                    Text(state.t("damas.blackPieces", mapOf("n" to game.piecesOf(board, DamasColor.Black))))
                    Text(state.t("damas.captures", mapOf(
                        "w" to (board.captures[DamasColor.White] ?: 0),
                        "b" to (board.captures[DamasColor.Black] ?: 0),
                    )))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    resultOpen = false
                    setupOpen = true
                }) { Text(state.t("common.newMatch")) }
            },
            dismissButton = {
                TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }
}

@Composable
private fun DamasSetupDialog(
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
        title = { Text(state.t("damas.setup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("damas.setupDesc"), style = MaterialTheme.typography.bodySmall)
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
                Text(
                    state.t("damas.rules"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { Button(onClick = onStart) { Text(state.t("common.start")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("common.close")) } },
    )
}

@Composable
private fun DamasReplayDialog(
    state: ShellState,
    seed: Long,
    moves: List<Pair<Int, Int>>,
    onClose: () -> Unit,
) {
    val frames = remember(seed, moves) {
        val replay = DamasGame(seed)
        var current = replay.newMatch()
        val list = mutableListOf(current)
        for ((from, to) in moves) {
            val result = replay.move(current, from, to)
            if (result.error != null) break
            current = result.state
            list.add(current)
        }
        list
    }
    var step by remember(frames) { mutableStateOf(frames.lastIndex.coerceAtLeast(0)) }
    val board = frames.getOrNull(step) ?: frames.last()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(state.t("replay.title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    state.t("replay.step", mapOf("n" to step, "total" to (frames.size - 1))),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    "${state.t("replay.seed")}: ${seed.toString().takeLast(6)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                ) {
                    val cell = maxWidth / 8
                    Column(Modifier.fillMaxSize()) {
                        for (row in 0 until 8) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until 8) {
                                    val index = row * 8 + col
                                    val piece = board.board[index]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (DamasGame.isDark(index)) DAMAS_DARK else DAMAS_LIGHT),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (piece != null) {
                                            val white = piece.color == DamasColor.White
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(cell * 0.10f)
                                                    .background(if (white) DAMAS_WHITE else DAMAS_BLACK, CircleShape)
                                                    .border(1.dp, if (white) Color(0xFF8A6A45) else Color(0xFF0B0806), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    if (piece.king) "♛" else "",
                                                    fontSize = (cell.value * 0.38f).sp,
                                                    color = if (white) Color(0xFF3A2817) else Color(0xFFE9C46A),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (step > 0) step -= 1 },
                        enabled = step > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text(state.t("replay.prev")) }
                    OutlinedButton(
                        onClick = { if (step < frames.lastIndex) step += 1 },
                        enabled = step < frames.lastIndex,
                        modifier = Modifier.weight(1f),
                    ) { Text(state.t("replay.next")) }
                }
                Text(
                    state.t("replay.note"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text(state.t("common.close")) }
        },
    )
}

private fun damasResultText(state: ShellState, game: DamasGame, board: DamasState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == DamasColor.White -> state.t("common.youWon")
    board.winner == DamasColor.Black && allHumans -> state.t("common.player2Won")
    board.winner == DamasColor.Black -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
