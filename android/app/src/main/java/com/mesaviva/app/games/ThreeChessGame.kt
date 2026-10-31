package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Ajedrez para tres ejércitos.
 *
 * Tablero cuadrado de 12×12 con tres campamentos de 8×2: Norte (arriba),
 * Oeste (izquierda) y Sur (abajo). Cada ejército conserva las piezas y los
 * movimientos del ajedrez ortodoxo, girados hacia su frente. No hay enroque.
 * Cuando un rey cae o queda sin jugadas legales, su ejército se retira y los
 * otros dos continúan. Gana el último rey en pie.
 */
enum class ThreeSide(val label: String) {
    North("Norte"),
    West("Oeste"),
    South("Sur"),
}

enum class ThreePieceType(val label: String, val value: Int) {
    King("Rey", 0),
    Queen("Dama", 9),
    Rook("Torre", 5),
    Bishop("Alfil", 3),
    Knight("Caballo", 3),
    Pawn("Peón", 1),
}

enum class ThreePhase { Play, MatchEnd }

data class ThreePiece(
    val id: Int,
    val side: ThreeSide,
    val type: ThreePieceType,
    val cell: Int,
)

data class ThreeMove(
    val from: Int,
    val to: Int,
    val side: ThreeSide,
    val piece: ThreePieceType,
    val captured: ThreePieceType?,
    val promoted: Boolean = false,
    val check: Boolean = false,
    val eliminated: List<ThreeSide> = emptyList(),
)

data class ThreeChessState(
    val pieces: List<ThreePiece>,
    val turn: ThreeSide = ThreeSide.North,
    val phase: ThreePhase = ThreePhase.Play,
    val winner: ThreeSide? = null,
    val eliminated: Set<ThreeSide> = emptySet(),
    val lastMove: ThreeMove? = null,
    val history: List<String> = emptyList(),
)

data class ThreeChessResult(val state: ThreeChessState, val error: String? = null)

class ThreeChessGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val SIZE = 12
        const val CELLS = SIZE * SIZE

        fun cell(row: Int, col: Int): Int = row * SIZE + col
        fun row(cell: Int): Int = cell / SIZE
        fun col(cell: Int): Int = cell % SIZE
        fun inBoard(row: Int, col: Int): Boolean = row in 0 until SIZE && col in 0 until SIZE

        fun isHome(side: ThreeSide, cell: Int): Boolean {
            val r = row(cell)
            val c = col(cell)
            return when (side) {
                ThreeSide.North -> r <= 1 && c in 2..9
                ThreeSide.West -> c <= 1 && r in 2..9
                ThreeSide.South -> r >= 10 && c in 2..9
            }
        }

        fun pawnStart(side: ThreeSide): Int = when (side) {
            ThreeSide.North -> 1
            ThreeSide.West -> 1
            ThreeSide.South -> 10
        }
    }

    fun newMatch(): ThreeChessState {
        var id = 0
        val pieces = mutableListOf<ThreePiece>()
        val back = listOf(
            ThreePieceType.Rook,
            ThreePieceType.Knight,
            ThreePieceType.Bishop,
            ThreePieceType.Queen,
            ThreePieceType.King,
            ThreePieceType.Bishop,
            ThreePieceType.Knight,
            ThreePieceType.Rook,
        )
        for (file in 0..7) {
            pieces += ThreePiece(id++, ThreeSide.North, back[file], cell(0, file + 2))
            pieces += ThreePiece(id++, ThreeSide.North, ThreePieceType.Pawn, cell(1, file + 2))
        }
        for (file in 0..7) {
            pieces += ThreePiece(id++, ThreeSide.West, back[file], cell(file + 2, 0))
            pieces += ThreePiece(id++, ThreeSide.West, ThreePieceType.Pawn, cell(file + 2, 1))
        }
        for (file in 0..7) {
            pieces += ThreePiece(id++, ThreeSide.South, back[file], cell(11, file + 2))
            pieces += ThreePiece(id++, ThreeSide.South, ThreePieceType.Pawn, cell(10, file + 2))
        }
        return ThreeChessState(pieces = pieces, history = listOf("Tres ejércitos en la mesa"))
    }

    fun pieceAt(state: ThreeChessState, cell: Int): ThreePiece? = state.pieces.firstOrNull { it.cell == cell }

    fun kingOf(state: ThreeChessState, side: ThreeSide): ThreePiece? =
        state.pieces.firstOrNull { it.side == side && it.type == ThreePieceType.King }

    fun activeSides(state: ThreeChessState): List<ThreeSide> =
        ThreeSide.entries.filter { it !in state.eliminated }

    private fun forward(side: ThreeSide): Pair<Int, Int> = when (side) {
        ThreeSide.North -> 1 to 0
        ThreeSide.West -> 0 to 1
        ThreeSide.South -> -1 to 0
    }

    private fun pseudoMoves(state: ThreeChessState, piece: ThreePiece): List<Int> {
        val r = row(piece.cell)
        val c = col(piece.cell)
        val result = linkedSetOf<Int>()

        fun add(targetRow: Int, targetCol: Int) {
            if (!inBoard(targetRow, targetCol)) return
            val occupant = pieceAt(state, cell(targetRow, targetCol))
            if (occupant == null || occupant.side != piece.side) result += cell(targetRow, targetCol)
        }

        fun slide(dr: Int, dc: Int) {
            var tr = r + dr
            var tc = c + dc
            while (inBoard(tr, tc)) {
                val occupant = pieceAt(state, cell(tr, tc))
                if (occupant == null) {
                    result += cell(tr, tc)
                } else {
                    if (occupant.side != piece.side) result += cell(tr, tc)
                    break
                }
                tr += dr
                tc += dc
            }
        }

        when (piece.type) {
            ThreePieceType.Pawn -> {
                val (dr, dc) = forward(piece.side)
                val oneRow = r + dr
                val oneCol = c + dc
                if (inBoard(oneRow, oneCol) && pieceAt(state, cell(oneRow, oneCol)) == null) {
                    result += cell(oneRow, oneCol)
                    val start = when (piece.side) {
                        ThreeSide.North -> r == 1
                        ThreeSide.West -> c == 1
                        ThreeSide.South -> r == 10
                    }
                    val twoRow = r + dr * 2
                    val twoCol = c + dc * 2
                    if (start && inBoard(twoRow, twoCol) && pieceAt(state, cell(twoRow, twoCol)) == null) {
                        result += cell(twoRow, twoCol)
                    }
                }
                // Capturas diagonales.
                if (dr != 0) {
                    add(oneRow, oneCol - 1)
                    add(oneRow, oneCol + 1)
                } else {
                    add(oneRow - 1, oneCol)
                    add(oneRow + 1, oneCol)
                }
            }
            ThreePieceType.Knight -> {
                val jumps = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1,
                )
                jumps.forEach { (dr, dc) -> add(r + dr, c + dc) }
            }
            ThreePieceType.Bishop -> {
                slide(1, 1); slide(1, -1); slide(-1, 1); slide(-1, -1)
            }
            ThreePieceType.Rook -> {
                slide(1, 0); slide(-1, 0); slide(0, 1); slide(0, -1)
            }
            ThreePieceType.Queen -> {
                slide(1, 1); slide(1, -1); slide(-1, 1); slide(-1, -1)
                slide(1, 0); slide(-1, 0); slide(0, 1); slide(0, -1)
            }
            ThreePieceType.King -> {
                for (dr in -1..1) for (dc in -1..1) {
                    if (dr != 0 || dc != 0) add(r + dr, c + dc)
                }
            }
        }
        return result.toList()
    }

    private fun attacks(state: ThreeChessState, piece: ThreePiece, target: Int): Boolean {
        if (piece.type == ThreePieceType.Pawn) {
            val (dr, dc) = forward(piece.side)
            val r = row(piece.cell) + dr
            return if (dr != 0) {
                inBoard(r, col(piece.cell) - 1) && cell(r, col(piece.cell) - 1) == target ||
                    inBoard(r, col(piece.cell) + 1) && cell(r, col(piece.cell) + 1) == target
            } else {
                inBoard(row(piece.cell) - 1, col(piece.cell) + dc) && cell(row(piece.cell) - 1, col(piece.cell) + dc) == target ||
                    inBoard(row(piece.cell) + 1, col(piece.cell) + dc) && cell(row(piece.cell) + 1, col(piece.cell) + dc) == target
            }
        }
        return target in pseudoMoves(state, piece)
    }

    fun isAttacked(state: ThreeChessState, cell: Int, by: ThreeSide): Boolean =
        state.pieces.any { it.side == by && it.side !in state.eliminated && attacks(state, it, cell) }

    fun isInCheck(state: ThreeChessState, side: ThreeSide): Boolean {
        val king = kingOf(state, side) ?: return false
        return activeSides(state).any { other -> other != side && isAttacked(state, king.cell, other) }
    }

    private fun withMove(state: ThreeChessState, piece: ThreePiece, target: Int): ThreeChessState {
        val pieces = state.pieces.toMutableList()
        pieces.removeAll { it.cell == target && it.side != piece.side }
        val index = pieces.indexOfFirst { it.id == piece.id }
        var moved = piece.copy(cell = target)
        val promote = when (piece.side) {
            ThreeSide.North -> row(target) == SIZE - 1
            ThreeSide.West -> col(target) == SIZE - 1
            ThreeSide.South -> row(target) == 0
        }
        if (piece.type == ThreePieceType.Pawn && promote) moved = moved.copy(type = ThreePieceType.Queen)
        pieces[index] = moved
        return state.copy(pieces = pieces.toList())
    }

    fun legalMoves(state: ThreeChessState, pieceId: Int): List<Int> {
        if (state.phase == ThreePhase.MatchEnd) return emptyList()
        val piece = state.pieces.firstOrNull { it.id == pieceId } ?: return emptyList()
        if (piece.side != state.turn || piece.side in state.eliminated) return emptyList()
        return pseudoMoves(state, piece).filter { to ->
            val next = withMove(state, piece, to)
            val king = kingOf(next, piece.side)
            king != null && activeSides(next).none { other -> other != piece.side && isAttacked(next, king.cell, other) }
        }
    }

    fun legalMovesForSide(state: ThreeChessState, side: ThreeSide): List<Pair<Int, Int>> =
        state.pieces.filter { it.side == side }.flatMap { piece ->
            pseudoMoves(state, piece).filter { to ->
                val next = withMove(state, piece, to)
                val king = kingOf(next, side)
                king != null && activeSides(next).none { other -> other != side && isAttacked(next, king.cell, other) }
            }.map { piece.id to it }
        }

    fun move(state: ThreeChessState, pieceId: Int, target: Int): ThreeChessResult {
        if (state.phase == ThreePhase.MatchEnd) return ThreeChessResult(state, "three.error.over")
        val piece = state.pieces.firstOrNull { it.id == pieceId }
            ?: return ThreeChessResult(state, "three.error.piece")
        if (piece.side != state.turn) return ThreeChessResult(state, "three.error.turn")
        if (target !in legalMoves(state, pieceId)) return ThreeChessResult(state, "three.error.move")
        val captured = pieceAt(state, target)
        val moved = withMove(state, piece, target)
        val promoted = piece.type == ThreePieceType.Pawn && moved.pieces.first { it.id == pieceId }.type == ThreePieceType.Queen
        var next = moved.copy(
            turn = nextActive(moved, piece.side),
            lastMove = ThreeMove(
                from = piece.cell,
                to = target,
                side = piece.side,
                piece = piece.type,
                captured = captured?.type,
                promoted = promoted,
            ),
            history = (
                state.history + buildString {
                    append("${piece.side.label} ")
                    append("${piece.type.label} ${coord(piece.cell)}→${coord(target)}")
                    if (captured != null) append(" ×${captured.type.label}")
                    if (promoted) append(" ♛")
                }
                ).takeLast(200),
        )
        val eliminated = mutableListOf<ThreeSide>()
        for (side in activeSides(next)) {
            val king = kingOf(next, side)
            if (king == null) {
                eliminated += side
            } else {
                val hasMove = legalMovesForSide(next, side).isNotEmpty()
                if (!hasMove) eliminated += side
            }
        }
        if (eliminated.isNotEmpty()) {
            next = next.copy(
                pieces = next.pieces.filterNot { it.side in eliminated },
                eliminated = next.eliminated + eliminated,
                history = (next.history + eliminated.joinToString { "${it.label} queda fuera" }).takeLast(200),
                lastMove = next.lastMove?.copy(eliminated = eliminated),
            )
        }
        val alive = activeSides(next)
        next = if (alive.size <= 1) {
            next.copy(phase = ThreePhase.MatchEnd, winner = alive.firstOrNull())
        } else if (next.turn in next.eliminated) {
            next.copy(turn = nextActive(next, next.turn))
        } else {
            next
        }
        val checked = alive.any { isInCheck(next, it) }
        if (checked && next.lastMove != null) next = next.copy(lastMove = next.lastMove.copy(check = true))
        return ThreeChessResult(next)
    }

    private fun nextActive(state: ThreeChessState, from: ThreeSide): ThreeSide {
        val order = ThreeSide.entries
        var index = order.indexOf(from)
        repeat(order.size) {
            index = (index + 1) % order.size
            if (order[index] !in state.eliminated) return order[index]
        }
        return from
    }

    fun resign(state: ThreeChessState, side: ThreeSide): ThreeChessResult {
        val eliminated = state.eliminated + side
        val next = state.copy(
            pieces = state.pieces.filterNot { it.side == side },
            eliminated = eliminated,
            phase = if (activeSides(state.copy(eliminated = eliminated)).size <= 1) ThreePhase.MatchEnd else ThreePhase.Play,
            winner = activeSides(state.copy(eliminated = eliminated)).singleOrNull(),
            turn = nextActive(state.copy(eliminated = eliminated), side),
        )
        return ThreeChessResult(next)
    }

    fun botMove(state: ThreeChessState): Pair<Int, Int>? {
        val options = legalMovesForSide(state, state.turn)
        if (options.isEmpty()) return null
        val scored = options.map { (pieceId, target) ->
            val piece = state.pieces.first { it.id == pieceId }
            val captured = pieceAt(state, target)
            var score = captured?.type?.value?.times(10) ?: 0
            if (piece.type == ThreePieceType.Pawn && target in promotionCells(piece.side)) score += 80
            val next = withMove(state, piece, target)
            if (isInCheck(next, nextActive(next, piece.side))) score += 15
            score += random.nextInt(0, 5)
            (pieceId to target) to score
        }
        return scored.maxByOrNull { it.second }?.first
    }

    private fun promotionCells(side: ThreeSide): Set<Int> = when (side) {
        ThreeSide.North -> (0 until SIZE).map { cell(SIZE - 1, it) }.toSet()
        ThreeSide.West -> (0 until SIZE).map { cell(it, SIZE - 1) }.toSet()
        ThreeSide.South -> (0 until SIZE).map { cell(0, it) }.toSet()
    }

    fun coord(cell: Int): String = "${('A' + col(cell))}${SIZE - row(cell)}"

    fun material(state: ThreeChessState, side: ThreeSide): Int =
        state.pieces.filter { it.side == side }.sumOf { it.type.value }
}
