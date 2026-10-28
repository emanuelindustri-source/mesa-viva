package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Ajedrez ortodoxo para la mesa nativa.
 *
 * Incluye enroque corto/largo, captura al paso, promoción elegible, jaque,
 * jaque mate, ahogado, regla de los 50 movimientos, triple repetición,
 * material insuficiente y bot alfa-beta con tres dificultades.
 */
enum class ChessColor {
    White,
    Black;

    fun opposite(): ChessColor = if (this == White) Black else White
}

enum class ChessPieceType { Pawn, Knight, Bishop, Rook, Queen, King }

data class ChessPiece(val type: ChessPieceType, val color: ChessColor)

data class ChessCastling(
    val whiteKing: Boolean = true,
    val whiteQueen: Boolean = true,
    val blackKing: Boolean = true,
    val blackQueen: Boolean = true,
)

data class ChessMove(
    val from: Int,
    val to: Int,
    val piece: ChessPiece,
    val captured: ChessPiece? = null,
    val promo: ChessPieceType? = null,
    val castle: Char? = null,
    val enPassant: Boolean = false,
)

enum class ChessPhase { Playing, MatchEnd }

data class ChessOutcome(
    val winner: ChessColor?,
    val draw: Boolean,
    val reasonKey: String,
)

data class ChessState(
    val board: List<ChessPiece?>,
    val turn: ChessColor,
    val castling: ChessCastling,
    val enPassant: Int?,
    val halfmove: Int,
    val fullmove: Int,
    val positions: List<String>,
    val lastMove: Pair<Int, Int>?,
    val history: List<String>,
    val phase: ChessPhase = ChessPhase.Playing,
    val outcome: ChessOutcome? = null,
)

class ChessGame {

    companion object {
        private const val MATE = 100000
        private val VALUES = mapOf(
            ChessPieceType.Pawn to 100,
            ChessPieceType.Knight to 320,
            ChessPieceType.Bishop to 330,
            ChessPieceType.Rook to 500,
            ChessPieceType.Queen to 900,
            ChessPieceType.King to 0,
        )
        private val KNIGHT_OFFSETS = listOf(
            1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2,
        )
        private val KING_OFFSETS = listOf(
            0 to 1, 1 to 1, 1 to 0, 1 to -1, 0 to -1, -1 to -1, -1 to 0, -1 to 1,
        )
        private val BISHOP_DIRS = listOf(1 to 1, 1 to -1, -1 to -1, -1 to 1)
        private val ROOK_DIRS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
        private val PROMOTIONS = listOf(
            ChessPieceType.Queen,
            ChessPieceType.Rook,
            ChessPieceType.Bishop,
            ChessPieceType.Knight,
        )
        fun pieceCode(type: ChessPieceType): String = when (type) {
            ChessPieceType.Pawn -> "p"
            ChessPieceType.Knight -> "n"
            ChessPieceType.Bishop -> "b"
            ChessPieceType.Rook -> "r"
            ChessPieceType.Queen -> "q"
            ChessPieceType.King -> "k"
        }

        fun sanLetter(type: ChessPieceType): String = when (type) {
            ChessPieceType.Pawn -> ""
            ChessPieceType.Knight -> "N"
            ChessPieceType.Bishop -> "B"
            ChessPieceType.Rook -> "R"
            ChessPieceType.Queen -> "Q"
            ChessPieceType.King -> "K"
        }

        val GLYPHS = mapOf(
            ChessColor.White to mapOf(
                ChessPieceType.King to "♔",
                ChessPieceType.Queen to "♕",
                ChessPieceType.Rook to "♖",
                ChessPieceType.Bishop to "♗",
                ChessPieceType.Knight to "♘",
                ChessPieceType.Pawn to "♙",
            ),
            ChessColor.Black to mapOf(
                ChessPieceType.King to "♚",
                ChessPieceType.Queen to "♛",
                ChessPieceType.Rook to "♜",
                ChessPieceType.Bishop to "♝",
                ChessPieceType.Knight to "♞",
                ChessPieceType.Pawn to "♟",
            ),
        )

        fun fileOf(index: Int): Int = index % 8
        fun rankOf(index: Int): Int = index / 8
        fun onBoard(file: Int, rank: Int): Boolean = file in 0..7 && rank in 0..7
        fun squareIndex(file: Int, rank: Int): Int = rank * 8 + file
        fun squareName(index: Int): String = "${('a' + fileOf(index))}${8 - rankOf(index)}"

        fun initialBoard(): List<ChessPiece?> {
            val board = MutableList<ChessPiece?>(64) { null }
            val back = listOf(
                ChessPieceType.Rook,
                ChessPieceType.Knight,
                ChessPieceType.Bishop,
                ChessPieceType.Queen,
                ChessPieceType.King,
                ChessPieceType.Bishop,
                ChessPieceType.Knight,
                ChessPieceType.Rook,
            )
            back.forEachIndexed { file, type ->
                board[squareIndex(file, 0)] = ChessPiece(type, ChessColor.Black)
                board[squareIndex(file, 7)] = ChessPiece(type, ChessColor.White)
            }
            for (file in 0..7) {
                board[squareIndex(file, 1)] = ChessPiece(ChessPieceType.Pawn, ChessColor.Black)
                board[squareIndex(file, 6)] = ChessPiece(ChessPieceType.Pawn, ChessColor.White)
            }
            return board
        }
    }

