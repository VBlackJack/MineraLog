package net.meshcore.mineralog.data.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.domain.model.Mineral
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates a PDF catalog of the mineral collection using Android's native PdfDocument API.
 *
 * Features:
 * - Cover page with collection statistics
 * - Grid layout (2 columns x 4 rows = 8 minerals per page)
 * - Each cell shows: photo, name, formula, provenance
 * - Memory-optimized: loads thumbnails only, recycles immediately
 * - API 27+ compatible (uses MediaStore for API < 28, ImageDecoder for API 28+)
 *
 * Page Format: A4 (595 x 842 points)
 *
 * @param context Android context for resource access
 */
class PdfCatalogGenerator(private val context: Context) {

    companion object {
        // A4 dimensions in points (72 points per inch)
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842

        // Grid configuration
        private const val COLUMNS = 2
        private const val ROWS = 4
        private const val ITEMS_PER_PAGE = COLUMNS * ROWS // 8

        // Margins and spacing
        private const val MARGIN = 40f
        private const val CELL_SPACING = 20f

        // Thumbnail size for memory efficiency
        private const val THUMBNAIL_SIZE = 300
    }

    /**
     * Generates a PDF catalog for the given minerals and saves it to the specified URI.
     *
     * Process:
     * 1. Creates cover page with title, date, and specimen count
     * 2. Generates grid pages with mineral details
     * 3. Handles pagination automatically (8 minerals per page)
     * 4. Optimizes memory by loading thumbnails on-demand
     *
     * @param minerals List of minerals to include in the catalog
     * @param outputUri URI where the PDF will be saved (from CreateDocument contract)
     * @return Result.success(Unit) if successful, Result.failure(Exception) on error
     */
    suspend fun generateCatalog(minerals: List<Mineral>, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var pageNumber = 1

            // Page 1: Cover page
            createCoverPage(document, pageNumber++, minerals.size)

            // Content pages: Grid of minerals
            var currentPage: PdfDocument.Page? = null
            var pageInfo: PdfDocument.PageInfo? = null
            var canvas: Canvas? = null

            minerals.forEachIndexed { index, mineral ->
                val positionOnPage = index % ITEMS_PER_PAGE

                // Start a new page if needed
                if (positionOnPage == 0) {
                    // Finish previous page if exists
                    currentPage?.let { document.finishPage(it) }

                    // Start new page
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
                    currentPage = document.startPage(pageInfo)
                    canvas = currentPage?.canvas
                }

                // Draw the mineral cell
                canvas?.let { drawMineralCell(it, mineral, positionOnPage) }
            }

            // Finish last page
            currentPage?.let { document.finishPage(it) }

            // Write PDF to output URI
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                document.writeTo(outputStream)
            }

            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates the cover page with collection information.
     *
     * @param document The PDF document
     * @param pageNumber Page number for this page
     * @param totalMinerals Total number of specimens in the collection
     */
    private fun createCoverPage(document: PdfDocument, pageNumber: Int, totalMinerals: Int) {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background color (light gray)
        canvas.drawColor(Color.parseColor("#F5F5F5"))

        // Title paint
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1976D2") // Material Blue
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Subtitle paint
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        // Info paint
        val infoPaint = Paint().apply {
            color = Color.GRAY
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }

        val centerX = PAGE_WIDTH / 2f
        var yPosition = PAGE_HEIGHT / 3f

        // Title
        canvas.drawText("MineraLog Collection", centerX, yPosition, titlePaint)
        yPosition += 80

        // Subtitle
        canvas.drawText("Mineral Catalog", centerX, yPosition, subtitlePaint)
        yPosition += 100

        // Generation date
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val dateText = "Generated on ${dateFormat.format(Date())}"
        canvas.drawText(dateText, centerX, yPosition, infoPaint)
        yPosition += 40

        // Total specimens
        val specimensText = "Total Specimens: $totalMinerals"
        canvas.drawText(specimensText, centerX, yPosition, infoPaint)

        // Decorative line
        val linePaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            strokeWidth = 2f
        }
        val lineY = yPosition + 60
        canvas.drawLine(
            centerX - 100,
            lineY,
            centerX + 100,
            lineY,
            linePaint
        )

