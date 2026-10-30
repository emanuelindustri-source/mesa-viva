package com.mesaviva.app.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.SorobanGame
import com.mesaviva.app.games.SorobanPhase
import com.mesaviva.app.games.SorobanState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SorobanScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    val game = remember { SorobanGame() }
    var board by remember { mutableStateOf(game.newGame(1)) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }
    var level by remember { mutableIntStateOf(1) }

    fun start(selectedLevel: Int) {
        board = game.newGame(selectedLevel)
        setupOpen = false
        resultOpen = false
        historyOpen = false
        awarded = false
    }

    fun check() {
        val result = game.check(board)
        board = result.state
        when (result.correct) {
            true -> {
                NativeAudio.play(context, NativeAudio.Sfx.Promote, state.sfx)
                state.vibrate(context, 36)
                if (board.phase == SorobanPhase.Complete && !awarded) {
                    awarded = true
                    state.awardMatch(win = true)
                    NativeAudio.play(context, NativeAudio.Sfx.Win, state.sfx)
                    resultOpen = true
                }
            }
            false -> {
                NativeAudio.play(context, NativeAudio.Sfx.Capture, state.sfx)
                state.vibrate(context, 55)
            }
            null -> Unit
        }
    }

    fun loadLevel(targetLevel: Int) {
        board = board.copy(
            heaven = List(SorobanState.RODS) { false },
            earth = List(SorobanState.RODS) { 0 },
            level = targetLevel,
            message = null,
        )
    }

    LaunchedEffect(board.message) {
        if (board.message == "soroban.complete" && !awarded) {
            awarded = true
            state.awardMatch(win = true)
            resultOpen = true
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("soroban.title"),
            subtitle = state.t("soroban.subtitle"),
            badge = state.t("soroban.badge", mapOf("l" to board.level, "n" to board.solved)),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == SorobanPhase.Complete -> state.t("soroban.completeBanner")
                board.message == "soroban.correct" -> state.t("soroban.correct")
                board.message == "soroban.wrong" -> state.t("soroban.wrong")
                else -> state.t("soroban.prompt")
            },
            alert = board.message == "soroban.wrong",
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(state.t("soroban.target"), style = MaterialTheme.typography.labelSmall)
                    Text(
                        board.target.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(state.t("soroban.value"), style = MaterialTheme.typography.labelSmall)
                    Text(
                        game.value(board).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2A1B12))
                .border(1.dp, Color(0xFFE0A63A).copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        ) {
            Abacus(
                board = board,
                onSelect = { index -> board = game.selectRod(board, index) },
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            state.t("soroban.selected", mapOf("n" to (board.selectedRod + 1), "d" to game.digit(board, board.selectedRod))),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            OutlinedButton(
                onClick = { board = game.changeEarth(board, 1) },
                modifier = Modifier.weight(1f),
            ) { Text("+1") }
            OutlinedButton(
                onClick = { board = game.changeEarth(board, -1) },
                modifier = Modifier.weight(1f),
            ) { Text("−1") }
            OutlinedButton(
                onClick = { board = game.toggleHeaven(board) },
                modifier = Modifier.weight(1f),
            ) { Text("5") }
            OutlinedButton(
                onClick = { board = game.clearRod(board) },
                modifier = Modifier.weight(1f),
            ) { Text("0") }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(onClick = { check() }, modifier = Modifier.weight(1f)) {
                Text(state.t("soroban.check"))
            }
            OutlinedButton(onClick = { board = game.clearAll(board) }) {
                Text(state.t("soroban.clear"))
            }
            OutlinedButton(onClick = { board = game.nextLevel(board) }) {
                Text(state.t("soroban.next"))
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
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("soroban.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("soroban.level"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(1, 3, 5, 7, 10).forEach { value ->
                            FilterChip(
                                selected = level == value,
                                onClick = { level = value },
                                label = { Text("$value") },
                            )
                        }
                    }
                    Text(
                        state.t("soroban.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start(level) }) { Text(state.t("common.start")) } },
            dismissButton = {
                TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) {
                    Text(state.t("common.close"))
                }
            },
        )
    }

    if (historyOpen) NativeHistoryDialog(
        state,
        board.history,
        pdfTitle = state.t("soroban.title"),
        pdfSubtitle = state.t("soroban.subtitle"),
        stats = listOf(
            state.t("soroban.score") to board.score.toString(),
            state.t("soroban.solved") to board.solved.toString(),
            state.t("soroban.bestStreak") to board.bestStreak.toString(),
        ),
    ) { historyOpen = false }

    if (resultOpen && board.phase == SorobanPhase.Complete) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(state.t("soroban.completeTitle")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("soroban.finalScore", mapOf("n" to board.score)), style = MaterialTheme.typography.bodyMedium)
                    Text(state.t("soroban.bestStreakLine", mapOf("n" to board.bestStreak)), style = MaterialTheme.typography.bodySmall)
                    Text(
                        state.t("soroban.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

@Composable
private fun Abacus(
    board: SorobanState,
    onSelect: (Int) -> Unit,
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
            .pointerInput(board.selectedRod) {
                detectTapGestures { tap ->
                    val step = size.width / (SorobanState.RODS + 1f)
                    val index = ((tap.x / step) - 1f).roundToInt().coerceIn(0, SorobanState.RODS - 1)
                    onSelect(index)
                }
            },
    ) {
        val rods = SorobanState.RODS
        val step = size.width / (rods + 1f)
        val beamY = size.height * 0.34f
        val radius = (step * 0.26f).coerceAtLeast(3f)
        for (index in 0 until rods) {
            val x = step * (index + 1)
            val selected = index == board.selectedRod
            drawLine(
                color = if (selected) Color(0xFFE0A63A) else Color(0xFF8A6A45),
                start = Offset(x, size.height * 0.08f),
                end = Offset(x, size.height * 0.94f),
                strokeWidth = if (selected) 4f else 2f,
            )
            drawLine(
                color = Color(0xFFE0A63A).copy(alpha = 0.65f),
                start = Offset(0f, beamY),
                end = Offset(size.width, beamY),
                strokeWidth = 3f,
            )
            val heavenY = if (board.heaven.getOrElse(index) { false }) beamY - radius * 1.4f else size.height * 0.14f
            drawCircle(
                color = if (board.heaven.getOrElse(index) { false }) Color(0xFFE0A63A) else Color(0xFF5A3A22),
                radius = radius,
                center = Offset(x, heavenY),
            )
            drawCircle(Color(0xFFE0A63A).copy(alpha = 0.4f), radius, Offset(x, heavenY), style = Stroke(1.2f))
            for (bead in 0 until 4) {
                val active = bead < board.earth.getOrElse(index) { 0 }
                val y = beamY + radius * 1.5f + bead * radius * 2.1f
                drawCircle(
                    color = if (active) Color(0xFFD9B36A) else Color(0xFF5A3A22),
                    radius = radius,
                    center = Offset(x, y),
                )
                drawCircle(Color(0xFFE0A63A).copy(alpha = 0.4f), radius, Offset(x, y), style = Stroke(1.2f))
            }
        }
    }
}
