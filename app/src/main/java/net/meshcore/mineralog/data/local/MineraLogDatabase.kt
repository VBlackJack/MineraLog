package net.meshcore.mineralog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import net.meshcore.mineralog.data.local.converter.Converters
import net.meshcore.mineralog.data.local.dao.FilterPresetDao
import net.meshcore.mineralog.data.local.dao.MineralBasicDao
import net.meshcore.mineralog.data.local.dao.MineralComponentDao
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.MineralDaoComposite
import net.meshcore.mineralog.data.local.dao.MineralPagingDao
import net.meshcore.mineralog.data.local.dao.MineralQueryDao
import net.meshcore.mineralog.data.local.dao.MineralStatisticsDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.ReferenceMineralDao
import net.meshcore.mineralog.data.local.dao.SimplePropertiesDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.FilterPresetEntity
import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.data.local.migration.MIGRATION_1_2
import net.meshcore.mineralog.data.local.migration.MIGRATION_2_3
import net.meshcore.mineralog.data.local.migration.MIGRATION_3_4
import net.meshcore.mineralog.data.local.migration.MIGRATION_4_5
import net.meshcore.mineralog.data.local.migration.MIGRATION_5_6
import net.meshcore.mineralog.data.local.migration.MIGRATION_6_7
import net.meshcore.mineralog.data.local.migration.MIGRATION_7_8
import net.meshcore.mineralog.util.AppLogger

/**
 * Main Room database for MineraLog application.
 * Version 1: Initial schema with minerals, provenances, storage, and photos tables.
 * Version 2: Added lifecycle status, quality rating, completeness, and FK fields to minerals.
 * Version 3: Added filter_presets table for saved filter combinations (v1.2.0).
 * Version 4: Added currency field to provenances table for multi-currency support (v1.4.1).
 * Version 5: Added support for mineral aggregates with simple_properties and mineral_components tables (v2.0.0).
 * Version 6: Added reference_minerals table for mineral library and referenceMineralId links (v3.0.0).
 * Version 7: Extended reference_minerals with collector-focused fields (care, safety, identification, geology) (v3.0.0).
 * Version 8: Added aggregate-focused fields to minerals (rockType, texture, dominantMinerals, interestingFeatures)
 *            and collector fields to provenances (mineName, collectorName, dealer, catalogNumber, acquisitionNotes) (v3.1.0).
 */
@Database(
    entities = [
        MineralEntity::class,
        SimplePropertiesEntity::class,
        MineralComponentEntity::class,
        ReferenceMineralEntity::class,
        ProvenanceEntity::class,
        StorageEntity::class,
        PhotoEntity::class,
        FilterPresetEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MineraLogDatabase : RoomDatabase() {

    // ========== LEGACY DAO (deprecated, use mineralDaoComposite() instead) ==========

    /**
     * Legacy MineralDao - use [mineralDaoComposite] instead for better maintainability.
     * @deprecated Use [mineralDaoComposite] which delegates to specialized DAOs
     */
    @Deprecated(
        message = "Use mineralDaoComposite() for better maintainability",
        replaceWith = ReplaceWith("mineralDaoComposite()"),
        level = DeprecationLevel.WARNING
    )
    abstract fun mineralDao(): MineralDao

    // ========== SPECIALIZED MINERAL DAOs ==========

    /**
     * Basic CRUD operations for minerals.
     */
    abstract fun mineralBasicDao(): MineralBasicDao

    /**
     * Query operations with filters and search for minerals.
     */
    abstract fun mineralQueryDao(): MineralQueryDao

    /**
     * Statistics and aggregation operations for minerals.
     */
    abstract fun mineralStatisticsDao(): MineralStatisticsDao

    /**
     * Paginated query operations for minerals.
     */
    abstract fun mineralPagingDao(): MineralPagingDao

    /**
     * Composite DAO that provides the same API as the legacy MineralDao
     * but delegates to specialized DAOs for better maintainability.
     * This is the recommended way to access mineral data.
     */
    fun mineralDaoComposite(): MineralDaoComposite {
        return MineralDaoComposite(
            basicDao = mineralBasicDao(),
            queryDao = mineralQueryDao(),
            statisticsDao = mineralStatisticsDao(),
            pagingDao = mineralPagingDao()
        )
    }

    // ========== OTHER DAOs ==========

    abstract fun simplePropertiesDao(): SimplePropertiesDao
    abstract fun mineralComponentDao(): MineralComponentDao
    abstract fun referenceMineralDao(): ReferenceMineralDao
    abstract fun provenanceDao(): ProvenanceDao
    abstract fun storageDao(): StorageDao
    abstract fun photoDao(): PhotoDao
    abstract fun filterPresetDao(): FilterPresetDao

    companion object {
        @Volatile
        private var INSTANCE: MineraLogDatabase? = null

        fun getDatabase(context: Context): MineraLogDatabase {
            return INSTANCE ?: synchronized(this) {
                // Check if migration from plaintext to encrypted is needed
                // This must happen BEFORE we try to open the database
                val migrationResult = DatabaseMigrationHelper.migrateIfNeeded(context)
                when (migrationResult) {
                    is DatabaseMigrationHelper.MigrationResult.Success -> {
                        AppLogger.i("MineraLogDB", "Database migrated to encrypted format. Backup at: ${migrationResult.backupPath}")
                        // Optionally delete backup after verification in production
                        // DatabaseMigrationHelper.deleteBackup(migrationResult.backupPath)
                    }
                    is DatabaseMigrationHelper.MigrationResult.Error -> {
                        AppLogger.e("MineraLogDB", "Migration failed: ${migrationResult.message}", migrationResult.cause ?: Exception("Unknown error"))
                        // In production, you might want to show user a dialog or handle this gracefully
                        throw IllegalStateException("Failed to migrate database to encrypted format", migrationResult.cause)
                    }
                    is DatabaseMigrationHelper.MigrationResult.AlreadyEncrypted,
                    is DatabaseMigrationHelper.MigrationResult.NoDatabase -> {
                        // Normal case, continue with database initialization
                    }
                }

                // Initialize SQLCipher native libraries
                System.loadLibrary("sqlcipher")

                // Get or create database encryption passphrase
                val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

                // Create SupportFactory for SQLCipher encryption
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MineraLogDatabase::class.java,
                    "mineralog_database"
                )
                    .openHelperFactory(factory) // Enable SQLCipher encryption
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    ) // Proper migrations for schema evolution
                    // Note: fallbackToDestructiveMigration() has been removed to protect user data
                    // All migrations must be properly defined before releasing new schema versions
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Database created with encryption enabled
                            AppLogger.i("MineraLogDB", "Encrypted database created")
                            // Note: Initial reference minerals dataset population happens
                            // at application startup via a dedicated initializer or repository check.
                            // See ReferenceMineralRepository.populateInitialDataset()
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Database opened successfully with encryption
                            AppLogger.d("MineraLogDB", "Encrypted database opened")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
