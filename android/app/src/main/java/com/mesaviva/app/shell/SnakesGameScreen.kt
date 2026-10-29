package com.mesaviva.app.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

private val LADDERS: Map<Int, Int> = mapOf(
    3 to 22, 8 to 26, 20 to 41, 28 to 77, 50 to 91, 71 to 92,
)
private val SNAKES: Map<Int, Int> = mapOf(
    17 to 4, 54 to 34, 62 to 19, 64 to 60, 87 to 24, 93 to 73, 95 to 75, 99 to 78,
)
private val PLAYER_COLORS = listOf(
    Color(0xFFE0A63A),
    Color(0xFF7FD8B4),
    Color(0xFFE57373),
    Color(0xFF8AB4F8),
)

private class SnakesPlayer(
    val id: Int,
    val name: String,
    val emoji: String,
    val isBot: Boolean,
    initial: Int = 0,
) {
    var position by mutableStateOf(initial)
}

@Composable
fun SnakesGameScreen(state: ShellState, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val players = remember {
        mutableStateListOf(
            SnakesPlayer(0, state.t("snakes.you"), "🐆", false),
            SnakesPlayer(1, "Bot Sayona", "👻", true),
            SnakesPlayer(2, "Bot Silbón", "🎩", true),
        )
    }
    var current by remember { mutableIntStateOf(0) }
    var dice by remember { mutableIntStateOf(0) }
    var rolling by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<SnakesPlayer?>(null) }
    var message by remember { mutableStateOf("") }
    var historyOpen by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }

    fun label(key: String, vararg params: Pair<String, Any?>): String =
        ShellStrings.t(state.language, key, params.toMap())

    fun reset() {
        players.forEach { it.position = 0 }
        current = 0
        dice = 0
        winner = null
        message = ""
        log = emptyList()
        rolling = false
    }

    fun roll() {
        if (rolling || winner != null) return
        val player = players[current]
        rolling = true
        state.vibrate(context, 28)
        NativeAudio.play(context, NativeAudio.Sfx.Dice, state.sfx)
        scope.launch {
            repeat(9) {
                dice = Random.nextInt(1, 7)
                delay(55)
            }
            val value = Random.nextInt(1, 7)
            dice = value
            delay(140)
            val start = player.position
            if (start + value > 100) {
                message = label("snakes.needExact")
                rolling = false
                current = (current + 1) % players.size
                return@launch
            }
            var position = start
            repeat(value) {
                position += 1
                player.position = position
                state.vibrate(context, 12)
                delay(115)
            }
            delay(180)
            val ladder = LADDERS[position]
            val snake = SNAKES[position]
            if (ladder != null) {
                player.position = ladder
                message = label("snakes.ladder", "name" to player.name, "n" to ladder)
                state.vibrate(context, 70)
                delay(520)
            } else if (snake != null) {
                player.position = snake
                message = label("snakes.snake", "name" to player.name, "n" to snake)
                state.vibrate(context, 90)
                delay(520)
            } else {
                message = ""
            }
            if (player.position == 100) {
                winner = player
                message = label("snakes.winner", "name" to player.name)
                state.vibrate(context, 160)
                state.awardMatch(win = !player.isBot)
                NativeAudio.play(
                    context,
                    if (player.isBot) NativeAudio.Sfx.Lose else NativeAudio.Sfx.Win,
                    state.sfx,
                )
            } else {
                current = (current + 1) % players.size
            }
            rolling = false
        }
    }

    LaunchedEffect(current, winner) {
        val player = players[current]
        if (winner == null && player.isBot && !rolling) {
            delay(780)
            roll()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("snakes.title"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t("snakes.subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                Text("100", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    players.forEachIndexed { index, player ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (current == index && winner == null) {
                                PLAYER_COLORS[index].copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(vertical = 7.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(player.emoji, fontSize = 18.sp)
                                Text(player.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                Text(
                                    player.position.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PLAYER_COLORS[index],
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Text(
                    if (winner != null) label("snakes.winner", "name" to winner!!.name)
                    else if (current == 0) state.t("snakes.yourTurn")
                    else label("snakes.botTurn", "name" to players[current].name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
        ) {
            val cell = maxWidth / 10
            val cellPx = with(LocalDensity.current) { cell.toPx() }
            Canvas(Modifier.matchParentSize()) {
                for (number in 1..100) {
                    val (row, col) = cellFromNumber(number)
                    val isLadder = LADDERS.containsKey(number) || LADDERS.containsValue(number)
                    val isSnake = SNAKES.containsKey(number) || SNAKES.containsValue(number)
                    val color = when {
                        isLadder -> Color(0xFF4FB286).copy(alpha = 0.12f)
                        isSnake -> Color(0xFFD96A5A).copy(alpha = 0.12f)
                        (row + col) % 2 == 0 -> Color.White.copy(alpha = 0.035f)
                        else -> Color.Transparent
                    }
                    drawRect(color, topLeft = Offset(col * cellPx, row * cellPx), size = Size(cellPx, cellPx))
                    drawRect(
                        Color.White.copy(alpha = 0.09f),
                        topLeft = Offset(col * cellPx, row * cellPx),
                        size = Size(cellPx, cellPx),
                        style = Stroke(width = 1f),
                    )
                }
                LADDERS.forEach { (from, to) -> drawLadder(from, to, cellPx) }
                SNAKES.forEach { (from, to) -> drawSnake(from, to, cellPx) }
            }

            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 10) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 10) {
                            val number = cellNumber(row, col)
                            val here = players.filter { it.position == number }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            ) {
                                Text(
                                    number.toString(),
                                    fontSize = 7.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(start = 2.dp, top = 1.dp),
                                )
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    here.forEach { player ->
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(PLAYER_COLORS[player.id])
                                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DiceFace(dice)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    message.ifBlank { state.t("snakes.rules") },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (winner != null) state.t("snakes.again") else if (current == 0) state.t("snakes.yourTurn") else label("snakes.botTurn", "name" to players[current].name),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                enabled = current == 0 && winner == null && !rolling,
                onClick = { roll() },
            ) {
                Text(if (rolling) "…" else state.t("snakes.roll"))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            state.t("snakes.rules"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { historyOpen = true }) { Text(state.t("common.history")) }
        Spacer(Modifier.height(14.dp))
    }

    LaunchedEffect(message) {
        if (message.isNotBlank()) log = (log + message).takeLast(200)
    }

    if (historyOpen) {
        NativeHistoryDialog(
            state,
            log,
            pdfTitle = state.t("snakes.title"),
            pdfSubtitle = state.t("snakes.subtitle"),
        ) { historyOpen = false }
    }

    winner?.let { champion ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text(state.t("snakes.winner").replace("{name}", champion.name)) },
            text = { Text(state.t("snakes.rules")) },
            confirmButton = {
                TextButton(onClick = {
                    reset()
                }) { Text(state.t("snakes.again")) }
            },
            dismissButton = {
                TextButton(onClick = onBack) { Text(state.t("common.back")) }
            },
        )
    }
}

private fun cellNumber(row: Int, col: Int): Int {
    val fromBottom = 9 - row
    val base = fromBottom * 10
    return if (fromBottom % 2 == 0) base + col + 1 else base + (9 - col) + 1
}

private fun cellFromNumber(number: Int): Pair<Int, Int> {
    val fromBottom = (number - 1) / 10
    val index = (number - 1) % 10
    val col = if (fromBottom % 2 == 0) index else 9 - index
    val row = 9 - fromBottom
    return row to col
}

private fun centerOf(number: Int, cell: Float): Offset {
    val (row, col) = cellFromNumber(number)
    return Offset((col + 0.5f) * cell, (row + 0.5f) * cell)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLadder(from: Int, to: Int, cell: Float) {
    val start = centerOf(from, cell)
    val end = centerOf(to, cell)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val nx = -dy / length * cell * 0.16f
    val ny = dx / length * cell * 0.16f
    val color = Color(0xFF4FB286).copy(alpha = 0.7f)
    drawLine(color, Offset(start.x + nx, start.y + ny), Offset(end.x + nx, end.y + ny), strokeWidth = cell * 0.035f)
    drawLine(color, Offset(start.x - nx, start.y - ny), Offset(end.x - nx, end.y - ny), strokeWidth = cell * 0.035f)
    val rungs = 7
    for (index in 0..rungs) {
        val t = index.toFloat() / rungs
        val x = start.x + dx * t
        val y = start.y + dy * t
        drawLine(color.copy(alpha = 0.55f), Offset(x + nx, y + ny), Offset(x - nx, y - ny), strokeWidth = cell * 0.025f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnake(from: Int, to: Int, cell: Float) {
    val start = centerOf(from, cell)
    val end = centerOf(to, cell)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    val nx = -dy / length
    val ny = dx / length
    val color = Color(0xFFD96A5A).copy(alpha = 0.72f)
    val path = Path()
    val points = 36
    for (index in 0..points) {
        val t = index.toFloat() / points
        val offset = sin(t * PI.toFloat() * 7f) * cell * 0.17f
        val x = start.x + dx * t + nx * offset
        val y = start.y + dy * t + ny * offset
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = cell * 0.07f))
    drawCircle(color, radius = cell * 0.11f, center = start)
    val eye = cell * 0.025f
    drawCircle(Color.White.copy(alpha = 0.9f), radius = eye, center = Offset(start.x - cell * 0.03f, start.y - cell * 0.02f))
    drawCircle(Color.White.copy(alpha = 0.9f), radius = eye, center = Offset(start.x + cell * 0.03f, start.y - cell * 0.02f))
}

@Composable
private fun DiceFace(value: Int) {
    val face = MaterialTheme.colorScheme.onSurface
    Canvas(
        Modifier
            .size(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        )
        val r = size.minDimension * 0.065f
        fun pip(column: Int, row: Int) {
            drawCircle(face, r, Offset(size.width * (0.25f + column * 0.25f), size.height * (0.25f + row * 0.25f)))
        }
        when (value) {
            0 -> Unit
            1 -> pip(1, 1)
            2 -> {
                pip(0, 0); pip(2, 2)
            }
            3 -> {
                pip(0, 0); pip(1, 1); pip(2, 2)
            }
            4 -> {
                pip(0, 0); pip(2, 0); pip(0, 2); pip(2, 2)
            }
            5 -> {
                pip(0, 0); pip(2, 0); pip(1, 1); pip(0, 2); pip(2, 2)
            }
            6 -> {
                pip(0, 0); pip(2, 0); pip(0, 1); pip(2, 1); pip(0, 2); pip(2, 2)
            }
        }
    }
}
