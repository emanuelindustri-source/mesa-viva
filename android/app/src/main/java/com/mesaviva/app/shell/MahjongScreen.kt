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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.MahjongClaim
import com.mesaviva.app.games.MahjongClaimKind
import com.mesaviva.app.games.MahjongGame
import com.mesaviva.app.games.MahjongMeldKind
import com.mesaviva.app.games.MahjongPhase
import com.mesaviva.app.games.MahjongPlayer
import com.mesaviva.app.games.MahjongState
import com.mesaviva.app.games.MahjongSuit
import com.mesaviva.app.games.MahjongTile
import com.mesaviva.app.games.MahjongVariant
import kotlinx.coroutines.delay

private val MJ_IVORY = Color(0xFFF8F1DE)
private val MJ_EDGE = Color(0xFF9C8E70)
private val MJ_GREEN = Color(0xFF1F6B4A)
private val MJ_RED = Color(0xFFB43B3B)
private val MJ_BLUE = Color(0xFF2F5FA8)
private val MJ_BACK = Color(0xFF1D5540)

@Composable
fun MahjongScreen(
    state: ShellState,
    variant: MahjongVariant,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val game = remember(variant) { MahjongGame(variant) }
    var allHumans by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(game.newMatch()) }
    var setupOpen by remember { mutableStateOf(true) }
    var historyOpen by remember { mutableStateOf(false) }
    var resultOpen by remember { mutableStateOf(false) }
    var awarded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }

    val viewSeat = if (allHumans) board.turn else 0
    val humanTurn = allHumans || board.turn == 0
    val visibleHand = board.players.getOrNull(viewSeat)?.hand.orEmpty()
    val claims = if (allHumans) board.pending else board.pending.filter { it.seat == 0 }
    val title = when (variant) {
        MahjongVariant.American -> state.t("mahjong.american")
        MahjongVariant.Riichi -> state.t("mahjong.riichi")
        MahjongVariant.Cards -> state.t("mahjong.cards")
    }

    fun start() {
        board = game.newMatch()
        awarded = false
        selected = null
        setupOpen = false
        resultOpen = false
    }

    fun showError(key: String?) {
        if (key != null) Toast.makeText(context, state.t(key), Toast.LENGTH_SHORT).show()
    }

    fun apply(result: com.mesaviva.app.games.MahjongResult, sound: NativeAudio.Sfx = NativeAudio.Sfx.Card) {
        if (result.error != null) {
            showError(result.error)
            return
        }
        board = result.state
        NativeAudio.play(context, sound, state.sfx)
        state.vibrate(context, 18)
        selected = null
        if (board.phase == MahjongPhase.HandEnd) {
            resultOpen = true
        }
        if (board.phase == MahjongPhase.MatchEnd && !awarded) {
            awarded = true
            if (!allHumans) state.awardMatch(win = game.matchWinner(board) == 0)
            resultOpen = true
        }
    }

    fun discardSelected() {
        if (!humanTurn || selected == null || board.phase != MahjongPhase.Play || board.pending.isNotEmpty()) return
        apply(game.discard(board, selected!!))
    }

    // Turno automático de los bots y resolución de sus reclamaciones.
    LaunchedEffect(board.turn, board.drawn?.id, board.pending, board.phase, allHumans) {
        if (allHumans || board.phase != MahjongPhase.Play) return@LaunchedEffect
        if (board.pending.isNotEmpty()) {
            if (board.pending.any { it.seat == 0 }) return@LaunchedEffect
            delay(500)
            val bot = game.botClaim(board)
            if (bot != null) apply(game.claim(board, bot), NativeAudio.Sfx.Promote) else apply(game.pass(board))
            return@LaunchedEffect
        }
        if (board.turn == 0) return@LaunchedEffect
        delay(620)
        if (board.phase != MahjongPhase.Play) return@LaunchedEffect
        val tsumo = game.declareTsumo(board)
        if (tsumo.error == null) {
            apply(tsumo, NativeAudio.Sfx.Win)
            return@LaunchedEffect
        }
        val tile = game.botDiscard(board) ?: return@LaunchedEffect
        apply(game.discard(board, tile))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        NativeGameHeader(
            state = state,
            title = title,
            subtitle = state.t("mahjong.subtitle"),
            badge = "${board.round}/4 · 🀫 ${board.wallCount}",
            onBack = onBack,
        )
        NativeTurnBanner(
            text = when {
                board.phase == MahjongPhase.HandEnd -> state.t("mahjong.handOver")
                board.phase == MahjongPhase.MatchEnd -> state.t("mahjong.matchOver")
                claims.isNotEmpty() -> state.t("mahjong.claimPrompt")
                humanTurn -> state.t("mahjong.yourTurn")
                else -> state.t("mahjong.botTurn", mapOf("name" to MahjongGame.BOT_NAMES[board.turn - 1]))
            },
            alert = claims.isNotEmpty() || board.phase != MahjongPhase.Play,
        )
        Spacer(Modifier.height(5.dp))

        // Rivales.
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            board.players.forEachIndexed { seat, player ->
                if (seat == viewSeat) return@forEachIndexed
                MahjongOpponentRow(state, player, game)
            }
        }
        Spacer(Modifier.height(5.dp))

        // Río de descartes, cuatro columnas.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF173D2E))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            board.players.forEach { player ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        MahjongGame.SEAT_NAMES[player.seat],
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC9DCC9),
                        fontSize = 9.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        player.discards.takeLast(6).forEach { tile ->
                            MahjongTileFace(tile, variant, small = true)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))

        // Melds del asiento visible.
        val shownPlayer = board.players.getOrNull(viewSeat)
        if (shownPlayer != null && shownPlayer.melds.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.t("mahjong.melds"), style = MaterialTheme.typography.labelSmall)
                shownPlayer.melds.forEach { meld ->
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        meld.tiles.forEach { MahjongTileFace(it, variant, small = true) }
                    }
                }
            }
        }

        Text(
            state.t("mahjong.hand", mapOf("name" to if (allHumans) MahjongGame.SEAT_NAMES[viewSeat] else state.t("mahjong.you"))),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            items(visibleHand.sortedWith(compareBy({ it.suit.ordinal }, { it.rank })), key = { it.id }) { tile ->
                MahjongTileFace(
                    tile = tile,
                    variant = variant,
                    selected = selected == tile.id,
                    onClick = {
                        if (board.phase == MahjongPhase.Play && humanTurn && board.pending.isEmpty()) {
                            selected = if (selected == tile.id) null else tile.id
                            NativeAudio.play(context, NativeAudio.Sfx.Tap, state.sfx)
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(5.dp))

        // Acciones.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { discardSelected() },
                enabled = humanTurn && selected != null && board.phase == MahjongPhase.Play && board.pending.isEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(state.t("mahjong.discard"))
            }
            if (variant == MahjongVariant.Riichi) {
                OutlinedButton(
                    onClick = { if (selected != null) apply(game.declareRiichi(board, selected!!), NativeAudio.Sfx.Promote) },
                    enabled = humanTurn && selected != null && !board.players[viewSeat].riichi && board.pending.isEmpty(),
                ) {
                    Text(state.t("mahjong.riichiAction"))
                }
            }
            OutlinedButton(
                onClick = { apply(game.declareTsumo(board), NativeAudio.Sfx.Win) },
                enabled = humanTurn && board.phase == MahjongPhase.Play && game.canWinHand(
                    board.players[viewSeat].hand,
                    board.players[viewSeat].melds,
                    variant,
                ),
            ) {
                Text(state.t("mahjong.tsumo"))
            }
        }

        if (claims.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                claims.forEach { claim ->
                    Button(onClick = {
                        apply(
                            game.claim(board, claim),
                            if (claim.kind == MahjongClaimKind.Ron) NativeAudio.Sfx.Win else NativeAudio.Sfx.Promote,
                        )
                    }) {
                        Text(claimLabel(state, claim, allHumans))
                    }
                }
                OutlinedButton(onClick = {
                    apply(if (allHumans) game.pass(board) else game.pass(board, 0))
                }) { Text(state.t("mahjong.pass")) }
            }
        }

        if (board.phase != MahjongPhase.Play) {
            OutlinedButton(onClick = { resultOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(state.t("mahjong.seeResult"))
            }
            Spacer(Modifier.height(4.dp))
        }
        NativeMoveLog(state, board.history) { historyOpen = true }
        Text(
            if (variant == MahjongVariant.Riichi) state.t("mahjong.rulesRiichi") else state.t("mahjong.rulesAmerican"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (setupOpen) {
        AlertDialog(
            onDismissRequest = { if (board.history.isNotEmpty()) setupOpen = false },
            title = { Text(state.t("mahjong.setup")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("mahjong.allHumans"), modifier = Modifier.weight(1f))
                        Switch(checked = allHumans, onCheckedChange = { allHumans = it })
                    }
                    Text(
                        if (variant == MahjongVariant.Riichi) state.t("mahjong.rulesRiichi") else state.t("mahjong.rulesAmerican"),
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
            pdfTitle = title,
            pdfSubtitle = state.t("mahjong.subtitle"),
            stats = listOf(
                state.t("mahjong.round") to "${board.round}/4",
                state.t("mahjong.wall") to board.wallCount.toString(),
                state.t("mahjong.flowers") to board.players.sumOf { it.flowers.size }.toString(),
            ),
        ) { historyOpen = false }
    }

    if (resultOpen) {
        MahjongResultDialog(
            state = state,
            board = board,
            game = game,
            allHumans = allHumans,
            onNext = {
                val next = game.nextHand(board)
                board = next
                selected = null
                if (next.phase == MahjongPhase.MatchEnd) {
                    if (!allHumans && !awarded) {
                        awarded = true
                        state.awardMatch(win = game.matchWinner(next) == 0)
                        NativeAudio.play(context, if (game.matchWinner(next) == 0) NativeAudio.Sfx.Win else NativeAudio.Sfx.Lose, state.sfx)
                    }
                    resultOpen = true
                } else {
                    awarded = false
                    resultOpen = false
                }
            },
            onClose = { resultOpen = false },
            onRestart = {
                resultOpen = false
                setupOpen = true
            },
        )
    }
}

@Composable
private fun MahjongOpponentRow(state: ShellState, player: MahjongPlayer, game: MahjongGame) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF12382A))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            if (player.seat == 0) state.t("mahjong.you") else MahjongGame.BOT_NAMES[(player.seat + 2) % 3],
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF1E8D4),
            modifier = Modifier.width(58.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.weight(1f)) {
            repeat(player.hand.size.coerceAtMost(14)) {
                Box(
                    Modifier
                        .width(7.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MJ_BACK)
                        .border(0.5.dp, Color(0xFF4F8A6F), RoundedCornerShape(2.dp)),
                )
            }
        }
        player.melds.take(2).forEach { meld ->
            Text(
                when (meld.kind) {
                    MahjongMeldKind.Chi -> "chi"
                    MahjongMeldKind.Pon -> "pon"
                    MahjongMeldKind.Kan -> "kan"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE0A63A),
                fontSize = 9.sp,
            )
        }
        if (player.riichi) Text("RIICHI", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE87373), fontSize = 8.sp)
        Text("${player.score}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC9DCC9))
    }
}

