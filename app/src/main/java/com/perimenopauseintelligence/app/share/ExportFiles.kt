package com.perimenopauseintelligence.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.perimenopauseintelligence.app.domain.ReportBlock
import java.io.File

/**
 * Exports are written to the app's private cache and shared only through an explicit user action.
 * They are deleted on the next app start, so no copy lingers in app storage.
 */
object ExportFiles {
    private fun dir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    fun cleanup(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }

    fun writeText(context: Context, name: String, content: String): File =
        File(dir(context), name).apply { writeText(content, Charsets.UTF_8) }

    fun writePdf(context: Context, name: String, blocks: List<ReportBlock>): File =
        File(dir(context), name).apply { outputStream().use { PdfRenderer.render(blocks, it) } }

    fun shareIntent(context: Context, file: File, mime: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share or save").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** Minimal text-only PDF renderer (A4). Plain, readable and printable. */
object PdfRenderer {
    private const val W = 595
    private const val H = 842
    private const val MARGIN = 48f

    fun render(blocks: List<ReportBlock>, out: java.io.OutputStream) {
        val doc = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = 0xFF1B1B1B.toInt() }
        val caption = Paint(body).apply { textSize = 9.5f; color = 0xFF555555.toInt() }
        val heading = Paint(body).apply { textSize = 14f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF1F5A56.toInt() }
        val title = Paint(body).apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; color = 0xFF1F5A56.toInt() }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f

        fun newPage() {
            page?.let { finish(doc, it, pageNo) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            canvas = page!!.canvas
            y = MARGIN
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f, gapAfter: Float = 4f, prefix: String? = null) {
            val width = W - 2 * MARGIN - indent
            val lineHeight = paint.textSize * 1.35f
            val lines = wrap(text, paint, width)
            lines.forEachIndexed { i, line ->
                if (page == null || y + lineHeight > H - MARGIN) newPage()
                if (i == 0 && prefix != null) canvas!!.drawText(prefix, MARGIN, y + paint.textSize, paint)
                canvas!!.drawText(line, MARGIN + indent, y + paint.textSize, paint)
                y += lineHeight
            }
            y += gapAfter
        }

        newPage()
        for (b in blocks) {
            when (b) {
                is ReportBlock.Title -> drawWrapped(b.text, title, gapAfter = 4f)
                is ReportBlock.Heading -> { y += 10f; drawWrapped(b.text, heading, gapAfter = 4f) }
                is ReportBlock.Paragraph -> drawWrapped(b.text, body, gapAfter = 6f)
                is ReportBlock.Bullet -> drawWrapped(b.text, body, indent = 12f, gapAfter = 3f, prefix = "•")
                is ReportBlock.Caption -> drawWrapped(b.text, caption, gapAfter = 6f)
            }
        }
        page?.let { finish(doc, it, pageNo) }
        doc.writeTo(out)
        doc.close()
    }

    private fun finish(doc: PdfDocument, page: PdfDocument.Page, number: Int) {
        val p = Paint().apply { textSize = 9f; color = 0xFF777777.toInt() }
        page.canvas.drawText("Self-recorded information, not a diagnosis.   Page $number", MARGIN, H - 24f, p)
        doc.finishPage(page)
    }

    private fun wrap(text: String, paint: Paint, width: Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ')) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= width) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) lines += line
                    var w = word
                    while (paint.measureText(w) > width && w.length > 1) {
                        val n = paint.breakText(w, true, width, null)
                        lines += w.substring(0, n)
                        w = w.substring(n)
                    }
                    line = w
                }
            }
            lines += line
        }
        return lines
    }
}
