package com.mesaviva.app.shell

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.ChessColor
import com.mesaviva.app.games.ChessGame
import com.mesaviva.app.games.ChessMove
import com.mesaviva.app.games.ChessPhase
import com.mesaviva.app.games.ChessPiece
import com.mesaviva.app.games.ChessPieceType
import com.mesaviva.app.games.ChessState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val LIGHT_SQUARE = Color(0xFFE9D8B4)
private val DARK_SQUARE = Color(0xFF7B5A3C)
private val WHITE_PIECE = Color(0xFFFFF9E8)
private val BLACK_PIECE = Color(0xFF17130F)

@Composable
fun ChessScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    val game = remember { ChessGame() }
    var board by remember { mutableStateOf(game.initialState()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var promotion by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var difficulty by remember { mutableStateOf("normal") }
    var historyOpen by remember { mutableStateOf(false) }
    var resignOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }

    val allMoves = remember(board) { game.legalMoves(board) }
    val selectedMoves = remember(selected, allMoves) {
        if (selected == null) emptyList() else allMoves.filter { it.from == selected }
    }
    val checked = remember(board) { game.inCheck(board.board, board.turn) }
    val kingSquare = remember(board) { game.findKing(board.board, board.turn) }

    fun apply(from: Int, to: Int, promo: ChessPieceType? = null) {
        val next = game.move(board, from, to, promo) ?: return
        board = next
        selected = null
        promotion = null
        state.vibrate(context, 24)
        if (next.phase == ChessPhase.MatchEnd) resultOpen = true
    }

    fun onCell(index: Int) {
        if (board.phase != ChessPhase.Playing || board.turn != ChessColor.White) return
        if (selected != null) {
            val moves = selectedMoves.filter { it.to == index }
            if (moves.isNotEmpty()) {
                if (moves.any { it.promo != null }) {
                    promotion = selected!! to index
                } else {
                    apply(selected!!, index)
                }
                return
            }
        }
        if (board.board[index]?.color == ChessColor.White) {
            selected = index
            state.vibrate(context, 14)
        } else {
            selected = null
        }
    }

    LaunchedEffect(board.turn, board.phase, board.history.size) {
        if (board.phase == ChessPhase.Playing && board.turn == ChessColor.Black) {
            delay(450)
            val move = withContext(Dispatchers.Default) {
                game.botMove(board, difficulty, Random.Default)
            }
            if (move != null) {
                apply(move.from, move.to, move.promo)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("chess.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("chess.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                Text(
                    state.t("chess.move") + " ${board.fullmove}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (checked) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    board.phase == ChessPhase.MatchEnd -> chessOutcomeLabel(state, board.outcome?.reasonKey)
                    checked -> state.t("chess.check")
                    board.turn == ChessColor.White -> state.t("chess.yourTurn")
                    else -> state.t("chess.botTurn")
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
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
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 8) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 8) {
                            val index = row * 8 + col
                            val light = (row + col) % 2 == 0
                            val piece = board.board[index]
                            val isSelected = selected == index
                            val isTarget = selectedMoves.any { it.to == index }
                            val isCapture = piece != null && isTarget
                            val isLast = board.lastMove?.let { it.first == index || it.second == index } == true
                            val isCheckKing = checked && index == kingSquare
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            isCheckKing -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                                            isLast -> Color(0x44E0A63A)
                                            light -> LIGHT_SQUARE
                                            else -> DARK_SQUARE
                                        },
                                    )
                                    .clickable { onCell(index) }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(ChessGame.squareName(index))
                                            if (piece != null) {
                                                append(", ")
                                                append(if (piece.color == ChessColor.White) state.t("chess.white") else state.t("chess.black"))
                                                append(" ")
                                                append(pieceName(state, piece.type))
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (col == 0) {
                                    Text(
                                        "${8 - row}",
                                        fontSize = 7.sp,
                                        color = if (light) DARK_SQUARE.copy(alpha = 0.65f) else LIGHT_SQUARE.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                                    )
                                }
                                if (row == 7) {
                                    Text(
                                        "${('a' + col)}",
                                        fontSize = 7.sp,
                                        color = if (light) DARK_SQUARE.copy(alpha = 0.65f) else LIGHT_SQUARE.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                                    )
                                }
                                if (piece != null) {
                                    val isWhite = piece.color == ChessColor.White
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp)
                                            .background(
                                                if (isWhite) Color(0xFFFFF3D6) else Color(0xFF241C14),
                                                CircleShape,
                                            )
                                            .border(
                                                1.dp,
                                                if (isWhite) Color(0xFF4A3824) else Color(0xFF0B0806),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            ChessGame.GLYPHS[ChessColor.Black]?.get(piece.type) ?: "",
                                            fontSize = 26.sp,
                                            color = if (isWhite) Color(0xFF2B2115) else Color(0xFFF6E9CE),
                                            style = TextStyle(
                                                shadow = Shadow(
                                                    color = if (isWhite) Color(0x33000000) else Color(0x66000000),
                                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                                    blurRadius = 1f,
                                                ),
                                            ),
                                        )
                                    }
                                }
                                if (isTarget) {
                                    if (isCapture) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(4.dp)
                                                .border(3.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.8f), CircleShape),
                                        )
                                    } else {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "♙ ${board.board.count { it?.color == ChessColor.White }}  ·  ♟ ${board.board.count { it?.color == ChessColor.Black }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.t("chess.lastMoves"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (board.history.isEmpty()) {
                Text(
                    state.t("chess.noMoves"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                board.history.takeLast(5).forEachIndexed { index, san ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ) {
                        Text(
                            san,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("easy", "normal", "hard").forEach { value ->
                FilterChip(
                    selected = difficulty == value,
                    onClick = { difficulty = value },
                    label = { Text(state.t("chess.$value")) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(onClick = {
                board = game.initialState()
                selected = null
                promotion = null
                resultOpen = false
            }) { Text(state.t("chess.newGame")) }
            OutlinedButton(onClick = { historyOpen = true }) { Text(state.t("chess.history")) }
            Button(onClick = { resignOpen = true }, enabled = board.phase == ChessPhase.Playing) {
                Text(state.t("chess.resign"))
            }
        }
    }

    if (promotion != null) {
        AlertDialog(
            onDismissRequest = { promotion = null },
            title = { Text(state.t("chess.promotion")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.t("chess.choosePromotion"))
                    val options = listOf(
                        ChessPieceType.Queen to state.t("chess.queen"),
                        ChessPieceType.Rook to state.t("chess.rook"),
                        ChessPieceType.Bishop to state.t("chess.bishop"),
                        ChessPieceType.Knight to state.t("chess.knight"),
                    )
                    for (row in 0..1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (column in 0..1) {
                                val (type, label) = options[row * 2 + column]
                                Button(
                                    onClick = {
                                        val pair = promotion ?: return@Button
                                        apply(pair.first, pair.second, type)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { promotion = null }) { Text(state.t("common.cancel")) } },
        )
    }

    if (resignOpen) {
        AlertDialog(
            onDismissRequest = { resignOpen = false },
            title = { Text(state.t("chess.resign")) },
            text = { Text(state.t("chess.resignConfirm")) },
            confirmButton = {
                TextButton(onClick = {
                    resignOpen = false
                    board = game.resign(board)
                    resultOpen = true
                }) { Text(state.t("common.confirm")) }
            },
            dismissButton = {
                TextButton(onClick = { resignOpen = false }) { Text(state.t("common.cancel")) }
            },
        )
    }

    if (historyOpen) {
        AlertDialog(
            onDismissRequest = { historyOpen = false },
            title = { Text(state.t("chess.history")) },
            text = {
                if (board.history.isEmpty()) {
                    Text(state.t("chess.noMoves"))
                } else {
                    LazyColumn(Modifier.height(280.dp)) {
                        itemsIndexed(board.history) { index, san ->
                            Text(
                                state.t("chess.moveNumber", mapOf("n" to (index + 1), "san" to san)),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyOpen = false }) { Text(state.t("common.close")) } },
        )
    }

    if (resultOpen && board.phase == ChessPhase.MatchEnd) {
        val outcome = board.outcome
        val title = when {
            outcome?.winner == ChessColor.White -> state.t("chess.youWon")
            outcome?.winner == ChessColor.Black -> state.t("chess.botWon")
            else -> state.t("chess.draw")
        }
        AlertDialog(
            onDismissRequest = { resultOpen = false },
            title = { Text(title) },
            text = {
                Text(
                    chessOutcomeLabel(state, outcome?.reasonKey),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    resultOpen = false
                    board = game.initialState()
                    selected = null
                }) { Text(state.t("chess.newGame")) }
            },
            dismissButton = {
                TextButton(onClick = { resultOpen = false }) { Text(state.t("common.close")) }
            },
        )
    }
}

private fun chessOutcomeLabel(state: ShellState, key: String?): String = when (key) {
    "result.reason.checkmate" -> state.t("chess.checkmate")
    "result.reason.resign" -> state.t("chess.result.resign")
    "result.reason.stalemate" -> state.t("chess.stalemate")
    "result.reason.fiftyMoves", "result.reason.repetition", "result.reason.material", "result.reason.draw" -> state.t("chess.draw")
    else -> state.t("chess.draw")
}

private fun pieceName(state: ShellState, type: ChessPieceType): String = when (type) {
    ChessPieceType.Pawn -> if (state.language == "en") "pawn" else "peón"
    ChessPieceType.Knight -> state.t("chess.knight")
    ChessPieceType.Bishop -> state.t("chess.bishop")
    ChessPieceType.Rook -> state.t("chess.rook")
    ChessPieceType.Queen -> state.t("chess.queen")
    ChessPieceType.King -> if (state.language == "en") "king" else "rey"
}
