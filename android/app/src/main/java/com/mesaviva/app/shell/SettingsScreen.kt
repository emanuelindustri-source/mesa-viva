package com.mesaviva.app.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(state: ShellState, onOpenWeb: (String) -> Unit) {
    var importOpen by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.t("settings.title"), style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.t("settings.subtitle"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.t("settings.language"), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.language == "es",
                            onClick = { state.updateLanguage("es") },
                            label = { Text(state.t("settings.spanish")) },
                        )
                        FilterChip(
                            selected = state.language == "en",
                            onClick = { state.updateLanguage("en") },
                            label = { Text(state.t("settings.english")) },
                        )
                    }
                    Text(
                        if (state.language == "es") "La web y el juego conservan los 11 idiomas completos." else "The web and the game keep all 11 languages.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SettingSwitch(
                title = state.t("settings.dynamic"),
                description = state.t("settings.dynamicDesc"),
                checked = state.dynamicColor,
                onChange = state::updateDynamicColor,
            )
        }
        item {
            SettingSwitch(
                title = state.t("settings.haptics"),
                description = if (state.language == "es") "Vibración al lanzar dados, mover fichas y ganar." else "Vibration when rolling dice, moving pieces and winning.",
                checked = state.haptics,
                onChange = state::updateHaptics,
            )
        }
        item {
            SettingSwitch(
                title = state.t("settings.music"),
                description = if (state.language == "es") "Música generativa adaptada a la festividad." else "Generative music adapted to the festival.",
                checked = state.music,
                onChange = state::updateMusic,
            )
        }
        item {
            SettingSwitch(
                title = state.t("settings.sfx"),
                description = if (state.language == "es") "Efectos de cartas, dados, cantos y victorias." else "Card, dice, call and victory effects.",
                checked = state.sfx,
                onChange = state::updateSfx,
            )
        }
        item {
            SettingSwitch(
                title = state.t("settings.reduceMotion"),
                description = if (state.language == "es") "Menos partículas y animaciones más suaves." else "Fewer particles and softer animations.",
                checked = state.reduceMotion,
                onChange = state::updateReduceMotion,
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (state.language == "es") "La mesa completa" else "The full table",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (state.language == "es") {
                            "Idioma de la mesa, país, festividad, color del fondo, música, sensores, PDF y servidor de salas."
                        } else {
                            "Table language, country, festival, wallpaper color, music, sensors, PDF and room server."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onOpenWeb("/settings") }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.t("settings.gameSettings"))
                    }
                    OutlinedButton(onClick = { onOpenWeb("/diagnostics") }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.t("settings.diagnostics"))
                    }
                    OutlinedButton(
                        onClick = {
                            importText = ""
                            importError = false
                            importOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(state.t("replay.import"))
                    }
                    OutlinedButton(onClick = { onOpenWeb("/about") }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.t("settings.about"))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                HorizontalDivider()
                Text(
                    state.t("settings.version"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    state.t("settings.privacy"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (importOpen) {
        AlertDialog(
            onDismissRequest = { importOpen = false },
            title = { Text(state.t("replay.import")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it.take(1200)
                            importError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(state.t("replay.importHint")) },
                    )
                    if (importError) {
                        Text(
                            state.t("replay.invalid"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val replay = ReplayCodec.parse(importText.trim())
                    if (replay == null) {
                        importError = true
                    } else {
                        importOpen = false
                        state.pendingReplay = replay
                        state.screen = if (replay.game == ReplayCodec.DAMAS) {
                            ShellScreen.Damas
                        } else {
                            ShellScreen.Mancala
                        }
                    }
                }) { Text(state.t("common.open")) }
            },
            dismissButton = {
                TextButton(onClick = { importOpen = false }) { Text(state.t("common.cancel")) }
            },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

