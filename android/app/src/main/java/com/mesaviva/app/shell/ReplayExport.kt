package com.mesaviva.app.shell

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class ReplayLink(
    val game: String,
    val seed: Long,
    val moves: List<Int>,
)

/**
 * Formato de replay de Mesa Viva.
 *
 * Damas guarda pares `origen-destino`; Mancala guarda hoyos. El enlace es
 * autocontenido: no necesita servidor ni cuenta, solo la semilla y las
 * jugadas. La app lo valida antes de reproducirlo.
 */
object ReplayCodec {

    const val DAMAS = "damas"
    const val MANCALA = "mancala"
    private const val MAX_MOVES = 600

    fun encodeDamas(seed: Long, moves: List<Pair<Int, Int>>): String =
        encode(DAMAS, seed, moves.flatMap { listOf(it.first, it.second) })

    fun encodeMancala(seed: Long, moves: List<Int>): String =
        encode(MANCALA, seed, moves)

    fun encode(game: String, seed: Long, moves: List<Int>): String {
        val encodedMoves = encodeMoves(game, moves)
        return buildString {
            append("mesaviva://replay?game=")
            append(URLEncoder.encode(game, "UTF-8"))
            append("&seed=")
            append(seed)
            if (encodedMoves.isNotEmpty()) {
                append("&moves=")
                append(URLEncoder.encode(encodedMoves, "UTF-8"))
            }
        }
    }

    fun webLink(replay: ReplayLink): String = buildString {
        append("https://emanuelindustri-source.github.io/mesa-viva/replay/?game=")
        append(URLEncoder.encode(replay.game, "UTF-8"))
        append("&seed=")
        append(replay.seed)
        val encodedMoves = encodeMoves(replay.game, replay.moves)
        if (encodedMoves.isNotEmpty()) {
            append("&moves=")
            append(URLEncoder.encode(encodedMoves, "UTF-8"))
        }
    }

    private fun encodeMoves(game: String, moves: List<Int>): String = when (game) {
        DAMAS -> moves.chunked(2)
            .filter { it.size == 2 }
            .joinToString(".") { "${it[0]}-${it[1]}" }
        else -> moves.joinToString(".")
    }

    fun parse(link: String): ReplayLink? {
        if (!link.startsWith("mesaviva://replay", ignoreCase = true)) return null
        val query = try {
            URI(link).rawQuery
        } catch (_: Exception) {
            null
        } ?: return null
        val values = query.split("&").mapNotNull { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) null else {
                URLDecoder.decode(pair.substring(0, index), "UTF-8") to
                    URLDecoder.decode(pair.substring(index + 1), "UTF-8")
            }
        }.toMap()
        val game = values["game"] ?: return null
        if (game != DAMAS && game != MANCALA) return null
        val seed = values["seed"]?.toLongOrNull() ?: return null
        val rawMoves = values["moves"].orEmpty()
        val moves = mutableListOf<Int>()
        if (rawMoves.isNotBlank()) {
            for (chunk in rawMoves.split(".")) {
                if (game == DAMAS) {
                    val parts = chunk.split("-")
                    if (parts.size != 2) return null
                    moves.add(parts[0].toIntOrNull() ?: return null)
                    moves.add(parts[1].toIntOrNull() ?: return null)
                } else {
                    moves.add(chunk.toIntOrNull() ?: return null)
                }
            }
        }
        if (moves.size > MAX_MOVES) return null
        if (game == DAMAS) {
            if (moves.size % 2 != 0) return null
            if (moves.any { it !in 0..63 }) return null
        } else {
            if (moves.any { it !in 0..13 }) return null
        }
        return ReplayLink(game, seed, moves)
    }

    fun damasPairs(replay: ReplayLink): List<Pair<Int, Int>> =
        replay.moves.chunked(2).filter { it.size == 2 }.map { it[0] to it[1] }
}

/** Guarda el replay como archivo de texto en Descargas/Mesa Viva. */
fun saveReplayFile(
    context: Context,
    replay: ReplayLink,
    link: String,
): String? {
    val name = "mesa-viva-replay-${replay.game}-${System.currentTimeMillis()}.txt"
    val payload = JSONObject()
        .put("app", "Mesa Viva")
        .put("format", 1)
        .put("game", replay.game)
        .put("seed", replay.seed)
        .put("moves", JSONArray(replay.moves))
        .put("link", link)
        .toString(2)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Mesa Viva")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            name
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Mesa Viva")
            if (!dir.exists() && !dir.mkdirs()) return null
            val file = File(dir, name)
            file.writeText(payload, Charsets.UTF_8)
            name
        }
    } catch (_: Exception) {
        null
    }
}

fun shareReplayLink(
    context: Context,
    link: String,
    chooserTitle: String,
    webLink: String? = null,
) {
    val text = if (webLink.isNullOrBlank()) link else "$link\n$webLink"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Mesa Viva · replay")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