    fun initialState(): ChessState {
        val board = initialBoard()
        val castling = ChessCastling()
        return ChessState(
            board = board,
            turn = ChessColor.White,
            castling = castling,
            enPassant = null,
            halfmove = 0,
            fullmove = 1,
            positions = listOf(keyOf(board, ChessColor.White, castling, null)),
            lastMove = null,
            history = emptyList(),
        )
    }

    fun findKing(board: List<ChessPiece?>, color: ChessColor): Int =
        board.indexOfFirst { it?.type == ChessPieceType.King && it.color == color }

    fun isAttacked(board: List<ChessPiece?>, square: Int, by: ChessColor): Boolean {
        val file = fileOf(square)
        val rank = rankOf(square)
        val pawnRow = if (by == ChessColor.White) rank + 1 else rank - 1
        for (df in listOf(-1, 1)) {
            if (onBoard(file + df, pawnRow)) {
                val piece = board[squareIndex(file + df, pawnRow)]
                if (piece?.type == ChessPieceType.Pawn && piece.color == by) return true
            }
        }
        for ((df, dr) in KNIGHT_OFFSETS) {
            if (onBoard(file + df, rank + dr)) {
                val piece = board[squareIndex(file + df, rank + dr)]
                if (piece?.type == ChessPieceType.Knight && piece.color == by) return true
            }
        }
        for ((df, dr) in KING_OFFSETS) {
            if (onBoard(file + df, rank + dr)) {
                val piece = board[squareIndex(file + df, rank + dr)]
                if (piece?.type == ChessPieceType.King && piece.color == by) return true
            }
        }
        for ((df, dr) in BISHOP_DIRS) {
            var nf = file + df
            var nr = rank + dr
            while (onBoard(nf, nr)) {
                val piece = board[squareIndex(nf, nr)]
                if (piece != null) {
                    if (piece.color == by && (piece.type == ChessPieceType.Bishop || piece.type == ChessPieceType.Queen)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }
        for ((df, dr) in ROOK_DIRS) {
            var nf = file + df
            var nr = rank + dr
            while (onBoard(nf, nr)) {
                val piece = board[squareIndex(nf, nr)]
                if (piece != null) {
                    if (piece.color == by && (piece.type == ChessPieceType.Rook || piece.type == ChessPieceType.Queen)) return true
                    break
                }
                nf += df
                nr += dr
            }
        }
        return false
    }

    fun inCheck(board: List<ChessPiece?>, color: ChessColor): Boolean {
        val king = findKing(board, color)
        if (king < 0) return false
        return isAttacked(board, king, color.opposite())
    }

    fun pseudoMoves(state: ChessState, color: ChessColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val board = state.board
        for (from in 0 until 64) {
            val piece = board[from] ?: continue
            if (piece.color != color) continue
            val file = fileOf(from)
            val rank = rankOf(from)
            fun add(to: Int, extra: ChessMove? = null) {
                val target = board[to]
                if (target?.color == color) return
                moves.add(
                    ChessMove(
                        from = from,
                        to = to,
                        piece = piece,
                        captured = target,
                        promo = extra?.promo,
                        castle = extra?.castle,
                        enPassant = extra?.enPassant ?: false,
                    ),
                )
            }
            when (piece.type) {
                ChessPieceType.Pawn -> {
                    val dir = if (color == ChessColor.White) -1 else 1
                    val startRow = if (color == ChessColor.White) 6 else 1
                    val promoRow = if (color == ChessColor.White) 0 else 7
                    val oneRank = rank + dir
                    if (onBoard(file, oneRank)) {
                        val one = squareIndex(file, oneRank)
                        if (board[one] == null) {
                            if (oneRank == promoRow) {
                                for (promo in PROMOTIONS) {
                                    moves.add(ChessMove(from, one, piece, promo = promo))
                                }
                            } else {
                                moves.add(ChessMove(from, one, piece))
                                if (rank == startRow) {
                                    val two = squareIndex(file, rank + dir * 2)
                                    if (board[two] == null) moves.add(ChessMove(from, two, piece))
                                }
                            }
                        }
                    }
                    for (df in listOf(-1, 1)) {
                        val nf = file + df
                        val nr = rank + dir
                        if (!onBoard(nf, nr)) continue
                        val to = squareIndex(nf, nr)
                        val target = board[to]
                        if (target != null && target.color != color) {
                            if (nr == promoRow) {
                                for (promo in PROMOTIONS) {
                                    moves.add(ChessMove(from, to, piece, captured = target, promo = promo))
                                }
                            } else {
                                moves.add(ChessMove(from, to, piece, captured = target))
                            }
                        } else if (target == null && state.enPassant == to) {
                            moves.add(ChessMove(from, to, piece, enPassant = true))
                        }
                    }
                }
                ChessPieceType.Knight -> {
                    for ((df, dr) in KNIGHT_OFFSETS) {
                        if (onBoard(file + df, rank + dr)) add(squareIndex(file + df, rank + dr))
                    }
                }
                ChessPieceType.King -> {
                    for ((df, dr) in KING_OFFSETS) {
                        if (onBoard(file + df, rank + dr)) add(squareIndex(file + df, rank + dr))
                    }
                    val rights = state.castling
                    if (color == ChessColor.White && from == 60) {
                        if (rights.whiteKing && board[61] == null && board[62] == null && board[63]?.type == ChessPieceType.Rook && board[63]?.color == ChessColor.White) {
                            if (!isAttacked(board, 60, ChessColor.Black) && !isAttacked(board, 61, ChessColor.Black) && !isAttacked(board, 62, ChessColor.Black)) {
                                moves.add(ChessMove(from, 62, piece, castle = 'K'))
                            }
                        }
                        if (rights.whiteQueen && board[59] == null && board[58] == null && board[57] == null && board[56]?.type == ChessPieceType.Rook && board[56]?.color == ChessColor.White) {
                            if (!isAttacked(board, 60, ChessColor.Black) && !isAttacked(board, 59, ChessColor.Black) && !isAttacked(board, 58, ChessColor.Black)) {
                                moves.add(ChessMove(from, 58, piece, castle = 'Q'))
                            }
                        }
                    }
                    if (color == ChessColor.Black && from == 4) {
                        if (rights.blackKing && board[5] == null && board[6] == null && board[7]?.type == ChessPieceType.Rook && board[7]?.color == ChessColor.Black) {
                            if (!isAttacked(board, 4, ChessColor.White) && !isAttacked(board, 5, ChessColor.White) && !isAttacked(board, 6, ChessColor.White)) {
                                moves.add(ChessMove(from, 6, piece, castle = 'K'))
                            }
                        }
                        if (rights.blackQueen && board[3] == null && board[2] == null && board[1] == null && board[0]?.type == ChessPieceType.Rook && board[0]?.color == ChessColor.Black) {
                            if (!isAttacked(board, 4, ChessColor.White) && !isAttacked(board, 3, ChessColor.White) && !isAttacked(board, 2, ChessColor.White)) {
                                moves.add(ChessMove(from, 2, piece, castle = 'Q'))
                            }
                        }
                    }
                }
                ChessPieceType.Bishop, ChessPieceType.Rook, ChessPieceType.Queen -> {
                    val dirs = when (piece.type) {
                        ChessPieceType.Bishop -> BISHOP_DIRS
                        ChessPieceType.Rook -> ROOK_DIRS
                        else -> BISHOP_DIRS + ROOK_DIRS
                    }
                    for ((df, dr) in dirs) {
                        var nf = file + df
                        var nr = rank + dr
                        while (onBoard(nf, nr)) {
                            val to = squareIndex(nf, nr)
                            val target = board[to]
                            if (target == null) {
                                moves.add(ChessMove(from, to, piece))
                            } else {
                                if (target.color != color) moves.add(ChessMove(from, to, piece, captured = target))
                                break
                            }
                            nf += df
                            nr += dr
                        }
                    }
                }
            }
        }
        return moves
    }

    fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val board = state.board.toMutableList()
        val piece = board[move.from] ?: return state
        board[move.from] = null
        if (move.enPassant) {
            val dir = if (piece.color == ChessColor.White) -1 else 1
            board[squareIndex(fileOf(move.to), rankOf(move.to) - dir)] = null
        }
        board[move.to] = move.promo?.let { ChessPiece(it, piece.color) } ?: piece
        if (move.castle == 'K') {
            val rookFrom = if (piece.color == ChessColor.White) 63 else 7
            val rookTo = if (piece.color == ChessColor.White) 61 else 5
            board[rookTo] = board[rookFrom]
            board[rookFrom] = null
        }
        if (move.castle == 'Q') {
            val rookFrom = if (piece.color == ChessColor.White) 56 else 0
            val rookTo = if (piece.color == ChessColor.White) 59 else 3
            board[rookTo] = board[rookFrom]
            board[rookFrom] = null
        }
        var wK = state.castling.whiteKing
        var wQ = state.castling.whiteQueen
        var bK = state.castling.blackKing
        var bQ = state.castling.blackQueen
        if (piece.type == ChessPieceType.King) {
            if (piece.color == ChessColor.White) {
                wK = false
                wQ = false
            } else {
                bK = false
                bQ = false
            }
        }
        if (move.from == 63 || move.to == 63) wK = false
        if (move.from == 56 || move.to == 56) wQ = false
        if (move.from == 7 || move.to == 7) bK = false
        if (move.from == 0 || move.to == 0) bQ = false

        val enPassant = if (piece.type == ChessPieceType.Pawn && abs(move.to - move.from) == 16) {
            (move.from + move.to) / 2
        } else {
            null
        }
        val castling = ChessCastling(wK, wQ, bK, bQ)
        val nextTurn = piece.color.opposite()
        val halfmove = if (piece.type == ChessPieceType.Pawn || move.captured != null || move.enPassant) 0 else state.halfmove + 1
        val fullmove = if (state.turn == ChessColor.Black) state.fullmove + 1 else state.fullmove
        return state.copy(
            board = board,
            turn = nextTurn,
            castling = castling,
            enPassant = enPassant,
            halfmove = halfmove,
            fullmove = fullmove,
            positions = state.positions + keyOf(board, nextTurn, castling, enPassant),
            lastMove = move.from to move.to,
        )
    }

    fun legalMoves(state: ChessState, color: ChessColor = state.turn): List<ChessMove> {
        val out = mutableListOf<ChessMove>()
        for (move in pseudoMoves(state, color)) {
            val next = applyMove(state, move)
            if (!inCheck(next.board, color)) out.add(move)
        }
        return out
    }

    fun positionKey(state: ChessState): String = keyOf(state.board, state.turn, state.castling, state.enPassant)

    fun move(state: ChessState, from: Int, to: Int, promo: ChessPieceType? = null): ChessState? {
        if (state.phase == ChessPhase.MatchEnd) return null
        val candidates = legalMoves(state).filter { it.from == from && it.to == to }
        if (candidates.isEmpty()) return null
        val chosen = if (promo != null) {
            candidates.firstOrNull { it.promo == promo } ?: return null
        } else {
            candidates.firstOrNull { it.promo == null } ?: candidates.first()
        }
        val san = sanFor(state, chosen)
        val next = checkGameEnd(applyMove(state, chosen).copy(history = state.history + san))
        return next
    }

    fun resign(state: ChessState): ChessState {
        if (state.phase == ChessPhase.MatchEnd) return state
        return state.copy(
            phase = ChessPhase.MatchEnd,
            outcome = ChessOutcome(state.turn.opposite(), false, "result.reason.resign"),
        )
    }

    fun botMove(state: ChessState, difficulty: String, random: Random): ChessMove? {
        if (state.phase == ChessPhase.MatchEnd) return null
        val moves = legalMoves(state, state.turn)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") {
            val captures = moves.filter { it.captured != null }
            val pool = if (captures.isNotEmpty() && random.nextDouble() < 0.55) captures else moves
            return pool[random.nextInt(pool.size)]
        }
        val depth = if (difficulty == "hard") 3 else 2
        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<ChessMove>()
        for (move in moves) {
            val next = applyMove(state, move)
            val score = -search(next, depth - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, state.turn.opposite())
            if (score > bestScore + 5) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(move)
            } else if (abs(score - bestScore) <= 5) {
                bestMoves.add(move)
            }
        }
        if (bestMoves.isEmpty()) return moves.first()
        return bestMoves[random.nextInt(bestMoves.size)]
    }

