package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.PokerAction
import com.mesaviva.app.games.PokerCard
import com.mesaviva.app.games.PokerGame
import com.mesaviva.app.games.PokerStreet
import com.mesaviva.app.games.PokerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun PokerScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var players by remember { mutableIntStateOf(4) }
    var chips by remember { mutableIntStateOf(800) }
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(PokerGame(4, 800)) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var raiseOpen by remember { mutableStateOf(false) }
    var raiseAmount by remember { mutableIntStateOf(40) }
    var handEndOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var effect by remember { mutableIntStateOf(0) }

    val humanTurn = board.street in listOf(PokerStreet.Preflop, PokerStreet.Flop, PokerStreet.Turn, PokerStreet.River) &&
        board.seats.getOrNull(board.turn)?.isBot == false
    val viewerSeat = if (allHumans) board.turn else 0
    val hero = board.players.firstOrNull { it.seat == viewerSeat }
    val heroHand = if (hero != null && hero.hole.size == 2) {
        game.evaluateHand(hero.hole + board.community)
    } else {
        null
    }
    val toCall = max(0, board.currentBet - (hero?.bet ?: 0))
    val callAmount = min(toCall, hero?.chips ?: 0)
    val maxRaise = (hero?.bet ?: 0) + (hero?.chips ?: 0)
    val minRaise = min(maxRaise, board.currentBet + board.minRaise)

    fun start() {
        game = PokerGame(players, chips, allHumans)
        board = game.newMatch()
        setupOpen = false
        historyOpen = false
        handEndOpen = false
        resultOpen = false
        effect = 0
    }

    fun apply(action: PokerAction) {
        val res = game.act(board, action)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        raiseOpen = false
        effect++
        state.vibrate(context, if (board.street == PokerStreet.MatchEnd) 70 else 22)
        NativeAudio.play(
            context,
            if (board.street == PokerStreet.MatchEnd) NativeAudio.Sfx.Win else NativeAudio.Sfx.Card,
            state.sfx,
        )
        if (board.street == PokerStreet.HandEnd) handEndOpen = true
        if (board.street == PokerStreet.MatchEnd) resultOpen = true
    }

    fun nextHand() {
        val res = game.nextHand(board)
        if (res.error != null) {
            Toast.makeText(context, state.t(res.error), Toast.LENGTH_SHORT).show()
            return
        }
        board = res.state
        handEndOpen = false
        resultOpen = false
        effect++
        NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
    }

    LaunchedEffect(board.turn, board.street, board.handNo, board.history.size, allHumans, difficulty) {
        val player = board.players.getOrNull(board.turn) ?: return@LaunchedEffect
        if (board.seats.getOrNull(player.seat)?.isBot != true) return@LaunchedEffect
        if (board.street !in listOf(PokerStreet.Preflop, PokerStreet.Flop, PokerStreet.Turn, PokerStreet.River)) return@LaunchedEffect
        delay(650)
        val action = withContext(Dispatchers.Default) { game.botAction(board, difficulty) }
        if (action != null) apply(action)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("poker.title"),
            subtitle = state.t("poker.subtitle"),
            badge = state.t("poker.badge", mapOf("n" to board.handNo, "s" to state.t("poker.street.${board.street.name.lowercase()}"))),
            onBack = onBack,
        )

        NativeTurnBanner(
            text = when {
                board.street == PokerStreet.MatchEnd -> pokerResultText(state, board)
                board.street == PokerStreet.HandEnd -> state.t("poker.handEnd")
                hero?.folded == true -> state.t("poker.youFolded")
                humanTurn -> if (toCall > 0) state.t("poker.yourTurnCall", mapOf("n" to callAmount)) else state.t("poker.yourTurn")
                else -> state.t("poker.waitTurn", mapOf("p" to (board.seats.getOrNull(board.turn)?.name ?: "—")))
            },
            alert = board.street == PokerStreet.HandEnd || board.street == PokerStreet.MatchEnd,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            board.players.forEach { player ->
                val seat = board.seats[player.seat]
                Surface(
                    modifier = Modifier.width(96.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = when {
                        player.seat == board.turn && board.street in listOf(PokerStreet.Preflop, PokerStreet.Flop, PokerStreet.Turn, PokerStreet.River) ->
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        player.folded || player.out -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                    },
                ) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (player.seat == 0) state.t("common.you") else seat.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (player.seat == board.turn) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            state.t("poker.chips", mapOf("n" to player.chips)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            if (player.out) state.t("poker.out")
                            else if (player.allIn) state.t("poker.allIn")
                            else if (player.folded) state.t("poker.folded")
                            else state.t("poker.bet", mapOf("n" to player.bet)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(2) {
                                PokerCardFace(
                                    card = PokerCard("back-${player.seat}-$it", com.mesaviva.app.games.PokerSuit.Spades, 2),
                                    modifier = Modifier.size(22.dp, 31.dp),
                                    faceDown = player.seat != viewerSeat,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color(0xFF0E3B34), Color(0xFF08201D)),
                    ),
                ),
        ) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        state.t("poker.pot", mapOf("n" to game.potTotal(board))),
                        color = Color(0xFFE0A63A),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.t("poker.blinds", mapOf("s" to board.smallBlind, "b" to board.bigBlind)),
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 5) {
                        val card = board.community.getOrNull(i)
                        if (card != null) {
                            PokerCardFace(card, Modifier.size(52.dp, 75.dp))
                        } else {
                            Box(
                                Modifier
                                    .size(52.dp, 75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                            )
                        }
                    }
                }
                Text(
                    state.t("poker.tableBet", mapOf("n" to board.currentBet)),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TableDust(effect, Modifier.fillMaxSize(), color = Color(0xFFE0C28A))
        }

        Spacer(Modifier.height(5.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    hero?.hole?.forEach { card ->
                        PokerCardFace(card, Modifier.size(48.dp, 70.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        state.t("poker.yourChips", mapOf("n" to (hero?.chips ?: 0))),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        heroHand?.let { state.t("poker.hand", mapOf("h" to state.t(it.nameKey))) }
                            ?: state.t("poker.waiting"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.t("poker.yourBet", mapOf("n" to (hero?.bet ?: 0))),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (humanTurn && hero != null && !hero.folded && !hero.allIn) {
                OutlinedButton(onClick = { apply(PokerAction.Fold) }) {
                    Text(state.t("poker.fold"))
                }
                if (toCall == 0) {
                    Button(onClick = { apply(PokerAction.Check) }, modifier = Modifier.weight(1f)) {
                        Text(state.t("poker.check"))
                    }
                } else {
                    Button(onClick = { apply(PokerAction.Call) }, modifier = Modifier.weight(1f)) {
                        Text("${state.t("poker.call")} ${callAmount}")
                    }
                }
                if (maxRaise > board.currentBet && (hero.chips > 0)) {
                    OutlinedButton(onClick = {
                        raiseAmount = minRaise.coerceAtMost(maxRaise)
                        raiseOpen = true
                    }) {
                        Text(state.t("poker.raise"))
                    }
                }
                Button(onClick = { apply(PokerAction.AllIn) }) {
                    Text(state.t("poker.allIn"))
                }
            } else {
                OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                    Text(state.t("common.history"))
                }
                OutlinedButton(onClick = { setupOpen = true }) { Text(state.t("common.newMatch")) }
            }
        }

        if (board.street == PokerStreet.HandEnd) {
            Button(onClick = { nextHand() }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(state.t("poker.nextHand"))
            }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("poker.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("poker.players"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(2, 3, 4, 5, 6).forEach { value ->
                            FilterChip(
                                selected = players == value,
                                onClick = { players = value },
                                label = { Text("$value") },
                            )
                        }
                    }
                    Text(state.t("poker.startingChips"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(400, 800, 1500).forEach { value ->
                            FilterChip(
                                selected = chips == value,
                                onClick = { chips = value },
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
                        state.t("poker.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = { TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (raiseOpen) {
        AlertDialog(
            onDismissRequest = { raiseOpen = false },
            title = { Text(state.t("poker.raise")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("poker.raiseRange", mapOf("min" to minRaise, "max" to maxRaise)))
                    Text("$raiseAmount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Slider(
                        value = raiseAmount.toFloat(),
                        onValueChange = { raiseAmount = (it / 10).toInt() * 10 },
                        valueRange = minRaise.toFloat()..maxRaise.toFloat(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { raiseAmount = minRaise }) { Text("min") }
                        OutlinedButton(onClick = { raiseAmount = maxRaise }) { Text("max") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { apply(PokerAction.Raise(raiseAmount)) }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { raiseOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (handEndOpen && board.street == PokerStreet.HandEnd) {
        AlertDialog(
            onDismissRequest = { handEndOpen = false },
            title = { Text(state.t("poker.handEnd")) },
            text = { PokerShowdownPanel(state, board) },
            confirmButton = { TextButton(onClick = { nextHand() }) { Text(state.t("poker.nextHand")) } },
            dismissButton = { TextButton(onClick = { handEndOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (historyOpen) NativeHistoryDialog(
        state,
        board.history,
        pdfTitle = state.t("poker.title"),
        pdfSubtitle = state.t("poker.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.street == PokerStreet.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(pokerResultText(state, board)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    board.players.forEach { player ->
                        Text(
                            "${if (player.seat == 0) state.t("common.you") else board.seats[player.seat].name}: ${player.chips} fichas",
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
private fun PokerShowdownPanel(state: ShellState, board: PokerState) {
    val showdown = board.showDown
    if (showdown == null) {
        board.handWinner?.let { winner ->
            val seat = board.seats.getOrNull(winner.seat)
            Text(
                state.t("poker.winner", mapOf("p" to (seat?.name ?: ""), "n" to winner.amount)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            state.t("poker.potWon", mapOf("n" to showdown.totalPot)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        showdown.rows.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (row.seat == 0) state.t("common.you") else board.seats[row.seat].name}: ${state.t("poker.hand", mapOf("h" to state.t(row.handKey)))} ${if (row.won > 0) "+${row.won}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                )
                row.best.take(5).forEach { card ->
                    Text(
                        "${PokerGame.rankLabel(card.rank)}${card.suit.symbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun pokerResultText(state: ShellState, board: PokerState): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && board.seats.getOrNull(board.winner ?: 0)?.isBot == false ->
        state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
