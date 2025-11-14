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
 * - Multi-step migration 1→4
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
}
