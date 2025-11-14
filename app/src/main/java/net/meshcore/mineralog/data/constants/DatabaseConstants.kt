package net.meshcore.mineralog.data.constants

/**
 * Database-related constants for the application.
 * Extracted from magic numbers across the codebase for better maintainability.
 */
object DatabaseConstants {

    // Database configuration
    const val DATABASE_NAME = "mineralog_database"
    const val DATABASE_VERSION = 5

    // Batch operation sizes
    const val BATCH_INSERT_SIZE = 100
    const val BATCH_QUERY_SIZE = 500
    const val BATCH_DELETE_SIZE = 100

    // Query limits
    const val DEFAULT_QUERY_LIMIT = 1000
    const val MAX_SEARCH_RESULTS = 500

    // Transaction timeouts (milliseconds)
    const val TRANSACTION_TIMEOUT_MS = 30000L
    const val QUERY_TIMEOUT_MS = 10000L

    // Backup file size limits (bytes)
    const val MAX_BACKUP_FILE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB
    const val MAX_DECOMPRESSED_SIZE_BYTES = 500 * 1024 * 1024L // 500 MB
    const val MAX_DECOMPRESSION_RATIO = 100 // ZIP bomb protection

    // CSV processing
    const val CSV_BUFFER_SIZE = 8192
    const val MAX_CSV_ROWS = 10000
    const val MAX_CSV_FILE_SIZE_MB = 50

    // Photo storage
    const val MAX_PHOTO_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB per photo
    const val MAX_PHOTOS_PER_MINERAL = 20
    const val PHOTO_COMPRESSION_QUALITY = 85 // JPEG quality 0-100

    // Cache sizes
    const val STATEMENT_CACHE_SIZE = 25
    const val PREPARED_STATEMENT_CACHE_SIZE = 50
}
