package com.mesaviva.app.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mesaviva.app.games.FuriaAction
import com.mesaviva.app.games.FuriaCard
import com.mesaviva.app.games.FuriaGame
import com.mesaviva.app.games.FuriaSuit
import com.mesaviva.app.games.PokerCard
import com.mesaviva.app.games.PokerGame
import com.mesaviva.app.games.PokerSuit
import com.mesaviva.app.games.SpanishCard
import com.mesaviva.app.games.SpanishSuit
import kotlin.math.cos
import kotlin.math.sin

val SPANISH_ORO = Color(0xFFB8862E)
val SPANISH_COPA = Color(0xFFB03A32)
val SPANISH_ESPADA = Color(0xFF3F5F8F)
val SPANISH_BASTO = Color(0xFF3F7A4A)

val FURIA_SOL = Color(0xFFE0A63A)
val FURIA_MAR = Color(0xFF2F6FED)
val FURIA_SELVA = Color(0xFF2E9E6B)
val FURIA_MAGMA = Color(0xFFD64545)
val FURIA_WILD = Color(0xFF2B2433)

fun spanishSuitColor(suit: SpanishSuit): Color = when (suit) {
    SpanishSuit.Oros -> SPANISH_ORO
    SpanishSuit.Copas -> SPANISH_COPA
    SpanishSuit.Espadas -> SPANISH_ESPADA
    SpanishSuit.Bastos -> SPANISH_BASTO
}

fun furiaColor(suit: FuriaSuit): Color = when (suit) {
    FuriaSuit.Sol -> FURIA_SOL
    FuriaSuit.Mar -> FURIA_MAR
    FuriaSuit.Selva -> FURIA_SELVA
    FuriaSuit.Magma -> FURIA_MAGMA
    FuriaSuit.Wild -> FURIA_WILD
}

@Composable
fun SpanishCardFace(
    card: SpanishCard,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier
        .then(modifier)
        .clip(shape)
        .then(if (onClick != null && playable) Modifier.clickable(onClick = onClick) else Modifier)
        .semantics {
            contentDescription = if (faceDown) {
                "Carta boca abajo"
            } else {
                "${card.rankLabel()} de ${card.suit.name.lowercase()}"
            }
        }
    if (faceDown) {
        Box(
            base
                .background(
                    Brush.linearGradient(listOf(Color(0xFF4B1F1A), Color(0xFF1F3B35), Color(0xFF4B1F1A))),
                )
                .border(1.5.dp, Color(0xFFE0A63A).copy(alpha = 0.75f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = Color(0xFFE0A63A).copy(alpha = 0.8f), fontSize = 26.sp)
        }
        return
    }
    val ink = spanishSuitColor(card.suit)
    Box(
        base
            .background(Color(0xFFFFF7E4))
            .border(1.5.dp, Color(0xFFCDB98F), shape)
            .then(if (playable) Modifier else Modifier.background(Color.Black.copy(alpha = 0.18f))),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val side = minOf(maxWidth.value, maxHeight.value)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            card.rankLabel(),
                            color = ink,
                            fontSize = (side * 0.19f).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (side * 0.19f).sp,
                        )
                        Text(card.suit.symbol, color = ink, fontSize = (side * 0.16f).sp, lineHeight = (side * 0.16f).sp)
                    }
                    Text(card.suit.symbol, color = ink.copy(alpha = 0.65f), fontSize = (side * 0.14f).sp)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(card.suit.symbol, color = ink, fontSize = (side * 0.42f).sp, lineHeight = (side * 0.42f).sp)
                        if (card.rank >= 10) {
                            Text(
                                when (card.rank) {
                                    10 -> "SOTA"
                                    11 -> "CABALLO"
                                    else -> "REY"
                                },
                                color = ink.copy(alpha = 0.8f),
                                fontSize = (side * 0.09f).sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        card.rankLabel(),
                        color = ink,
                        fontSize = (side * 0.17f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
            }
        }
    }
}

private fun SpanishCard.rankLabel(): String = when (rank) {
    10 -> "S"
    11 -> "C"
    12 -> "R"
    else -> rank.toString()
}

