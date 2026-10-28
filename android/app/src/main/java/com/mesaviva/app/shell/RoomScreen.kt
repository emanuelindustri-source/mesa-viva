package com.mesaviva.app.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val WEB_ROOM_URL = "https://emanuelindustri-source.github.io/mesa-viva/play/#/room"

@Composable
fun RoomScreen(state: ShellState, onOpenWeb: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf("caida") }
    val qr = remember { generateQrBitmap(WEB_ROOM_URL) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.t("room.title"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.t("room.subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📶  ${state.t("room.lan")}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.t("room.lanDesc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenWeb("/room") }, modifier = Modifier.weight(1f)) {
                            Text(state.t("room.create"))
                        }
                        OutlinedButton(onClick = { onOpenWeb("/room?scan=1") }, modifier = Modifier.weight(1f)) {
                            Text(state.t("room.scan"))
                        }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("room.join"), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { value ->
                            code = value.uppercase().filter { it.isLetterOrDigit() }.take(6)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(state.t("room.code")) },
                    )
                    Button(
                        onClick = {
                            val clean = code.trim().uppercase()
                            if (clean.length in 4..6) onOpenWeb("/room?code=$clean")
                        },
                        enabled = code.trim().length in 4..6,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(state.t("room.joinButton"))
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(state.t("room.qr"), style = MaterialTheme.typography.titleMedium)
                    if (qr != null) {
                        Image(
                            bitmap = qr,
                            contentDescription = state.t("room.qr"),
                            modifier = Modifier.size(190.dp),
                        )
                    } else {
                        Text("QR", fontSize = 42.sp)
                    }
                    Text(
                        state.t("room.qrDesc"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        item {
            Text(state.t("room.round"), style = MaterialTheme.typography.titleMedium)
        }
        item {
            Text(
                state.t("room.games"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (rowIndex in 0 until (PLAYABLE_GAMES.size + 1) / 2) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (column in 0..1) {
                            val index = rowIndex * 2 + column
                            val game = PLAYABLE_GAMES.getOrNull(index)
                            if (game != null) {
                                FilterChip(
                                    selected = selectedGame == game.id,
                                    onClick = { selectedGame = game.id },
                                    label = {
                                        Text(
                                            "${game.emoji} ${game.name}",
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { onOpenWeb("/setup/$selectedGame") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("▶  ${state.t("room.propose")}")
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(state.festival.accent).copy(alpha = 0.10f)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    state.t("room.note"),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
