package com.mesaviva.app.shell

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NativeGameHeader(
    state: ShellState,
    title: String,
    subtitle: String,
    badge: String = "",
    onBack: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.t("common.back"))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge.isNotBlank()) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun NativeTurnBanner(
    text: String,
    alert: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (alert) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun NativeMoveLog(
    state: ShellState,
    entries: List<String>,
    onHistory: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.t("common.lastMoves"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onHistory) {
                Text(state.t("common.history"), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (entries.isEmpty()) {
            Text(
                state.t("common.noMoves"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                entries.takeLast(5).forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun NativeHistoryDialog(
    state: ShellState,
    entries: List<String>,
    pdfTitle: String = state.t("common.history"),
    pdfSubtitle: String = state.t("app.name"),
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(state.t("common.history")) },
        text = {
            if (entries.isEmpty()) {
                Text(state.t("common.noMoves"))
            } else {
                LazyColumn(Modifier.height(320.dp)) {
                    itemsIndexed(entries) { index, line ->
                        Text(
                            "${index + 1}. $line",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val export = exportChroniclePdf(context, pdfTitle, pdfSubtitle, entries)
                    if (export == null) {
                        Toast.makeText(context, state.t("common.pdfError"), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, state.t("common.pdfSaved"), Toast.LENGTH_SHORT).show()
                        shareChroniclePdf(context, export, state.t("common.sharePdf"))
                    }
                }) {
                    Text(state.t("common.pdf"))
                }
                TextButton(onClick = onClose) { Text(state.t("common.close")) }
            }
        },
    )
}
