package net.meshcore.mineralog.data.local.converter

import androidx.room.TypeConverter
import net.meshcore.mineralog.data.local.entity.PhotoType
import java.time.Instant

/**
 * Room TypeConverters for custom types.
 * Handles conversion between complex types and SQLite-compatible types.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun fromPhotoType(value: PhotoType): String {
        return value.name
    }

    @TypeConverter
    fun toPhotoType(value: String): PhotoType {
        return try {
            PhotoType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PhotoType.NORMAL
        }
    }
}
