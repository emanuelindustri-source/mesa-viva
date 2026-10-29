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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.DominoEnd
import com.mesaviva.app.games.DominoGame
import com.mesaviva.app.games.DominoMoveType
import com.mesaviva.app.games.DominoPhase
import com.mesaviva.app.games.DominoResult
import com.mesaviva.app.games.DominoState
import com.mesaviva.app.games.DominoTile
import kotlinx.coroutines.delay

private data class DominoSetup(val players: Int, val teams: Boolean, val target: Int)

@Composable
fun DominoScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var setup by remember { mutableStateOf(DominoSetup(4, true, 100)) }
    var game by remember { mutableStateOf(DominoGame(setup.players, setup.teams, setup.target)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var selectedTile by remember { mutableStateOf<Int?>(null) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }
    var awarded by remember { mutableStateOf(false) }

    LaunchedEffect(board.messageKey, board.messageParams) {
        if (board.messageKey.isNotBlank()) {
            log = (log + state.t(board.messageKey, board.messageParams)).takeLast(200)
        }
    }

    fun start(config: DominoSetup) {
        setup = config
        game = DominoGame(config.players, config.teams, config.target)
        board = game.newMatch()
        selectedTile = null
        log = emptyList()
        awarded = false
        setupOpen = false
        resultOpen = false
    }

    fun apply(result: DominoResult) {
        if (result.error != null) {
            Toast.makeText(context, state.t(result.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = result.state
        selectedTile = null
        state.vibrate(context, 30)
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
        if (board.phase == DominoPhase.MatchEnd && !awarded) {
            awarded = true
            val me = board.players.firstOrNull()
            val myTeam = me?.team
            val won = board.winnerIndices.any { index ->
                val player = board.players.getOrNull(index)
                if (player == null) false else if (myTeam != null) player.team == myTeam else player === me
            }
            state.awardMatch(win = won, draw = board.winnerIndices.isEmpty())
            NativeAudio.play(
                context,
                if (won) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose,
                state.sfx,
            )
            resultOpen = true
        }
    }

    fun playSelected(end: DominoEnd) {
        val index = selectedTile ?: return
        apply(game.play(board, board.turn, index, end))
    }

    fun onTile(index: Int) {
        if (board.turn != 0 || board.phase != DominoPhase.Playing) return
        val tile = board.players[0].hand.getOrNull(index) ?: return
        val ends = game.playableEnds(board, tile)
        if (ends.isEmpty()) {
            Toast.makeText(context, state.t("domino.error.doesNotFit"), Toast.LENGTH_SHORT).show()
            return
        }
        if (ends.size == 1) {
            apply(game.play(board, 0, index, ends.first()))
        } else {
            selectedTile = index
            state.vibrate(context, 18)
        }
    }

    LaunchedEffect(board.turn, board.phase, board.boneyard.size, board.players.sumOf { it.hand.size }) {
        val player = board.players.getOrNull(board.turn)
        if (board.phase == DominoPhase.Playing && player != null && player.isBot) {
            delay(650)
            when (val move = game.botMove(board)) {
                null -> Unit
                else -> when (move.type) {
                    DominoMoveType.Play -> {
                        val index = move.tileIndex ?: return@LaunchedEffect
                        val end = move.end ?: return@LaunchedEffect
                        apply(game.play(board, board.turn, index, end))
                    }
                    DominoMoveType.Draw -> apply(game.draw(board, board.turn))
                    DominoMoveType.Pass -> apply(game.pass(board, board.turn))
                    DominoMoveType.NextHand -> board = game.nextHand(board)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("domino.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("domino.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                Text(
                    state.t("domino.target", mapOf("n" to setup.target)),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            board.players.forEachIndexed { index, player ->
                val isTurn = board.turn == index && board.phase == DominoPhase.Playing
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTurn) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                    ),
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (index == 0) state.t("domino.you") else player.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            state.t("domino.score", mapOf("n" to game.scoreForDisplay(board, index))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${player.hand.size} 🁢",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
                .border(1.dp, Color(0xFFD5B05C).copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B3A2C)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        state.t("domino.hand", mapOf("n" to board.handNo)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBFD8C8),
                    )
                    Text(
                        state.t("domino.boneyard", mapOf("n" to board.boneyard.size)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBFD8C8),
                    )
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EndBadge(state.t("domino.left"), board.left)
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            itemsIndexed(board.chain) { _, tile ->
                                DominoTileView(tile = tile, compact = true)
                            }
                        }
                        EndBadge(state.t("domino.right"), board.right)
                    }
                }
                Text(
                    state.t(board.messageKey, board.messageParams),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE7D7B0),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(board.players[0].hand) { index, tile ->
                val playable = board.turn == 0 && board.phase == DominoPhase.Playing && game.playableEnds(board, tile).isNotEmpty()
                DominoTileView(
                    tile = tile,
                    selected = selectedTile == index,
                    playable = playable,
                    onClick = if (playable) ({ onTile(index) }) else null,
                )
            }
        }

        if (selectedTile != null) {
            val tile = board.players[0].hand.getOrNull(selectedTile!!)
            val ends = if (tile != null) game.playableEnds(board, tile) else emptyList()
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.t("domino.selectEnd"), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (DominoEnd.Left in ends) {
                        Button(onClick = { playSelected(DominoEnd.Left) }) { Text("← ${state.t("domino.left")}") }
                    }
                    if (DominoEnd.Right in ends) {
                        Button(onClick = { playSelected(DominoEnd.Right) }) { Text("${state.t("domino.right")} →") }
                    }
                    TextButton(onClick = { selectedTile = null }) { Text(state.t("common.close")) }
                }
            }
        } else if (board.turn == 0 && board.phase == DominoPhase.Playing && !game.hasPlayable(board, 0)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
                if (board.boneyard.isNotEmpty()) {
                    Button(onClick = { apply(game.draw(board, 0)) }) { Text("🁢 ${state.t("domino.draw")}") }
                } else {
                    Button(onClick = { apply(game.pass(board, 0)) }) { Text(state.t("domino.pass")) }
                }
            }
        } else if (board.phase == DominoPhase.HandEnd) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
                Button(onClick = { board = game.nextHand(board) }) { Text(state.t("domino.nextHand")) }
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("domino.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
        }
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            log,
            pdfTitle = state.t("domino.title"),
            pdfSubtitle = state.t("domino.subtitle"),
        ) { historyOpen = false }
    }

    if (setupOpen) {
        DominoSetupDialog(
            state = state,
            initial = setup,
            onDismiss = { if (board.handNo > 0) setupOpen = false },
            onStart = { start(it) },
        )
    }

    if (resultOpen) {
        val winners = board.winnerIndices.mapNotNull { board.players.getOrNull(it)?.name }
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(state.t("domino.title")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (winners.isEmpty()) state.t("domino.msgDraw")
                        else state.t("domino.matchWon", mapOf("p" to winners.joinToString(" · "))),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    board.players.forEachIndexed { index, player ->
                        Text(
                            "${if (index == 0) state.t("domino.you") else player.name}: ${game.scoreForDisplay(board, index)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    resultOpen = false
                    setupOpen = true
                }) { Text(state.t("domino.newMatch")) }
            },
            dismissButton = {
                TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }
}

@Composable
private fun EndBadge(label: String, value: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DominoSetupDialog(
    state: ShellState,
    initial: DominoSetup,
    onDismiss: () -> Unit,
    onStart: (DominoSetup) -> Unit,
) {
    var players by remember { mutableStateOf(initial.players) }
    var teams by remember { mutableStateOf(initial.teams) }
    var target by remember { mutableStateOf(initial.target) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.t("domino.setup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("domino.players"), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4).forEach { value ->
                        FilterChip(
                            selected = players == value,
                            onClick = {
                                players = value
                                if (value != 4) teams = false
                            },
                            label = { Text(value.toString()) },
                        )
                    }
                }
                if (players == 4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("domino.teams"), modifier = Modifier.weight(1f))
                        Switch(checked = teams, onCheckedChange = { teams = it })
                    }
                }
                Text(state.t("domino.target", mapOf("n" to target)), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 150, 200).forEach { value ->
                        FilterChip(
                            selected = target == value,
                            onClick = { target = value },
                            label = { Text(value.toString()) },
                        )
                    }
                }
                Text(
                    state.t("domino.rules"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(DominoSetup(players, teams, target)) }) {
                Text(state.t("domino.start"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(state.t("common.close")) }
        },
    )
}