    private fun search(state: ChessState, depth: Int, alphaStart: Int, betaStart: Int, color: ChessColor): Int {
        var alpha = alphaStart
        val beta = betaStart
        val moves = legalMoves(state, color)
        if (moves.isEmpty()) return if (inCheck(state.board, color)) -MATE + (10 - depth) else 0
        if (depth <= 0) return evaluateFor(state, color)
        val ordered = moves.sortedByDescending {
            (it.captured?.let { piece -> VALUES[piece.type] ?: 0 } ?: 0) + if (it.promo != null) 800 else 0
        }
        var best = Int.MIN_VALUE / 2
        for (move in ordered) {
            val next = applyMove(state, move)
            val score = -search(next, depth - 1, -beta, -alpha, color.opposite())
            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return best
    }

    private fun evaluateFor(state: ChessState, color: ChessColor): Int {
        var score = 0
        for (index in 0 until 64) {
            val piece = state.board[index] ?: continue
            val file = fileOf(index)
            val rank = rankOf(index)
            val center = 4 - (abs(3.5 - file) + abs(3.5 - rank))
            var value = VALUES[piece.type] ?: 0
            when (piece.type) {
                ChessPieceType.Pawn -> value += (if (piece.color == ChessColor.White) 6 - rank else rank - 1) * 10 + (center * 2).toInt()
                ChessPieceType.Knight -> value += (center * 8).toInt()
                ChessPieceType.Bishop -> value += (center * 4).toInt()
                ChessPieceType.Rook -> value += if ((piece.color == ChessColor.White && rank == 1) || (piece.color == ChessColor.Black && rank == 6)) 10 else 0
                ChessPieceType.Queen -> value += (center * 2).toInt()
                ChessPieceType.King -> value -= (center * 3).toInt()
            }
            score += if (piece.color == color) value else -value
        }
        return score
    }

    private fun checkGameEnd(state: ChessState): ChessState {
        if (state.phase == ChessPhase.MatchEnd) return state
        val moves = legalMoves(state, state.turn)
        if (moves.isEmpty()) {
            return if (inCheck(state.board, state.turn)) {
                state.copy(
                    phase = ChessPhase.MatchEnd,
                    outcome = ChessOutcome(state.turn.opposite(), false, "result.reason.checkmate"),
                )
            } else {
                state.copy(
                    phase = ChessPhase.MatchEnd,
                    outcome = ChessOutcome(null, true, "result.reason.stalemate"),
                )
            }
        }
        if (state.halfmove >= 100) {
            return state.copy(
                phase = ChessPhase.MatchEnd,
                outcome = ChessOutcome(null, true, "result.reason.fiftyMoves"),
            )
        }
        if (insufficientMaterial(state.board)) {
            return state.copy(
                phase = ChessPhase.MatchEnd,
                outcome = ChessOutcome(null, true, "result.reason.material"),
            )
        }
        val key = positionKey(state)
        if (state.positions.count { it == key } >= 3) {
            return state.copy(
                phase = ChessPhase.MatchEnd,
                outcome = ChessOutcome(null, true, "result.reason.repetition"),
            )
        }
        return state
    }

    private fun insufficientMaterial(board: List<ChessPiece?>): Boolean {
        val pieces = board.filterNotNull()
        if (pieces.size == 2) return true
        if (pieces.size == 3) return pieces.any { it.type == ChessPieceType.Bishop || it.type == ChessPieceType.Knight }
        if (pieces.size == 4) {
            val minors = pieces.filter { it.type == ChessPieceType.Bishop || it.type == ChessPieceType.Knight }
            if (minors.size == 2 && minors.all { it.type == ChessPieceType.Bishop }) {
                val squares = board.mapIndexedNotNull { index, piece -> if (piece?.type == ChessPieceType.Bishop) index else null }
                if (squares.size == 2) {
                    val a = squares[0]
                    val b = squares[1]
                    return (fileOf(a) + rankOf(a)) % 2 == (fileOf(b) + rankOf(b)) % 2
                }
            }
        }
        return false
    }

    private fun keyOf(board: List<ChessPiece?>, turn: ChessColor, castling: ChessCastling, enPassant: Int?): String {
        val cells = board.joinToString("") { piece ->
            if (piece == null) "." else "${if (piece.color == ChessColor.White) "w" else "b"}${pieceCode(piece.type)}"
        }
        val c = "${if (castling.whiteKing) 1 else 0}${if (castling.whiteQueen) 1 else 0}${if (castling.blackKing) 1 else 0}${if (castling.blackQueen) 1 else 0}"
        return "$cells|${if (turn == ChessColor.White) "w" else "b"}|$c|${enPassant ?: "-"}"
    }

    private fun sanFor(state: ChessState, move: ChessMove): String {
        if (move.castle == 'K') return "O-O"
        if (move.castle == 'Q') return "O-O-O"
        val piece = move.piece
        var san = sanLetter(piece.type)
        if (piece.type != ChessPieceType.Pawn) {
            val others = legalMoves(state, piece.color).filter {
                it.piece.type == piece.type && it.to == move.to && it.from != move.from
            }
            if (others.isNotEmpty()) {
                val sameFile = others.any { fileOf(it.from) == fileOf(move.from) }
                val sameRank = others.any { rankOf(it.from) == rankOf(move.from) }
                san += when {
                    !sameFile -> squareName(move.from).first()
                    !sameRank -> squareName(move.from).last()
                    else -> squareName(move.from)
                }
            }
        } else if (move.captured != null || move.enPassant) {
            san += squareName(move.from).first()
        }
        if (move.captured != null || move.enPassant) san += "x"
        san += squareName(move.to)
        move.promo?.let { san += "=${sanLetter(it)}" }
        val next = applyMove(state, move)
        if (inCheck(next.board, next.turn)) {
            san += if (legalMoves(next, next.turn).isEmpty()) "#" else "+"
        }
        return san
    }
}
