package net.meshcore.mineralog.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.PhotoType

/**
 * DAO for Photo entity operations.
 */
@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Update
    suspend fun update(photo: PhotoEntity)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM photos WHERE mineralId = :mineralId")
    suspend fun deleteByMineralId(mineralId: String)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE mineralId = :mineralId ORDER BY takenAt DESC")
    suspend fun getByMineralId(mineralId: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE mineralId = :mineralId ORDER BY takenAt DESC")
    fun getByMineralIdFlow(mineralId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE mineralId = :mineralId AND type = :type ORDER BY takenAt DESC")
    fun getByMineralIdAndTypeFlow(mineralId: String, type: PhotoType): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY takenAt DESC")
    suspend fun getAll(): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE mineralId = :mineralId")
    suspend fun getCountByMineralId(mineralId: String): Int

    @Query("SELECT COUNT(*) FROM photos WHERE mineralId = :mineralId")
    fun getCountByMineralIdFlow(mineralId: String): Flow<Int>

    @Query("SELECT * FROM photos WHERE mineralId = :mineralId ORDER BY takenAt ASC LIMIT 1")
    suspend fun getFirstPhotoByMineralId(mineralId: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE mineralId = :mineralId ORDER BY takenAt ASC LIMIT 1")
    fun getFirstPhotoByMineralIdFlow(mineralId: String): Flow<PhotoEntity?>
}