@Composable
internal fun DominoTileView(
    tile: DominoTile,
    compact: Boolean = false,
    selected: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val width = if (compact) 40.dp else 52.dp
    val height = if (compact) 24.dp else 32.dp
    val border = if (selected) {
        MaterialTheme.colorScheme.primary
    } else if (playable) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(5.dp))
            .background(if (playable) Color(0xFFFFF8E8) else Color(0xFFD7D0C2))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(5.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics {
                contentDescription = "Ficha ${tile.a}-${tile.b}" +
                    if (selected) ", seleccionada" else ""
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val halfW = size.width / 2f
            val radius = size.minDimension * 0.055f
            drawLine(
                Color(0xFF6E5D48),
                Offset(halfW, size.height * 0.12f),
                Offset(halfW, size.height * 0.88f),
                strokeWidth = size.minDimension * 0.03f,
            )
            drawPips(tile.a, Offset(0f, 0f), Size(halfW, size.height), radius)
            drawPips(tile.b, Offset(halfW, 0f), Size(halfW, size.height), radius)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(
    value: Int,
    origin: Offset,
    area: Size,
    radius: Float,
) {
    val positions = when (value) {
        0 -> emptyList()
        1 -> listOf(0.5f to 0.5f)
        2 -> listOf(0.28f to 0.28f, 0.72f to 0.72f)
        3 -> listOf(0.28f to 0.28f, 0.5f to 0.5f, 0.72f to 0.72f)
        4 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f)
        5 -> listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f)
        else -> listOf(
            0.28f to 0.25f, 0.72f to 0.25f,
            0.28f to 0.5f, 0.72f to 0.5f,
            0.28f to 0.75f, 0.72f to 0.75f,
        )
    }
    for ((x, y) in positions) {
        drawCircle(
            color = Color(0xFF2D2820),
            radius = radius,
            center = Offset(origin.x + area.width * x, origin.y + area.height * y),
        )
    }
}