        document.finishPage(page)
    }

    /**
     * Draws a single mineral cell in the grid.
     *
     * @param canvas Canvas to draw on
     * @param mineral Mineral data to display
     * @param position Position in the grid (0-7)
     */
    private fun drawMineralCell(canvas: Canvas, mineral: Mineral, position: Int) {
        val row = position / COLUMNS
        val col = position % COLUMNS

        // Calculate cell dimensions
        val availableWidth = PAGE_WIDTH - (2 * MARGIN) - CELL_SPACING
        val availableHeight = PAGE_HEIGHT - (2 * MARGIN) - (CELL_SPACING * (ROWS - 1))
        val cellWidth = availableWidth / COLUMNS
        val cellHeight = availableHeight / ROWS

        // Calculate cell position
        val x = MARGIN + (col * cellWidth) + (col * CELL_SPACING)
        val y = MARGIN + (row * cellHeight) + (row * CELL_SPACING)

        // Draw cell border
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cellRect = RectF(x, y, x + cellWidth, y + cellHeight)
        canvas.drawRect(cellRect, borderPaint)

        // Padding inside cell
        val padding = 10f
        var contentY = y + padding

        // Draw photo (if available)
        val photoHeight = cellHeight * 0.5f
        mineral.photos.firstOrNull()?.fileName?.let { photoUri ->
            val bitmap = loadThumbnail(photoUri)
            bitmap?.let {
                val photoRect = calculatePhotoRect(x + padding, contentY, cellWidth - 2 * padding, photoHeight, it)
                canvas.drawBitmap(it, null, photoRect, null)
                it.recycle() // Immediate cleanup for memory
                contentY += photoHeight + 5
            }
        } ?: run {
            // No photo: draw placeholder
            val placeholderPaint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.FILL
            }
            val placeholderRect = RectF(
                x + padding,
                contentY,
                x + cellWidth - padding,
                contentY + photoHeight
            )
            canvas.drawRect(placeholderRect, placeholderPaint)

            // "No Photo" text
            val noPhotoPaint = Paint().apply {
                color = Color.GRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "No Photo",
                x + cellWidth / 2,
                contentY + photoHeight / 2,
                noPhotoPaint
            )
            contentY += photoHeight + 5
        }

        // Draw mineral name (bold)
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val truncatedName = truncateText(mineral.name, cellWidth - 2 * padding, namePaint)
        canvas.drawText(truncatedName, x + padding, contentY, namePaint)
        contentY += 20

        // Draw formula (if available)
        mineral.formula?.let { formula ->
            val formulaPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
            }
            val truncatedFormula = truncateText(formula, cellWidth - 2 * padding, formulaPaint)
            canvas.drawText(truncatedFormula, x + padding, contentY, formulaPaint)
            contentY += 15
        }

        // Draw provenance (if available)
        mineral.provenance?.let { provenance ->
            val provenanceText = buildString {
                provenance.country?.let { append(it) }
                if (provenance.country != null && provenance.locality != null) append(", ")
                provenance.locality?.let { append(it) }
            }

            if (provenanceText.isNotBlank()) {
                val provenancePaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 9f
                }
                val truncatedProvenance = truncateText(provenanceText, cellWidth - 2 * padding, provenancePaint)
                canvas.drawText(truncatedProvenance, x + padding, contentY, provenancePaint)
            }
        }
    }

    /**
     * Loads a thumbnail-sized bitmap from a file URI.
     * Uses API-compatible loading strategy (MediaStore for API < 28, ImageDecoder for API 28+).
     *
     * @param photoUri File path to the photo
     * @return Bitmap thumbnail, or null if loading fails
     */
    private fun loadThumbnail(photoUri: String): Bitmap? {
        return try {
            val file = File(photoUri)
            if (!file.exists()) return null

            val uri = Uri.fromFile(file)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+: Use ImageDecoder
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.setTargetSize(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                }
            } else {
                // API 27: Use MediaStore with manual scaling
                @Suppress("DEPRECATION")
                val fullBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val scaledBitmap = Bitmap.createScaledBitmap(fullBitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
                if (fullBitmap != scaledBitmap) {
                    fullBitmap.recycle()
                }
                scaledBitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculates the rect for drawing a photo while maintaining aspect ratio.
     *
     * @param x Left position
     * @param y Top position
     * @param maxWidth Maximum width available
     * @param maxHeight Maximum height available
     * @param bitmap The bitmap to draw
     * @return Rect for drawing
     */
    private fun calculatePhotoRect(x: Float, y: Float, maxWidth: Float, maxHeight: Float, bitmap: Bitmap): Rect {
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val availableRatio = maxWidth / maxHeight

        val (width, height) = if (bitmapRatio > availableRatio) {
            // Bitmap is wider: fit width
            maxWidth to maxWidth / bitmapRatio
        } else {
            // Bitmap is taller: fit height
            maxHeight * bitmapRatio to maxHeight
        }

        // Center the image
        val left = x + (maxWidth - width) / 2
        val top = y + (maxHeight - height) / 2

        return Rect(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
    }

    /**
     * Truncates text to fit within a maximum width.
     *
     * @param text Text to truncate
     * @param maxWidth Maximum width in pixels
     * @param paint Paint used for measuring text
     * @return Truncated text with "..." if needed
     */
    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        val textWidth = paint.measureText(text)
        if (textWidth <= maxWidth) return text

        var truncated = text
        while (paint.measureText("$truncated...") > maxWidth && truncated.isNotEmpty()) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }
}
