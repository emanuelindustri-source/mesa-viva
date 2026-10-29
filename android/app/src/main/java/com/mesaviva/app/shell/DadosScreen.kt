package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.DadosAction
import com.mesaviva.app.games.DadosGame
import com.mesaviva.app.games.DadosLog
import com.mesaviva.app.games.DadosPhase
import com.mesaviva.app.games.DadosState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

private val DIE_FACE = Color(0xFFFFF6E4)
private val DIE_EDGE = Color(0xFF6B4A2C)
private val DIE_PIP = Color(0xFF2A1B12)

@Composable
fun DadosScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableStateOf(4) }
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(DadosGame(4)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var resultOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var revealOpen by remember { mutableStateOf(false) }
    var diceRevealed by remember { mutableStateOf(false) }
    var qty by remember { mutableIntStateOf(1) }
    var face by remember { mutableIntStateOf(1) }
    var spin by remember { mutableStateOf(0f) }

    val current = board.players.getOrNull(board.turn)
    val currentLabel = if (board.turn == 0) state.t("common.you") else current?.name ?: state.t("common.bot")
    val humanTurn = current?.isBot == false
    val total = game.totalDice(board)
    val myDice = board.dice.getOrElse(board.turn) { emptyList() }
    val canSeeDice = humanTurn && (diceRevealed || !allHumans)
    val rotation by animateFloatAsState(
        targetValue = spin,
        animationSpec = tween(550),
        label = "dados-spin",
    )

    LaunchedEffect(board.turn, board.bid, board.round) {
        qty = max(1, (board.bid?.qty ?: 0) + 1).coerceAtMost(total.coerceAtLeast(1))
        face = 1
        diceRevealed = false
        spin = 0f
    }

    fun start() {
        game = DadosGame(players)
        board = game.newMatch()
        setupOpen = false
        resultOpen = false
        historyOpen = false
        revealOpen = false
        diceRevealed = false
    }

    fun doBid() {
        val res = game.placeBid(board, qty, face)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        state.vibrate(context, 20)
    }

    fun doDoubt() {
        val res = game.doubt(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        revealOpen = true
        spin += 360f
        state.vibrate(context, 60)
        if (board.phase == DadosPhase.MatchEnd) resultOpen = true
    }

    fun doContinue() {
        val res = game.continueRound(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        revealOpen = false
        state.vibrate(context, 20)
    }

    LaunchedEffect(board.turn, board.phase, board.bid, board.round, board.history.size, allHumans, difficulty) {
        val player = board.players.getOrNull(board.turn) ?: return@LaunchedEffect
        if (!player.isBot || allHumans || board.phase == DadosPhase.MatchEnd) return@LaunchedEffect
        delay(650)
        val action = withContext(Dispatchers.Default) { game.botAction(board, difficulty) }
        when (action) {
            is DadosAction.Bid -> {
                val res = game.placeBid(board, action.qty, action.face)
                if (res.error == null) board = res.state
            }
            DadosAction.Doubt -> {
                val res = game.doubt(board)
                if (res.error == null) {
                    board = res.state
                    revealOpen = true
                    spin += 360f
                    if (board.phase == DadosPhase.MatchEnd) resultOpen = true
                }
            }
            DadosAction.Continue -> {
                val res = game.continueRound(board)
                if (res.error == null) {
                    board = res.state
                    revealOpen = false
                }
            }
            null -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("dados.title"),
            subtitle = state.t("dados.subtitle"),
            badge = state.t("dados.roundBadge", mapOf("n" to board.round)),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.phase == DadosPhase.MatchEnd -> dadosResultText(state, board, allHumans)
                board.phase == DadosPhase.Reveal -> state.t("dados.revealBanner")
                humanTurn -> state.t("dados.yourTurn", mapOf("n" to total))
                else -> state.t("dados.botTurn", mapOf("p" to currentLabel))
            },
            alert = board.phase == DadosPhase.Reveal,
        )

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            board.players.forEachIndexed { index, player ->
                val alive = board.diceCount[index] > 0
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(9.dp),
                    color = when {
                        index == board.turn && alive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        !alive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                    },
                ) {
                    Column(
                        Modifier.padding(vertical = 5.dp, horizontal = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (index == 0) state.t("common.you") else player.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (index == board.turn) FontWeight.Bold else FontWeight.Normal,
                            color = if (alive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (alive) "🎲 ${board.diceCount[index]}" else "—",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.t("dados.currentBid"),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        board.bid?.let { "${it.qty} × ${it.face}" } ?: state.t("dados.openTable"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    state.t("dados.totalDice", mapOf("n" to total)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (board.phase == DadosPhase.Reveal && board.reveal != null) {
            DadosRevealPanel(state, board, allHumans)
        } else if (humanTurn) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.t("dados.yourDice", mapOf("p" to currentLabel)),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (allHumans && humanTurn && !diceRevealed) {
                            TextButton(onClick = { diceRevealed = true }) {
                                Text(state.t("dados.showDice"))
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val visible = canSeeDice
                        val values = if (visible) myDice else List(myDice.size) { 0 }
                        if (values.isEmpty()) {
                            Text(
                                state.t("dados.eliminated"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        values.forEach { value ->
                            Die(value = value, hidden = value == 0, rotation = rotation)
                        }
                    }
                    if (allHumans && humanTurn && !diceRevealed) {
                        Text(
                            state.t("dados.privacy"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (board.phase == DadosPhase.Bid && humanTurn) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("dados.quantity"), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { qty = (qty - 1).coerceAtLeast(1) }) { Text("−") }
                        Text(
                            "$qty",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        OutlinedButton(onClick = { qty = (qty + 1).coerceAtMost(total) }) { Text("+") }
                    }
                    Text(state.t("dados.face"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (value in 1..6) {
                            FilterChip(
                                selected = face == value,
                                onClick = { face = value },
                                label = { Text("$value") },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { doBid() },
                            enabled = qty in 1..total && (board.bid == null || qty > board.bid!!.qty || (qty == board.bid!!.qty && face > board.bid!!.face)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(state.t("dados.bid"))
                        }
                        if (board.bid != null) {
                            OutlinedButton(onClick = { doDoubt() }, modifier = Modifier.weight(1f)) {
                                Text("${state.t("dados.doubt")}!")
                            }
                        }
                    }
                }
            }
        }

        if (board.phase == DadosPhase.Reveal) {
            Button(onClick = { doContinue() }, modifier = Modifier.fillMaxWidth()) {
                Text(state.t("dados.continue"))
            }
        }

        NativeMoveLog(state, board.history.map { dadosLogText(state, board, it) }) { historyOpen = true }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
        }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("dados.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("dados.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { value ->
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
                        state.t("dados.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = { TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (revealOpen && board.reveal != null && board.phase == DadosPhase.Reveal) {
        AlertDialog(
            onDismissRequest = { revealOpen = false },
            title = { Text(state.t("dados.revealTitle")) },
            text = { DadosRevealPanel(state, board, allHumans) },
            confirmButton = { TextButton(onClick = { revealOpen = false; doContinue() }) { Text(state.t("dados.continue")) } },
            dismissButton = { TextButton(onClick = { revealOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history.map { dadosLogText(state, board, it) },
            pdfTitle = state.t("dados.title"),
            pdfSubtitle = state.t("dados.subtitle"),
        ) { historyOpen = false }
    }

    if (resultOpen && board.phase == DadosPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(dadosResultText(state, board, allHumans)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.players.forEachIndexed { index, player ->
                        Text(
                            "${if (index == 0) state.t("common.you") else player.name}: ${board.diceCount[index]} 🎲",
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

@Composable
private fun DadosRevealPanel(state: ShellState, board: DadosState, allHumans: Boolean) {
    val reveal = board.reveal ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            state.t("dados.revealResult", mapOf(
                "actual" to reveal.actual,
                "qty" to reveal.bid.qty,
                "face" to reveal.bid.face,
                "p" to nameOf(state, board, reveal.loser, allHumans),
            )),
            style = MaterialTheme.typography.bodySmall,
        )
        for (index in board.players.indices) {
            if (board.diceCount[index] <= 0 && reveal.dice.getOrElse(index) { emptyList() }.isEmpty()) continue
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (index == 0) state.t("common.you") else board.players[index].name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                for (value in reveal.dice.getOrElse(index) { emptyList() }) {
                    Die(value = value, hidden = false, rotation = 0f, size = 28.dp)
                }
            }
        }
    }
}

@Composable
private fun Die(value: Int, hidden: Boolean, rotation: Float, size: Dp = 34.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(if (hidden) 0f else rotation)
            .clip(RoundedCornerShape(size * 0.18f))
            .background(if (hidden) MaterialTheme.colorScheme.surfaceVariant else DIE_FACE)
            .border(1.5.dp, DIE_EDGE, RoundedCornerShape(size * 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (hidden) {
            Text("?", fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold, color = DIE_EDGE)
        } else {
            val pip = size * 0.13f
            val positions = when (value) {
                1 -> listOf(1 to 1)
                2 -> listOf(0 to 0, 2 to 2)
                3 -> listOf(0 to 0, 1 to 1, 2 to 2)
                4 -> listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2)
                5 -> listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2)
                else -> listOf(0 to 0, 2 to 0, 0 to 1, 2 to 1, 0 to 2, 2 to 2)
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(size * 0.12f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                for (row in 0..2) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (col in 0..2) {
                            Box(
                                Modifier.size(pip).clip(RoundedCornerShape(50)).background(
                                    if (positions.contains(col to row)) DIE_PIP else Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun nameOf(state: ShellState, board: DadosState, index: Int, allHumans: Boolean): String = when {
    index == 0 -> state.t("common.you")
    else -> board.players.getOrNull(index)?.name ?: state.t("common.bot")
}

private fun dadosLogText(state: ShellState, board: DadosState, log: DadosLog): String {
    val name = nameOf(state, board, log.player, true)
    return when (log.kind) {
        "bid" -> state.t("dados.log.bid", mapOf("p" to name, "q" to log.qty, "f" to log.face))
        "doubt" -> state.t("dados.log.doubt", mapOf("p" to name, "q" to log.qty, "f" to log.face))
        "reveal" -> state.t("dados.log.reveal", mapOf("actual" to log.actual, "q" to log.qty, "f" to log.face))
        "lostDie" -> state.t("dados.log.lostDie", mapOf("p" to name))
        "won" -> state.t("dados.log.won", mapOf("p" to name))
        else -> log.kind
    }
}

private fun dadosResultText(state: ShellState, board: DadosState, allHumans: Boolean): String = when {
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && allHumans -> state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
