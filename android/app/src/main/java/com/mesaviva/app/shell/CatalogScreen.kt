package com.mesaviva.app.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CatalogScreen(
    state: ShellState,
    onOpenWeb: (String) -> Unit,
    onOpenNative: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    val filtered = remember(query, filter) {
        SHELL_GAMES.filter { game ->
            val matchesQuery = query.isBlank() || game.name.contains(query, ignoreCase = true) || game.note.contains(query, ignoreCase = true)
            val matchesFilter = filter == "all" || game.status == filter
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.t("catalog.title"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.t("catalog.subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(state.t("catalog.search")) },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all", "playable", "soon", "research").forEach { value ->
                    FilterChip(
                        selected = filter == value,
                        onClick = { filter = value },
                        label = {
                            Text(
                                when (value) {
                                    "playable" -> state.t("catalog.playable")
                                    "soon" -> state.t("catalog.soon")
                                    "research" -> state.t("catalog.research")
                                    else -> state.t("catalog.all")
                                },
                            )
                        },
                    )
                }
            }
        }
        item {
            Card(
                onClick = { onOpenWeb("/room") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📷", fontSize = 24.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(state.t("home.room"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.t("common.online"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("QR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        items(filtered, key = { it.id }) { game ->
            GameCard(state, game, onOpenWeb, onOpenNative)
        }
        item {
            Text(
                "Mesa Viva 1.4.0 · 15 juegos jugables · 15 nativos · 11 idiomas · sin anuncios",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun GameCard(
    state: ShellState,
    game: ShellGame,
    onOpenWeb: (String) -> Unit,
    onOpenNative: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                ) {
                    Box(Modifier.padding(8.dp)) { Text(game.emoji, fontSize = 22.sp) }
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(game.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${game.players} ${state.t("common.players")} · ${categoryLabel(state, game.category)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(state, game.status)
                    if (game.native) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                        ) {
                            Text(
                                state.t("native.badge"),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
            }
            if (game.note.isNotBlank()) {
                Text(
                    game.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (game.status == "playable") {
                Button(
                    onClick = {
                        if (game.native) onOpenNative(game.id) else onOpenWeb("/setup/${game.id}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("▶  ${state.t("catalog.play")}")
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(state: ShellState, status: String) {
    val (label, color) = when (status) {
        "playable" -> state.t("catalog.playable") to MaterialTheme.colorScheme.primary
        "soon" -> state.t("catalog.soon") to Color(0xFFE0A63A)
        else -> state.t("catalog.research") to Color(0xFF9B7BD6)
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun categoryLabel(state: ShellState, category: String): String = when (category) {
    "cartas" -> state.t("catalog.cards")
    "dados" -> state.t("catalog.dice")
    "herramienta" -> state.t("catalog.research")
    else -> state.t("catalog.board")
}
