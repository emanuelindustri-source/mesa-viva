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

/** Catálogo de la cáscara. Los jugables abren la mesa web; Serpientes es nativo. */
val SHELL_GAMES: List<ShellGame> = listOf(
    ShellGame("poker", "Póker de la casa", "♠️", "2–6", "cartas", "playable", "Texas Hold'em con fichas de práctica."),
    ShellGame("caida", "Caída", "🃏", "2–4", "cartas", "playable", "Cantos, caídas y mesa limpia."),
    ShellGame("furia", "Furia de Colores", "🎴", "2–4", "cartas", "playable", "Combina colores y grita ¡Última!"),
    ShellGame("domino", "Dominó", "🁫", "2–4", "tablero", "playable", "Doble 6, parejas y tranca. Nativo en Compose.", native = true),
    ShellGame("dados", "Dados Mentirosos", "🎲", "2–6", "dados", "playable", "Cubiletes, pujas y mentiras."),
    ShellGame("ajedrez", "Ajedrez", "♟️", "2", "tablero", "playable", "Reglas completas e IA alfa-beta. Nativo en Compose.", native = true),
    ShellGame("damas", "Damas", "⛀", "2", "tablero", "playable", "Captura obligatoria y coronación."),
    ShellGame("serpientes", "Serpientes y Escaleras", "🐍", "2–4", "tablero", "playable", "Tablero, dado y bots. Nativo en Compose.", native = true),
    ShellGame("mancala", "Mancala", "🫘", "2", "tablero", "playable", "Siembra, captura y repite turno."),
    ShellGame("go", "Go", "⚫", "2", "tablero", "playable", "9×9 y 13×13, capturas y ko."),
    ShellGame("jungle", "Jungle", "🐯", "2", "tablero", "playable", "Río, trampas y saltos de tigre."),
    ShellGame("ludo", "Ludo", "🎯", "2–4", "tablero", "playable", "Cuatro fichas, capturas y meta exacta."),
    ShellGame("backgammon", "Backgammon", "🎲", "2", "tablero", "playable", "Barra, dobles y bear-off."),
    ShellGame("xiangqi", "Xiangqi", "🀄", "2", "tablero", "playable", "Ajedrez chino con palacio y río."),
    ShellGame("chino", "Damas Chinas", "✳️", "2–6", "tablero", "playable", "Estrella de 121 hoyos."),
    ShellGame("shogi", "Shogi", "将", "2", "tablero", "soon", "Ajedrez japonés con piezas capturadas."),
    ShellGame("janggi", "Janggi", "象", "2", "tablero", "research", "Ajedrez coreano."),
    ShellGame("luzhanqi", "Luzhanqi", "🎖️", "2–4", "tablero", "research", "Guerra de piezas con niebla."),
    ShellGame("ur", "Juego Real de Ur", "🏺", "2", "tablero", "soon", "El abuelo del backgammon."),
    ShellGame("mexican-train", "Tren Mexicano", "🚂", "2–8", "tablero", "soon", "Dominó con trenes y pozo."),
    ShellGame("chickenfoot", "Chickenfoot", "🐔", "2–8", "tablero", "soon", "Dominó en forma de pata."),
    ShellGame("aeroplane", "Aeroplane Chess", "✈️", "2–4", "tablero", "soon", "Carrera aérea de dados."),
    ShellGame("mahjong", "Mahjong", "🀄", "4", "tablero", "research", "Mahjong americano."),
    ShellGame("riichi", "Riichi Mahjong", "🀄", "4", "tablero", "research", "Mahjong japonés."),
    ShellGame("mahjong-cards", "Mahjong con naipes", "🃏", "4", "cartas", "research", "Mahjong con baraja."),
    ShellGame("three-chess", "Ajedrez para tres", "♞", "3", "tablero", "research", "Tres ejércitos en un tablero."),
    ShellGame("soroban", "Soroban", "🧮", "1", "herramienta", "soon", "Ábaco japonés."),
)

val PLAYABLE_GAMES = SHELL_GAMES.filter { it.status == "playable" }
