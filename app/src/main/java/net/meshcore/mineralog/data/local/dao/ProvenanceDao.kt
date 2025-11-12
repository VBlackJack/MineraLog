package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity

/**
 * DAO for Provenance entity operations.
 */
@Dao
interface ProvenanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provenance: ProvenanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(provenances: List<ProvenanceEntity>)

    @Update
    suspend fun update(provenance: ProvenanceEntity)

    @Delete
    suspend fun delete(provenance: ProvenanceEntity)

    @Query("DELETE FROM provenances WHERE mineralId = :mineralId")
    suspend fun deleteByMineralId(mineralId: String)

    @Query("DELETE FROM provenances")
    suspend fun deleteAll()

    @Query("SELECT * FROM provenances WHERE id = :id")
    suspend fun getById(id: String): ProvenanceEntity?

    @Query("SELECT * FROM provenances WHERE mineralId = :mineralId")
    suspend fun getByMineralId(mineralId: String): ProvenanceEntity?

    @Query("SELECT * FROM provenances WHERE mineralId = :mineralId")
    fun getByMineralIdFlow(mineralId: String): Flow<ProvenanceEntity?>

    @Query("SELECT * FROM provenances")
    suspend fun getAll(): List<ProvenanceEntity>

    @Query("SELECT * FROM provenances WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    fun getAllWithCoordinatesFlow(): Flow<List<ProvenanceEntity>>

    @Query("SELECT * FROM provenances WHERE latitude IS NOT NULL AND longitude IS NOT NULL")
    suspend fun getAllWithCoordinates(): List<ProvenanceEntity>

    @Query("SELECT DISTINCT country FROM provenances WHERE country IS NOT NULL ORDER BY country")
    fun getDistinctCountriesFlow(): Flow<List<String>>
}
