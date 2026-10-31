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
import androidx.compose.foundation.shape.CircleShape
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
import com.mesaviva.app.games.LuzhanqiGame
import com.mesaviva.app.games.LuzhanqiPhase
import com.mesaviva.app.games.LuzhanqiRank
import com.mesaviva.app.games.LuzhanqiSide
import com.mesaviva.app.games.LuzhanqiState
import kotlinx.coroutines.delay

private val LZ_RED = Color(0xFFB53737)
private val LZ_BLUE = Color(0xFF2B5FA8)
private val LZ_BOARD = Color(0xFFE8D9B5)
private val LZ_RAIL = Color(0xFFD7C49A)
private val LZ_CAMP = Color(0xFFB9D4B1)
private val LZ_HQ = Color(0xFFE7C56E)

@Composable
fun LuzhanqiScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    val game = remember { LuzhanqiGame() }
    var allHumans by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == LuzhanqiSide.Red
    val legal = remember(board, selected) {
        if (selected != null && humanTurn) game.moves(board, selected!!) else emptyList()
    }

    fun finishIfNeeded(current: LuzhanqiState) {
        if (current.phase != LuzhanqiPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == LuzhanqiSide.Red
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
        NativeAudio.play(context, NativeAudio.Sfx.Draw, state.sfx)
    }

    fun move(pieceId: Int, target: Int) {
        val result = game.move(board, pieceId, target)
        if (result.error != null) {
            Toast.makeText(context, state.t(result.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = result.state
        selected = null
        when {
            result.state.lastMove?.flagCaptured == true -> NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
            result.state.lastMove?.battle != null -> NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
            else -> NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        }
        state.vibrate(context, if (result.state.lastMove?.battle != null) 45 else 18)
        finishIfNeeded(result.state)
    }

    LaunchedEffect(board.turn, board.phase, allHumans) {
        if (allHumans || board.phase != LuzhanqiPhase.Play || board.turn != LuzhanqiSide.Blue) return@LaunchedEffect
        delay(700)
        val bot = game.botMove(board) ?: return@LaunchedEffect
        move(bot.first, bot.second)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp),
    ) {
        NativeGameHeader(
            state = state,
            title = state.t("luzhanqi.title"),
            subtitle = state.t("luzhanqi.subtitle"),
            badge = "${game.piecesOf(board, LuzhanqiSide.Red).size} · ${game.piecesOf(board, LuzhanqiSide.Blue).size}",
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == LuzhanqiPhase.MatchEnd -> state.t("luzhanqi.matchOver")
                humanTurn -> state.t("luzhanqi.yourTurn", mapOf("side" to if (board.turn == LuzhanqiSide.Red) state.t("luzhanqi.red") else state.t("luzhanqi.blue")))
                else -> state.t("luzhanqi.botTurn")
            },
            alert = board.phase == LuzhanqiPhase.MatchEnd,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            state.t("luzhanqi.legend"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 13.2f)
                .clip(RoundedCornerShape(10.dp))
                .background(LZ_BOARD)
                .border(2.dp, Color(0xFF5A4327), RoundedCornerShape(10.dp))
                .padding(3.dp),
        ) {
            val cellW = maxWidth / LuzhanqiGame.COLS
            Column(Modifier.fillMaxWidth()) {
                for (r in 0 until LuzhanqiGame.ROWS) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until LuzhanqiGame.COLS) {
                            val index = LuzhanqiGame.cell(r, c)
                            val piece = game.pieceAt(board, index)
                            val camp = LuzhanqiGame.isCamp(index)
                            val rail = LuzhanqiGame.isRailway(index)
                            val hq = LuzhanqiGame.isHeadquarters(index)
                            val isLegal = index in legal
                            val selectedHere = selected == piece?.id
                            val visibleRank = piece != null && (piece.side == LuzhanqiSide.Red || piece.revealed || allHumans)
                            val bg = when {
                                isLegal -> Color(0x88E0A63A)
                                selectedHere -> Color(0x99E0A63A)
                                camp -> LZ_CAMP
                                rail -> LZ_RAIL
                                else -> LZ_BOARD
                            }
                            Box(
                                modifier = Modifier
                                    .width(cellW)
                                    .height(cellW * 1.1f)
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(if (camp) 50 else 5))
                                    .background(bg)
                                    .border(
                                        width = if (hq) 2.dp else 0.6.dp,
                                        color = if (hq) Color(0xFFB8862B) else Color(0xFF9C855E),
                                        shape = RoundedCornerShape(if (camp) 50 else 5),
                                    )
                                    .clickable {
                                        if (board.phase != LuzhanqiPhase.Play || !humanTurn) return@clickable
                                        if (piece != null && piece.side == board.turn) {
                                            selected = if (selected == piece.id) null else piece.id
                                            NativeAudio.play(context, NativeAudio.Sfx.Tap, state.sfx)
                                        } else if (selected != null && isLegal) {
                                            move(selected!!, index)
                                        }
                                    }
                                    .semantics {
                                        contentDescription = buildString {
                                            append("${'A' + c}${LuzhanqiGame.ROWS - r}")
                                            if (camp) append(", campamento")
                                            if (hq) append(", cuartel")
                                            if (piece != null) {
                                                append(", ")
                                                append(
                                                    when {
                                                        piece.side == LuzhanqiSide.Red -> "rojo"
                                                        visibleRank -> "azul"
                                                        else -> "azul oculto"
                                                    },
                                                )
                                                if (visibleRank) append(" ${piece.rank.label}")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece != null) {
                                    if (!visibleRank) {
                                        Text("?", color = Color(0xFFF0E6CF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(
                                            piece.rank.label,
                                            color = if (piece.side == LuzhanqiSide.Red) LZ_RED else LZ_BLUE,
                                            fontSize = if (piece.rank.label.length > 2) 8.sp else 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (camp && piece == null) {
                                    Text("✦", color = Color(0xFF5E7D55), fontSize = 7.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "${state.t("luzhanqi.red")}: ${game.piecesOf(board, LuzhanqiSide.Red).size} · " +
                "${state.t("luzhanqi.blue")}: ${game.piecesOf(board, LuzhanqiSide.Blue).size} · " +
                "${state.t("luzhanqi.flag")}: ${game.flagOf(board, LuzhanqiSide.Red)?.let { LuzhanqiGame.label(it.cell) } ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            OutlinedButton(onClick = { resignOpen = true }, enabled = board.phase == LuzhanqiPhase.Play) {
                Text(state.t("common.resign"))
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
        Text(
            state.t("luzhanqi.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.size > 1) setupOpen = false },
            title = { Text(state.t("luzhanqi.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("luzhanqi.rules"),
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
                    board = game.resign(board).state
                    finishIfNeeded(board)
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("luzhanqi.title"),
            pdfSubtitle = state.t("luzhanqi.subtitle"),
            stats = listOf(
                state.t("luzhanqi.red") to game.piecesOf(board, LuzhanqiSide.Red).size.toString(),
                state.t("luzhanqi.blue") to game.piecesOf(board, LuzhanqiSide.Blue).size.toString(),
                state.t("luzhanqi.battles") to board.history.count { it.contains("vence") || it.contains("bomba") || it.contains("mina") }.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen && board.phase == LuzhanqiPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = {
                Text(
                    when {
                        board.winner == LuzhanqiSide.Red -> state.t("luzhanqi.redWon")
                        board.winner == LuzhanqiSide.Blue -> state.t("luzhanqi.blueWon")
                        else -> state.t("common.draw")
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
