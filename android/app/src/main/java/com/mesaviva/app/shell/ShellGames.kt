package com.mesaviva.app.shell

data class ShellGame(
    val id: String,
    val name: String,
    val emoji: String,
    val players: String,
    val category: String,
    val status: String,
    val note: String = "",
    val native: Boolean = false,
)

/** Catálogo de la cáscara. Los treinta juegos abren su mesa nativa en Compose. */
val SHELL_GAMES: List<ShellGame> = listOf(
    ShellGame("poker", "Póker de la casa", "♠️", "2–6", "cartas", "playable", "Texas Hold'em con fichas de práctica. Nativo en Compose.", native = true),
    ShellGame("caida", "Caída", "🃏", "2–4", "cartas", "playable", "Cantos, caídas y mesa limpia. Nativo en Compose.", native = true),
    ShellGame("furia", "Furia de Colores", "🎴", "2–4", "cartas", "playable", "Combina colores y grita ¡Última! Nativo en Compose.", native = true),
    ShellGame("domino", "Dominó", "🁫", "2–4", "tablero", "playable", "Doble 6, parejas y tranca. Nativo en Compose.", native = true),
    ShellGame("dados", "Dados Mentirosos", "🎲", "2–6", "dados", "playable", "Cubiletes, pujas y mentiras. Nativo en Compose.", native = true),
    ShellGame("ajedrez", "Ajedrez", "♟️", "2", "tablero", "playable", "Reglas completas e IA alfa-beta. Nativo en Compose.", native = true),
    ShellGame("damas", "Damas", "⛀", "2", "tablero", "playable", "Captura obligatoria y coronación. Nativo en Compose.", native = true),
    ShellGame("serpientes", "Serpientes y Escaleras", "🐍", "2–4", "tablero", "playable", "Tablero, dado y bots. Nativo en Compose.", native = true),
    ShellGame("mancala", "Mancala", "🫘", "2", "tablero", "playable", "Siembra, captura y repite turno. Nativo en Compose.", native = true),
    ShellGame("go", "Go", "⚫", "2", "tablero", "playable", "9×9 y 13×13, capturas y ko. Nativo en Compose.", native = true),
    ShellGame("jungle", "Jungle", "🐯", "2", "tablero", "playable", "Río, trampas y saltos de tigre. Nativo en Compose.", native = true),
    ShellGame("ludo", "Ludo", "🎯", "2–4", "tablero", "playable", "Cuatro fichas, capturas y meta exacta. Nativo en Compose.", native = true),
    ShellGame("backgammon", "Backgammon", "🎲", "2", "tablero", "playable", "Barra, dobles y bear-off. Nativo en Compose.", native = true),
    ShellGame("xiangqi", "Xiangqi", "🀄", "2", "tablero", "playable", "Ajedrez chino con palacio y río. Nativo en Compose.", native = true),
    ShellGame("chino", "Damas Chinas", "✳️", "2–6", "tablero", "playable", "Estrella de 121 hoyos. Nativo en Compose.", native = true),
    ShellGame("reversi", "Reversi", "⚫", "2", "tablero", "playable", "Voltea fichas y controla las esquinas. Nativo en Compose.", native = true),
    ShellGame("ur", "Juego Real de Ur", "🏺", "2", "tablero", "playable", "Carrera ancestral con rosetas y capturas. Nativo en Compose.", native = true),
    ShellGame("brisca", "Brisca", "🃏", "2–4", "cartas", "playable", "Triunfo, bazas y baraja española. Nativo en Compose.", native = true),
    ShellGame("truco", "Truco", "🎴", "2", "cartas", "playable", "Envido, truco y retruco. Nativo en Compose.", native = true),
    ShellGame("shogi", "Shogi", "将", "2", "tablero", "playable", "Ajedrez japonés con promociones, capturas y drops. Nativo en Compose.", native = true),
    ShellGame("janggi", "Janggi", "象", "2", "tablero", "playable", "Ajedrez coreano con palacio, río y cañón. Nativo en Compose.", native = true),
    ShellGame("luzhanqi", "Luzhanqi", "🎖️", "2–4", "tablero", "playable", "Guerra de piezas con niebla, trenes y minas. Nativo en Compose.", native = true),
    ShellGame("mexican-train", "Tren Mexicano", "🚂", "2–8", "tablero", "playable", "Dominó doble 12 con trenes y tres rondas. Nativo en Compose.", native = true),
    ShellGame("chickenfoot", "Chickenfoot", "🐔", "2–8", "tablero", "playable", "Dominó en forma de pata con doble 9. Nativo en Compose.", native = true),
    ShellGame("aeroplane", "Aeroplane Chess", "✈️", "2–4", "tablero", "playable", "Carrera aérea con saltos y capturas. Nativo en Compose.", native = true),
    ShellGame("mahjong", "Mahjong", "🀄", "4", "tablero", "playable", "Mahjong americano con flores y comodines. Nativo en Compose.", native = true),
    ShellGame("riichi", "Riichi Mahjong", "🀄", "4", "tablero", "playable", "Mahjong japonés con riichi, chi y yaku. Nativo en Compose.", native = true),
    ShellGame("mahjong-cards", "Mahjong con naipes", "🃏", "4", "cartas", "playable", "Mahjong americano en edición de naipes. Nativo en Compose.", native = true),
    ShellGame("three-chess", "Ajedrez para tres", "♞", "3", "tablero", "playable", "Tres ejércitos en un tablero de 12×12. Nativo en Compose.", native = true),
    ShellGame("soroban", "Soroban", "🧮", "1", "herramienta", "playable", "Ábaco japonés de 13 varillas. Nativo en Compose.", native = true),
)

val PLAYABLE_GAMES = SHELL_GAMES.filter { it.status == "playable" }

/** Ruta estable para abrir una mesa desde el catálogo, un QR o una prueba. */
fun screenForGame(id: String): ShellScreen? = when (id) {
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
    "reversi" -> ShellScreen.Reversi
    "ur" -> ShellScreen.Ur
    "brisca" -> ShellScreen.Brisca
    "truco" -> ShellScreen.Truco
    "soroban" -> ShellScreen.Soroban
    "shogi" -> ShellScreen.Shogi
    "janggi" -> ShellScreen.Janggi
    "aeroplane" -> ShellScreen.Aeroplane
    "mahjong" -> ShellScreen.Mahjong
    "riichi" -> ShellScreen.Riichi
    "mahjong-cards" -> ShellScreen.MahjongCards
    "luzhanqi" -> ShellScreen.Luzhanqi
    "mexican-train" -> ShellScreen.MexicanTrain
    "chickenfoot" -> ShellScreen.Chickenfoot
    "three-chess" -> ShellScreen.ThreeChess
    else -> null
}
