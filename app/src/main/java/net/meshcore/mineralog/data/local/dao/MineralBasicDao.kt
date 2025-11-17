package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * Basic DAO for Mineral CRUD operations.
 * Handles basic insert, update, delete, and retrieval operations.
 *
 * Part of the refactored DAO architecture to improve maintainability.
 * @see MineralQueryDao for filtering and search operations
 * @see MineralStatisticsDao for aggregations and statistics
 * @see MineralPagingDao for paginated queries
 */
@Dao
interface MineralBasicDao {

    // ========== INSERT OPERATIONS ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mineral: MineralEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(minerals: List<MineralEntity>)

    // ========== UPDATE OPERATIONS ==========

    @Update
    suspend fun update(mineral: MineralEntity)

    // ========== DELETE OPERATIONS ==========

    @Delete
    suspend fun delete(mineral: MineralEntity)

    @Query("DELETE FROM minerals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM minerals WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM minerals")
    suspend fun deleteAll()

    // ========== BASIC RETRIEVAL OPERATIONS ==========

    @Query("SELECT * FROM minerals WHERE id = :id")
    suspend fun getById(id: String): MineralEntity?

    @Query("SELECT * FROM minerals WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MineralEntity>

    @Query("SELECT * FROM minerals WHERE id = :id")
    fun getByIdFlow(id: String): Flow<MineralEntity?>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<MineralEntity>>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MineralEntity>

    // ========== COUNT OPERATIONS ==========

    @Query("SELECT COUNT(*) FROM minerals")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM minerals")
    fun getCountFlow(): Flow<Int>
}
