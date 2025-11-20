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
 * Migration from version 5 to version 6.
 *
 * Changes:
 * - Create reference_minerals table (library of mineral templates)
 * - Add referenceMineralId column to simple_properties (link to reference)
 * - Add referenceMineralId column to mineral_components (link to reference)
 * - Add specimen-specific columns to simple_properties (colorVariety, actualDiaphaneity, qualityNotes)
 * - Add indices for new columns and tables
 *
 * v3.0.0 feature: Reference Mineral Library
 *
 * The reference_minerals table contains a library of mineral templates with
 * standardized properties. When creating a specimen, users can select a
 * reference mineral to auto-fill technical properties.
 *
 * Backward compatibility:
 * - All existing specimens have referenceMineralId = NULL (manual entry mode)
 * - New specimen-specific fields default to NULL
 * - Initial dataset population happens via database callback (not in migration)
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create reference_minerals table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reference_minerals (
                id TEXT PRIMARY KEY NOT NULL,
                nameFr TEXT NOT NULL,
                nameEn TEXT NOT NULL,
                synonyms TEXT,
                mineralGroup TEXT,
                formula TEXT,
                mohsMin REAL,
                mohsMax REAL,
                density REAL,
                crystalSystem TEXT,
                cleavage TEXT,
                fracture TEXT,
                habit TEXT,
                luster TEXT,
                streak TEXT,
                diaphaneity TEXT,
                fluorescence TEXT,
                magnetism TEXT,
                radioactivity TEXT,
                notes TEXT,
                isUserDefined INTEGER NOT NULL DEFAULT 0,
                source TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Create indices on reference_minerals table
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_reference_minerals_nameFr
            ON reference_minerals(nameFr)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_reference_minerals_nameEn
            ON reference_minerals(nameEn)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_reference_minerals_mineralGroup
            ON reference_minerals(mineralGroup)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_reference_minerals_crystalSystem
            ON reference_minerals(crystalSystem)
        """.trimIndent())

        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_reference_minerals_isUserDefined
            ON reference_minerals(isUserDefined)
        """.trimIndent())

        // 3. Add referenceMineralId column to simple_properties
        db.execSQL("""
            ALTER TABLE simple_properties
            ADD COLUMN referenceMineralId TEXT DEFAULT NULL
        """.trimIndent())

        // Create index for referenceMineralId (for usage statistics queries)
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_simple_properties_referenceMineralId
            ON simple_properties(referenceMineralId)
        """.trimIndent())

        // 4. Add specimen-specific columns to simple_properties
        // These fields store specimen-specific variations that override reference properties
        db.execSQL("""
            ALTER TABLE simple_properties
            ADD COLUMN colorVariety TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE simple_properties
            ADD COLUMN actualDiaphaneity TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE simple_properties
            ADD COLUMN qualityNotes TEXT DEFAULT NULL
        """.trimIndent())

        // 5. Add referenceMineralId column to mineral_components
        db.execSQL("""
            ALTER TABLE mineral_components
            ADD COLUMN referenceMineralId TEXT DEFAULT NULL
        """.trimIndent())

        // Create index for referenceMineralId
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_mineral_components_referenceMineralId
            ON mineral_components(referenceMineralId)
        """.trimIndent())

        // Note: Initial dataset population (50-100 minerals) happens via
        // a database callback when the database is first created, not in this migration.
        // This ensures clean separation between schema evolution and data seeding.
    }
}

