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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.mesaviva.app.games.ThreeChessGame
import com.mesaviva.app.games.ThreePhase
import com.mesaviva.app.games.ThreePieceType
import com.mesaviva.app.games.ThreeSide
import com.mesaviva.app.games.ThreeChessState
import kotlinx.coroutines.delay

private val TC_NORTH = Color(0xFFE8C56A)
private val TC_WEST = Color(0xFF6FA8E8)
private val TC_SOUTH = Color(0xFFE87373)
private val TC_LIGHT = Color(0xFFF0E4C8)
private val TC_DARK = Color(0xFF9C8A68)
private val TC_SELECT = Color(0xAAE0A63A)

@Composable
fun ThreeChessScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    val game = remember { ThreeChessGame() }
    var allHumans by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == ThreeSide.North
    val legal = remember(board, selected, allHumans) {
        if (selected != null && humanTurn) game.legalMoves(board, selected!!) else emptyList()
    }

    fun finishIfNeeded(current: ThreeChessState) {
        if (current.phase != ThreePhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == ThreeSide.North
        if (!allHumans) state.awardMatch(win = won)
        NativeAudio.play(context, if (won) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
        resultOpen = true
    }

    fun start() {
        board = game.newMatch()
        selected = null
        awarded = false
        setupOpen = false
        resultOpen = false
    }

    fun apply(next: ThreeChessState, capture: Boolean = false) {
        board = next
        selected = null
        NativeAudio.play(context, if (capture) NativeAudio.Sfx.Capture else NativeAudio.Sfx.Card, state.sfx)
        state.vibrate(context, if (capture) 42 else 18)
        finishIfNeeded(next)
    }

    LaunchedEffect(board.turn, board.phase, allHumans) {
        if (allHumans || board.phase != ThreePhase.Play || board.turn == ThreeSide.North) return@LaunchedEffect
        delay(700)
        val move = game.botMove(board) ?: return@LaunchedEffect
        val captured = game.pieceAt(board, move.second) != null
        apply(game.move(board, move.first, move.second).state, captured)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        NativeGameHeader(
            state = state,
            title = state.t("three.title"),
            subtitle = state.t("three.subtitle"),
            badge = state.t("three.turnBadge", mapOf("name" to board.turn.label)),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == ThreePhase.MatchEnd -> state.t("three.matchOver")
                humanTurn -> state.t("three.yourTurn")
                else -> state.t("three.botTurn", mapOf("name" to board.turn.label))
            },
            alert = board.phase == ThreePhase.MatchEnd,
        )
        Spacer(Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ThreeSide.entries.forEach { side ->
                val alive = side !in board.eliminated
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sideColor(side).copy(alpha = if (alive) 0.22f else 0.08f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(5.dp)) {
                        Text(
                            side.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sideColor(side),
                        )
                        Text(
                            "${game.material(board, side)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!alive) {
                            Text("✕", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        } else if (game.isInCheck(board, side)) {
                            Text("JAQUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF5A4327), RoundedCornerShape(8.dp)),
        ) {
            val cell = maxWidth / ThreeChessGame.SIZE
            Column {
                for (r in 0 until ThreeChessGame.SIZE) {
                    Row {
                        for (c in 0 until ThreeChessGame.SIZE) {
                            val index = ThreeChessGame.cell(r, c)
                            val piece = game.pieceAt(board, index)
                            val home = ThreeSide.entries.firstOrNull { ThreeChessGame.isHome(it, index) }
                            val bg = when {
                                index in legal -> Color(0x99E0A63A)
                                selected == piece?.id -> TC_SELECT
                                home != null -> sideColor(home).copy(alpha = 0.18f)
                                (r + c) % 2 == 0 -> TC_LIGHT
                                else -> TC_DARK
                            }
                            Box(
                                modifier = Modifier
                                    .width(cell)
                                    .height(cell)
                                    .background(bg)
                                    .border(0.4.dp, Color(0x446A5436))
                                    .clickable {
                                        if (board.phase != ThreePhase.Play || !humanTurn) return@clickable
                                        if (piece != null && piece.side == board.turn) {
                                            selected = if (selected == piece.id) null else piece.id
                                            NativeAudio.play(context, NativeAudio.Sfx.Tap, state.sfx)
                                        } else if (selected != null && index in legal) {
                                            val captured = game.pieceAt(board, index) != null
                                            apply(game.move(board, selected!!, index).state, captured)
                                        }
                                    }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(game.coord(index))
                                            if (piece != null) append(", ${piece.side.label} ${piece.type.label}")
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece != null) {
                                    Text(
                                        glyph(piece.type),
                                        color = sideColor(piece.side),
                                        fontSize = if (cell.value > 34f) 20.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            state.t("three.legend"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            OutlinedButton(onClick = { resignOpen = true }, enabled = board.phase == ThreePhase.Play) {
                Text(state.t("common.resign"))
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
        Text(
            state.t("three.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.size > 1) setupOpen = false },
            title = { Text(state.t("three.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("three.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    if (allHumans) {
                        apply(game.resign(board, board.turn).state)
                    } else {
                        board = board.copy(
                            phase = ThreePhase.MatchEnd,
                            eliminated = board.eliminated + ThreeSide.North,
                            winner = null,
                        )
                        awarded = true
                        state.awardMatch(win = false)
                        NativeAudio.play(context, NativeAudio.Sfx.Lose, state.sfx)
                        resultOpen = true
                    }
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("three.title"),
            pdfSubtitle = state.t("three.subtitle"),
            stats = ThreeSide.entries.map { side ->
                side.label to "${game.material(board, side)} · ${if (side in board.eliminated) "✕" else "✓"}"
            },
        ) { historyOpen = false }
    }

    if (resultOpen && board.phase == ThreePhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = {
                Text(
                    when (board.winner) {
                        ThreeSide.North -> state.t("three.youWon")
                        null -> state.t("three.retired")
                        else -> state.t("three.wonBy", mapOf("name" to board.winner!!.label))
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.history.takeLast(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = { resultOpen = false; setupOpen = true }) { Text(state.t("common.newMatch")) }
            },
            dismissButton = { TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) } },
        )
    }
}

private fun sideColor(side: ThreeSide): Color = when (side) {
    ThreeSide.North -> TC_NORTH
    ThreeSide.West -> TC_WEST
    ThreeSide.South -> TC_SOUTH
}

private fun glyph(type: ThreePieceType): String = when (type) {
    ThreePieceType.King -> "♚"
    ThreePieceType.Queen -> "♛"
    ThreePieceType.Rook -> "♜"
    ThreePieceType.Bishop -> "♝"
    ThreePieceType.Knight -> "♞"
    ThreePieceType.Pawn -> "♟"
}
