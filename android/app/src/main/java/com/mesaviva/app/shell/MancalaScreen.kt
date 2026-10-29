package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.MancalaGame
import com.mesaviva.app.games.MancalaPhase
import com.mesaviva.app.games.MancalaSide
import com.mesaviva.app.games.MancalaState
import kotlinx.coroutines.delay

@Composable
fun MancalaScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var game by remember { mutableStateOf(MancalaGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var winnerOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }

    fun picture(side: MancalaSide): String = when {
        side == MancalaSide.White -> state.t("mancala.you")
        allHumans -> state.t("mancala.player2")
        else -> state.t("mancala.bot")
    }

    fun addLog(text: String) {
        log = (log + text).takeLast(200)
    }

    fun start(asHumans: Boolean) {
        allHumans = asHumans
        game = MancalaGame()
        board = game.newMatch()
        log = emptyList()
        setupOpen = false
        winnerOpen = false
    }

    fun doSow(pit: Int) {
        val actor = board.turn
        val res = game.sow(board, pit)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        state.vibrate(context, 24)
        addLog(state.t("mancala.log.sow", mapOf("p" to picture(actor), "n" to (pit + 1))))
        if (res.state.lastCapture != null) {
            addLog(state.t("mancala.log.capture", mapOf("p" to picture(actor), "n" to res.state.lastCapture)))
            state.vibrate(context, 50)
        }
        if (res.state.phase == MancalaPhase.MatchEnd) winnerOpen = true
    }

    LaunchedEffect(board.turn, board.pits, board.phase) {
        if (board.phase == MancalaPhase.MatchEnd) return@LaunchedEffect
        val bot = !allHumans && board.turn == MancalaSide.Black
        if (!bot) return@LaunchedEffect
        delay(650)
        val pit = game.botPit(board) ?: return@LaunchedEffect
        doSow(pit)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("mancala.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("mancala.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("mancala.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
        }

        Text(
            when {
                board.phase == MancalaPhase.MatchEnd -> state.t("mancala.finished")
                board.turn == MancalaSide.White -> state.t("mancala.yourTurn")
                allHumans -> state.t("mancala.turnPlayer", mapOf("p" to state.t("mancala.player2")))
                else -> state.t("mancala.botTurn")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(vertical = 4.dp)
                .border(1.dp, Color(0xFFD5B05C).copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7B4E2E)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Store(state, board.blackStore, state.t("mancala.black"), Modifier.width(56.dp).fillMaxHeight())
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (pit in listOf(12, 11, 10, 9, 8, 7)) {
                            Pit(
                                state = state,
                                board = board,
                                pit = pit,
                                movable = board.phase == MancalaPhase.Play &&
                                    board.turn == MancalaSide.Black &&
                                    (allHumans || !allHumans) &&
                                    board.pits[pit] > 0,
                                onClick = { doSow(pit) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (pit in 0..5) {
                            Pit(
                                state = state,
                                board = board,
                                pit = pit,
                                movable = board.phase == MancalaPhase.Play &&
                                    board.turn == MancalaSide.White &&
                                    board.pits[pit] > 0,
                                onClick = { doSow(pit) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
                Store(state, board.whiteStore, state.t("mancala.you"), Modifier.width(56.dp).fillMaxHeight())
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${state.t("mancala.you")}: ${board.whiteStore}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${state.t("mancala.bot")}: ${board.blackStore}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                state.t("mancala.lastMoves"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            if (log.isEmpty()) {
                Text(
                    state.t("mancala.rules"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            } else {
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
            pdfTitle = state.t("mancala.title"),
            pdfSubtitle = state.t("mancala.subtitle"),
        ) { historyOpen = false }
    }

    if (setupOpen) {
        var humans by remember { mutableStateOf(allHumans) }
        AlertDialog(
            onDismissRequest = { if (board.pits.sum() > 0) setupOpen = false },
            title = { Text(state.t("mancala.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("mancala.setupDesc"), style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("mancala.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = humans, onCheckedChange = { humans = it })
                    }
                    Text(state.t("mancala.rules"), style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                Button(onClick = { start(humans) }) { Text(state.t("mancala.start")) }
            },
            dismissButton = { TextButton(onClick = { setupOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (winnerOpen && board.phase == MancalaPhase.MatchEnd) {
        val title = when (board.winner) {
            MancalaSide.White -> state.t("mancala.youWon")
            MancalaSide.Black -> if (allHumans) state.t("mancala.player2Won") else state.t("mancala.botWon")
            null -> state.t("mancala.draw")
        }
        AlertDialog(
            onDismissRequest = { winnerOpen = false },
            title = { Text(title) },
            text = {
                Text(
                    "${state.t("mancala.you")}: ${board.whiteStore} · ${state.t("mancala.bot")}: ${board.blackStore}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    winnerOpen = false
                    setupOpen = true
                }) { Text(state.t("mancala.newMatch")) }
            },
        )
    }
}

@Composable
private fun Store(state: ShellState, seeds: Int, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF2C1D12))
            .border(1.dp, Color(0xFFD5B05C).copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFE7D7B0), maxLines = 1)
            Text("$seeds", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE0A63A))
        }
    }
}

@Composable
private fun Pit(
    state: ShellState,
    board: MancalaState,
    pit: Int,
    movable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seeds = board.pits.getOrElse(pit) { 0 }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.66f)
                .clip(RoundedCornerShape(50))
                .background(if (movable) Color(0xFF2E4A33) else Color(0xFF2C1D12))
                .border(
                    if (movable) 2.dp else 1.dp,
                    if (movable) Color(0xFFE0A63A) else Color(0xFFD5B05C).copy(alpha = 0.25f),
                    CircleShape,
                )
                .then(if (movable) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val count = seeds.coerceAtMost(12)
                val columns = 4
                val radius = size.minDimension * 0.065f
                for (index in 0 until count) {
                    val column = index % columns
                    val row = index / columns
                    val x = size.width * (0.18f + column * 0.21f)
                    val y = size.height * (0.25f + row * 0.22f)
                    drawCircle(Color(0xFFE0A63A), radius, Offset(x, y))
                }
            }
            Text(
                "$seeds",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFF3D6),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
