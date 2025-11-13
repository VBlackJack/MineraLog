package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.FilterPresetEntity

/**
 * DAO for FilterPreset entity operations.
 * Manages saved filter presets for quick access.
 */
@Dao
interface FilterPresetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: FilterPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<FilterPresetEntity>)

    @Update
    suspend fun update(preset: FilterPresetEntity)

    @Delete
    suspend fun delete(preset: FilterPresetEntity)

    @Query("DELETE FROM filter_presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM filter_presets")
    suspend fun deleteAll()

    @Query("SELECT * FROM filter_presets WHERE id = :id")
    suspend fun getById(id: String): FilterPresetEntity?

    @Query("SELECT * FROM filter_presets WHERE id = :id")
    fun getByIdFlow(id: String): Flow<FilterPresetEntity?>

    @Query("SELECT * FROM filter_presets ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<FilterPresetEntity>>

    @Query("SELECT * FROM filter_presets ORDER BY updatedAt DESC")
    suspend fun getAll(): List<FilterPresetEntity>

    @Query("SELECT * FROM filter_presets WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchFlow(query: String): Flow<List<FilterPresetEntity>>

    @Query("SELECT COUNT(*) FROM filter_presets")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM filter_presets")
    fun getCountFlow(): Flow<Int>
}
