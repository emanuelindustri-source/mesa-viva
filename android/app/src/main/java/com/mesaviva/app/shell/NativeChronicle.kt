package com.mesaviva.app.shell

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChronicleExport(val uri: Uri, val name: String, val inDownloads: Boolean)

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 42f

/**
 * Genera una crónica PDF nativa con la partida, el historial y la fecha.
 * En Android 10+ se guarda en Descargas/Mesa Viva; en versiones anteriores
 * queda en el almacenamiento privado de la app y se comparte con FileProvider.
 */
fun exportChroniclePdf(
    context: Context,
    title: String,
    subtitle: String,
    lines: List<String>,
    stats: List<Pair<String, String>> = emptyList(),
): ChronicleExport? {
    val document = PdfDocument()
    val titlePaint = Paint().apply {
        color = Color.rgb(43, 33, 21)
        textSize = 24f
        typeface = Typeface.create("serif", Typeface.BOLD)
        isAntiAlias = true
    }
    val subtitlePaint = Paint().apply {
        color = Color.rgb(122, 96, 62)
        textSize = 11f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        color = Color.rgb(48, 40, 30)
        textSize = 11f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }
    val accentPaint = Paint().apply {
        color = Color.rgb(224, 166, 58)
        isAntiAlias = true
    }
    val linePaint = Paint().apply {
        color = Color.rgb(224, 216, 200)
        strokeWidth = 0.8f
        isAntiAlias = true
    }
    val footerPaint = Paint().apply {
        color = Color.rgb(140, 124, 100)
        textSize = 9f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }
    val statLabelPaint = Paint().apply {
        color = Color.rgb(122, 96, 62)
        textSize = 8f
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isAntiAlias = true
    }
    val statValuePaint = Paint().apply {
        color = Color.rgb(43, 33, 21)
        textSize = 15f
        typeface = Typeface.create("serif", Typeface.BOLD)
        isAntiAlias = true
    }
    val statsBackground = Paint().apply {
        color = Color.rgb(250, 245, 234)
        isAntiAlias = true
    }

    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
    var canvas = page.canvas
    var y = MARGIN

    fun drawHeader() {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 16f, accentPaint)
        canvas.drawText(title.take(46), MARGIN, y + 24f, titlePaint)
        y += 38f
        canvas.drawText(subtitle.take(78), MARGIN, y, subtitlePaint)
        y += 22f
        val date = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Mesa Viva · $date · ${lines.size} movimientos", MARGIN, y, subtitlePaint)
        y += 16f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 22f
    }

    fun newPage() {
        canvas.drawText("Mesa Viva · página $pageNumber", MARGIN, PAGE_HEIGHT - 24f, footerPaint)
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        y = MARGIN
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, accentPaint)
    }

    drawHeader()
    if (stats.isNotEmpty()) {
        val visible = stats.take(4)
        val rows = (visible.size + 1) / 2
        val cardHeight = 18f + rows * 34f
        canvas.drawRoundRect(
            MARGIN,
            y - 10f,
            PAGE_WIDTH - MARGIN,
            y + cardHeight,
            8f,
            8f,
            statsBackground,
        )
        visible.forEachIndexed { index, (label, value) ->
            val column = index % 2
            val row = index / 2
            val x = MARGIN + 14f + column * ((PAGE_WIDTH - MARGIN * 2) / 2f)
            val top = y + 8f + row * 34f
            canvas.drawText(label.take(26), x, top, statLabelPaint)
            canvas.drawText(value.take(26), x, top + 18f, statValuePaint)
        }
        y += cardHeight + 16f
    }
    if (lines.isEmpty()) {
        canvas.drawText("Sin movimientos registrados.", MARGIN, y, bodyPaint)
    } else {
        lines.forEachIndexed { index, raw ->
            val prefix = "${index + 1}. "
            val available = PAGE_WIDTH - MARGIN * 2 - 12f
            val words = raw.replace("\n", " ").split(" ")
            val chunks = mutableListOf<String>()
            var current = prefix
            for (word in words) {
                val candidate = if (current == prefix) current + word else "$current $word"
                if (bodyPaint.measureText(candidate) <= available || current == prefix) {
                    current = candidate
                } else {
                    chunks.add(current)
                    current = "   $word"
                }
            }
            if (current.isNotBlank()) chunks.add(current)
            for ((lineIndex, chunk) in chunks.withIndex()) {
                if (y > PAGE_HEIGHT - 64f) {
                    newPage()
                }
                if (lineIndex == 0) {
                    canvas.drawCircle(MARGIN + 2f, y - 3f, 2.2f, accentPaint)
                }
                canvas.drawText(chunk, MARGIN + (if (lineIndex == 0) 12f else 0f), y, bodyPaint)
                y += 16f
            }
            y += 3f
        }
    }
    canvas.drawText("Mesa Viva · página $pageNumber", MARGIN, PAGE_HEIGHT - 24f, footerPaint)
    document.finishPage(page)

    val safeTitle = title.lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "cronica" }
    val name = "mesa-viva-$safeTitle-${System.currentTimeMillis()}.pdf"
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Mesa Viva")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { document.writeTo(it) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            document.close()
            ChronicleExport(uri, name, true)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Mesa Viva")
            if (!dir.exists() && !dir.mkdirs()) return null
            val file = File(dir, name)
            file.outputStream().use { document.writeTo(it) }
            document.close()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            ChronicleExport(uri, name, false)
        }
    } catch (_: Exception) {
        document.close()
        null
    }
}

fun shareChroniclePdf(context: Context, export: ChronicleExport, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, export.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
