package net.meshcore.mineralog.data.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.domain.model.Mineral
import java.io.FileOutputStream

/**
 * Generator for printable QR code labels in PDF format.
 *
 * Creates A4-sized PDFs with multiple labels per page (2x4 grid = 8 labels per page).
 * Each label contains:
 * - QR code (mineral ID)
 * - Mineral name
 * - Formula (if available)
 * - Group (if available)
 *
 * Performance target: 100 labels in <10 seconds
 */
class QrLabelPdfGenerator(
    private val context: Context
) {

    companion object {
        // A4 dimensions in points (1 point = 1/72 inch)
        private const val PAGE_WIDTH = 595  // 210mm
        private const val PAGE_HEIGHT = 842 // 297mm

        // Label grid: 2 columns x 4 rows = 8 labels per page
        private const val COLS = 2
        private const val ROWS = 4
        private const val LABELS_PER_PAGE = COLS * ROWS

        // Label dimensions (with margins)
        private const val LABEL_WIDTH = PAGE_WIDTH / COLS
        private const val LABEL_HEIGHT = PAGE_HEIGHT / ROWS

        // Content dimensions within label
        private const val MARGIN = 20f
        private const val QR_SIZE = 120
        private const val TEXT_START_Y = MARGIN + QR_SIZE + 10f
    }

    /**
     * Generate PDF with QR labels for selected minerals.
     *
     * @param minerals List of minerals to generate labels for
     * @param outputUri URI to write the PDF to
     * @return Result with success or error
     */
    suspend fun generate(
        minerals: List<Mineral>,
        outputUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            val totalPages = (minerals.size + LABELS_PER_PAGE - 1) / LABELS_PER_PAGE

            var mineralIndex = 0

            for (pageNum in 0 until totalPages) {
                // Create page
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Draw grid of labels on this page
                for (row in 0 until ROWS) {
                    for (col in 0 until COLS) {
                        if (mineralIndex >= minerals.size) break

                        val mineral = minerals[mineralIndex]
                        val x = col * LABEL_WIDTH
                        val y = row * LABEL_HEIGHT

                        drawLabel(canvas, mineral, x.toFloat(), y.toFloat())
                        mineralIndex++
                    }
                }

                document.finishPage(page)
            }

            // Write to file
            try {
                context.contentResolver.openOutputStream(outputUri)?.use { output ->
                    document.writeTo(output)
                } ?: throw java.io.IOException("Failed to open output stream")
            } finally {
                // Always close document even if write fails
                document.close()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Ensure document is closed on any exception
            try {
                document.close()
            } catch (closeException: Exception) {
                // Ignore close exception, throw original
            }
            Result.failure(e)
        }
    }

    /**
     * Draw a single label on the canvas.
     *
     * @param canvas Canvas to draw on
     * @param mineral Mineral data for the label
     * @param x X position of label
     * @param y Y position of label
     */
    private fun drawLabel(canvas: Canvas, mineral: Mineral, x: Float, y: Float) {
        // Draw border (for cutting guides)
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.LTGRAY
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        canvas.drawRect(
            x,
            y,
            x + LABEL_WIDTH,
            y + LABEL_HEIGHT,
            borderPaint
        )

        // Generate and draw QR code
        val qrBitmap = QrCodeGenerator.generate(
            data = QrCodeGenerator.encodeMineralUri(mineral.id),
            size = QR_SIZE,
            margin = 0
        )

        val qrX = x + MARGIN
        val qrY = y + MARGIN
        canvas.drawBitmap(qrBitmap, qrX, qrY, null)

        // Draw text information
        val textX = x + MARGIN
        var textY = y + TEXT_START_Y

        // Mineral name (bold, larger)
        val namePaint = Paint().apply {
            textSize = 16f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val nameLines = wrapText(mineral.name, LABEL_WIDTH - 2 * MARGIN, namePaint)
        for (line in nameLines) {
            canvas.drawText(line, textX, textY, namePaint)
            textY += namePaint.textSize + 4f
        }

        // Formula (if available)
        mineral.formula?.let { formula ->
            textY += 4f
            val formulaPaint = Paint().apply {
                textSize = 12f
                color = Color.DKGRAY
                isAntiAlias = true
            }
            canvas.drawText(formula, textX, textY, formulaPaint)
            textY += formulaPaint.textSize + 4f
        }

        // Group (if available)
        mineral.group?.let { group ->
            textY += 2f
            val groupPaint = Paint().apply {
                textSize = 10f
                color = Color.GRAY
                isAntiAlias = true
            }
            val groupLines = wrapText(group, LABEL_WIDTH - 2 * MARGIN, groupPaint)
            for (line in groupLines) {
                canvas.drawText(line, textX, textY, groupPaint)
                textY += groupPaint.textSize + 2f
            }
        }

        // ID at bottom (small, for reference)
        val idY = y + LABEL_HEIGHT - MARGIN
        val idPaint = Paint().apply {
            textSize = 8f
            color = Color.LTGRAY
            isAntiAlias = true
        }
        canvas.drawText("ID: ${mineral.id.take(8)}…", textX, idY, idPaint)
    }

    /**
     * Wrap text to fit within a given width.
     *
     * @param text Text to wrap
     * @param maxWidth Maximum width in pixels
     * @param paint Paint to measure text with
     * @return List of wrapped lines
     */
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    /**
     * Estimate generation time for a given number of labels.
     *
     * @param labelCount Number of labels to generate
     * @return Estimated time in milliseconds
     */
    fun estimateGenerationTime(labelCount: Int): Long {
        // Empirical: ~100ms per label (includes QR generation + PDF writing)
        return labelCount * 100L
    }
}
