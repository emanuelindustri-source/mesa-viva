package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.random.Random

/**
 * Shogi (ajedrez japonés) para dos jugadores.
 *
 * Tablero 9×9, promociones, piezas capturadas que vuelven a la mano y drops.
 * El jaque y las jugadas legales se calculan con el rey propio a salvo.
 */
enum class ShogiKind(val kanji: String, val promotedKanji: String, val value: Int) {
    King("玉", "玉", 10000),
    Rook("飛", "龍", 900),
    Bishop("角", "馬", 800),
    Gold("金", "金", 600),
    Silver("銀", "全", 500),
    Knight("桂", "圭", 400),
    Lance("香", "杏", 300),
    Pawn("歩", "と", 100),
}

data class ShogiPiece(
    val owner: Int,
    val kind: ShogiKind,
    val promoted: Boolean = false,
)

data class ShogiMove(
    val from: Int?,
    val to: Int,
    val promote: Boolean = false,
    val drop: ShogiKind? = null,
)

enum class ShogiPhase { Play, MatchEnd }

data class ShogiState(
    val board: List<ShogiPiece?>,
    val hands: List<Map<ShogiKind, Int>>,
    val turn: Int = 0,
    val phase: ShogiPhase = ShogiPhase.Play,
    val lastMove: ShogiMove? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val inCheck: Boolean = false,
    val history: List<String> = emptyList(),
)

data class ShogiResult(val state: ShogiState, val error: String? = null)

class ShogiGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        const val SIZE = 9
        const val CELLS = SIZE * SIZE

        val HAND_ORDER = listOf(
            ShogiKind.Rook,
            ShogiKind.Bishop,
            ShogiKind.Gold,
            ShogiKind.Silver,
            ShogiKind.Knight,
            ShogiKind.Lance,
            ShogiKind.Pawn,
        )

        private val KING_STEPS = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        private val ORTHO = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        private val DIAG = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

        fun row(index: Int): Int = index / SIZE
        fun col(index: Int): Int = index % SIZE
        fun index(row: Int, col: Int): Int = row * SIZE + col
        fun square(index: Int): String = "${'a' + col(index)}${SIZE - row(index)}"
        fun label(piece: ShogiPiece): String =
            if (piece.promoted) piece.kind.promotedKanji else piece.kind.kanji

        fun initial(): ShogiState {
            val board = MutableList<ShogiPiece?>(CELLS) { null }
            val back = listOf(
                ShogiKind.Lance,
                ShogiKind.Knight,
                ShogiKind.Silver,
                ShogiKind.Gold,
                ShogiKind.King,
                ShogiKind.Gold,
                ShogiKind.Silver,
                ShogiKind.Knight,
                ShogiKind.Lance,
            )
            for (col in 0 until SIZE) {
                board[index(0, col)] = ShogiPiece(1, back[col])
                board[index(8, col)] = ShogiPiece(0, back[col])
                board[index(2, col)] = ShogiPiece(1, ShogiKind.Pawn)
                board[index(6, col)] = ShogiPiece(0, ShogiKind.Pawn)
            }
            board[index(1, 1)] = ShogiPiece(1, ShogiKind.Rook)
            board[index(1, 7)] = ShogiPiece(1, ShogiKind.Bishop)
            board[index(7, 1)] = ShogiPiece(0, ShogiKind.Bishop)
            board[index(7, 7)] = ShogiPiece(0, ShogiKind.Rook)
            return ShogiState(
                board = board,
                hands = listOf(emptyMap(), emptyMap()),
                history = listOf("Shogi · las negras mueven primero"),
            )
        }
    }

    fun newMatch(): ShogiState = initial()

    fun legalMoves(state: ShogiState, seat: Int = state.turn): List<ShogiMove> {
        if (state.phase == ShogiPhase.MatchEnd) return emptyList()
        val moves = mutableListOf<ShogiMove>()
        for (from in state.board.indices) {
            val piece = state.board[from] ?: continue
            if (piece.owner != seat) continue
            for (move in pieceMoves(state, from, piece)) {
                val next = applyRaw(state, move)
                if (!isAttacked(next.board, kingSquare(next.board, seat), 1 - seat)) moves.add(move)
            }
        }
        for ((kind, count) in state.hands.getOrElse(seat) { emptyMap() }) {
            if (count <= 0) continue
            for (to in state.board.indices) {
                if (state.board[to] != null) continue
                if (!canDrop(state, seat, kind, to)) continue
                val move = ShogiMove(null, to, drop = kind)
                val next = applyRaw(state, move)
                if (!isAttacked(next.board, kingSquare(next.board, seat), 1 - seat)) moves.add(move)
            }
        }
        return moves
    }

    fun move(state: ShogiState, move: ShogiMove): ShogiResult {
        if (state.phase == ShogiPhase.MatchEnd) return ShogiResult(state, "shogi.error.matchOver")
        val legal = legalMoves(state).firstOrNull {
            it.from == move.from && it.to == move.to && it.promote == move.promote && it.drop == move.drop
        } ?: return ShogiResult(state, "shogi.error.invalidMove")
        return ShogiResult(finish(applyRaw(state, legal), state.turn))
    }

    fun drop(state: ShogiState, kind: ShogiKind, to: Int): ShogiResult =
        move(state, ShogiMove(null, to, drop = kind))

    fun resign(state: ShogiState): ShogiResult {
        if (state.phase == ShogiPhase.MatchEnd) return ShogiResult(state, "shogi.error.matchOver")
        val winner = 1 - state.turn
        return ShogiResult(
            state.copy(
                phase = ShogiPhase.MatchEnd,
                winner = winner,
                reason = "shogi.reason.resign",
                history = (state.history + "Rendición · gana J${winner + 1}").takeLast(160),
            ),
        )
    }

    fun inCheck(state: ShogiState, seat: Int): Boolean =
        isAttacked(state.board, kingSquare(state.board, seat), 1 - seat)

    fun winnerName(state: ShogiState): String =
        state.winner?.let { "J${it + 1}" } ?: ""

    fun handCount(state: ShogiState, seat: Int, kind: ShogiKind): Int =
        state.hands.getOrElse(seat) { emptyMap() }.getOrElse(kind) { 0 }

    fun botMove(state: ShogiState, difficulty: String = "normal"): ShogiMove? {
        val moves = legalMoves(state)
        if (moves.isEmpty()) return null
        if (difficulty == "easy") return moves.random(random)
        val seat = state.turn
        val scored = moves.map { move ->
            val next = applyRaw(state, move)
            val material = material(next, seat) - material(next, 1 - seat)
            val capture = captureValue(state, move)
            val promotion = if (move.promote) 40 else 0
            val check = if (isAttacked(next.board, kingSquare(next.board, 1 - seat), seat)) 60 else 0
            move to (material + capture * 2 + promotion + check)
        }
        if (difficulty != "hard") return scored.maxByOrNull { it.second }?.first
        // Dos jugadas: evita entregar una pieza grande al rival.
        var best = scored.maxByOrNull { it.second }!!.first
        var bestScore = Int.MIN_VALUE
        for ((move, base) in scored.sortedByDescending { it.second }.take(12)) {
            val next = applyRaw(state, move)
            val replies = legalMoves(next)
            val worst = replies.maxOfOrNull { reply ->
                val after = applyRaw(next, reply)
                material(next, seat) - material(after, seat) + captureValue(next, reply)
            } ?: 0
            val score = base - worst
            if (score > bestScore) {
                bestScore = score
                best = move
            }
        }
        return best
    }

    private fun captureValue(state: ShogiState, move: ShogiMove): Int =
        state.board.getOrNull(move.to)?.let { it.kind.value } ?: 0

    private fun material(state: ShogiState, seat: Int): Int {
        var total = 0
        for (piece in state.board) {
            if (piece != null && piece.owner == seat) {
                total += piece.kind.value + if (piece.promoted) 150 else 0
            }
        }
        for ((kind, count) in state.hands.getOrElse(seat) { emptyMap() }) {
            total += kind.value * count
        }
        return total
    }

    private fun finish(next: ShogiState, mover: Int): ShogiState {
        val check = isAttacked(next.board, kingSquare(next.board, 1 - mover), mover)
        val replies = legalMoves(next.copy(turn = 1 - mover))
        if (replies.isEmpty()) {
            return next.copy(
                phase = ShogiPhase.MatchEnd,
                winner = mover,
                inCheck = check,
                reason = "shogi.reason.mate",
                history = (next.history + "J${mover + 1} da mate").takeLast(160),
            )
        }
        return next.copy(
            inCheck = check,
            history = (next.history + if (check) "Jaque a J${mover + 2}" else "").filter { it.isNotBlank() }.takeLast(160),
        )
    }

    private fun applyRaw(state: ShogiState, move: ShogiMove): ShogiState {
        val board = state.board.toMutableList()
        val hands = state.hands.map { it.toMutableMap() }.toMutableList()
        val mover = state.turn
        if (move.drop != null) {
            val kind = move.drop
            val count = hands[mover][kind] ?: 0
            hands[mover][kind] = count - 1
            if (hands[mover][kind] == 0) hands[mover].remove(kind)
            board[move.to] = ShogiPiece(mover, kind)
        } else {
            val from = move.from ?: return state
            val piece = board[from] ?: return state
            val captured = board[move.to]
            if (captured != null) {
                val capturedKind = captured.kind
                hands[mover][capturedKind] = (hands[mover][capturedKind] ?: 0) + 1
            }
            board[from] = null
            board[move.to] = piece.copy(promoted = piece.promoted || move.promote)
        }
        val entry = buildString {
            append("J${mover + 1}: ")
            append(if (move.drop != null) "${move.drop.kanji}*" else square(move.from!!))
            append("-")
            append(square(move.to))
            if (move.promote) append("+")
        }
        return state.copy(
            board = board,
            hands = hands,
            turn = 1 - mover,
            lastMove = move,
            history = (state.history + entry).takeLast(160),
        )
    }

    private fun pieceMoves(state: ShogiState, from: Int, piece: ShogiPiece): List<ShogiMove> {
        val result = mutableListOf<ShogiMove>()
        val r = row(from)
        val c = col(from)
        fun add(to: Int) {
            if (to !in 0 until CELLS) return
            val target = state.board[to]
            if (target != null && target.owner == piece.owner) return
            val zone = inPromotionZone(piece.owner, r) || inPromotionZone(piece.owner, row(to))
            val must = mustPromote(piece, to)
            if (!must) result.add(ShogiMove(from, to, promote = false))
            if ((zone || must) && canPromote(piece)) result.add(ShogiMove(from, to, promote = true))
        }
        val dirs = effectiveSteps(piece)
        for ((dr, dc) in dirs) add(index(r + dr, c + dc))
        for ((dr, dc) in effectiveSlides(piece)) {
            var nr = r + dr
            var nc = c + dc
            while (nr in 0 until SIZE && nc in 0 until SIZE) {
                val to = index(nr, nc)
                val target = state.board[to]
                if (target != null && target.owner == piece.owner) break
                add(to)
                if (target != null) break
                nr += dr
                nc += dc
            }
        }
        return result.distinctBy { it.to to it.promote }
    }

    private fun effectiveSteps(piece: ShogiPiece): List<Pair<Int, Int>> {
        val forward = if (piece.owner == 0) -1 else 1
        if (piece.promoted) {
            return when (piece.kind) {
                ShogiKind.Rook, ShogiKind.Bishop -> KING_STEPS
                else -> goldSteps(forward)
            }
        }
        return when (piece.kind) {
            ShogiKind.King -> KING_STEPS
            ShogiKind.Gold -> goldSteps(forward)
            ShogiKind.Silver -> listOf(
                forward to 0, forward to -1, forward to 1,
                -forward to -1, -forward to 1,
            )
            ShogiKind.Knight -> listOf(
                forward * 2 to -1,
                forward * 2 to 1,
            )
            ShogiKind.Pawn -> listOf(forward to 0)
            ShogiKind.Lance, ShogiKind.Rook, ShogiKind.Bishop -> emptyList()
        }
    }

    private fun effectiveSlides(piece: ShogiPiece): List<Pair<Int, Int>> {
        val forward = if (piece.owner == 0) -1 else 1
        return when {
            piece.promoted && piece.kind == ShogiKind.Rook -> ORTHO
            piece.promoted && piece.kind == ShogiKind.Bishop -> DIAG
            piece.kind == ShogiKind.Rook -> ORTHO
            piece.kind == ShogiKind.Bishop -> DIAG
            piece.kind == ShogiKind.Lance -> listOf(forward to 0)
            else -> emptyList()
        }
    }

    private fun goldSteps(forward: Int): List<Pair<Int, Int>> = listOf(
        forward to -1, forward to 0, forward to 1,
        0 to -1, 0 to 1, -forward to 0,
    )

    private fun canPromote(piece: ShogiPiece): Boolean =
        !piece.promoted && piece.kind != ShogiKind.King && piece.kind != ShogiKind.Gold

    private fun mustPromote(piece: ShogiPiece, to: Int): Boolean {
        if (!canPromote(piece)) return false
        val r = row(to)
        val last = if (piece.owner == 0) 0 else 8
        val second = if (piece.owner == 0) 1 else 7
        return when (piece.kind) {
            ShogiKind.Pawn, ShogiKind.Lance -> r == last
            ShogiKind.Knight -> r == last || r == second
            else -> false
        }
    }

    private fun inPromotionZone(owner: Int, r: Int): Boolean =
        if (owner == 0) r <= 2 else r >= 6

    private fun canDrop(state: ShogiState, seat: Int, kind: ShogiKind, to: Int): Boolean {
        val r = row(to)
        val last = if (seat == 0) 0 else 8
        val second = if (seat == 0) 1 else 7
        if (kind == ShogiKind.Pawn || kind == ShogiKind.Lance) {
            if (r == last) return false
        }
        if (kind == ShogiKind.Knight && (r == last || r == second)) return false
        if (kind == ShogiKind.Pawn) {
            val file = col(to)
            for (index in 0 until CELLS) {
                val piece = state.board[index] ?: continue
                if (piece.owner == seat && !piece.promoted && piece.kind == ShogiKind.Pawn && col(index) == file) return false
            }
        }
        return true
    }

    private fun kingSquare(board: List<ShogiPiece?>, owner: Int): Int =
        board.indexOfFirst { it != null && it.owner == owner && it.kind == ShogiKind.King }

    private fun isAttacked(board: List<ShogiPiece?>, target: Int, byOwner: Int): Boolean {
        if (target < 0) return false
        val tr = row(target)
        val tc = col(target)
        for (from in board.indices) {
            val piece = board[from] ?: continue
            if (piece.owner != byOwner) continue
            val r = row(from)
            val c = col(from)
            val forward = if (piece.owner == 0) -1 else 1
            val steps = effectiveSteps(piece)
            for ((dr, dc) in steps) {
                if (r + dr == tr && c + dc == tc) return true
            }
            for ((dr, dc) in effectiveSlides(piece)) {
                var nr = r + dr
                var nc = c + dc
                while (nr in 0 until SIZE && nc in 0 until SIZE) {
                    if (nr == tr && nc == tc) return true
                    if (board[index(nr, nc)] != null) break
                    nr += dr
                    nc += dc
                }
            }
        }
        return false
    }
}
