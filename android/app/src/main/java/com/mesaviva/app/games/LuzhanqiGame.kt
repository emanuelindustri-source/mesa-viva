package com.mesaviva.app.games

import kotlin.random.Random

/**
 * Luzhanqi (陆战棋), la guerra de piezas con niebla.
 *
 * Tablero de 5 columnas por 12 filas. Cada bando tiene 25 piezas: 1 bandera,
 * 3 minas, 2 bombas y 19 combatientes. Las piezas rivales se muestran boca
 * abajo hasta que combaten. Las minas y la bandera no se mueven; el ingeniero
 * desactiva minas; la bomba muere con cualquier rival. Gana quien capture la
 * bandera o deje al rival sin piezas móviles.
 */
enum class LuzhanqiSide { Red, Blue }

enum class LuzhanqiRank(val label: String, val power: Int) {
    Commander("司令", 10),
    General("军长", 9),
    Division("师长", 8),
    Brigade("旅长", 7),
    Regiment("团长", 6),
    Battalion("营长", 5),
    Company("连长", 4),
    Platoon("排长", 3),
    Engineer("工兵", 2),
    Bomb("炸弹", 1),
    Landmine("地雷", 0),
    Flag("军旗", -1),
}

enum class LuzhanqiPhase { Play, MatchEnd }

data class LuzhanqiPiece(
    val id: Int,
    val side: LuzhanqiSide,
    val rank: LuzhanqiRank,
    val cell: Int,
    val revealed: Boolean = false,
)

data class LuzhanqiBattle(
    val attackerRank: LuzhanqiRank,
    val defenderRank: LuzhanqiRank,
    val outcome: String,
)

data class LuzhanqiMove(
    val from: Int,
    val to: Int,
    val side: LuzhanqiSide,
    val attacker: LuzhanqiRank,
    val defender: LuzhanqiRank?,
    val battle: LuzhanqiBattle?,
    val flagCaptured: Boolean = false,
)

data class LuzhanqiState(
    val pieces: List<LuzhanqiPiece>,
    val turn: LuzhanqiSide = LuzhanqiSide.Red,
    val phase: LuzhanqiPhase = LuzhanqiPhase.Play,
    val winner: LuzhanqiSide? = null,
    val lastMove: LuzhanqiMove? = null,
    val history: List<String> = emptyList(),
    val redRevealed: Int = 0,
    val blueRevealed: Int = 0,
)

data class LuzhanqiResult(val state: LuzhanqiState, val error: String? = null)

class LuzhanqiGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val COLS = 5
        const val ROWS = 12
        const val CELLS = COLS * ROWS

        fun cell(row: Int, col: Int): Int = row * COLS + col
        fun row(cell: Int): Int = cell / COLS
        fun col(cell: Int): Int = cell % COLS
        fun other(side: LuzhanqiSide): LuzhanqiSide = if (side == LuzhanqiSide.Red) LuzhanqiSide.Blue else LuzhanqiSide.Red

        fun isCamp(cell: Int): Boolean {
            val r = row(cell)
            val c = col(cell)
            return r in setOf(1, 3, 5, 6, 8, 10) && (
                (c == 1 || c == 3) && r != 5 && r != 6 ||
                    ((r == 5 || r == 6) && c == 2)
                )
        }

        fun isRailway(cell: Int): Boolean {
            val r = row(cell)
            val c = col(cell)
            return r in setOf(1, 5, 6, 10) || c == 0 || c == 4
        }

        fun isHeadquarters(cell: Int): Boolean {
            val r = row(cell)
            val c = col(cell)
            return (r == 0 || r == ROWS - 1) && (c == 1 || c == 3)
        }

        fun label(cell: Int): String = "${('A' + col(cell))}${ROWS - row(cell)}"
    }

    fun newMatch(): LuzhanqiState {
        val red = setupSide(LuzhanqiSide.Red, 0)
        val blue = setupSide(LuzhanqiSide.Blue, 25)
        return LuzhanqiState(pieces = red + blue, history = listOf("Comienza la guerra de piezas"))
    }

    private fun setupSide(side: LuzhanqiSide, idOffset: Int): List<LuzhanqiPiece> {
        val red = side == LuzhanqiSide.Red
        val rows = if (red) 0..4 else 7..11
        val cells = mutableListOf<Int>()
        for (r in rows) for (c in 0 until COLS) cells += cell(r, c)
        val flagCell = if (red) cell(0, 1) else cell(11, 1)
        val mineCells = if (red) listOf(cell(0, 0), cell(0, 2), cell(0, 3)) else listOf(cell(11, 0), cell(11, 2), cell(11, 3))
        val available = cells.filterNot { it == flagCell || it in mineCells }.toMutableList()
        available.shuffle(random)

        val ranks = mutableListOf<LuzhanqiRank>()
        ranks += LuzhanqiRank.Commander
        ranks += LuzhanqiRank.General
        repeat(2) { ranks += LuzhanqiRank.Division }
        repeat(2) { ranks += LuzhanqiRank.Brigade }
        repeat(2) { ranks += LuzhanqiRank.Regiment }
        repeat(2) { ranks += LuzhanqiRank.Battalion }
        repeat(3) { ranks += LuzhanqiRank.Company }
        repeat(3) { ranks += LuzhanqiRank.Platoon }
        repeat(3) { ranks += LuzhanqiRank.Engineer }
        repeat(2) { ranks += LuzhanqiRank.Bomb }
        ranks.shuffle(random)

        val pieces = mutableListOf<LuzhanqiPiece>()
        var id = idOffset
        pieces += LuzhanqiPiece(id++, side, LuzhanqiRank.Flag, flagCell)
        mineCells.forEach { pieces += LuzhanqiPiece(id++, side, LuzhanqiRank.Landmine, it) }
        ranks.forEachIndexed { index, rank ->
            pieces += LuzhanqiPiece(id++, side, rank, available[index])
        }
        return pieces
    }

    fun pieceAt(state: LuzhanqiState, cell: Int): LuzhanqiPiece? = state.pieces.firstOrNull { it.cell == cell }

    fun piecesOf(state: LuzhanqiState, side: LuzhanqiSide): List<LuzhanqiPiece> = state.pieces.filter { it.side == side }

    fun flagOf(state: LuzhanqiState, side: LuzhanqiSide): LuzhanqiPiece? =
        state.pieces.firstOrNull { it.side == side && it.rank == LuzhanqiRank.Flag }

    fun moves(state: LuzhanqiState, pieceId: Int): List<Int> {
        if (state.phase == LuzhanqiPhase.MatchEnd) return emptyList()
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return emptyList()
        if (piece.side != state.turn) return emptyList()
        if (piece.rank == LuzhanqiRank.Flag || piece.rank == LuzhanqiRank.Landmine) return emptyList()
        val r = row(piece.cell)
        val c = col(piece.cell)
        val result = linkedSetOf<Int>()

        fun consider(target: Int, enqueue: Boolean, queue: ArrayDeque<Int>) {
            if (target !in 0 until CELLS) return
            val tr = row(target)
            val tc = col(target)
            if (kotlin.math.abs(tr - r) + kotlin.math.abs(tc - c) > 8) return
            val occupant = pieceAt(state, target)
            if (occupant != null) {
                if (occupant.side != piece.side && !isCamp(target)) result += target
                return
            }
            result += target
            if (enqueue) queue.addLast(target)
        }

        val queue = ArrayDeque<Int>()
        val visited = mutableSetOf<Int>()
        val startRailway = isRailway(piece.cell)
        // Los movimientos de un paso siempre están disponibles.
        listOf(piece.cell - COLS, piece.cell + COLS, piece.cell - 1, piece.cell + 1).forEach { target ->
            if (target in 0 until CELLS) {
                val sameRow = row(target) == r
                val sameCol = col(target) == c
                if ((sameRow || sameCol) && kotlin.math.abs((target % COLS) - c) <= 1) {
                    consider(target, false, queue)
                }
            }
        }
        if (startRailway) {
            queue.addLast(piece.cell)
            visited += piece.cell
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val cr = row(current)
                val cc = col(current)
                listOf(current - COLS, current + COLS, current - 1, current + 1).forEach { target ->
                    if (target !in 0 until CELLS) return@forEach
                    val tr = row(target)
                    val tc = col(target)
                    if (kotlin.math.abs(tr - cr) + kotlin.math.abs(tc - cc) != 1) return@forEach
                    if (!isRailway(target)) return@forEach
                    val occupant = pieceAt(state, target)
                    if (occupant != null) {
                        if (occupant.side != piece.side && target !in visited && !isCamp(target)) result += target
                        return@forEach
                    }
                    if (target !in visited) {
                        visited += target
                        result += target
                        queue.addLast(target)
                    }
                }
            }
        }
        return result.toList()
    }

    fun allMoves(state: LuzhanqiState, side: LuzhanqiSide): List<Pair<Int, Int>> {
        val temp = state.copy(turn = side)
        return piecesOf(state, side).flatMap { piece -> moves(temp, piece.id).map { piece.id to it } }
    }

    fun move(state: LuzhanqiState, pieceId: Int, target: Int): LuzhanqiResult {
        if (state.phase == LuzhanqiPhase.MatchEnd) return LuzhanqiResult(state, "luzhanqi.error.over")
        val piece = state.pieces.firstOrNull { it.id == pieceId }
            ?: return LuzhanqiResult(state, "luzhanqi.error.piece")
        if (piece.side != state.turn) return LuzhanqiResult(state, "luzhanqi.error.turn")
        if (target !in moves(state, pieceId)) return LuzhanqiResult(state, "luzhanqi.error.move")

        val pieces = state.pieces.toMutableList()
        val attackerIndex = pieces.indexOfFirst { it.id == pieceId }
        val defender = pieces.firstOrNull { it.cell == target }
        var flagCaptured = false
        var battle: LuzhanqiBattle? = null
        var attackerSurvives = true
        var defenderSurvives = false
        val lines = mutableListOf<String>()
        lines += "${if (piece.side == LuzhanqiSide.Red) "Rojo" else "Azul"} mueve ${piece.rank.label} ${label(piece.cell)} → ${label(target)}"

        if (defender == null) {
            pieces[attackerIndex] = piece.copy(cell = target, revealed = piece.revealed)
        } else {
            val ar = piece.rank
            val dr = defender.rank
            when {
                dr == LuzhanqiRank.Flag -> {
                    flagCaptured = true
                    attackerSurvives = true
                    defenderSurvives = false
                    lines += "¡Bandera capturada!"
                }
                ar == LuzhanqiRank.Bomb || dr == LuzhanqiRank.Bomb -> {
                    attackerSurvives = false
                    defenderSurvives = false
                    battle = LuzhanqiBattle(ar, dr, "bomba")
                    lines += "La bomba arrasa: ambos caen"
                }
                dr == LuzhanqiRank.Landmine -> {
                    if (ar == LuzhanqiRank.Engineer) {
                        attackerSurvives = true
                        defenderSurvives = false
                        battle = LuzhanqiBattle(ar, dr, "ingeniero")
                        lines += "El ingeniero desactiva la mina"
                    } else {
                        attackerSurvives = false
                        defenderSurvives = true
                        battle = LuzhanqiBattle(ar, dr, "mina")
                        lines += "La mina destruye al atacante"
                    }
                }
                ar.power > dr.power -> {
                    attackerSurvives = true
                    defenderSurvives = false
                    battle = LuzhanqiBattle(ar, dr, "ataque")
                    lines += "${ar.label} vence a ${dr.label}"
                }
                ar.power < dr.power -> {
                    attackerSurvives = false
                    defenderSurvives = true
                    battle = LuzhanqiBattle(ar, dr, "defensa")
                    lines += "${dr.label} resiste a ${ar.label}"
                }
                else -> {
                    attackerSurvives = false
                    defenderSurvives = false
                    battle = LuzhanqiBattle(ar, dr, "empate")
                    lines += "Fuerzas iguales: ambos caen"
                }
            }
            // Revela a los participantes del combate.
            val defenderIndex = pieces.indexOfFirst { it.id == defender.id }
            pieces[defenderIndex] = pieces[defenderIndex].copy(revealed = true)
            pieces[attackerIndex] = pieces[attackerIndex].copy(revealed = true)
            if (attackerSurvives) {
                pieces[attackerIndex] = pieces[attackerIndex].copy(cell = target)
            }
            if (!defenderSurvives) {
                pieces.removeAt(pieces.indexOfFirst { it.id == defender.id })
            }
            if (!attackerSurvives) {
                pieces.removeAt(pieces.indexOfFirst { it.id == pieceId })
            }
        }

        val next = state.copy(
            pieces = pieces.toList(),
            turn = other(state.turn),
            lastMove = LuzhanqiMove(
                from = piece.cell,
                to = target,
                side = piece.side,
                attacker = piece.rank,
                defender = defender?.rank,
                battle = battle,
                flagCaptured = flagCaptured,
            ),
            history = (state.history + lines).takeLast(160),
            redRevealed = pieces.count { it.side == LuzhanqiSide.Red && it.revealed },
            blueRevealed = pieces.count { it.side == LuzhanqiSide.Blue && it.revealed },
        )
        if (flagCaptured) {
            return LuzhanqiResult(next.copy(phase = LuzhanqiPhase.MatchEnd, winner = piece.side))
        }
        // Si un bando no puede mover, pierde.
        val redMoves = allMoves(next, LuzhanqiSide.Red)
        val blueMoves = allMoves(next, LuzhanqiSide.Blue)
        if (redMoves.isEmpty() && blueMoves.isNotEmpty()) {
            return LuzhanqiResult(next.copy(phase = LuzhanqiPhase.MatchEnd, winner = LuzhanqiSide.Blue))
        }
        if (blueMoves.isEmpty() && redMoves.isNotEmpty()) {
            return LuzhanqiResult(next.copy(phase = LuzhanqiPhase.MatchEnd, winner = LuzhanqiSide.Red))
        }
        if (redMoves.isEmpty() && blueMoves.isEmpty()) {
            return LuzhanqiResult(next.copy(phase = LuzhanqiPhase.MatchEnd, winner = null))
        }
        return LuzhanqiResult(next)
    }

    fun resign(state: LuzhanqiState): LuzhanqiResult =
        LuzhanqiResult(state.copy(phase = LuzhanqiPhase.MatchEnd, winner = other(state.turn)))

    /** Bot: prioriza bandera, capturas rentables y avance hacia el rival. */
    fun botMove(state: LuzhanqiState): Pair<Int, Int>? {
        val options = allMoves(state, state.turn)
        if (options.isEmpty()) return null
        val enemyFlag = flagOf(state, other(state.turn)) ?: return options.random(random)
        val enemyRow = row(enemyFlag.cell)
        val scored = options.map { (pieceId, target) ->
            val piece = state.pieces.first { it.id == pieceId }
            val defender = pieceAt(state, target)
            var score = -kotlin.math.abs(row(target) - enemyRow) * 3
            if (defender == null) {
                if (isCamp(target)) score += 6
            } else {
                score -= 20
                when {
                    defender.rank == LuzhanqiRank.Flag -> score += 10000
                    piece.rank == LuzhanqiRank.Bomb -> score += 120
                    defender.rank == LuzhanqiRank.Landmine && piece.rank == LuzhanqiRank.Engineer -> score += 220
                    piece.rank.power > defender.rank.power -> score += 100 + defender.rank.power * 8
                    piece.rank.power == defender.rank.power -> score -= 40
                    else -> score -= 80
                }
            }
            if (isHeadquarters(target)) score += 12
            (pieceId to target) to score
        }
        return scored.maxByOrNull { it.second }?.first
    }
}