@Composable
private fun MahjongTileFace(
    tile: MahjongTile,
    variant: MahjongVariant,
    small: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val width = if (small) 15.dp else 42.dp
    val height = if (small) 21.dp else 58.dp
    val accent = when (tile.suit) {
        MahjongSuit.Dots -> MJ_BLUE
        MahjongSuit.Bams -> MJ_GREEN
        MahjongSuit.Cracks -> MJ_RED
        MahjongSuit.Wind -> Color(0xFF3A3A3A)
        MahjongSuit.Dragon -> if (tile.rank == 1) MJ_RED else if (tile.rank == 2) MJ_GREEN else Color(0xFF5A5A5A)
        MahjongSuit.Flower -> Color(0xFF9B5DE5)
        MahjongSuit.Joker -> Color(0xFFD9822B)
    }
    val shape = RoundedCornerShape(if (small) 2.dp else 6.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(if (tile.isJoker) Color(0xFFFFE7C2) else MJ_IVORY)
            .border(if (selected) 2.dp else 0.5.dp, if (selected) Color(0xFFE0A63A) else MJ_EDGE, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .semantics { contentDescription = MahjongGame.tileName(tile) },
        contentAlignment = Alignment.Center,
    ) {
        if (small) {
            Text(tile.shortLabel, color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (variant == MahjongVariant.Cards) {
                    Text(cardCorner(tile), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    tile.shortLabel,
                    color = accent,
                    fontSize = if (tile.isJoker) 9.sp else 17.sp,
                    fontFamily = if (tile.suit in setOf(MahjongSuit.Wind, MahjongSuit.Dragon, MahjongSuit.Cracks)) FontFamily.Serif else FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                tile.rank.toString(),
                color = Color(0xFF7A6A50),
                fontSize = 7.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 1.dp),
            )
        }
    }
}

private fun cardCorner(tile: MahjongTile): String = when (tile.suit) {
    MahjongSuit.Dots -> "${if (tile.rank <= 9) tile.rank else ""}●"
    MahjongSuit.Bams -> "${if (tile.rank <= 9) tile.rank else ""}♣"
    MahjongSuit.Cracks -> "${if (tile.rank <= 9) tile.rank else ""}♦"
    MahjongSuit.Wind -> listOf("E", "S", "W", "N")[tile.rank - 1]
    MahjongSuit.Dragon -> listOf("R", "G", "W")[tile.rank - 1]
    MahjongSuit.Flower -> "✿"
    MahjongSuit.Joker -> "J"
}

private fun claimLabel(state: ShellState, claim: MahjongClaim, includeSeat: Boolean): String {
    val kind = when (claim.kind) {
        MahjongClaimKind.Ron -> state.t("mahjong.ron")
        MahjongClaimKind.Pon -> state.t("mahjong.pon")
        MahjongClaimKind.Chi -> state.t("mahjong.chi")
        MahjongClaimKind.Kan -> state.t("mahjong.kan")
    }
    return if (includeSeat) "${kind} · ${MahjongGame.SEAT_NAMES[claim.seat]}" else kind
}

@Composable
private fun MahjongResultDialog(
    state: ShellState,
    board: MahjongState,
    game: MahjongGame,
    allHumans: Boolean,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onRestart: () -> Unit,
) {
    val finished = board.phase == MahjongPhase.MatchEnd ||
        (board.phase == MahjongPhase.HandEnd && board.round >= MahjongGame.HANDS)
    val winner = if (finished) game.matchWinner(board) else board.winner
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                when {
                    finished -> state.t("mahjong.matchOver")
                    board.winner == null -> state.t("mahjong.drawHand")
                    allHumans -> state.t("mahjong.wonBy", mapOf("name" to MahjongGame.SEAT_NAMES[board.winner!!]))
                    board.winner == 0 -> state.t("mahjong.youWon")
                    else -> state.t("mahjong.botWon", mapOf("name" to MahjongGame.BOT_NAMES[(board.winner!! + 2) % 3]))
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                board.history.takeLast(4).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(4.dp))
                board.players.sortedByDescending { it.score }.forEach { player ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            if (player.seat == 0) state.t("mahjong.you") else MahjongGame.SEAT_NAMES[player.seat],
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("${player.score}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (!finished) {
                    Text(
                        state.t("mahjong.nextHandHint"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (finished) {
                Button(onClick = onRestart) { Text(state.t("common.newMatch")) }
            } else {
                Button(onClick = onNext) { Text(state.t("mahjong.nextHand")) }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(state.t("common.close")) } },
    )
}
