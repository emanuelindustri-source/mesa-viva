package com.mesaviva.app.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.DominoTile

/** Ficha de dominó legible para doble 9 y doble 12. */
@Composable
fun TrainTileView(
    tile: DominoTile,
    selected: Boolean = false,
    small: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val width = if (small) 34.dp else 46.dp
    val height = if (small) 22.dp else 30.dp
    Row(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Color(0xFFFFE7B0) else Color(0xFFFFF8E8))
            .border(
                if (selected) 2.dp else 0.8.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color(0xFF7A6A50),
                RoundedCornerShape(4.dp),
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .semantics { contentDescription = "Ficha ${tile.a}-${tile.b}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("${tile.a}", color = Color(0xFF3A2A18), fontSize = if (small) 9.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(height)
                .background(Color(0xFF8A7658)),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("${tile.b}", color = Color(0xFF3A2A18), fontSize = if (small) 9.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
