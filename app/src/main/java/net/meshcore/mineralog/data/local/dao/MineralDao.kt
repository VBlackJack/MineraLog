package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * DAO for Mineral entity operations.
 * Provides CRUD operations and complex queries for searching and filtering.
 */
@Dao
interface MineralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mineral: MineralEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(minerals: List<MineralEntity>)

    @Update
    suspend fun update(mineral: MineralEntity)

    @Delete
    suspend fun delete(mineral: MineralEntity)

    @Query("DELETE FROM minerals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM minerals")
    suspend fun deleteAll()

    @Query("SELECT * FROM minerals WHERE id = :id")
    suspend fun getById(id: String): MineralEntity?

    @Query("SELECT * FROM minerals WHERE id = :id")
    fun getByIdFlow(id: String): Flow<MineralEntity?>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<MineralEntity>>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MineralEntity>

    @Query("""
        SELECT * FROM minerals
        WHERE name LIKE '%' || :query || '%'
           OR group LIKE '%' || :query || '%'
           OR formula LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchFlow(query: String): Flow<List<MineralEntity>>

    @Query("""
        SELECT * FROM minerals
        WHERE (:group IS NULL OR group = :group)
          AND (:crystalSystem IS NULL OR crystalSystem = :crystalSystem)
          AND (:status IS NULL OR status = :status)
          AND (:mohsMin IS NULL OR mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR mohsMin <= :mohsMax)
        ORDER BY updatedAt DESC
    """)
    fun filterFlow(
        group: String? = null,
        crystalSystem: String? = null,
        status: String? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null
    ): Flow<List<MineralEntity>>

    @Query("SELECT COUNT(*) FROM minerals")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM minerals")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT DISTINCT group FROM minerals WHERE group IS NOT NULL ORDER BY group")
    fun getDistinctGroupsFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT crystalSystem FROM minerals WHERE crystalSystem IS NOT NULL ORDER BY crystalSystem")
    fun getDistinctCrystalSystemsFlow(): Flow<List<String>>
}
