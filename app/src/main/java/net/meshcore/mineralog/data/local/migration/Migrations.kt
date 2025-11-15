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
 * Migration from version 2 to version 3.
 *
 * Changes:
 * - Add filter_presets table for saving filter combinations
 * - Indices on name and createdAt for query performance
 *
 * v1.2.0 feature: Advanced filtering with saved presets
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create filter_presets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS filter_presets (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                icon TEXT NOT NULL DEFAULT 'filter_list',
                criteriaJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // Create indices for performance
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_filter_presets_name
            ON filter_presets(name)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_filter_presets_createdAt
            ON filter_presets(createdAt)
        """.trimIndent())
    }
}

/**
 * Migration from version 3 to version 4.
 *
 * Changes:
 * - Add currency column to provenances table for multi-currency support
 *
 * v1.4.1 enhancement: Currency tracking for international collections
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add currency column with default USD value
        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN currency TEXT DEFAULT 'USD'
        """.trimIndent())
    }
}

/**
 * Migration from version 4 to version 5.
 *
 * Changes:
 * - Add type column to minerals table (SIMPLE or AGGREGATE)
 * - Create simple_properties table for simple mineral properties
 * - Create mineral_components table for aggregate components
 * - Migrate existing mineral properties to simple_properties table
 * - Add indices for new tables and columns
 *
 * v2.0.0 feature: Mineral aggregates support (Granite, Gneiss, etc.)
 *
 * Backward compatibility:
 * - All existing minerals are treated as type='SIMPLE'
 * - Existing property fields in minerals table are preserved but deprecated
 * - Properties are copied to simple_properties table
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add type column to minerals table (default = 'SIMPLE' for backward compatibility)
        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN type TEXT NOT NULL DEFAULT 'SIMPLE'
        """.trimIndent())

        // Create index for type column
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_type
            ON minerals(type)
        """.trimIndent())

        // 2. Create simple_properties table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS simple_properties (
                id TEXT PRIMARY KEY NOT NULL,
                mineralId TEXT NOT NULL,
                `group` TEXT,
                mohsMin REAL,
                mohsMax REAL,
                density REAL,
                formula TEXT,
                crystalSystem TEXT,
                luster TEXT,
                diaphaneity TEXT,
                cleavage TEXT,
                fracture TEXT,
                habit TEXT,
                streak TEXT,
                fluorescence TEXT,
                FOREIGN KEY(mineralId) REFERENCES minerals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create unique index on mineralId (one-to-one relationship)
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_simple_properties_mineralId
            ON simple_properties(mineralId)
        """.trimIndent())

        // 3. Create mineral_components table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS mineral_components (
                id TEXT PRIMARY KEY NOT NULL,
                aggregateId TEXT NOT NULL,
                displayOrder INTEGER NOT NULL,
                mineralName TEXT NOT NULL,
                mineralGroup TEXT,
                percentage REAL,
                role TEXT NOT NULL,
                mohsMin REAL,
                mohsMax REAL,
                density REAL,
                formula TEXT,
                crystalSystem TEXT,
                luster TEXT,
                diaphaneity TEXT,
                cleavage TEXT,
                fracture TEXT,
                habit TEXT,
                streak TEXT,
                fluorescence TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(aggregateId) REFERENCES minerals(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create indices for mineral_components table
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_mineral_components_aggregateId
            ON mineral_components(aggregateId)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_mineral_components_role
            ON mineral_components(role)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_mineral_components_mineralName
            ON mineral_components(mineralName)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_mineral_components_displayOrder
            ON mineral_components(displayOrder)
        """.trimIndent())

        // 4. Migrate existing mineral data to simple_properties table
        // Generate unique IDs by appending '_props' to mineral ID
        db.execSQL("""
            INSERT INTO simple_properties (
                id,
                mineralId,
                `group`,
                mohsMin,
                mohsMax,
                density,
                formula,
                crystalSystem,
                luster,
                diaphaneity,
                cleavage,
                fracture,
                habit,
                streak,
                fluorescence
            )
            SELECT
                id || '_props' AS id,
                id AS mineralId,
                `group`,
                mohsMin,
                mohsMax,
                specificGravity AS density,
                formula,
                crystalSystem,
                luster,
                diaphaneity,
                cleavage,
                fracture,
                habit,
                streak,
                fluorescence
            FROM minerals
            WHERE type = 'SIMPLE'
        """.trimIndent())

        // Note: The deprecated columns in minerals table are intentionally kept
        // for backward compatibility. They will be removed in a future migration
        // once all code has been updated to use the new tables.
    }
}

/**
 * Future migrations will be added here as the schema evolves.
 */