@Composable
fun FuriaCardFace(
    card: FuriaCard,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(9.dp)
    val base = Modifier
        .then(modifier)
        .clip(shape)
        .then(if (onClick != null && playable) Modifier.clickable(onClick = onClick) else Modifier)
        .semantics {
            contentDescription = if (faceDown) "Carta boca abajo" else FuriaGame.cardLabel(card)
        }
    if (faceDown) {
        Box(
            base.background(
                Brush.linearGradient(listOf(Color(0xFF241B2C), Color(0xFF4B1F1A), Color(0xFF241B2C))),
            ).border(1.5.dp, Color(0xFFE0A63A), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = Color(0xFFE0A63A), fontSize = 24.sp)
        }
        return
    }
    val color = furiaColor(card.suit)
    Box(
        base
            .background(color)
            .border(2.dp, if (playable) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.35f), shape)
            .then(if (playable) Modifier else Modifier.background(Color.Black.copy(alpha = 0.18f))),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
            val side = minOf(maxWidth.value, maxHeight.value)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (card.action) {
                            FuriaAction.Skip -> "⊘"
                            FuriaAction.Reverse -> "⇄"
                            FuriaAction.Draw2 -> "+2"
                            FuriaAction.Wild -> "★"
                            FuriaAction.Wild4 -> "+4"
                            FuriaAction.None -> card.rank.toString()
                        },
                        color = if (card.suit == FuriaSuit.Wild) FURIA_WILD else color,
                        fontSize = (side * 0.40f).sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = (side * 0.42f).sp,
                        textAlign = TextAlign.Center,
                    )
                    if (card.action != FuriaAction.None) {
                        Text(
                            when (card.action) {
                                FuriaAction.Skip -> "SALTO"
                                FuriaAction.Reverse -> "REVERSA"
                                FuriaAction.Draw2 -> "ROBA 2"
                                FuriaAction.Wild -> "COMODÍN"
                                FuriaAction.Wild4 -> "ROBA 4"
                                FuriaAction.None -> ""
                            },
                            color = if (card.suit == FuriaSuit.Wild) FURIA_WILD else color,
                            fontSize = (side * 0.10f).sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    if (card.suit == FuriaSuit.Wild) "✧" else card.suit.name.first().toString(),
                    color = if (card.suit == FuriaSuit.Wild) FURIA_WILD else color.copy(alpha = 0.75f),
                    fontSize = (side * 0.13f).sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                )
                Text(
                    if (card.suit == FuriaSuit.Wild) "✧" else card.suit.name.first().toString(),
                    color = if (card.suit == FuriaSuit.Wild) FURIA_WILD else color.copy(alpha = 0.75f),
                    fontSize = (side * 0.13f).sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                )
            }
        }
    }
}

@Composable
fun PokerCardFace(
    card: PokerCard,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    playable: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier
        .then(modifier)
        .clip(shape)
        .then(if (onClick != null && playable) Modifier.clickable(onClick = onClick) else Modifier)
        .semantics {
            contentDescription = if (faceDown) "Carta boca abajo" else PokerGame.cardLabel(card)
        }
    if (faceDown) {
        Box(
            base
                .background(Brush.linearGradient(listOf(Color(0xFF0E3B34), Color(0xFF1F6A58), Color(0xFF0E3B34))))
                .border(1.5.dp, Color(0xFFE0A63A).copy(alpha = 0.8f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text("♠", color = Color(0xFFE0A63A).copy(alpha = 0.85f), fontSize = 22.sp)
        }
        return
    }
    val red = card.suit == PokerSuit.Hearts || card.suit == PokerSuit.Diamonds
    val ink = if (red) Color(0xFFB03A32) else Color(0xFF22201C)
    Box(
        base
            .background(Color(0xFFFFFCF3))
            .border(1.5.dp, Color(0xFFCDB98F), shape)
            .then(if (playable) Modifier else Modifier.background(Color.Black.copy(alpha = 0.15f))),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val side = minOf(maxWidth.value, maxHeight.value)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            com.mesaviva.app.games.PokerGame.rankLabel(card.rank),
                            color = ink,
                            fontSize = (side * 0.22f).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (side * 0.22f).sp,
                        )
                        Text(card.suit.symbol, color = ink, fontSize = (side * 0.18f).sp, lineHeight = (side * 0.18f).sp)
                    }
                    Text(card.suit.symbol, color = ink.copy(alpha = 0.55f), fontSize = (side * 0.15f).sp)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(card.suit.symbol, color = ink, fontSize = (side * 0.45f).sp, lineHeight = (side * 0.45f).sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        com.mesaviva.app.games.PokerGame.rankLabel(card.rank),
                        color = ink,
                        fontSize = (side * 0.18f).sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun CardDeckBack(modifier: Modifier = Modifier, count: Int? = null) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4B1F1A), Color(0xFF1F3B35))))
                .border(1.5.dp, Color(0xFFE0A63A).copy(alpha = 0.8f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = Color(0xFFE0A63A), fontSize = 22.sp)
        }
        if (count != null) {
            Text(
                count.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
            )
        }
    }
}

/**
 * Pequeña ráfaga de polvo para acompañar una carta o una captura.
 * Se reinicia cuando cambia [trigger].
 */
@Composable
fun TableDust(trigger: Int, modifier: Modifier = Modifier, color: Color = Color(0xFFE0C28A)) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(620))
    }
    Canvas(modifier) {
        if (progress.value <= 0f || progress.value >= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val p = progress.value
        val fade = (1f - p).coerceIn(0f, 1f)
        for (i in 0 until 18) {
            val angle = (Math.PI * 2.0 * i / 18.0) + p * 0.7
            val distance = size.minDimension * (0.05f + p * 0.42f)
            val point = Offset(
                center.x + (cos(angle) * distance).toFloat(),
                center.y + (sin(angle) * distance).toFloat(),
            )
            drawCircle(
                color = color.copy(alpha = 0.65f * fade),
                radius = (2.5f + (i % 4)) * (1f - p * 0.4f),
                center = point,
            )
        }
    }
}

@Composable
fun CardMiniLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun CardSlot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.06f))
            .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
    )
}
