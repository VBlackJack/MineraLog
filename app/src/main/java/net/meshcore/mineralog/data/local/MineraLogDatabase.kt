package net.meshcore.mineralog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.meshcore.mineralog.data.local.converter.Converters
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity

/**
 * Main Room database for MineraLog application.
 * Version 1: Initial schema with minerals, provenances, storage, and photos tables.
 */
@Database(
    entities = [
        MineralEntity::class,
        ProvenanceEntity::class,
        StorageEntity::class,
        PhotoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MineraLogDatabase : RoomDatabase() {

    abstract fun mineralDao(): MineralDao
    abstract fun provenanceDao(): ProvenanceDao
    abstract fun storageDao(): StorageDao
    abstract fun photoDao(): PhotoDao

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
                    .fallbackToDestructiveMigration() // For v1.0, accept data loss on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
