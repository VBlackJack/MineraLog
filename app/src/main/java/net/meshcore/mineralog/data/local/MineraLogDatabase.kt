package net.meshcore.mineralog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.meshcore.mineralog.data.local.converter.Converters
import net.meshcore.mineralog.data.local.dao.FilterPresetDao
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.FilterPresetEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.data.local.migration.MIGRATION_1_2
import net.meshcore.mineralog.data.local.migration.MIGRATION_2_3

/**
 * Main Room database for MineraLog application.
 * Version 1: Initial schema with minerals, provenances, storage, and photos tables.
 * Version 2: Added lifecycle status, quality rating, completeness, and FK fields to minerals.
 * Version 3: Added filter_presets table for saved filter combinations (v1.2.0).
 */
@Database(
    entities = [
        MineralEntity::class,
        ProvenanceEntity::class,
        StorageEntity::class,
        PhotoEntity::class,
        FilterPresetEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MineraLogDatabase : RoomDatabase() {

    abstract fun mineralDao(): MineralDao
    abstract fun provenanceDao(): ProvenanceDao
    abstract fun storageDao(): StorageDao
    abstract fun photoDao(): PhotoDao
    abstract fun filterPresetDao(): FilterPresetDao

    companion object {
        @Volatile
        private var INSTANCE: MineraLogDatabase? = null

        fun getDatabase(context: Context): MineraLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MineraLogDatabase::class.java,
                    "mineralog_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // v1.1 & v1.2: Proper migrations
                    .fallbackToDestructiveMigration() // Fallback for unhandled migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
