package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.mesaviva.app.games.ChickenResult
import com.mesaviva.app.games.ChickenState
import com.mesaviva.app.games.ChickenfootGame
import com.mesaviva.app.games.DominoTile
import com.mesaviva.app.games.TrainGamePhase
import kotlinx.coroutines.delay

@Composable
fun ChickenfootScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var playerCount by remember { mutableStateOf(4) }
    var game by remember { mutableStateOf(ChickenfootGame(4)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf(0) }

    val viewSeat = if (allHumans) board.turn else 0
    val humanTurn = allHumans || board.turn == 0
    val hand = board.players.getOrNull(viewSeat)?.hand.orEmpty()
    val selectedTile = selected?.let { hand.getOrNull(it) }
    val playable = selectedTile?.let { game.playableTargets(board, it) }.orEmpty()

    fun start() {
        game = ChickenfootGame(playerCount)
        board = game.newMatch()
        selected = null
        target = 0
        awarded = false
        setupOpen = false
        resultOpen = false
    }

    fun apply(result: ChickenResult, sound: NativeAudio.Sfx = NativeAudio.Sfx.Card) {
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
                if (!allHumans) state.awardMatch(win = board.winner == 0)
                NativeAudio.play(context, if (board.winner == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
            }
            resultOpen = true
        }
    }

    LaunchedEffect(board.turn, board.phase, allHumans) {
        if (allHumans || board.phase != TrainGamePhase.Playing || board.turn == 0) return@LaunchedEffect
        delay(650)
        val next = game.botStep(board) ?: return@LaunchedEffect
        apply(ChickenResult(next), if (next.foot != null) NativeAudio.Sfx.Promote else NativeAudio.Sfx.Card)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        NativeGameHeader(
            state = state,
            title = state.t("chicken.title"),
            subtitle = state.t("chicken.subtitle"),
            badge = "${board.round}/3 · 🐔 ${board.boneyardCount}",
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == TrainGamePhase.MatchEnd -> state.t("chicken.matchOver")
                board.phase == TrainGamePhase.RoundEnd -> state.t("chicken.roundOver")
                board.foot != null -> state.t("chicken.foot", mapOf("n" to board.foot!!.pip))
                humanTurn -> state.t("chicken.yourTurn")
                else -> state.t("chicken.botTurn", mapOf("name" to board.players[board.turn].name))
            },
            alert = board.phase != TrainGamePhase.Playing || board.foot != null,
        )
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6E4A32)) {
                Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.t("chicken.hub"), style = MaterialTheme.typography.labelSmall, color = Color(0xFFF8E7C8))
                    TrainTileView(DominoTile(board.hubPip, board.hubPip), small = true)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("chicken.board"), style = MaterialTheme.typography.labelSmall)
                if (board.foot != null) {
                    Text(
                        state.t("chicken.footHint", mapOf("n" to board.foot!!.pip)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        if (board.foot != null) {
            val foot = board.foot!!
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                foot.branches.forEachIndexed { index, branch ->
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${state.t("chicken.toe")} ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (target == index) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (branch.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ) {
                                Text(
                                    state.t("chicken.need", mapOf("n" to foot.pip)),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(branch.size) { i -> TrainTileView(branch[i], small = true) }
                            }
                        }
                    }
                }
            }
        } else {
            board.branches.forEachIndexed { index, branch ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (target == index) Color(0x221E88E5) else Color.Transparent)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.width(82.dp)) {
                        Text("${state.t("chicken.branch")} ${index + 1}", style = MaterialTheme.typography.labelSmall)
                        Text(
                            state.t("chicken.end", mapOf("n" to branch.openEnd(board.hubPip))),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                        )
                    }
                    if (branch.tiles.isEmpty()) {
                        Text(state.t("chicken.empty"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(branch.tiles.size) { i -> TrainTileView(branch.tiles[i], small = true) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(state.t("chicken.chooseBranch"), style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val targets = when {
                board.foot != null -> board.foot!!.branches.indices.filter { board.foot!!.branches[it].isEmpty() }
                else -> board.branches.indices
            }
            targets.forEach { index ->
                FilterChip(
                    selected = target == index,
                    onClick = { target = index },
                    label = {
                        Text(
                            if (board.foot != null) "${state.t("chicken.toe")} ${index + 1}" else "${state.t("chicken.branch")} ${index + 1}",
                            fontSize = 10.sp,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Text(
            state.t("chicken.hand", mapOf("name" to (if (allHumans) board.players[viewSeat].name else state.t("chicken.you")))),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(hand.size) { index ->
                TrainTileView(
                    tile = hand[index],
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
            ) { Text(state.t("chicken.play")) }
            OutlinedButton(
                onClick = { apply(game.draw(board), NativeAudio.Sfx.Draw) },
                enabled = humanTurn && board.phase == TrainGamePhase.Playing && board.boneyardCount > 0,
            ) { Text(state.t("chicken.draw")) }
            OutlinedButton(
                onClick = { apply(game.pass(board)) },
                enabled = humanTurn && board.phase == TrainGamePhase.Playing,
            ) { Text(state.t("chicken.pass")) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
        }
        if (board.phase != TrainGamePhase.Playing) {
            OutlinedButton(onClick = { resultOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.t("chicken.seeResult"))
            }
            Spacer(Modifier.height(4.dp))
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
        Text(
            state.t("chicken.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.size > 1) setupOpen = false },
            title = { Text(state.t("chicken.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(state.t("chicken.players"), style = MaterialTheme.typography.labelSmall)
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
                        state.t("chicken.rules"),
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
            pdfTitle = state.t("chicken.title"),
            pdfSubtitle = state.t("chicken.subtitle"),
            stats = listOf(
                state.t("chicken.round") to "${board.round}/3",
                state.t("chicken.boneyard") to board.boneyardCount.toString(),
                state.t("chicken.branches") to board.branches.size.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen) {
        val final = board.phase == TrainGamePhase.MatchEnd
        val winnerName = board.players.getOrNull(board.winner ?: 0)?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = {
                Text(
                    when {
                        final && board.winner == 0 -> state.t("chicken.youWon")
                        final -> state.t("chicken.wonBy", mapOf("name" to winnerName))
                        else -> state.t("chicken.roundOver")
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
                        if (final) state.t("chicken.finalHint") else state.t("chicken.roundHint"),
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
                        target = 0
                        if (next.phase == TrainGamePhase.MatchEnd) {
                            if (!allHumans && !awarded) {
                                awarded = true
                                state.awardMatch(win = next.winner == 0)
                            }
                            NativeAudio.play(context, if (next.winner == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
                            resultOpen = true
                        } else {
                            resultOpen = false
                        }
                    }) { Text(state.t(if (board.round >= 3) "chicken.finish" else "chicken.nextRound")) }
                }
            },
            dismissButton = { TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) } },
        )
    }
}