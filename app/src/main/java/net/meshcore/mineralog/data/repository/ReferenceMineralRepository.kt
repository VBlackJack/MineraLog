package net.meshcore.mineralog.data.repository

import android.content.Context
import net.meshcore.mineralog.util.AppLogger
import androidx.paging.PagingSource
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.local.dao.ReferenceMineralDao
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.local.initializer.ReferenceMineralDatasetLoader
import net.meshcore.mineralog.util.AppLogger

/**
 * Repository for managing the reference minerals library.
 *
 * Provides a clean API for interacting with the reference minerals database,
 * abstracting away Room-specific details and providing domain-level operations.
 */
interface ReferenceMineralRepository {

    // CRUD Operations
    suspend fun insert(mineral: ReferenceMineralEntity)
    suspend fun insertAll(minerals: List<ReferenceMineralEntity>)
    suspend fun update(mineral: ReferenceMineralEntity)
    suspend fun delete(mineral: ReferenceMineralEntity)
    suspend fun deleteById(id: String)

    // Query Operations
    suspend fun getById(id: String): ReferenceMineralEntity?
    fun getByIdFlow(id: String): Flow<ReferenceMineralEntity?>
    fun getAllFlow(): Flow<List<ReferenceMineralEntity>>
    fun getAllPaged(): PagingSource<Int, ReferenceMineralEntity>

    // Search Operations
    fun searchByName(query: String): Flow<List<ReferenceMineralEntity>>
    fun searchByNameLimit(query: String, limit: Int = 10): Flow<List<ReferenceMineralEntity>>

    // Filter Operations
    fun filterPaged(
        groups: List<String>? = null,
        crystalSystems: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        isUserDefined: Boolean? = null
    ): PagingSource<Int, ReferenceMineralEntity>

    // Statistics Operations
    suspend fun countSimpleSpecimensUsingReference(referenceMineralId: String): Int
    suspend fun countComponentsUsingReference(referenceMineralId: String): Int
    suspend fun getTotalUsageCount(referenceMineralId: String): Int

    // Metadata Operations
    fun getDistinctGroups(): Flow<List<String>>
    fun getDistinctCrystalSystems(): Flow<List<String>>
    fun getUserDefinedMinerals(): Flow<List<ReferenceMineralEntity>>
    fun getStandardMinerals(): Flow<List<ReferenceMineralEntity>>

    // Utility Operations
    suspend fun count(): Int
    suspend fun countUserDefined(): Int
    suspend fun existsByName(nameFr: String, nameEn: String): Boolean
    suspend fun deleteAll()

    /**
     * Check if the reference minerals table is empty.
     * Useful for determining if initial population is needed.
     */
    suspend fun isEmpty(): Boolean

    /**
     * Check if a reference mineral can be safely deleted.
     * Returns true if no specimens or components reference this mineral.
     */
    suspend fun canDelete(referenceMineralId: String): Boolean

    /**
     * Populate the database with the initial reference minerals dataset.
     * This should be called once at first app launch or after database creation.
     *
     * @param context Application context to access assets.
     * @return Number of minerals inserted, or 0 if database is already populated.
     */
    suspend fun populateInitialDataset(context: Context): Int
}

class ReferenceMineralRepositoryImpl(
    private val referenceMineralDao: ReferenceMineralDao,
    private val context: Context
) : ReferenceMineralRepository {

    override suspend fun insert(mineral: ReferenceMineralEntity) {
        referenceMineralDao.insert(mineral)
    }

    override suspend fun insertAll(minerals: List<ReferenceMineralEntity>) {
        referenceMineralDao.insertAll(minerals)
    }

    override suspend fun update(mineral: ReferenceMineralEntity) {
        referenceMineralDao.update(mineral)
    }

    override suspend fun delete(mineral: ReferenceMineralEntity) {
        referenceMineralDao.delete(mineral)
    }

    override suspend fun deleteById(id: String) {
        referenceMineralDao.deleteById(id)
    }

    override suspend fun getById(id: String): ReferenceMineralEntity? {
        return referenceMineralDao.getById(id)
    }

    override fun getByIdFlow(id: String): Flow<ReferenceMineralEntity?> {
        return referenceMineralDao.getByIdFlow(id)
    }

    override fun getAllFlow(): Flow<List<ReferenceMineralEntity>> {
        return referenceMineralDao.getAllFlow()
    }

    override fun getAllPaged(): PagingSource<Int, ReferenceMineralEntity> {
        return referenceMineralDao.getAllPaged()
    }

    override fun searchByName(query: String): Flow<List<ReferenceMineralEntity>> {
        return referenceMineralDao.searchByName(query)
    }

    override fun searchByNameLimit(query: String, limit: Int): Flow<List<ReferenceMineralEntity>> {
        return referenceMineralDao.searchByNameLimit(query, limit)
    }

    override fun filterPaged(
        groups: List<String>?,
        crystalSystems: List<String>?,
        mohsMin: Float?,
        mohsMax: Float?,
        isUserDefined: Boolean?
    ): PagingSource<Int, ReferenceMineralEntity> {
        return referenceMineralDao.filterPaged(groups, crystalSystems, mohsMin, mohsMax, isUserDefined)
    }

    override suspend fun countSimpleSpecimensUsingReference(referenceMineralId: String): Int {
        return referenceMineralDao.countSimpleSpecimensUsingReference(referenceMineralId)
    }

    override suspend fun countComponentsUsingReference(referenceMineralId: String): Int {
        return referenceMineralDao.countComponentsUsingReference(referenceMineralId)
    }

    override suspend fun getTotalUsageCount(referenceMineralId: String): Int {
        return referenceMineralDao.getTotalUsageCount(referenceMineralId)
    }

    override fun getDistinctGroups(): Flow<List<String>> {
        return referenceMineralDao.getDistinctGroups()
    }

    override fun getDistinctCrystalSystems(): Flow<List<String>> {
        return referenceMineralDao.getDistinctCrystalSystems()
    }

    override fun getUserDefinedMinerals(): Flow<List<ReferenceMineralEntity>> {
        return referenceMineralDao.getUserDefinedMinerals()
    }

    override fun getStandardMinerals(): Flow<List<ReferenceMineralEntity>> {
        return referenceMineralDao.getStandardMinerals()
    }

    override suspend fun count(): Int {
        return referenceMineralDao.count()
    }

    override suspend fun countUserDefined(): Int {
        return referenceMineralDao.countUserDefined()
    }

    override suspend fun existsByName(nameFr: String, nameEn: String): Boolean {
        return referenceMineralDao.existsByName(nameFr, nameEn)
    }

    override suspend fun deleteAll() {
        referenceMineralDao.deleteAll()
    }

    override suspend fun isEmpty(): Boolean {
        return count() == 0
    }

    override suspend fun canDelete(referenceMineralId: String): Boolean {
        return getTotalUsageCount(referenceMineralId) == 0
    }

    override suspend fun populateInitialDataset(context: Context): Int {
        // Check if database is already populated
        if (!isEmpty()) {
            AppLogger.d("RefMineralRepo", "Database already populated, skipping initial dataset load")
            return 0
        }

        return try {
            val loader = ReferenceMineralDatasetLoader(context)
            val minerals = loader.loadInitialDataset()
            insertAll(minerals)
            AppLogger.i("RefMineralRepo", "Successfully loaded ${minerals.size} reference minerals")
            minerals.size
        } catch (e: Exception) {
            AppLogger.e("RefMineralRepo", "Failed to load initial dataset", e)
            // Don't throw - we can continue without initial data
            0
        }
    }
}
