package com.mesaviva.app.shell

/**
 * Estado de Serpientes y Escaleras para salas nativas de 2–4 jugadores.
 *
 * Las posiciones y los nombres viajan completos porque no hay información
 * oculta. Las reglas son puras para poder probarlas y para que el anfitrión
 * sea la única autoridad.
 */
data class SnakesRoomState(
    val names: List<String>,
    val bots: List<Boolean>,
    val positions: List<Int>,
    val turn: Int,
    val dice: Int = 0,
    val winner: Int? = null,
    val lastSeat: Int? = null,
    val lastFrom: Int = 0,
    val lastTo: Int = 0,
    val lastEvent: String = "start",
) {
    val size: Int get() = names.size.coerceAtLeast(2).coerceAtMost(4)
    val finished: Boolean get() = winner != null
}

object SnakesRoomRules {

    val LADDERS: Map<Int, Int> = mapOf(
        3 to 22, 8 to 26, 20 to 41, 28 to 77, 50 to 91, 71 to 92,
    )

    val SNAKES: Map<Int, Int> = mapOf(
        17 to 4, 54 to 34, 62 to 19, 64 to 60, 87 to 24, 93 to 73, 95 to 75, 99 to 78,
    )

    fun newMatch(names: List<String>, bots: List<Boolean>): SnakesRoomState {
        val safeNames = names.take(4).ifEmpty { listOf("Anfitrión", "Invitado") }
        val safeBots = bots.take(4).let { list ->
            if (list.size == safeNames.size) list else List(safeNames.size) { it != 0 }
        }
        return SnakesRoomState(
            names = safeNames,
            bots = safeBots,
            positions = List(safeNames.size) { 0 },
            turn = 0,
            lastEvent = "start",
        )
    }

    fun roll(state: SnakesRoomState, seat: Int, dice: Int): SnakesRoomState {
        if (state.finished) return state
        if (seat !in state.positions.indices || seat != state.turn) return state
        val value = dice.coerceIn(1, 6)
        val start = state.positions[seat]
        val nextTurn = (state.turn + 1) % state.positions.size
        if (start + value > 100) {
            return state.copy(
                dice = value,
                turn = nextTurn,
                lastSeat = seat,
                lastFrom = start,
                lastTo = start,
                lastEvent = "needExact",
            )
        }
        var position = start + value
        var event = "move"
        LADDERS[position]?.let {
            position = it
            event = "ladder"
        }
        SNAKES[position]?.let {
            position = it
            event = "snake"
        }
        val positions = state.positions.toMutableList().also { it[seat] = position }
        val winner = if (position == 100) seat else null
        return state.copy(
            positions = positions,
            dice = value,
            turn = if (winner != null) state.turn else nextTurn,
            winner = winner,
            lastSeat = seat,
            lastFrom = start,
            lastTo = position,
            lastEvent = if (winner != null) "win" else event,
        )
    }
}
