package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.BackgammonGame
import com.mesaviva.app.games.BgMove
import com.mesaviva.app.games.BgPhase
import com.mesaviva.app.games.BgSide
import com.mesaviva.app.games.BgState
import kotlinx.coroutines.delay

private val BG_WOOD = Color(0xFF6E4A2C)
private val BG_LIGHT = Color(0xFFE8D3AE)
private val BG_DARK = Color(0xFF3A2617)
private val BG_WHITE = Color(0xFFF6EEDC)
private val BG_BLACK = Color(0xFF1C1510)

@Composable
fun BackgammonScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(BackgammonGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var winnerOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }

    fun addLog(text: String) {
        log = (log + text).takeLast(200)
    }

    fun sideName(side: BgSide): String = when {
        side == BgSide.White -> state.t("bg.white")
        allHumans -> state.t("bg.blackPlayer")
        else -> state.t("bg.bot")
    }

    fun start(humans: Boolean) {
        allHumans = humans
        game = BackgammonGame()
        board = game.newMatch()
        selected = null
        log = emptyList()
        setupOpen = false
        winnerOpen = false
    }

    fun doRoll() {
        val actor = board.turn
        val res = game.roll(board)
        board = res.state
        state.vibrate(context, 24)
        NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
        addLog(
            state.t(
                "bg.log.roll",
                mapOf(
                    "p" to sideName(actor),
                    "a" to (res.state.lastDice?.first ?: 0),
                    "b" to (res.state.lastDice?.second ?: 0),
                ),
            ),
        )
        if (res.error == "bg.error.noMoves") {
            Toast.makeText(context, state.t("bg.noMoves"), Toast.LENGTH_SHORT).show()
        }
    }

    fun doMove(from: Int, to: Int) {
        val actor = board.turn
        val res = game.move(board, from, to)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        selected = null
        state.vibrate(context, 30)
        addLog(
            state.t(
                "bg.log.move",
                mapOf(
                    "p" to sideName(actor),
                    "from" to if (from == -1) state.t("bg.bar") else (from + 1).toString(),
                    "to" to if (to == 24) state.t("bg.off") else (to + 1).toString(),
                ),
            ),
        )
        if (res.state.phase == BgPhase.MatchEnd) winnerOpen = true
    }

    fun onPoint(point: Int) {
        val index = point - 1
        val legal = game.legalMoves(board)
        if (selected != null) {
            val move = legal.firstOrNull { it.from == selected && it.to == index }
            if (move != null) {
                doMove(selected!!, index)
                return
            }
        }
        if (legal.any { it.from == index }) {
            selected = index
            state.vibrate(context, 14)
        } else {
            selected = null
        }
    }

    fun onBar() {
        val legal = game.legalMoves(board)
        if (legal.any { it.from == -1 }) {
            selected = -1
            state.vibrate(context, 14)
        }
    }

    LaunchedEffect(board.turn, board.phase, board.movesLeft, board.points, board.lastMove) {
        val bot = !allHumans && board.turn == BgSide.Black
        if (board.phase == BgPhase.MatchEnd || !bot) return@LaunchedEffect
        delay(600)
        if (board.phase == BgPhase.Roll) {
            doRoll()
        } else if (board.phase == BgPhase.Move) {
            val move = game.botMove(board)
            if (move == null) {
                board = game.pass(board).state
            } else {
                doMove(move.from, move.to)
            }
        }
    }

    val legal = game.legalMoves(board)
    val sources = legal.map { it.from }.toSet()
    val destinations = legal.map { it.to }.toSet()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("bg.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("bg.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "⚪ ${BackgammonGame.pipCount(board, BgSide.White)} · ⚫ ${BackgammonGame.pipCount(board, BgSide.Black)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${board.offWhite}/15 · ${board.offBlack}/15",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        val current = sideName(board.turn)
        val status = when {
            board.phase == BgPhase.MatchEnd -> state.t("bg.finished")
            board.phase == BgPhase.Roll -> state.t("bg.rollTurn", mapOf("p" to current))
            legal.isEmpty() -> state.t("bg.noMoves")
            selected == null -> state.t("bg.pickSource", mapOf("n" to board.movesLeft.joinToString(" · ")))
            else -> state.t("bg.pickDestination")
        }
        Text(
            status,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = BG_WOOD),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(Modifier.fillMaxSize().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (point in listOf(13, 14, 15, 16, 17, 18)) {
                            PointColumn(
                                state = state,
                                board = board,
                                point = point,
                                top = true,
                                selected = selected == point - 1,
                                source = point - 1 in sources,
                                target = point - 1 in destinations,
                                onClick = { onPoint(point) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (point in listOf(12, 11, 10, 9, 8, 7)) {
                            PointColumn(
                                state = state,
                                board = board,
                                point = point,
                                top = false,
                                selected = selected == point - 1,
                                source = point - 1 in sources,
                                target = point - 1 in destinations,
                                onClick = { onPoint(point) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
                Column(
                    Modifier.width(38.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.t("bg.bar"), style = MaterialTheme.typography.labelSmall, color = BG_LIGHT)
                    BarStack(board.barBlack, BG_BLACK, selected == -1, sources.contains(-1)) { onBar() }
                    Spacer(Modifier.height(8.dp))
                    BarStack(board.barWhite, BG_WHITE, selected == -1, sources.contains(-1)) { onBar() }
                    if (selected != null && destinations.contains(24)) {
                        OutlinedButton(
                            onClick = { doMove(selected!!, 24) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text(state.t("bg.offShort"), fontSize = 9.sp) }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (point in listOf(19, 20, 21, 22, 23, 24)) {
                            PointColumn(
                                state = state,
                                board = board,
                                point = point,
                                top = true,
                                selected = selected == point - 1,
                                source = point - 1 in sources,
                                target = point - 1 in destinations,
                                onClick = { onPoint(point) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (point in listOf(6, 5, 4, 3, 2, 1)) {
                            PointColumn(
                                state = state,
                                board = board,
                                point = point,
                                top = false,
                                selected = selected == point - 1,
                                source = point - 1 in sources,
                                target = point - 1 in destinations,
                                onClick = { onPoint(point) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shownDice = board.dice ?: board.lastDice
            Text(
                if (shownDice != null) "🎲 ${shownDice.first} · ${shownDice.second}" else "🎲 —",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                state.t("bg.movesLeft", mapOf("n" to (board.movesLeft.joinToString(" · ").ifBlank { "—" }))),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { doRoll() },
                enabled = board.phase == BgPhase.Roll && (board.turn == BgSide.White || allHumans),
            ) { Text(state.t("bg.roll")) }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("bg.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
        }

        if (log.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    state.t("bg.lastMoves"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                log.takeLast(5).forEach { line ->
                    Text(line, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            log,
            pdfTitle = state.t("bg.title"),
            pdfSubtitle = state.t("bg.subtitle"),
        ) { historyOpen = false }
    }

    if (setupOpen) {
        var humans by remember { mutableStateOf(allHumans) }
        AlertDialog(
            onDismissRequest = { if (board.points.any { it != 0 }) setupOpen = false },
            title = { Text(state.t("bg.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("bg.setupDesc"), style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("bg.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = humans, onCheckedChange = { humans = it })
                    }
                    Text(state.t("bg.rules"), style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = { Button(onClick = { start(humans) }) { Text(state.t("bg.start")) } },
            dismissButton = { TextButton(onClick = { setupOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (winnerOpen && board.phase == BgPhase.MatchEnd) {
        val winner = board.winner
        AlertDialog(
            onDismissRequest = { winnerOpen = false },
            title = {
                Text(
                    when (winner) {
                        BgSide.White -> state.t("bg.whiteWon")
                        BgSide.Black -> if (allHumans) state.t("bg.blackWon") else state.t("bg.botWon")
                        null -> state.t("bg.draw")
                    },
                )
            },
            text = {
                Text(
                    "${state.t("bg.white")}: ${board.offWhite}/15 · ${state.t("bg.black")}: ${board.offBlack}/15",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    winnerOpen = false
                    setupOpen = true
                }) { Text(state.t("bg.newMatch")) }
            },
        )
    }
}

@Composable
private fun PointColumn(
    state: ShellState,
    board: BgState,
    point: Int,
    top: Boolean,
    selected: Boolean,
    source: Boolean,
    target: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = point - 1
    val count = board.points[index]
    val owner = when {
        count > 0 -> BgSide.White
        count < 0 -> BgSide.Black
        else -> null
    }
    val stack = (kotlin.math.abs(count)).coerceAtMost(5)
    Column(
        modifier
            .padding(1.dp)
            .background(
                when {
                    selected -> Color(0x66E0A63A)
                    target -> Color(0x443FA66A)
                    source -> Color(0x22FFFFFF)
                    else -> if ((point + (if (top) 1 else 0)) % 2 == 0) Color(0x11FFFFFF) else Color.Transparent
                },
                RoundedCornerShape(4.dp),
            )
            .border(
                if (selected) 2.dp else if (target) 1.dp else 0.dp,
                if (selected) Color(0xFFE0A63A) else Color(0xFF3FA66A),
                RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (top) Arrangement.Top else Arrangement.Bottom,
    ) {
        Text(
            point.toString(),
            fontSize = 7.sp,
            color = BG_LIGHT.copy(alpha = 0.65f),
        )
        repeat(stack) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (owner == BgSide.White) BG_WHITE else BG_BLACK)
                    .border(1.dp, if (owner == BgSide.White) BG_DARK else BG_LIGHT, CircleShape),
            )
        }
        if (kotlin.math.abs(count) > 5) {
            Text("+${kotlin.math.abs(count) - 5}", fontSize = 6.sp, color = BG_LIGHT)
        }
    }
}

@Composable
private fun BarStack(count: Int, color: Color, selected: Boolean, source: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(30.dp)
            .padding(2.dp)
            .background(if (selected) Color(0x66E0A63A) else Color(0x22000000), RoundedCornerShape(4.dp))
            .border(if (source) 1.dp else 0.dp, Color(0xFFE0A63A), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$count", fontSize = 8.sp, color = BG_LIGHT, fontWeight = FontWeight.Bold)
        repeat(count.coerceAtMost(4)) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, BG_LIGHT, CircleShape),
            )
        }
    }
}


