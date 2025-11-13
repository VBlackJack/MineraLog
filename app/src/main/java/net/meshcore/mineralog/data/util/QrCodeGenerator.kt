package net.meshcore.mineralog.data.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utility for generating QR codes for minerals.
 *
 * QR codes encode the mineral ID for quick lookup via scanning.
 * Optimized for batch generation of 100+ codes.
 *
 * Usage:
 * ```kotlin
 * val qrBitmap = QrCodeGenerator.generate(
 *     data = mineralId,
 *     size = 512
 * )
 * ```
 */
object QrCodeGenerator {

    /**
     * Generate a QR code bitmap from the given data.
     *
     * @param data The data to encode (typically mineral ID)
     * @param size The size of the QR code in pixels (default 512)
     * @param margin The margin around the QR code in modules (default 1)
     * @return Bitmap of the QR code
     * @throws Exception if QR code generation fails
     */
    fun generate(
        data: String,
        size: Int = 512,
        margin: Int = 1
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to margin
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    /**
     * Generate QR codes for multiple mineral IDs in batch.
     *
     * Optimized for generating many QR codes efficiently.
     *
     * @param mineralIds List of mineral IDs to generate QR codes for
     * @param size Size of each QR code in pixels
     * @return Map of mineral ID to QR code bitmap
     */
    fun generateBatch(
        mineralIds: List<String>,
        size: Int = 512
    ): Map<String, Bitmap> {
        return mineralIds.associateWith { mineralId ->
            generate(data = mineralId, size = size)
        }
    }

    /**
     * Encode mineral data as a QR-compatible string.
     *
     * Format: "mineralog://mineral/{id}"
     * This allows the app to handle QR code scans via deep link.
     *
     * @param mineralId The mineral ID
     * @return Encoded string for QR code
     */
    fun encodeMineralUri(mineralId: String): String {
        return "mineralog://mineral/$mineralId"
    }

    /**
     * Decode a mineral ID from a QR code data string.
     *
     * @param data The QR code data string
     * @return Mineral ID if valid, null otherwise
     */
    fun decodeMineralUri(data: String): String? {
        return if (data.startsWith("mineralog://mineral/")) {
            data.removePrefix("mineralog://mineral/")
        } else {
            // Also support plain mineral IDs
            data
        }
    }
}
