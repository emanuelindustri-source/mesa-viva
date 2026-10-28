package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.LudoAction
import com.mesaviva.app.games.LudoColor
import com.mesaviva.app.games.LudoGame
import com.mesaviva.app.games.LudoMove
import com.mesaviva.app.games.LudoPhase
import com.mesaviva.app.games.LudoState
import com.mesaviva.app.games.LudoToken
import kotlinx.coroutines.delay

private val LUDO_COLORS = mapOf(
    LudoColor.Red to Color(0xFFD9534F),
    LudoColor.Green to Color(0xFF4FA36B),
    LudoColor.Yellow to Color(0xFFE0B23A),
    LudoColor.Blue to Color(0xFF4C7BD9),
)

private data class LudoSetup(val players: Int, val allHumans: Boolean)

@Composable
fun LudoScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var setup by remember { mutableStateOf(LudoSetup(4, false)) }
    var game by remember { mutableStateOf(LudoGame(setup.players, setup.allHumans)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var winnerOpen by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }

    fun addLog(text: String) {
        log = (log + text).takeLast(5)
    }

    fun start(config: LudoSetup) {
        setup = config
        game = LudoGame(config.players, config.allHumans)
        board = game.newMatch()
        log = emptyList()
        setupOpen = false
        winnerOpen = false
    }

    fun doRoll() {
        val res = game.roll(board)
        board = res.state
        val player = board.players.getOrNull(board.turn)
        if (res.error == "ludo.error.threeSixes") {
            addLog(state.t("ludo.log.threeSixes", mapOf("p" to (player?.name ?: ""))))
            Toast.makeText(context, state.t("ludo.threeSixes"), Toast.LENGTH_SHORT).show()
        } else if (res.error == "ludo.error.noMoves") {
            addLog(state.t("ludo.log.noMoves", mapOf("p" to (player?.name ?: ""), "n" to (res.state.lastRoll ?: 0))))
        } else {
            state.vibrate(context, 28)
            addLog(state.t("ludo.log.roll", mapOf("p" to (player?.name ?: ""), "n" to (res.state.lastRoll ?: 0))))
        }
    }

    fun doMove(tokenId: String) {
        val before = board
        val res = game.move(board, tokenId)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        val player = before.players.getOrNull(before.turn)
        val move = res.state.lastMove
        state.vibrate(context, 32)
        addLog(state.t("ludo.log.move", mapOf("p" to (player?.name ?: ""), "n" to (move?.to ?: 0))))
        val captured = before.tokens.count { old ->
            val now = res.state.tokens.firstOrNull { it.id == old.id }
            old.progress >= 0 && now?.progress == -1
        }
        if (captured > 0) {
            addLog(state.t("ludo.log.capture", mapOf("p" to (player?.name ?: ""), "n" to captured)))
            state.vibrate(context, 60)
        }
        if (move != null && move.to >= LudoGame.FINISH) {
            addLog(state.t("ludo.log.finish", mapOf("p" to (player?.name ?: ""))))
        }
        if (res.state.phase == LudoPhase.MatchEnd) winnerOpen = true
    }

    LaunchedEffect(board.turn, board.phase, board.dice, board.tokens, board.lastMove) {
        val player = board.players.getOrNull(board.turn) ?: return@LaunchedEffect
        if (board.phase == LudoPhase.MatchEnd || !player.isBot) return@LaunchedEffect
        delay(600)
        when (val action = game.botAction(board)) {
            LudoAction.Roll -> doRoll()
            is LudoAction.Move -> doMove(action.tokenId)
            null -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("ludo.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("ludo.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                Text(
                    "🎲 ${board.lastRoll ?: "—"}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        val current = board.players.getOrNull(board.turn)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    board.phase == LudoPhase.MatchEnd -> state.t("ludo.winner", mapOf("p" to game.winnerName(board)))
                    board.phase == LudoPhase.Roll -> state.t(
                        if (board.players[board.turn].isBot) "ludo.botRoll" else "ludo.yourRoll",
                        mapOf("p" to (current?.name ?: "")),
                    )
                    else -> state.t("ludo.chooseToken", mapOf("n" to (board.dice ?: 0)))
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        ) {
            val cell = maxWidth / 15
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 15) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 15) {
                            val zone = ludoZone(col, row)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(zone.first)
                                    .border(0.4.dp, Color.Black.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (zone.second) {
                                    Text("★", fontSize = (cell.value * 0.35f).sp, color = Color(0x66FFFFFF))
                                }
                            }
                        }
                    }
                }
            }

            val legal = game.legalMoves(board).map { it.tokenId }.toSet()
            val byCell = board.tokens.groupBy { LudoGame.tokenCoord(it) }
            for ((coord, tokens) in byCell) {
                tokens.forEachIndexed { index, token ->
                    val tokenSize = cell * 0.62f
                    val baseX = cell * coord.first + cell * 0.19f + (index * 2).dp
                    val baseY = cell * coord.second + cell * 0.19f + (index * 2).dp
                    val x by animateDpAsState(baseX, label = "ludo-x-${token.id}")
                    val y by animateDpAsState(baseY, label = "ludo-y-${token.id}")
                    Box(
                        Modifier
                            .offset(x = x, y = y)
                            .size(tokenSize)
                            .clip(CircleShape)
                            .background(LUDO_COLORS[token.color] ?: Color.Gray)
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                            .then(
                                if (token.id in legal && current?.isBot == false) {
                                    Modifier.clickable { doMove(token.id) }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${token.slot + 1}",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            board.players.forEachIndexed { index, player ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    color = LUDO_COLORS[player.color]?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            player.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            "${game.finishedCount(board, index)}/4 · ${player.captures}⚔",
                            style = MaterialTheme.typography.labelSmall,
                            color = LUDO_COLORS[player.color] ?: MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(
                onClick = { doRoll() },
                enabled = board.phase == LudoPhase.Roll && board.players.getOrNull(board.turn)?.isBot == false,
                modifier = Modifier.weight(1f),
            ) {
                Text("🎲 ${state.t("ludo.roll")}")
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("ludo.newMatch")) }
            Button(onClick = { winnerOpen = false; board = game.resign(board).state; winnerOpen = true }) {
                Text(state.t("ludo.resign"))
            }
        }

        if (log.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    state.t("ludo.lastMoves"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                log.takeLast(3).forEach { line ->
                    Text(line, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }

    if (setupOpen) {
        LudoSetupDialog(state, setup, onDismiss = { if (board.tokens.any { it.progress > -1 }) setupOpen = false }, onStart = { start(it) })
    }

    if (winnerOpen && board.phase == LudoPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { winnerOpen = false },
            title = { Text(state.t("ludo.winner", mapOf("p" to game.winnerName(board)))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    board.players.forEachIndexed { index, player ->
                        Text(
                            "${player.name}: ${game.finishedCount(board, index)}/4",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    winnerOpen = false
                    setupOpen = true
                }) { Text(state.t("ludo.newMatch")) }
            },
            dismissButton = {
                TextButton(onClick = { winnerOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }
}

@Composable
private fun LudoSetupDialog(
    state: ShellState,
    initial: LudoSetup,
    onDismiss: () -> Unit,
    onStart: (LudoSetup) -> Unit,
) {
    var players by remember { mutableStateOf(initial.players) }
    var allHumans by remember { mutableStateOf(initial.allHumans) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.t("ludo.setup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("ludo.players"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4).forEach { value ->
                        FilterChip(
                            selected = players == value,
                            onClick = { players = value },
                            label = { Text(value.toString()) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.t("ludo.allHumans"), modifier = Modifier.weight(1f))
                    Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                }
                Text(
                    state.t("ludo.rules"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(LudoSetup(players, allHumans)) }) { Text(state.t("ludo.start")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("common.close")) } },
    )
}

private fun ludoZone(col: Int, row: Int): Pair<Color, Boolean> {
    if (col < 6 && row < 6) return Color(0xFF4FA36B).copy(alpha = 0.22f) to false
    if (col > 8 && row < 6) return Color(0xFFE0B23A).copy(alpha = 0.24f) to false
    if (col < 6 && row > 8) return Color(0xFFD9534F).copy(alpha = 0.22f) to false
    if (col > 8 && row > 8) return Color(0xFF4C7BD9).copy(alpha = 0.22f) to false
    if (col in 6..8 && row in 6..8) return Color(0xFFE0A63A).copy(alpha = 0.35f) to false
    val coord = col to row
    when (coord) {
        in LudoGame.HOME[LudoColor.Red] ?: emptyList() -> return Color(0xFFD9534F).copy(alpha = 0.35f) to false
        in LudoGame.HOME[LudoColor.Green] ?: emptyList() -> return Color(0xFF4FA36B).copy(alpha = 0.35f) to false
        in LudoGame.HOME[LudoColor.Yellow] ?: emptyList() -> return Color(0xFFE0B23A).copy(alpha = 0.38f) to false
        in LudoGame.HOME[LudoColor.Blue] ?: emptyList() -> return Color(0xFF4C7BD9).copy(alpha = 0.35f) to false
    }
    val trackIndex = LudoGame.TRACK.indexOf(coord)
    if (trackIndex >= 0) {
        val startColor = LudoColor.entries.firstOrNull { LudoGame.START[it] == trackIndex }
        if (startColor != null) return (LUDO_COLORS[startColor] ?: Color.White).copy(alpha = 0.35f) to false
        return Color(0xFFFFF7E6) to LudoGame.SAFE.contains(trackIndex)
    }
    return Color(0xFFE8DDC8) to false
}
