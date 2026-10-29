package com.mesaviva.app.games

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/** Una casilla de la estrella de 121 hoyos. */
data class ChinoCell(
    val q: Int,
    val r: Int,
    val triangle: Int?,
    val x: Float,
    val y: Float,
)

data class ChinoMove(val from: Int, val to: Int, val jumps: Int = 0)

enum class ChinoPhase { Play, MatchEnd }

data class ChinoState(
    val pieces: List<Int?>,
    val playerCount: Int,
    val turn: Int,
    val phase: ChinoPhase = ChinoPhase.Play,
    val lastMove: ChinoMove? = null,
    val winner: Int? = null,
    val draw: Boolean = false,
    val reason: String? = null,
    val history: List<String> = emptyList(),
)

data class ChinoResult(val state: ChinoState, val error: String? = null)

/**
 * Damas Chinas (Chinese Checkers) para 2–6 jugadores.
 *
 * Estrella de 121 hoyos, movimientos de un paso y saltos encadenados por
 * encima de cualquier ficha. Gana quien ocupa primero el triángulo opuesto.
 */
class ChinoGame(seed: Long = Random.nextLong()) {

    private val random = Random(seed)

    companion object {
        val DIRECTIONS: List<Pair<Int, Int>> = listOf(
            1 to 0, 1 to -1, 0 to -1, -1 to 0, -1 to 1, 0 to 1,
        )

        val COLOR_NAMES = listOf("rojo", "azul", "verde", "amarillo", "morado", "naranja")

        val BOARD: ChinoBoard by lazy { buildBoard() }
        val CELLS: List<ChinoCell> get() = BOARD.cells
        val NEIGHBORS: List<List<Int>> get() = BOARD.neighbors
        val TRIANGLES: List<List<Int>> get() = BOARD.triangles

        private fun buildBoard(): ChinoBoard {
            val all = LinkedHashMap<String, ChinoCell>()
            val h = (sqrt(3.0) / 2.0).toFloat()

            for (q in -4..4) {
                for (r in -4..4) {
                    if (abs(q) > 4 || abs(r) > 4 || abs(q + r) > 4) continue
                    all["$q,$r"] = ChinoCell(q, r, null, q + r / 2f, r * h)
                }
            }

            fun rotate(point: Pair<Int, Int>): Pair<Int, Int> = -point.second to point.first + point.second

            for (t in 0..5) {
                for (k in 1..4) {
                    for (r in -4..-k) {
                        var point = (4 + k) to r
                        repeat(t) { point = rotate(point) }
                        val key = "${point.first},${point.second}"
                        val cell = all[key]
                        if (cell != null) {
                            if (cell.triangle == null) {
                                all[key] = cell.copy(triangle = t)
                            }
                        } else {
                            all[key] = ChinoCell(
                                q = point.first,
                                r = point.second,
                                triangle = t,
                                x = point.first + point.second / 2f,
                                y = point.second * h,
                            )
                        }
                    }
                }
            }

            val cells = all.values.toList()
            val index = cells.withIndex().associate { (i, cell) -> "${cell.q},${cell.r}" to i }
            val neighbors = cells.map { cell ->
                DIRECTIONS.mapNotNull { (dq, dr) -> index["${cell.q + dq},${cell.r + dr}"] }
            }
            val triangles = (0..5).map { t ->
                cells.mapIndexedNotNull { i, cell -> if (cell.triangle == t) i else null }
            }
            val distances = triangles.map { triangle ->
                val dist = MutableList(cells.size) { Int.MAX_VALUE }
                val queue = ArrayDeque<Int>()
                for (cell in triangle) {
                    dist[cell] = 0
                    queue.add(cell)
                }
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    for (next in neighbors[current]) {
                        if (dist[next] > dist[current] + 1) {
                            dist[next] = dist[current] + 1
                            queue.add(next)
                        }
                    }
                }
                dist
            }
            return ChinoBoard(cells, neighbors, triangles, distances)
        }

        fun colorsForPlayers(count: Int): List<Int> = when {
            count <= 2 -> listOf(0, 3)
            count == 3 -> listOf(0, 2, 4)
            count == 4 -> listOf(0, 1, 3, 4)
            else -> listOf(0, 1, 2, 3, 4, 5).take(count.coerceIn(2, 6))
        }

