package com.mesaviva.app.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MesaVivaApp(
    state: ShellState,
    onOpenWeb: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    BackHandler(enabled = state.screen != ShellScreen.Home) {
        state.screen = ShellScreen.Home
    }

    val showBar = state.screen !in setOf(
        ShellScreen.Snakes,
        ShellScreen.Domino,
        ShellScreen.Chess,
        ShellScreen.Ludo,
        ShellScreen.Mancala,
        ShellScreen.Backgammon,
        ShellScreen.Damas,
        ShellScreen.Go,
        ShellScreen.Jungle,
        ShellScreen.Xiangqi,
        ShellScreen.Chino,
        ShellScreen.Dados,
        ShellScreen.Caida,
        ShellScreen.Furia,
        ShellScreen.Poker,
    )
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavItem(state, ShellScreen.Home, "nav.home", Icons.Filled.Home)
                    NavItem(state, ShellScreen.Catalog, "nav.catalog", Icons.AutoMirrored.Filled.List)
                    NavItem(state, ShellScreen.Room, "nav.room", Icons.Filled.Share)
                    NavItem(state, ShellScreen.Profile, "nav.profile", Icons.Filled.Star)
                    NavItem(state, ShellScreen.Settings, "nav.settings", Icons.Filled.Settings)
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        ),
                    ),
                )
                .padding(padding),
        ) {
            val openNative: (String) -> Unit = { id ->
                state.screen = when (id) {
                    "serpientes" -> ShellScreen.Snakes
                    "domino" -> ShellScreen.Domino
                    "ajedrez" -> ShellScreen.Chess
                    "ludo" -> ShellScreen.Ludo
                    "mancala" -> ShellScreen.Mancala
                    "backgammon" -> ShellScreen.Backgammon
                    "damas" -> ShellScreen.Damas
                    "go" -> ShellScreen.Go
                    "jungle" -> ShellScreen.Jungle
                    "xiangqi" -> ShellScreen.Xiangqi
                    "chino" -> ShellScreen.Chino
                    "dados" -> ShellScreen.Dados
                    "caida" -> ShellScreen.Caida
                    "furia" -> ShellScreen.Furia
                    "poker" -> ShellScreen.Poker
                    else -> ShellScreen.Catalog
                }
            }
            Crossfade(targetState = state.screen, label = "shell-screen") { screen ->
                when (screen) {
                    ShellScreen.Home -> HomeScreen(state, onOpenWeb, onOpenUrl, openNative)
                    ShellScreen.Catalog -> CatalogScreen(state, onOpenWeb, openNative)
                    ShellScreen.Profile -> ProfileScreen(state)
                    ShellScreen.Settings -> SettingsScreen(state, onOpenWeb)
                    ShellScreen.Room -> RoomScreen(state, onOpenWeb)
                    ShellScreen.Snakes -> SnakesGameScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Domino -> DominoScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Chess -> ChessScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Ludo -> LudoScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Mancala -> MancalaScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Backgammon -> BackgammonScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Damas -> DamasScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Go -> GoScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Jungle -> JungleScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Xiangqi -> XiangqiScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Chino -> ChinoScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Dados -> DadosScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Caida -> CaidaScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Furia -> FuriaScreen(state) { state.screen = ShellScreen.Home }
                    ShellScreen.Poker -> PokerScreen(state) { state.screen = ShellScreen.Home }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(state: ShellState, screen: ShellScreen, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    NavigationBarItem(
        selected = state.screen == screen,
        onClick = { state.screen = screen },
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(state.t(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

/* -------------------------------------------------------------------- Home */

@Composable
private fun HomeScreen(
    state: ShellState,
    onOpenWeb: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onNative: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHeader(state) }
        item { HeroCard(state, onOpenWeb, onNative) }
        item { SectionTitle(state.t("home.quick")) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("♠️", state.t("home.poker"), state.t("home.pokerDesc"), Modifier.weight(1f)) {
                        onNative("poker")
                    }
                    QuickCard("🎨", state.t("furia.title"), state.t("furia.subtitle"), Modifier.weight(1f)) {
                        onNative("furia")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("📱", state.t("home.room"), state.t("common.online"), Modifier.weight(1f)) {
                        state.screen = ShellScreen.Room
                    }
                    QuickCard("🐍", state.t("home.native"), state.t("snakes.subtitle"), Modifier.weight(1f)) {
                        onNative("serpientes")
                    }
                }
            }
        }
        item { SectionTitle(state.t("home.nativeGames")) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("🁫", state.t("domino.title"), state.t("domino.subtitle"), Modifier.weight(1f)) {
                        onNative("domino")
                    }
                    QuickCard("♟️", state.t("chess.title"), state.t("chess.subtitle"), Modifier.weight(1f)) {
                        onNative("ajedrez")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("🎯", state.t("ludo.title"), state.t("ludo.subtitle"), Modifier.weight(1f)) {
                        onNative("ludo")
                    }
                    QuickCard("🫘", state.t("mancala.title"), state.t("mancala.subtitle"), Modifier.weight(1f)) {
                        onNative("mancala")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("🎲", state.t("bg.title"), state.t("bg.subtitle"), Modifier.weight(1f)) {
                        onNative("backgammon")
                    }
                    QuickCard("🐍", state.t("home.native"), state.t("snakes.subtitle"), Modifier.weight(1f)) {
                        onNative("serpientes")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("⛀", state.t("damas.title"), state.t("damas.subtitle"), Modifier.weight(1f)) {
                        onNative("damas")
                    }
                    QuickCard("⚫", state.t("go.title"), state.t("go.subtitle"), Modifier.weight(1f)) {
                        onNative("go")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("🐯", state.t("jungle.title"), state.t("jungle.subtitle"), Modifier.weight(1f)) {
                        onNative("jungle")
                    }
                    QuickCard("🀄", state.t("xiangqi.title"), state.t("xiangqi.subtitle"), Modifier.weight(1f)) {
                        onNative("xiangqi")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("✳️", state.t("chino.title"), state.t("chino.subtitle"), Modifier.weight(1f)) {
                        onNative("chino")
                    }
                    QuickCard("🎲", state.t("dados.title"), state.t("dados.subtitle"), Modifier.weight(1f)) {
                        onNative("dados")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("🃏", state.t("caida.title"), state.t("caida.subtitle"), Modifier.weight(1f)) {
                        onNative("caida")
                    }
                    QuickCard("🎨", state.t("furia.title"), state.t("furia.subtitle"), Modifier.weight(1f)) {
                        onNative("furia")
                    }
                }
            }
        }
        item { ProgressCard(state) }
        item { NewsCard(state) }
        item { OfflineCard(state) }
        item {
            Text(
                state.t("home.footer"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeHeader(state: ShellState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🪅", fontSize = 24.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.t("app.name"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                state.t("app.tagline"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(state.festival.accent).copy(alpha = 0.16f),
        ) {
            Text(
                "${state.festival.emoji} ${state.festival.name(state.language)}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(state.festival.accent),
            )
        }
    }
}

@Composable
private fun HeroCard(state: ShellState, onOpenWeb: (String) -> Unit, onNative: (String) -> Unit) {
    val accent = Color(state.festival.accent)
    val accent2 = Color(state.festival.accent2)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.surface,
                        accent2.copy(alpha = 0.22f),
                    ),
                ),
            ),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                state.t("home.welcome"),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                state.t("home.subtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onOpenWeb("/") }) {
                    Text("▶  ${state.t("home.playNow")}")
                }
                OutlinedButton(onClick = { onNative("serpientes") }) {
                    Text("🐍 ${state.t("home.native")}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniBadge("15", state.t("home.games"))
                MiniBadge("11", state.t("home.languages"))
            }
        }
    }
}

@Composable
private fun MiniBadge(value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun QuickCard(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(emoji, fontSize = 26.sp)
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgressCard(state: ShellState) {
    val progress = xpProgress(state.xp)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.t("home.progress"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    state.t("home.level").replace("{n}", levelForXp(state.xp).toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(state.t("home.xp").replace("{n}", state.xp.toString()), style = MaterialTheme.typography.bodySmall)
                Text(state.t("home.matches").replace("{n}", state.matches.toString()), style = MaterialTheme.typography.bodySmall)
                Text(state.t("home.wins").replace("{n}", state.wins.toString()), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NewsCard(state: ShellState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.t("home.news"), style = MaterialTheme.typography.titleMedium)
            NewsRow("⚫", state.t("home.newsDomino"))
            NewsRow("🀄", state.t("home.newsChess"))
            NewsRow("✳️", state.t("home.newsRoom"))
        }
    }
}

@Composable
private fun NewsRow(emoji: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 14.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OfflineCard(state: ShellState) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("📴  ${state.t("home.offlineTitle")}", style = MaterialTheme.typography.titleMedium)
            Text(
                state.t("home.offlineDesc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
