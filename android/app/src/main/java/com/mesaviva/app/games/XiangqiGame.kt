package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Xiangqi (ajedrez chino) para la mesa nativa.
 *
 * Palacio, río, caballo bloqueado, cañón, soldados, generales enfrentados,
 * jaque y mate. El motor filtra todas las jugadas que dejan al general en
 * jaque y el bot usa alfa-beta con material y avance.
 */
enum class XqSide { Red, Black }

enum class XqKind { King, Advisor, Elephant, Horse, Rook, Cannon, Soldier }

data class XqPiece(val side: XqSide, val kind: XqKind)

data class XqMove(
    val from: Int,
    val to: Int,
    val captured: XqKind? = null,
)

enum class XqPhase { Play, MatchEnd }

data class XqState(
    val board: List<XqPiece?>,
    val turn: XqSide,
    val phase: XqPhase = XqPhase.Play,
    val lastMove: XqMove? = null,
    val check: XqSide? = null,
    val winner: XqSide? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class XqResult(val state: XqState, val error: String? = null)

class XiangqiGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val COLS = 9
        const val ROWS = 10

        val GLYPHS: Map<XqSide, Map<XqKind, String>> = mapOf(
            XqSide.Red to mapOf(
                XqKind.King to "帅",
                XqKind.Advisor to "仕",
                XqKind.Elephant to "相",
                XqKind.Horse to "马",
                XqKind.Rook to "车",
                XqKind.Cannon to "炮",
                XqKind.Soldier to "兵",
            ),
            XqSide.Black to mapOf(
                XqKind.King to "将",
                XqKind.Advisor to "士",
                XqKind.Elephant to "象",
                XqKind.Horse to "马",
                XqKind.Rook to "车",
                XqKind.Cannon to "炮",
                XqKind.Soldier to "卒",
            ),
        )

        val VALUES: Map<XqKind, Int> = mapOf(
            XqKind.King to 10_000,
            XqKind.Rook to 900,
            XqKind.Cannon to 450,
            XqKind.Horse to 400,
            XqKind.Elephant to 200,
            XqKind.Advisor to 200,
            XqKind.Soldier to 100,
        )

        fun indexOf(col: Int, row: Int): Int = row * COLS + col
        fun colOf(index: Int): Int = index % COLS
        fun rowOf(index: Int): Int = index / COLS
        fun inBoard(col: Int, row: Int): Boolean = col in 0 until COLS && row in 0 until ROWS
        fun other(side: XqSide): XqSide = if (side == XqSide.Red) XqSide.Black else XqSide.Red

        fun initialBoard(): List<XqPiece?> {
            val board = MutableList<XqPiece?>(COLS * ROWS) { null }
            val back = listOf(
                XqKind.Rook, XqKind.Horse, XqKind.Elephant, XqKind.Advisor, XqKind.King,
                XqKind.Advisor, XqKind.Elephant, XqKind.Horse, XqKind.Rook,
            )
            for ((col, kind) in back.withIndex()) {
                board[indexOf(col, 0)] = XqPiece(XqSide.Black, kind)
                board[indexOf(col, 9)] = XqPiece(XqSide.Red, kind)
            }
            board[indexOf(1, 2)] = XqPiece(XqSide.Black, XqKind.Cannon)
            board[indexOf(7, 2)] = XqPiece(XqSide.Black, XqKind.Cannon)
            board[indexOf(1, 7)] = XqPiece(XqSide.Red, XqKind.Cannon)
            board[indexOf(7, 7)] = XqPiece(XqSide.Red, XqKind.Cannon)
            for (col in listOf(0, 2, 4, 6, 8)) {
                board[indexOf(col, 3)] = XqPiece(XqSide.Black, XqKind.Soldier)
                board[indexOf(col, 6)] = XqPiece(XqSide.Red, XqKind.Soldier)
            }
            return board
        }

        fun inPalace(index: Int, side: XqSide): Boolean {
            val col = colOf(index)
            val row = rowOf(index)
            if (col !in 3..5) return false
            return if (side == XqSide.Red) row in 7..9 else row in 0..2
        }

        fun crossedRiver(index: Int, side: XqSide): Boolean =
            if (side == XqSide.Red) rowOf(index) <= 4 else rowOf(index) >= 5

        fun ownSide(index: Int, side: XqSide): Boolean =
            if (side == XqSide.Red) rowOf(index) >= 5 else rowOf(index) <= 4

        fun coord(index: Int): String = "${('A' + colOf(index))}${ROWS - rowOf(index)}"
    }

    fun newMatch(): XqState = XqState(
        board = initialBoard(),
        turn = XqSide.Red,
    )

    private fun addTarget(
        moves: MutableList<XqMove>,
        board: List<XqPiece?>,
        from: Int,
        to: Int,
        side: XqSide,
    ): Boolean {
        val target = board[to]
        if (target != null && target.side == side) return false
        moves.add(XqMove(from, to, target?.kind))
        return target == null
    }

    private fun pseudoMoves(state: XqState, from: Int): List<XqMove> {
        val piece = state.board.getOrNull(from) ?: return emptyList()
        val moves = mutableListOf<XqMove>()
        val board = state.board
        val col = colOf(from)
        val row = rowOf(from)
        val side = piece.side

        fun step(dc: Int, dr: Int, limit: Int) {
            for (distance in 1..limit) {
                val nc = col + dc * distance
                val nr = row + dr * distance
                if (!inBoard(nc, nr)) return
                val to = indexOf(nc, nr)
                val target = board[to]
                if (target == null) {
                    moves.add(XqMove(from, to))
                    continue
                }
                if (target.side != side) moves.add(XqMove(from, to, target.kind))
                return
            }
        }

        when (piece.kind) {
            XqKind.King -> {
                for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    val to = indexOf(nc, nr)
                    if (!inPalace(to, side)) continue
                    addTarget(moves, board, from, to, side)
                }
                val rival = board.indexOfFirst { it?.kind == XqKind.King && it.side != side }
                if (rival >= 0 && colOf(rival) == col) {
                    val min = minOf(row, rowOf(rival))
                    val max = maxOf(row, rowOf(rival))
                    var clear = true
                    for (r in min + 1 until max) {
                        if (board[indexOf(col, r)] != null) {
                            clear = false
                            break
                        }
                    }
                    if (clear) moves.add(XqMove(from, rival, XqKind.King))
                }
            }

            XqKind.Advisor -> {
                for ((dc, dr) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    val to = indexOf(nc, nr)
                    if (inPalace(to, side)) addTarget(moves, board, from, to, side)
                }
            }

            XqKind.Elephant -> {
                for ((dc, dr) in listOf(2 to 2, 2 to -2, -2 to 2, -2 to -2)) {
                    val nc = col + dc
                    val nr = row + dr
                    if (!inBoard(nc, nr)) continue
                    val to = indexOf(nc, nr)
                    if (!ownSide(to, side)) continue
                    val eye = indexOf(col + dc / 2, row + dr / 2)
                    if (board[eye] != null) continue
                    addTarget(moves, board, from, to, side)
                }
            }

            XqKind.Horse -> {
                val legs = listOf(
                    intArrayOf(1, 0, 2, 1), intArrayOf(1, 0, 2, -1),
                    intArrayOf(-1, 0, -2, 1), intArrayOf(-1, 0, -2, -1),
                    intArrayOf(0, 1, 1, 2), intArrayOf(0, 1, -1, 2),
                    intArrayOf(0, -1, 1, -2), intArrayOf(0, -1, -1, -2),
                )
                for (leg in legs) {
                    val legCol = col + leg[0]
                    val legRow = row + leg[1]
                    if (!inBoard(legCol, legRow) || board[indexOf(legCol, legRow)] != null) continue
                    val nc = col + leg[2]
                    val nr = row + leg[3]
                    if (!inBoard(nc, nr)) continue
                    addTarget(moves, board, from, indexOf(nc, nr), side)
                }
            }

            XqKind.Rook -> {
                step(1, 0, 8)
                step(-1, 0, 8)
                step(0, 1, 9)
                step(0, -1, 9)
            }

            XqKind.Cannon -> {
                for ((dc, dr) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    var jumped = false
                    for (distance in 1 until 10) {
                        val nc = col + dc * distance
                        val nr = row + dr * distance
                        if (!inBoard(nc, nr)) break
                        val to = indexOf(nc, nr)
                        val target = board[to]
                        if (!jumped) {
                            if (target == null) moves.add(XqMove(from, to))
                            else jumped = true
                        } else if (target != null) {
                            if (target.side != side) moves.add(XqMove(from, to, target.kind))
                            break
                        }
                    }
                }
            }

            XqKind.Soldier -> {
                val forward = if (side == XqSide.Red) -1 else 1
                val nr = row + forward
                if (inBoard(col, nr)) addTarget(moves, board, from, indexOf(col, nr), side)
                if (crossedRiver(from, side)) {
                    for (dc in listOf(-1, 1)) {
                        if (inBoard(col + dc, row)) {
                            addTarget(moves, board, from, indexOf(col + dc, row), side)
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun generalIndex(board: List<XqPiece?>, side: XqSide): Int =
        board.indexOfFirst { it?.kind == XqKind.King && it.side == side }

    fun inCheck(board: List<XqPiece?>, side: XqSide): Boolean {
        val general = generalIndex(board, side)
        if (general < 0) return false
        val rival = other(side)
        val rivalGeneral = generalIndex(board, rival)
        if (rivalGeneral >= 0 && colOf(rivalGeneral) == colOf(general)) {
            val min = minOf(rowOf(general), rowOf(rivalGeneral))
            val max = maxOf(rowOf(general), rowOf(rivalGeneral))
            var clear = true
            for (r in min + 1 until max) {
                if (board[indexOf(colOf(general), r)] != null) {
                    clear = false
                    break
                }
            }
            if (clear) return true
        }
        val pretend = XqState(board = board, turn = rival)
        for (index in board.indices) {
            val piece = board[index] ?: continue
            if (piece.side != rival) continue
            if (pseudoMoves(pretend, index).any { it.to == general }) return true
        }
        return false
    }

    private fun applyRaw(state: XqState, move: XqMove, record: Boolean = true): XqState {
        val board = state.board.toMutableList()
        val piece = board.getOrNull(move.from) ?: return state
        val captured = board[move.to]
        board[move.to] = piece
        board[move.from] = null
        val entry = "${if (piece.side == XqSide.Red) "R" else "N"} ${GLYPHS[piece.side]?.get(piece.kind) ?: "?"} " +
            "${coord(move.from)}-${coord(move.to)}" + (if (captured != null) " x" else "")
        val next = state.copy(
            board = board,
            turn = other(piece.side),
            lastMove = move,
            history = if (record) (state.history + entry).takeLast(160) else state.history,
        )
        if (captured?.kind == XqKind.King) {
            return next.copy(
                phase = XqPhase.MatchEnd,
                winner = piece.side,
                reason = "xiangqi.reason.general",
            )
        }
        return next
    }

    fun legalMoves(state: XqState, side: XqSide = state.turn): List<XqMove> {
        if (state.phase == XqPhase.MatchEnd) return emptyList()
        val out = mutableListOf<XqMove>()
        for (from in state.board.indices) {
            val piece = state.board[from] ?: continue
            if (piece.side != side) continue
            for (move in pseudoMoves(state, from)) {
                val next = applyRaw(state, move, record = false)
                if (!inCheck(next.board, side)) out.add(move)
            }
        }
        return out
    }

    fun move(state: XqState, from: Int, to: Int): XqResult {
        if (state.phase == XqPhase.MatchEnd) return XqResult(state, "xiangqi.error.matchOver")
        val move = legalMoves(state).firstOrNull { it.from == from && it.to == to }
            ?: return XqResult(state, "xiangqi.error.invalidMove")
        var next = applyRaw(state, move)
        if (next.phase == XqPhase.MatchEnd) return XqResult(next)
        val rival = next.turn
        val rivalMoves = legalMoves(next, rival)
        if (rivalMoves.isEmpty()) {
            val checked = inCheck(next.board, rival)
            next = next.copy(
                phase = XqPhase.MatchEnd,
                winner = state.turn,
                check = null,
                reason = if (checked) "xiangqi.reason.checkmate" else "xiangqi.reason.noMoves",
            )
            return XqResult(next)
        }
        next = next.copy(check = if (inCheck(next.board, rival)) rival else null)
        return XqResult(next)
    }

    fun resign(state: XqState): XqResult {
        if (state.phase == XqPhase.MatchEnd) return XqResult(state, "xiangqi.error.matchOver")
        return XqResult(
            state.copy(
                phase = XqPhase.MatchEnd,
                winner = other(state.turn),
                reason = "xiangqi.reason.resign",
            ),
        )
    }

    fun botMove(state: XqState, difficulty: String = "normal", rng: Random = random): XqMove? {
        if (state.phase == XqPhase.MatchEnd) return null
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") {
            val captures = moves.filter { it.captured != null }
            val pool = if (captures.isNotEmpty()) captures else moves
            return pool[rng.nextInt(pool.size)]
        }
        val depth = if (difficulty == "hard") 3 else 2
        val root = state.turn
        var best = Int.MIN_VALUE
        val pool = mutableListOf<XqMove>()
        for (move in moves.sortedByDescending { it.captured != null }) {
            val value = search(applyRaw(state, move, record = false), depth - 1, Int.MIN_VALUE + 1, Int.MAX_VALUE - 1, root)
            if (value > best + 2) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (abs(value - best) <= 2) {
                pool.add(move)
            }
        }
        return pool.randomOrNull(rng) ?: moves.first()
    }

    private fun search(state: XqState, depth: Int, alphaIn: Int, betaIn: Int, root: XqSide): Int {
        var alpha = alphaIn
        var beta = betaIn
        if (state.phase == XqPhase.MatchEnd) {
            return if (state.winner == root) 100_000 + depth else -100_000 - depth
        }
        if (depth <= 0) return evaluate(state, root)
        val moves = legalMoves(state)
        if (moves.isEmpty()) return if (state.turn == root) -100_000 else 100_000
        val maximizing = state.turn == root
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        for (move in moves) {
            val value = search(applyRaw(state, move, record = false), depth - 1, alpha, beta, root)
            if (maximizing) {
                if (value > best) best = value
                if (best > alpha) alpha = best
            } else {
                if (value < best) best = value
                if (best < beta) beta = best
            }
            if (alpha >= beta) break
        }
        return best
    }

    private fun evaluate(state: XqState, side: XqSide): Int {
        var score = 0
        for (index in state.board.indices) {
            val piece = state.board[index] ?: continue
            val value = VALUES[piece.kind] ?: 0
            val center = 4 - abs(colOf(index) - 4)
            val advance = if (side == XqSide.Red) 9 - rowOf(index) else rowOf(index)
            val positional = center * 2 + if (piece.kind == XqKind.Soldier) advance * 3 else 0
            score += if (piece.side == side) value + positional else -(value + positional)
        }
        return score
    }

    fun winnerName(state: XqState, redName: String, blackName: String): String = when (state.winner) {
        XqSide.Red -> redName
        XqSide.Black -> blackName
        null -> ""
    }
}