        fun targetTriangle(seat: Int, starts: List<Int>): Int =
            ((starts.getOrElse(seat) { 0 }) + 3) % 6
    }

    fun newMatch(playerCount: Int = 2): ChinoState {
        val count = playerCount.coerceIn(2, 6)
        val starts = colorsForPlayers(count)
        val pieces = MutableList<Int?>(CELLS.size) { null }
        starts.forEachIndexed { seat, triangle ->
            for (cell in TRIANGLES[triangle]) pieces[cell] = seat
        }
        return ChinoState(
            pieces = pieces,
            playerCount = count,
            turn = 0,
        )
    }

    fun targetCells(seat: Int, count: Int): List<Int> =
        TRIANGLES[targetTriangle(seat, colorsForPlayers(count))]

    fun legalFor(state: ChinoState, seat: Int = state.turn): List<ChinoMove> {
        if (state.phase == ChinoPhase.MatchEnd) return emptyList()
        val starts = colorsForPlayers(state.playerCount)
        val target = targetCells(seat, state.playerCount).toSet()
        val moves = mutableListOf<ChinoMove>()
        for (from in state.pieces.indices) {
            if (state.pieces[from] != seat) continue
            for (next in NEIGHBORS[from]) {
                if (state.pieces[next] == null) moves.add(ChinoMove(from, next, 0))
            }
            val seen = mutableSetOf(from)
            val queue = ArrayDeque<Pair<Int, Int>>()
            queue.add(from to 0)
            while (queue.isNotEmpty()) {
                val (current, jumps) = queue.removeFirst()
                val cell = CELLS[current]
                for ((dq, dr) in DIRECTIONS) {
                    val mid = BOARD.index["${cell.q + dq},${cell.r + dr}"] ?: continue
                    if (state.pieces[mid] == null) continue
                    val midCell = CELLS[mid]
                    val landing = BOARD.index["${midCell.q + dq},${midCell.r + dr}"] ?: continue
                    if (state.pieces[landing] != null || landing in seen) continue
                    seen.add(landing)
                    queue.add(landing to jumps + 1)
                    if (landing !in target) moves.add(ChinoMove(from, landing, jumps + 1))
                }
            }
        }
        return moves.distinctBy { it.from to it.to }
    }

    fun reachedTarget(state: ChinoState, seat: Int): Int =
        targetCells(seat, state.playerCount).count { state.pieces[it] == seat }

    fun winnerName(state: ChinoState, names: List<String>): String =
        state.winner?.let { names.getOrNull(it) } ?: ""

    private fun apply(state: ChinoState, move: ChinoMove): ChinoState {
        val pieces = state.pieces.toMutableList()
        pieces[move.from] = null
        pieces[move.to] = state.turn
        val entry = "J${state.turn + 1}: ${move.jumps} salto(s)"
        val next = state.copy(
            pieces = pieces,
            turn = (state.turn + 1) % state.playerCount,
            lastMove = move,
            history = (state.history + entry).takeLast(120),
        )
        val target = targetCells(state.turn, state.playerCount)
        if (target.all { pieces[it] == state.turn }) {
            return next.copy(
                phase = ChinoPhase.MatchEnd,
                winner = state.turn,
                reason = "chino.reason.home",
            )
        }
        return next
    }

    fun move(state: ChinoState, from: Int, to: Int): ChinoResult {
        if (state.phase == ChinoPhase.MatchEnd) return ChinoResult(state, "chino.error.matchOver")
        val move = legalFor(state).firstOrNull { it.from == from && it.to == to }
            ?: return ChinoResult(state, "chino.error.invalidMove")
        return ChinoResult(apply(state, move))
    }

    fun resign(state: ChinoState): ChinoResult {
        if (state.phase == ChinoPhase.MatchEnd) return ChinoResult(state, "chino.error.matchOver")
        val winner = (state.turn + 1) % state.playerCount
        return ChinoResult(
            state.copy(
                phase = ChinoPhase.MatchEnd,
                winner = winner,
                reason = "chino.reason.resign",
            ),
        )
    }

    fun botMove(state: ChinoState, difficulty: String = "normal", rng: Random = random): ChinoMove? {
        if (state.phase == ChinoPhase.MatchEnd) return null
        val moves = legalFor(state)
        if (moves.isEmpty()) return null
        val target = targetTriangle(state.turn, colorsForPlayers(state.playerCount))
        val dist = BOARD.distances[target]
        var best = Int.MIN_VALUE
        val pool = mutableListOf<ChinoMove>()
        for (move in moves) {
            val distance = dist.getOrElse(move.to) { 99 }
            val value = -distance * 10 + move.jumps * 3 + if (difficulty == "easy") rng.nextInt(8) else 0
            if (value > best + 1) {
                best = value
                pool.clear()
                pool.add(move)
            } else if (abs(value - best) <= 1) {
                pool.add(move)
            }
        }
        return pool.randomOrNull(rng) ?: moves.first()
    }
}

data class ChinoBoard(
    val cells: List<ChinoCell>,
    val neighbors: List<List<Int>>,
    val triangles: List<List<Int>>,
    val distances: List<List<Int>>,
) {
    val index: Map<String, Int> = cells.withIndex().associate { (i, cell) -> "${cell.q},${cell.r}" to i }
}
