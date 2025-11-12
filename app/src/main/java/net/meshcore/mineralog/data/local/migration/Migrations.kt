package net.meshcore.mineralog.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations for MineraLog.
 * Each migration preserves user data while evolving the schema.
 */

/**
 * Migration from version 1 to version 2.
 *
 * Changes:
 * - Add statusType column (lifecycle status: in_collection, on_display, loaned, etc.)
 * - Add statusDetails column (JSON field for extensible status metadata)
 * - Add qualityRating column (1-5 specimen quality assessment)
 * - Add completeness column (0-100 percentage of filled fields)
 * - Add provenanceId column (FK to provenance table)
 * - Add storageId column (FK to storage table)
 * - Add indices for new columns for query performance
 *
 * Backward compatibility:
 * - Default statusType = 'in_collection' for existing minerals
 * - Default completeness = 0 (will be recalculated by app)
 * - NULL for optional fields (qualityRating, statusDetails, provenanceId, storageId)
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns to minerals table with default values
        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN statusType TEXT NOT NULL DEFAULT 'in_collection'
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN statusDetails TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN qualityRating INTEGER DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN completeness INTEGER NOT NULL DEFAULT 0
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN provenanceId TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN storageId TEXT DEFAULT NULL
        """.trimIndent())

        // Create indices for new columns to optimize queries
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_statusType
            ON minerals(statusType)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_completeness
            ON minerals(completeness)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_qualityRating
            ON minerals(qualityRating)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_provenanceId
            ON minerals(provenanceId)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_storageId
            ON minerals(storageId)
        """.trimIndent())

        // Note: We intentionally do NOT add foreign key constraints here
        // to maintain flexibility and avoid cascading delete complexity.
        // The app layer enforces referential integrity.
    }
}

/**
 * Future migrations will be added here as the schema evolves.
 * Example:
 *
 * val MIGRATION_2_3 = object : Migration(2, 3) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         // Schema changes for v3
 *     }
 * }
 */
