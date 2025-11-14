package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.StorageEntity

/**
 * DAO for Storage entity operations.
 */
@Dao
interface StorageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storage: StorageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(storages: List<StorageEntity>)

    @Update
    suspend fun update(storage: StorageEntity)

    @Delete
    suspend fun delete(storage: StorageEntity)

    @Query("DELETE FROM storage WHERE mineralId = :mineralId")
    suspend fun deleteByMineralId(mineralId: String)

    @Query("DELETE FROM storage WHERE mineralId IN (:mineralIds)")
    suspend fun deleteByMineralIds(mineralIds: List<String>)

    @Query("DELETE FROM storage")
    suspend fun deleteAll()

    @Query("SELECT * FROM storage WHERE id = :id")
    suspend fun getById(id: String): StorageEntity?

    @Query("SELECT * FROM storage WHERE mineralId = :mineralId")
    suspend fun getByMineralId(mineralId: String): StorageEntity?

    @Query("SELECT * FROM storage WHERE mineralId IN (:mineralIds)")
    suspend fun getByMineralIds(mineralIds: List<String>): List<StorageEntity>

    @Query("SELECT * FROM storage WHERE mineralId = :mineralId")
    fun getByMineralIdFlow(mineralId: String): Flow<StorageEntity?>

    @Query("SELECT * FROM storage")
    suspend fun getAll(): List<StorageEntity>

    @Query("""
        SELECT * FROM storage
        WHERE (:place IS NULL OR place = :place)
          AND (:container IS NULL OR container = :container)
          AND (:box IS NULL OR box = :box)
    """)
    fun filterByLocationFlow(
        place: String? = null,
        container: String? = null,
        box: String? = null
    ): Flow<List<StorageEntity>>

    @Query("SELECT DISTINCT place FROM storage WHERE place IS NOT NULL ORDER BY place")
    fun getDistinctPlacesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT container FROM storage WHERE container IS NOT NULL AND (:place IS NULL OR place = :place) ORDER BY container")
    fun getDistinctContainersFlow(place: String? = null): Flow<List<String>>

    @Query(
        "SELECT DISTINCT box FROM storage WHERE box IS NOT NULL AND " +
            "(:place IS NULL OR place = :place) AND (:container IS NULL OR container = :container) ORDER BY box"
    )
    fun getDistinctBoxesFlow(place: String? = null, container: String? = null): Flow<List<String>>
}