/**
 * Migration from version 6 to version 7.
 *
 * Changes:
 * - Add collector-focused fields to reference_minerals table:
 *   - Care & Safety: careInstructions, sensitivity, hazards, storageRecommendations
 *   - Identification: identificationTips, diagnosticProperties, colors, varieties, confusionWith
 *   - Geology: geologicalEnvironment, typicalLocations, associatedMinerals
 *   - Additional: uses, rarity, collectingDifficulty, historicalInfo, etymology
 *
 * v3.0.0 enhancement: Comprehensive reference mineral information for collectors
 *
 * These fields transform the reference library from basic scientific data
 * to a complete collector's resource with practical care, identification,
 * and safety information.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Practical information & Safety
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN careInstructions TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN sensitivity TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN hazards TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN storageRecommendations TEXT DEFAULT NULL
        """.trimIndent())

        // Identification & Recognition
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN identificationTips TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN diagnosticProperties TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN colors TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN varieties TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN confusionWith TEXT DEFAULT NULL
        """.trimIndent())

        // Geological context
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN geologicalEnvironment TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN typicalLocations TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN associatedMinerals TEXT DEFAULT NULL
        """.trimIndent())

        // Additional information
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN uses TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN rarity TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN collectingDifficulty TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN historicalInfo TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN etymology TEXT DEFAULT NULL
        """.trimIndent())
    }
}

/**
 * Migration from version 7 to version 8.
 *
 * Changes:
 * - Add collector-focused fields to provenances table:
 *   - mineName: Specific mine or quarry name
 *   - collectorName: Original collector's name if source="collected"
 *   - dealer: Dealer or vendor name if source="purchase"
 *   - catalogNumber: Museum catalog number, dealer reference, or collection ID
 *   - acquisitionNotes: Additional notes about acquisition, provenance chain, or authenticity
 * - Add aggregate-specific fields to minerals table (for type=AGGREGATE or ROCK):
 *   - rockType: Type of rock/aggregate (Granite, Basalte, Pegmatite, etc.)
 *   - texture: Texture description (Grenu, Porphyrique, Microgrenu, etc.)
 *   - dominantMinerals: Comma-separated list of visually dominant minerals
 *   - interestingFeatures: Notable characteristics for collectors
 *
 * v3.1.0 enhancement: Aggregate-focused fields
 *
 * These fields optimize the database for aggregate and rock specimens where:
 * - Provenance information (mine name, dealer, catalog number) is more valuable
 *   than chemical formulas
 * - Visual/textural properties are more relevant than crystallographic data
 * - Collector context (where obtained, from whom, reference numbers) is essential
 *
 * Backward compatibility:
 * - All new fields are nullable (default NULL)
 * - Existing specimens remain unchanged
 * - SIMPLE specimens can still use existing deprecated mineralogical fields
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ===== Add collector-focused fields to provenances table =====

        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN mineName TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN collectorName TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN dealer TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN catalogNumber TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE provenances
            ADD COLUMN acquisitionNotes TEXT DEFAULT NULL
        """.trimIndent())

        // ===== Add aggregate-specific fields to minerals table =====

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN rockType TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN texture TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN dominantMinerals TEXT DEFAULT NULL
        """.trimIndent())

        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN interestingFeatures TEXT DEFAULT NULL
        """.trimIndent())

        // Note: No indices created for these fields as they are primarily used for display
        // rather than filtering. If filtering by rockType or texture becomes a common
        // use case, indices can be added in a future migration.
    }
}

/**
 * Migration from version 8 to version 9.
 *
 * Changes:
 * - Add dominantColor column for photo analysis results (v3.2.0)
 *
 * New field:
 * - dominantColor: String detected by ImageAnalyzer (e.g., "Red", "Blue", "Green")
 *   Used for statistics and visualization of color distribution in collection
 *
 * v3.2.0 enhancement: Photo analysis integration
 *
 * This field stores the dominant color detected from the first photo of each specimen.
 * - Populated automatically when using the photo analysis feature
 * - Enables color-based statistics and filtering
 * - Indexed for efficient query performance in statistics screens
 *
 * Backward compatibility:
 * - Field is nullable (default NULL)
 * - Existing specimens will have NULL until photo analysis is run
 * - Index created to optimize GROUP BY queries for statistics
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add dominantColor column to minerals table
        db.execSQL("""
            ALTER TABLE minerals
            ADD COLUMN dominantColor TEXT DEFAULT NULL
        """.trimIndent())

        // Create index for efficient statistics queries
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS index_minerals_dominantColor
            ON minerals(dominantColor)
        """.trimIndent())
    }
}

/**
 * Migration from version 9 to version 10.
 *
 * Changes:
 * - Add imageUrl column to reference_minerals (remote image URLs)
 * - Add localIconName column to reference_minerals (local drawable resource names)
 *
 * v3.3.0 feature: Image support for reference minerals (hybrid cloud/local)
 *
 * Backward compatibility:
 * - Both fields are nullable (default NULL)
 * - Existing reference minerals will have NULL until images are added
 * - No indices needed (not used for querying, only for display)
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add imageUrl column for remote/cloud-hosted images
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN imageUrl TEXT DEFAULT NULL
        """.trimIndent())

        // Add localIconName column for bundled drawable resources
        db.execSQL("""
            ALTER TABLE reference_minerals
            ADD COLUMN localIconName TEXT DEFAULT NULL
        """.trimIndent())
    }
}

/**
 * Future migrations will be added here as the schema evolves.
 */
