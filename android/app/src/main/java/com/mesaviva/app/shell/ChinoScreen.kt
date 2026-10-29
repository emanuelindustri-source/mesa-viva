package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.ChinoGame
import com.mesaviva.app.games.ChinoPhase
import com.mesaviva.app.games.ChinoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.min

private val CHINO_COLORS = listOf(
    Color(0xFFD9534F),
    Color(0xFF4C7BD9),
    Color(0xFF4FA36B),
    Color(0xFFE0B23A),
    Color(0xFF9B59B6),
    Color(0xFFE67E22),
)

private data class ChinoLayout(
    val centers: List<Offset>,
    val radius: Float,
)

@Composable
fun ChinoScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableStateOf(2) }
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(ChinoGame()) }
    var board by remember { mutableStateOf(game.newMatch(2)) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == 0
    val legal = remember(board, humanTurn) {
        if (board.phase == ChinoPhase.Play && humanTurn) game.legalFor(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }
    val destinations = remember(selectedMoves) { selectedMoves.map { it.to }.toSet() }

    fun start() {
        game = ChinoGame()
        board = game.newMatch(players)
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
        val jumped = (res.state.lastMove?.jumps ?: 0) > 0
        NativeAudio.play(
            context,
            when {
                res.state.phase == ChinoPhase.MatchEnd && res.state.winner == 0 -> NativeAudio.Sfx.Win
                res.state.phase == ChinoPhase.MatchEnd -> NativeAudio.Sfx.Lose
                jumped -> NativeAudio.Sfx.Capture
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        state.vibrate(context, if (jumped) 40 else 20)
        if (res.state.phase == ChinoPhase.MatchEnd) resultOpen = true
    }

    fun onCell(index: Int) {
        if (board.phase != ChinoPhase.Play || !humanTurn) return
        if (selected != null) {
            val move = selectedMoves.firstOrNull { it.to == index }
            if (move != null) {
                applyMove(move.from, move.to)
                return
            }
        }
        if (board.pieces.getOrNull(index) == board.turn) {
            selected = if (selected == index) null else index
            state.vibrate(context, 12)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, allHumans, difficulty) {
        if (allHumans || board.phase == ChinoPhase.MatchEnd || board.turn == 0) return@LaunchedEffect
        delay(480)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) }
        if (move != null && board.phase == ChinoPhase.Play && board.turn != 0) {
            val res = game.move(board, move.from, move.to)
            if (res.error == null) {
                board = res.state
                if (res.state.phase == ChinoPhase.MatchEnd) resultOpen = true
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("chino.title"),
            subtitle = state.t("chino.subtitle"),
            badge = state.t("chino.badge", mapOf(
                "p" to board.playerCount,
                "n" to game.reachedTarget(board, board.turn),
            )),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == ChinoPhase.MatchEnd -> chinoResultText(state, board, allHumans)
                !humanTurn -> state.t("chino.botTurn")
                board.turn == 0 -> state.t("chino.yourTurn")
                else -> state.t("chino.turnPlayer", mapOf("p" to chinoName(state, board, board.turn, allHumans)))
            },
            alert = board.phase == ChinoPhase.MatchEnd,
        )

        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.12f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val layout = remember(widthPx, heightPx) {
                val cells = ChinoGame.CELLS
                val minX = cells.minOf { it.x }
                val maxX = cells.maxOf { it.x }
                val minY = cells.minOf { it.y }
                val maxY = cells.maxOf { it.y }
                val margin = widthPx * 0.05f
                val scale = min(
                    (widthPx - margin * 2f) / (maxX - minX),
                    (heightPx - margin * 2f) / (maxY - minY),
                )
                val offsetX = (widthPx - (maxX - minX) * scale) / 2f - minX * scale
                val offsetY = (heightPx - (maxY - minY) * scale) / 2f - minY * scale
                ChinoLayout(
                    centers = cells.map { Offset(offsetX + it.x * scale, offsetY + it.y * scale) },
                    radius = scale * 0.34f,
                )
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(board, layout) {
                        detectTapGestures { tap ->
                            if (board.phase != ChinoPhase.Play || !humanTurn) return@detectTapGestures
                            var best = -1
                            var bestDistance = Float.MAX_VALUE
                            for (index in layout.centers.indices) {
                                val distance = hypot(
                                    (tap.x - layout.centers[index].x).toDouble(),
                                    (tap.y - layout.centers[index].y).toDouble(),
                                ).toFloat()
                                if (distance < bestDistance) {
                                    bestDistance = distance
                                    best = index
                                }
                            }
                            if (best >= 0 && bestDistance <= layout.radius * 2.2f) onCell(best)
                        }
                    },
            ) {
                val lineColor = Color(0x335A3A1A)
                val drawn = mutableSetOf<Pair<Int, Int>>()
                for (from in layout.centers.indices) {
                    for (to in ChinoGame.NEIGHBORS[from]) {
                        val pair = minOf(from, to) to maxOf(from, to)
                        if (!drawn.add(pair)) continue
                        drawLine(lineColor, layout.centers[from], layout.centers[to], 1.2f)
                    }
                }
                for (index in layout.centers.indices) {
                    val cell = ChinoGame.CELLS[index]
                    val center = layout.centers[index]
                    val triangleColor = cell.triangle?.let { CHINO_COLORS[it % CHINO_COLORS.size].copy(alpha = 0.14f) }
                        ?: Color(0x22FFFFFF)
                    drawCircle(triangleColor, layout.radius * 1.02f, center)
                    drawCircle(Color(0x554A2E16), layout.radius * 1.02f, center, style = Stroke(1f))
                }
                val last = board.lastMove
                if (last != null) {
                    drawCircle(Color(0x66E0A63A), layout.radius * 1.1f, layout.centers[last.from])
                    drawCircle(Color(0x88E0A63A), layout.radius * 1.1f, layout.centers[last.to])
                }
                if (selected != null) {
                    drawCircle(Color(0x88E0A63A), layout.radius * 1.25f, layout.centers[selected!!])
                }
                for (index in destinations) {
                    drawCircle(Color(0x66FFFFFF), layout.radius * 0.32f, layout.centers[index])
                }
                for (index in board.pieces.indices) {
                    val seat = board.pieces[index] ?: continue
                    val color = CHINO_COLORS[seat % CHINO_COLORS.size]
                    val center = layout.centers[index]
                    drawCircle(Color(0x44000000), layout.radius * 1.02f, center + Offset(1.5f, 2.5f))
                    drawCircle(color, layout.radius * 0.92f, center)
                    drawCircle(Color.White.copy(alpha = 0.55f), layout.radius * 0.92f, center, style = Stroke(1.5f))
                    drawCircle(
                        Color.White.copy(alpha = 0.22f),
                        layout.radius * 0.32f,
                        center - Offset(layout.radius * 0.28f, layout.radius * 0.30f),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            for (seat in 0 until board.playerCount) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = CHINO_COLORS[seat % CHINO_COLORS.size].copy(alpha = 0.14f),
                ) {
                    Column(Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            chinoName(state, board, seat, allHumans),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (seat == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${game.reachedTarget(board, seat)}/10",
                            style = MaterialTheme.typography.labelSmall,
                            color = CHINO_COLORS[seat % CHINO_COLORS.size],
                        )
                    }
                }
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == ChinoPhase.Play) {
                Text(state.t("common.resign"))
            }
        }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("chino.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("chino.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 6).forEach { value ->
                            FilterChip(
                                selected = players == value,
                                onClick = { players = value },
                                label = { Text("$value") },
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
                        state.t("chino.rules"),
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
        pdfTitle = state.t("chino.title"),
        pdfSubtitle = state.t("chino.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == ChinoPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(chinoResultText(state, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (seat in 0 until board.playerCount) {
                        Text(
                            "${chinoName(state, board, seat, allHumans)}: ${game.reachedTarget(board, seat)}/10",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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

private fun chinoName(state: ShellState, board: ChinoState, seat: Int, allHumans: Boolean): String = when {
    seat == 0 -> state.t("common.you")
    allHumans -> state.t("common.playerN", mapOf("n" to (seat + 1)))
    else -> listOf("El Silbón", "La Sayona", "El Hachero", "La Llorona", "El Duende")
        .getOrElse(seat - 1) { state.t("common.bot") }
}

private fun chinoResultText(state: ShellState, board: ChinoState, allHumans: Boolean): String = when {
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && allHumans -> state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
