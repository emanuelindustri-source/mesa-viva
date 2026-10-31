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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.MexicanTrainGame
import com.mesaviva.app.games.MexicanTrainResult
import com.mesaviva.app.games.MexicanTrainState
import com.mesaviva.app.games.TrainGamePhase
import kotlinx.coroutines.delay

@Composable
fun MexicanTrainScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var overlapHumans by remember { mutableStateOf(false) }
    var playerCount by remember { mutableStateOf(4) }
    var game by remember { mutableStateOf(MexicanTrainGame(4)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf(0) }

    val viewSeat = if (overlapHumans) board.turn else 0
    val humanTurn = overlapHumans || board.turn == 0
    val hand = board.players.getOrNull(viewSeat)?.hand.orEmpty()
    val selectedTile = selected?.let { hand.getOrNull(it) }
    val playable = selectedTile?.let { game.playableTargets(board, it) }.orEmpty()

    fun start() {
        game = MexicanTrainGame(playerCount)
        board = game.newMatch()
        selected = null
        target = 0
        awarded = false
        setupOpen = false
        resultOpen = false
    }

    fun apply(result: MexicanTrainResult, sound: NativeAudio.Sfx = NativeAudio.Sfx.Card) {
        if (result.error != null) {
            Toast.makeText(context, state.t(result.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = result.state
        selected = null
        NativeAudio.play(context, sound, state.sfx)
        state.vibrate(context, 18)
        if ((board.phase == TrainGamePhase.RoundEnd || board.phase == TrainGamePhase.MatchEnd) && !awarded) {
            if (board.phase == TrainGamePhase.MatchEnd) {
                awarded = true
                if (!overlapHumans) state.awardMatch(win = board.winner == 0)
                NativeAudio.play(context, if (board.winner == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
            }
            resultOpen = true
        }
    }

    LaunchedEffect(board.turn, board.phase, overlapHumans) {
        if (overlapHumans || board.phase != TrainGamePhase.Playing || board.turn == 0) return@LaunchedEffect
        delay(650)
        val next = game.botStep(board) ?: return@LaunchedEffect
        apply(MexicanTrainResult(next), if (next.doubleTrain != null) NativeAudio.Sfx.Promote else NativeAudio.Sfx.Card)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        NativeGameHeader(
            state = state,
            title = state.t("train.title"),
            subtitle = state.t("train.subtitle"),
            badge = "${board.round}/3 · 🁫 ${board.boneyardCount}",
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == TrainGamePhase.MatchEnd -> state.t("train.matchOver")
                board.phase == TrainGamePhase.RoundEnd -> state.t("train.roundOver")
                board.doubleTrain != null -> state.t("train.coverDouble", mapOf("n" to (board.doublePip ?: 0)))
                humanTurn -> state.t("train.yourTurn")
                else -> state.t("train.botTurn", mapOf("name" to board.players[board.turn].name))
            },
            alert = board.phase != TrainGamePhase.Playing || board.doubleTrain != null,
        )
        Spacer(Modifier.height(4.dp))

        // Tren público y hub.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6E4A32)) {
                Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.t("train.hub"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF8E7C8))
                    TrainTileView(com.mesaviva.app.games.DominoTile(board.hubPip, board.hubPip), small = true)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("train.public"), style = MaterialTheme.typography.labelSmall)
                if (board.publicTrain.tiles.isEmpty()) {
                    Text(state.t("train.empty"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(board.publicTrain.tiles.size) { index ->
                            TrainTileView(board.publicTrain.tiles[index], small = true)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        // Trenes privados.
        board.trains.forEach { train ->
            val owner = board.players[train.owner]
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (train.owner == board.turn) Color(0x221E88E5) else Color.Transparent)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(105.dp)) {
                    Text(
                        owner.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (train.owner == board.turn) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        (if (train.open) state.t("train.open") else state.t("train.closed")) + " · ${owner.hand.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                }
                if (train.tiles.isEmpty()) {
                    Text(state.t("train.empty"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(train.tiles.size) { index -> TrainTileView(train.tiles[index], small = true) }
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(state.t("train.chooseTarget"), style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val options = buildList {
                add(board.turn to state.t("train.mine"))
                add(-1 to state.t("train.public"))
                board.trains.indices.forEach { index ->
                    if (index != board.turn && board.trains[index].open) add(index to "${state.t("train.train")} ${index + 1}")
                }
            }
            options.forEach { (value, label) ->
                FilterChip(
                    selected = target == value,
                    onClick = { target = value },
                    enabled = board.doubleTrain == null || board.doubleTrain == value,
                    label = { Text(label, fontSize = 10.sp) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Text(
            state.t("train.hand", mapOf("name" to (if (overlapHumans) board.players[viewSeat].name else state.t("train.you")))),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(hand.size) { index ->
                val tile = hand[index]
                TrainTileView(
                    tile = tile,
                    selected = selected == index,
                    onClick = {
                        if (humanTurn && board.phase == TrainGamePhase.Playing) {
                            selected = if (selected == index) null else index
                            NativeAudio.play(context, NativeAudio.Sfx.Tap, state.sfx)
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val index = selected ?: return@Button
                    apply(game.play(board, index, target), NativeAudio.Sfx.Card)
                },
                enabled = humanTurn && selectedTile != null && target in playable && board.phase == TrainGamePhase.Playing,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("train.play")) }
            OutlinedButton(
                onClick = { apply(game.draw(board), NativeAudio.Sfx.Draw) },
                enabled = humanTurn && board.phase == TrainGamePhase.Playing && board.boneyardCount > 0,
            ) { Text(state.t("train.draw")) }
            OutlinedButton(
                onClick = { apply(game.pass(board)) },
                enabled = humanTurn && board.phase == TrainGamePhase.Playing,
            ) { Text(state.t("train.pass")) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
        }
        if (board.phase != TrainGamePhase.Playing) {
            OutlinedButton(onClick = { resultOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.t("train.seeResult"))
            }
            Spacer(Modifier.height(4.dp))
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
        Text(
            state.t("train.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.size > 1) setupOpen = false },
            title = { Text(state.t("train.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = overlapHumans, onCheckedChange = { overlapHumans = it })
                    }
                    Text(state.t("train.players"), style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(2, 3, 4, 5, 6, 7, 8).forEach { count ->
                            FilterChip(
                                selected = playerCount == count,
                                onClick = { playerCount = count },
                                label = { Text(count.toString(), fontSize = 11.sp) },
                            )
                        }
                    }
                    Text(
                        state.t("train.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = { TextButton(onClick = { setupOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            board.history,
            pdfTitle = state.t("train.title"),
            pdfSubtitle = state.t("train.subtitle"),
            stats = listOf(
                state.t("train.round") to "${board.round}/3",
                state.t("train.boneyard") to board.boneyardCount.toString(),
                state.t("train.public") to board.publicTrain.tiles.size.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen) {
        val final = board.phase == TrainGamePhase.MatchEnd
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = {
                Text(
                    when {
                        final && board.winner == 0 -> state.t("train.youWon")
                        final -> state.t("train.wonBy", mapOf("name" to board.players.getOrNull(board.winner ?: 0)?.name.orEmpty()))
                        else -> state.t("train.roundOver")
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.players.sortedBy { it.score }.forEach { player ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(player.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text("${player.score}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (final) state.t("train.finalHint") else state.t("train.roundHint"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                if (final) {
                    Button(onClick = { resultOpen = false; setupOpen = true }) { Text(state.t("common.newMatch")) }
                } else {
                    Button(onClick = {
                        val next = game.nextRound(board)
                        board = next
                        selected = null
                        target = next.turn
                        if (next.phase == TrainGamePhase.MatchEnd) {
                            if (!overlapHumans && !awarded) {
                                awarded = true
                                state.awardMatch(win = next.winner == 0)
                            }
                            NativeAudio.play(context, if (next.winner == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
                            resultOpen = true
                        } else {
                            resultOpen = false
                        }
                    }) { Text(state.t(if (board.round >= 3) "train.finish" else "train.nextRound")) }
                }
            },
            dismissButton = { TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) } },
        )
    }
}
