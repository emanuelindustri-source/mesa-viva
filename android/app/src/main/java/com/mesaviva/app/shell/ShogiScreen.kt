package com.mesaviva.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.ShogiGame
import com.mesaviva.app.games.ShogiKind
import com.mesaviva.app.games.ShogiMove
import com.mesaviva.app.games.ShogiPhase
import com.mesaviva.app.games.ShogiPiece
import com.mesaviva.app.games.ShogiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ShogiScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    var allHumans by remember { mutableStateOf(false) }
    var difficulty by remember { mutableStateOf("normal") }
    var game by remember { mutableStateOf(ShogiGame()) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var dropKind by remember { mutableStateOf<ShogiKind?>(null) }
    var pendingPromotion by remember { mutableStateOf<ShogiMove?>(null) }
    var awarded by remember { mutableStateOf(false) }

    val humanTurn = allHumans || board.turn == 0
    val handSeat = if (allHumans) board.turn else 0
    val legal = remember(board, humanTurn) {
        if (board.phase == ShogiPhase.Play && humanTurn) game.legalMoves(board) else emptyList()
    }
    val selectedMoves = remember(selected, legal) {
        if (selected == null) emptyList() else legal.filter { it.from == selected }
    }
    val targets = remember(selectedMoves) { selectedMoves.map { it.to }.toSet() }
    val dropTargets = remember(dropKind, legal) {
        dropKind?.let { kind -> legal.filter { it.drop == kind }.map { it.to }.toSet() } ?: emptySet()
    }

    fun start() {
        game = ShogiGame()
        board = game.newMatch()
        selected = null
        dropKind = null
        pendingPromotion = null
        setupOpen = false
        historyOpen = false
        resultOpen = false
        awarded = false
    }

    fun finishIfNeeded(current: ShogiState) {
        if (current.phase != ShogiPhase.MatchEnd || awarded) return
        awarded = true
        val won = current.winner == 0
        state.awardMatch(win = won, draw = current.draw)
        NativeAudio.play(
            context,
            when {
                current.draw -> NativeAudio.Sfx.Draw
                won -> NativeAudio.Sfx.Win
                else -> NativeAudio.Sfx.Lose
            },
            state.sfx,
        )
        resultOpen = true
    }

    fun applyMove(move: ShogiMove) {
        val wasCapture = board.board.getOrNull(move.to) != null && move.drop == null
        val result = game.move(board, move)
        if (result.error != null) {
            // Si el movimiento requiere promoción, la mesa pregunta.
            selected = null
            dropKind = null
            return
        }
        board = result.state
        selected = null
        dropKind = null
        pendingPromotion = null
        NativeAudio.play(
            context,
            when {
                result.state.phase == ShogiPhase.MatchEnd && result.state.winner == 0 -> NativeAudio.Sfx.Win
                result.state.phase == ShogiPhase.MatchEnd -> NativeAudio.Sfx.Lose
                wasCapture -> NativeAudio.Sfx.Capture
                result.state.lastMove?.promote == true -> NativeAudio.Sfx.Promote
                else -> NativeAudio.Sfx.Card
            },
            state.sfx,
        )
        state.vibrate(context, if (wasCapture) 42 else 20)
        finishIfNeeded(result.state)
    }

    fun attempt(move: ShogiMove) {
        val options = legal.filter { it.from == move.from && it.to == move.to }
        if (options.size > 1 && options.any { it.promote }) {
            pendingPromotion = move
        } else {
            applyMove(move)
        }
    }

    fun tapCell(index: Int) {
        if (!humanTurn || board.phase != ShogiPhase.Play) return
        if (dropKind != null) {
            if (index in dropTargets) {
                applyMove(ShogiMove(null, index, drop = dropKind))
            } else {
                dropKind = null
            }
            return
        }
        if (selected != null) {
            val move = selectedMoves.firstOrNull { it.to == index }
            if (move != null) {
                attempt(move)
                return
            }
        }
        val piece = board.board.getOrNull(index)
        if (piece != null && piece.owner == board.turn) {
            selected = if (selected == index) null else index
            dropKind = null
            state.vibrate(context, 12)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size, allHumans, difficulty) {
        if (allHumans || board.phase != ShogiPhase.Play || board.turn != 1) return@LaunchedEffect
        delay(520)
        val move = withContext(Dispatchers.Default) { game.botMove(board, difficulty) } ?: return@LaunchedEffect
        val result = game.move(board, move)
        if (result.error == null) {
            board = result.state
            NativeAudio.play(context, NativeAudio.Sfx.Card, state.sfx)
            finishIfNeeded(result.state)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        NativeGameHeader(
            state = state,
            title = state.t("shogi.title"),
            subtitle = state.t("shogi.subtitle"),
            badge = if (board.inCheck) state.t("shogi.check") else state.t("shogi.badge"),
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == ShogiPhase.MatchEnd -> shogiResultText(state, board, allHumans)
                board.inCheck && humanTurn -> state.t("shogi.yourCheck")
                !humanTurn -> state.t("shogi.botTurn")
                allHumans -> state.t("shogi.turnPlayer", mapOf("p" to (board.turn + 1)))
                else -> state.t("shogi.yourTurn")
            },
            alert = board.inCheck || board.phase == ShogiPhase.MatchEnd,
        )

        HandRow(
            state = state,
            hand = board.hands.getOrElse(1 - handSeat) { emptyMap() },
            title = state.t("shogi.opponentHand"),
            enabled = false,
            selected = null,
            onSelect = {},
        )

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(2.dp, Color(0xFF6E4A32), RoundedCornerShape(10.dp))
                .background(Color(0xFFE9D8B4)),
        ) {
            val cell = maxWidth / 9
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 9) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 9) {
                            val index = row * 9 + col
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val isTarget = index in targets || index in dropTargets
                            val isLast = board.lastMove?.to == index || board.lastMove?.from == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .border(0.5.dp, Color(0xFF8A6A45))
                                    .background(
                                        when {
                                            isSelected -> Color(0x66E0A63A)
                                            isTarget -> Color(0x554FA36B)
                                            isLast -> Color(0x44E0A63A)
                                            else -> Color(0xFFE9D8B4)
                                        },
                                    )
                                    .clickable { tapCell(index) }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(ShogiGame.square(index))
                                            if (piece == null) append(", vacía")
                                            else append(", ${ShogiGame.label(piece)} de J${piece.owner + 1}")
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece != null) {
                                    Text(
                                        ShogiGame.label(piece),
                                        color = if (piece.owner == 0) Color(0xFF8B2E23) else Color(0xFF1F2933),
                                        fontSize = (cell.value * 0.44f).sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    )
                                } else if (isTarget) {
                                    Box(
                                        Modifier
                                            .size(cell * 0.22f)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF2E7D52)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HandRow(
            state = state,
            hand = board.hands.getOrElse(handSeat) { emptyMap() },
            title = state.t("shogi.yourHand"),
            enabled = humanTurn && board.phase == ShogiPhase.Play,
            selected = dropKind,
            onSelect = { kind ->
                dropKind = if (dropKind == kind) null else kind
                selected = null
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = { setupOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.newMatch"))
            }
            OutlinedButton(onClick = { historyOpen = true }, modifier = Modifier.weight(1f)) {
                Text(state.t("common.history"))
            }
            Button(
                onClick = { resignOpen = true },
                enabled = board.phase == ShogiPhase.Play,
                modifier = Modifier.weight(1f),
            ) { Text(state.t("common.resign")) }
        }

        NativeMoveLog(state, board.history) { historyOpen = true }
    }

    if (pendingPromotion != null) {
        val move = pendingPromotion!!
        AlertDialog(
            onDismissRequest = { pendingPromotion = null },
            title = { Text(state.t("shogi.promoteTitle")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("shogi.promoteBody"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { move.promote.let { applyMove(move.copy(promote = true)) } }) {
                            Text(state.t("shogi.promoteYes"))
                        }
                        OutlinedButton(onClick = { applyMove(move.copy(promote = false)) }) {
                            Text(state.t("shogi.promoteNo"))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pendingPromotion = null }) { Text(state.t("common.cancel")) } },
        )
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("shogi.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.t("shogi.difficulty"), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("easy", "normal", "hard").forEach { value ->
                            FilterChip(
                                selected = difficulty == value,
                                onClick = { difficulty = value },
                                label = { Text(value) },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("common.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        state.t("shogi.rules"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { Button(onClick = { start() }) { Text(state.t("common.start")) } },
            dismissButton = {
                TextButton(onClick = { if (board.history.isNotEmpty()) setupOpen = false }) {
                    Text(state.t("common.close"))
                }
            },
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
                    awarded = true
                    resultOpen = true
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = { TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) } },
        )
    }

    if (historyOpen) NativeHistoryDialog(
        state,
        board.history,
        pdfTitle = state.t("shogi.title"),
        pdfSubtitle = state.t("shogi.subtitle"),
    ) { historyOpen = false }

    if (resultOpen && board.phase == ShogiPhase.MatchEnd) {
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(shogiResultText(state, board, allHumans)) },
            text = {
                Text(
                    state.t("shogi.resultBody", mapOf("n" to board.history.size)),
                    style = MaterialTheme.typography.bodySmall,
                )
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
private fun HandRow(
    state: ShellState,
    hand: Map<ShogiKind, Int>,
    title: String,
    enabled: Boolean,
    selected: ShogiKind?,
    onSelect: (ShogiKind) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ShogiGame.HAND_ORDER.forEach { kind ->
            val count = hand[kind] ?: 0
            if (count <= 0) return@forEach
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (selected == kind) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = if (enabled) Modifier.clickable { onSelect(kind) } else Modifier,
            ) {
                Text(
                    "${kind.kanji}×$count",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (hand.values.sum() == 0) {
            Text(state.t("shogi.emptyHand"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun shogiResultText(state: ShellState, board: ShogiState, allHumans: Boolean): String = when {
    board.draw -> state.t("common.draw")
    board.winner == 0 -> state.t("common.youWon")
    board.winner != null && allHumans -> state.t("common.playerNWon", mapOf("n" to ((board.winner ?: 0) + 1)))
    board.winner != null -> state.t("common.botWon")
    else -> state.t("common.matchOver")
}
