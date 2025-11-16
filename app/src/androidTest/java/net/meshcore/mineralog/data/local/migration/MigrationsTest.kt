package net.meshcore.mineralog.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.migration.MIGRATION_1_2
import net.meshcore.mineralog.data.local.migration.MIGRATION_2_3
import net.meshcore.mineralog.data.local.migration.MIGRATION_3_4
import net.meshcore.mineralog.data.local.migration.MIGRATION_4_5
import net.meshcore.mineralog.data.local.migration.MIGRATION_5_6
import net.meshcore.mineralog.data.local.migration.MIGRATION_6_7
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for Room database migrations.
 *
 * Coverage: P0 (Critical) tests for:
 * - Migration 1→2: Data preservation and default values
 * - Migration 2→3: Filter presets table creation
 * - Migration 3→4: Currency column addition
 * - Migration 4→5: Mineral types and properties tables
 * - Migration 5→6: Reference minerals library
 * - Migration 6→7: Collector-focused fields
 * - Multi-step migrations: 1→4, 1→7
 * - Indices and constraints verification
 */
@RunWith(AndroidJUnit4::class)
class MigrationsTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MineraLogDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        // Clear any existing test database
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        // Cleanup handled by MigrationTestHelper
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_preservesData() {
        // Given - Create v1 database and insert test data
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO minerals (id, name, formula, `group`, createdAt, updatedAt, status)
                VALUES ('test-001', 'Quartz', 'SiO₂', 'Silicates', 1000000, 1000000, 'complete')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO minerals (id, name, formula, `group`, createdAt, updatedAt, status)
                VALUES ('test-002', 'Calcite', 'CaCO₃', 'Carbonates', 1000000, 1000000, 'incomplete')
                """.trimIndent()
            )
            close()
        }

        // When - Run migration 1→2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Then - Verify all original data is preserved
        val cursor = db.query("SELECT * FROM minerals WHERE id = 'test-001'")
        assertTrue("Should have data after migration", cursor.moveToFirst())

        val nameIndex = cursor.getColumnIndex("name")
        val formulaIndex = cursor.getColumnIndex("formula")
        val groupIndex = cursor.getColumnIndex("group")

        assertEquals("Quartz", cursor.getString(nameIndex))
        assertEquals("SiO₂", cursor.getString(formulaIndex))
        assertEquals("Silicates", cursor.getString(groupIndex))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_addsDefaultStatusType() {
        // Given
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO minerals (id, name, formula, createdAt, updatedAt, status)
                VALUES ('test-001', 'Quartz', 'SiO₂', 1000000, 1000000, 'complete')
                """.trimIndent()
            )
            close()
        }

        // When
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Then - Verify statusType column exists with default value
        val cursor = db.query("SELECT * FROM minerals WHERE id = 'test-001'")
        assertTrue(cursor.moveToFirst())

        val statusTypeIndex = cursor.getColumnIndex("statusType")
        assertTrue("statusType column should exist", statusTypeIndex >= 0)
        assertEquals("in_collection", cursor.getString(statusTypeIndex))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_addsCompleteness() {
        // Given
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO minerals (id, name, formula, createdAt, updatedAt, status)
                VALUES ('test-001', 'Quartz', 'SiO₂', 1000000, 1000000, 'complete')
                """.trimIndent()
            )
            close()
        }

        // When
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Then - Verify completeness column exists with default value 0
        val cursor = db.query("SELECT * FROM minerals WHERE id = 'test-001'")
        assertTrue(cursor.moveToFirst())

        val completenessIndex = cursor.getColumnIndex("completeness")
        assertTrue("completeness column should exist", completenessIndex >= 0)
        assertEquals(0, cursor.getInt(completenessIndex))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_2_createsIndices() {
        // Given
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // When
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Then - Verify indices were created
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='minerals'")

        val indexNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexNames.add(cursor.getString(0))
        }
        cursor.close()

        // Verify expected indices exist (note: Room auto-creates indices for @Index annotations)
        assertTrue("Should have index on statusType",
            indexNames.any { it.contains("statusType") })
        assertTrue("Should have index on completeness",
            indexNames.any { it.contains("completeness") })
        assertTrue("Should have index on qualityRating",
            indexNames.any { it.contains("qualityRating") })
        assertTrue("Should have index on provenanceId",
            indexNames.any { it.contains("provenanceId") })
        assertTrue("Should have index on storageId",
            indexNames.any { it.contains("storageId") })
    }

    @Test
    @Throws(IOException::class)
    fun migration_2_to_3_createsFilterPresets() {
        // Given - Start with v2 database
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // When - Migrate to v3
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Then - Verify filter_presets table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='filter_presets'")
        assertTrue("filter_presets table should exist", cursor.moveToFirst())
        cursor.close()

        // Verify can insert into table
        db.execSQL(
            """
            INSERT INTO filter_presets (id, name, icon, criteriaJson, createdAt, updatedAt)
            VALUES ('preset-001', 'My Filter', 'filter_list', '{}', 1000000, 1000000)
            """.trimIndent()
        )

        val presetCursor = db.query("SELECT * FROM filter_presets WHERE id = 'preset-001'")
        assertTrue("Should be able to insert filter preset", presetCursor.moveToFirst())
        assertEquals("My Filter", presetCursor.getString(presetCursor.getColumnIndex("name")))
        presetCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_3_to_4_addsCurrency() {
        // Given - Create v1, migrate to v2, then v3
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Insert a provenance in v3
        val db3 = helper.runMigrationsAndValidate(TEST_DB, 3, false)
        db3.execSQL(
            """
            INSERT INTO provenances (id, mineralId, country, price)
            VALUES ('prov-001', 'min-001', 'France', 100.0)
            """.trimIndent()
        )
        db3.close()

        // When - Migrate to v4
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // Then - Verify currency column exists with default value
        val cursor = db.query("SELECT * FROM provenances WHERE id = 'prov-001'")
        assertTrue(cursor.moveToFirst())

        val currencyIndex = cursor.getColumnIndex("currency")
        assertTrue("currency column should exist", currencyIndex >= 0)
        assertEquals("USD", cursor.getString(currencyIndex))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_4_multiStep_succeeds() {
        // Given - Create v1 database with data
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO minerals (id, name, formula, createdAt, updatedAt, status)
                VALUES ('test-001', 'Quartz', 'SiO₂', 1000000, 1000000, 'complete')
                """.trimIndent()
            )
            close()
        }

        // When - Run all migrations 1→2→3→4
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        // Then - Verify data survived all migrations
        val cursor = db.query("SELECT * FROM minerals WHERE id = 'test-001'")
        assertTrue("Data should survive multi-step migration", cursor.moveToFirst())
        assertEquals("Quartz", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("SiO₂", cursor.getString(cursor.getColumnIndex("formula")))

        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_5_to_6_addsReferenceMinerals() {
        // Given - Create v1, migrate through v2→v3→v4→v5
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // When - Migrate to v6
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - Verify reference_minerals table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='reference_minerals'")
        assertTrue("reference_minerals table should exist", cursor.moveToFirst())
        cursor.close()

        // Verify we can insert a reference mineral
        db.execSQL("""
            INSERT INTO reference_minerals (
                id, nameFr, nameEn, formula, mohsMin, mohsMax,
                isUserDefined, source, createdAt, updatedAt
            )
            VALUES (
                'ref-001', 'Quartz', 'Quartz', 'SiO₂', 7.0, 7.0,
                0, 'Test', 1000000, 1000000
            )
        """.trimIndent())

        val refCursor = db.query("SELECT * FROM reference_minerals WHERE id = 'ref-001'")
        assertTrue("Should be able to insert reference mineral", refCursor.moveToFirst())
        assertEquals("Quartz", refCursor.getString(refCursor.getColumnIndex("nameFr")))
        assertEquals("SiO₂", refCursor.getString(refCursor.getColumnIndex("formula")))
        refCursor.close()

        // Verify simple_properties has referenceMineralId column
        val spCursor = db.query("PRAGMA table_info(simple_properties)")
        val columnNames = mutableListOf<String>()
        while (spCursor.moveToNext()) {
            columnNames.add(spCursor.getString(spCursor.getColumnIndex("name")))
        }
        spCursor.close()
        assertTrue("simple_properties should have referenceMineralId",
            columnNames.contains("referenceMineralId"))

        // Verify mineral_components has referenceMineralId column
        val mcCursor = db.query("PRAGMA table_info(mineral_components)")
        val mcColumnNames = mutableListOf<String>()
        while (mcCursor.moveToNext()) {
            mcColumnNames.add(mcCursor.getString(mcCursor.getColumnIndex("name")))
        }
        mcCursor.close()
        assertTrue("mineral_components should have referenceMineralId",
            mcColumnNames.contains("referenceMineralId"))
    }

    @Test
    @Throws(IOException::class)
    fun migration_6_to_7_addsCollectorFields() {
        // Given - Create v1, migrate through v2→v3→v4→v5→v6
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)
        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Insert a reference mineral in v6 to verify data preservation
        val db6 = helper.runMigrationsAndValidate(TEST_DB, 6, false)
        db6.execSQL("""
            INSERT INTO reference_minerals (
                id, nameFr, nameEn, formula, isUserDefined, source, createdAt, updatedAt
            )
            VALUES (
                'ref-001', 'Quartz', 'Quartz', 'SiO₂', 0, 'Test', 1000000, 1000000
            )
        """.trimIndent())
        db6.close()

        // When - Migrate to v7
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // Then - Verify new collector-focused columns exist
        val cursor = db.query("PRAGMA table_info(reference_minerals)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndex("name")))
        }
        cursor.close()

        // Verify care & safety fields
        assertTrue("Should have careInstructions column", columnNames.contains("careInstructions"))
        assertTrue("Should have sensitivity column", columnNames.contains("sensitivity"))
        assertTrue("Should have hazards column", columnNames.contains("hazards"))
        assertTrue("Should have storageRecommendations column", columnNames.contains("storageRecommendations"))

        // Verify identification fields
        assertTrue("Should have identificationTips column", columnNames.contains("identificationTips"))
        assertTrue("Should have diagnosticProperties column", columnNames.contains("diagnosticProperties"))
        assertTrue("Should have colors column", columnNames.contains("colors"))
        assertTrue("Should have varieties column", columnNames.contains("varieties"))
        assertTrue("Should have confusionWith column", columnNames.contains("confusionWith"))

        // Verify geological fields
        assertTrue("Should have geologicalEnvironment column", columnNames.contains("geologicalEnvironment"))
        assertTrue("Should have typicalLocations column", columnNames.contains("typicalLocations"))
        assertTrue("Should have associatedMinerals column", columnNames.contains("associatedMinerals"))

        // Verify additional fields
        assertTrue("Should have uses column", columnNames.contains("uses"))
        assertTrue("Should have rarity column", columnNames.contains("rarity"))
        assertTrue("Should have collectingDifficulty column", columnNames.contains("collectingDifficulty"))
        assertTrue("Should have historicalInfo column", columnNames.contains("historicalInfo"))
        assertTrue("Should have etymology column", columnNames.contains("etymology"))

        // Verify existing data preserved
        val dataCursor = db.query("SELECT * FROM reference_minerals WHERE id = 'ref-001'")
        assertTrue("Data should be preserved after migration", dataCursor.moveToFirst())
        assertEquals("Quartz", dataCursor.getString(dataCursor.getColumnIndex("nameFr")))
        assertEquals("SiO₂", dataCursor.getString(dataCursor.getColumnIndex("formula")))
        dataCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migration_1_to_7_multiStep_succeeds() {
        // Given - Create v1 database with test data
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO minerals (id, name, formula, createdAt, updatedAt, status)
                VALUES ('test-001', 'Quartz', 'SiO₂', 1000000, 1000000, 'complete')
            """.trimIndent())
            close()
        }

        // When - Run all migrations 1→2→3→4→5→6→7
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 7, true,
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
        )

        // Then - Verify data survived all migrations
        val cursor = db.query("SELECT * FROM minerals WHERE id = 'test-001'")
        assertTrue("Data should survive multi-step migration to v7", cursor.moveToFirst())
        assertEquals("Quartz", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("SiO₂", cursor.getString(cursor.getColumnIndex("formula")))

        // Verify v2 columns exist
        val statusTypeIndex = cursor.getColumnIndex("statusType")
        assertTrue("statusType column should exist", statusTypeIndex >= 0)
        assertEquals("in_collection", cursor.getString(statusTypeIndex))

        // Verify v5 columns exist
        val typeIndex = cursor.getColumnIndex("type")
        assertTrue("type column should exist", typeIndex >= 0)
        assertEquals("SIMPLE", cursor.getString(typeIndex))

        cursor.close()

        // Verify all tables exist
        val tablesCursor = db.query("""
            SELECT name FROM sqlite_master
            WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'
            ORDER BY name
        """.trimIndent())

        val tables = mutableListOf<String>()
        while (tablesCursor.moveToNext()) {
            tables.add(tablesCursor.getString(0))
        }
        tablesCursor.close()

        assertTrue("Should have minerals table", tables.contains("minerals"))
        assertTrue("Should have provenances table", tables.contains("provenances"))
        assertTrue("Should have storage table", tables.contains("storage"))
        assertTrue("Should have photos table", tables.contains("photos"))
        assertTrue("Should have filter_presets table", tables.contains("filter_presets"))
        assertTrue("Should have simple_properties table", tables.contains("simple_properties"))
        assertTrue("Should have mineral_components table", tables.contains("mineral_components"))
        assertTrue("Should have reference_minerals table", tables.contains("reference_minerals"))
    }
}
