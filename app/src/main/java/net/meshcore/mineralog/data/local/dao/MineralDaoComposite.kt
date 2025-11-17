package net.meshcore.mineralog.data.local.dao

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * Composite DAO that delegates to specialized DAOs for better maintainability.
 * This class preserves the original MineralDao API while distributing responsibilities
 * across focused, specialized DAOs.
 *
 * Benefits:
 * - Each specialized DAO has a single, clear responsibility
 * - Reduced complexity per class (each DAO is ~100-200 lines instead of 748)
 * - Easier to test individual components
 * - Maintains backward compatibility with existing code
 *
 * @see MineralBasicDao for CRUD operations
 * @see MineralQueryDao for filtering and search operations
 * @see MineralStatisticsDao for aggregations and statistics
 * @see MineralPagingDao for paginated queries
 */
class MineralDaoComposite(
    private val basicDao: MineralBasicDao,
    private val queryDao: MineralQueryDao,
    private val statisticsDao: MineralStatisticsDao,
    private val pagingDao: MineralPagingDao
) {

    // ========== BASIC CRUD OPERATIONS (delegated to MineralBasicDao) ==========

    suspend fun insert(mineral: MineralEntity): Long = basicDao.insert(mineral)

    suspend fun insertAll(minerals: List<MineralEntity>) = basicDao.insertAll(minerals)

    suspend fun update(mineral: MineralEntity) = basicDao.update(mineral)

    suspend fun delete(mineral: MineralEntity) = basicDao.delete(mineral)

    suspend fun deleteById(id: String) = basicDao.deleteById(id)

    suspend fun deleteByIds(ids: List<String>) = basicDao.deleteByIds(ids)

    suspend fun deleteAll() = basicDao.deleteAll()

    suspend fun getById(id: String): MineralEntity? = basicDao.getById(id)

    suspend fun getByIds(ids: List<String>): List<MineralEntity> = basicDao.getByIds(ids)

    fun getByIdFlow(id: String): Flow<MineralEntity?> = basicDao.getByIdFlow(id)

    fun getAllFlow(): Flow<List<MineralEntity>> = basicDao.getAllFlow()

    suspend fun getAll(): List<MineralEntity> = basicDao.getAll()

    suspend fun getCount(): Int = basicDao.getCount()

    fun getCountFlow(): Flow<Int> = basicDao.getCountFlow()

    // ========== TYPE-BASED QUERIES (delegated to MineralQueryDao) ==========

    fun getAllSimpleMinerals(): Flow<List<MineralEntity>> = queryDao.getAllSimpleMinerals()

    fun getAllAggregates(): Flow<List<MineralEntity>> = queryDao.getAllAggregates()

    fun getMineralsByType(types: List<String>): Flow<List<MineralEntity>> =
        queryDao.getMineralsByType(types)

    suspend fun countByType(type: String): Int = queryDao.countByType(type)

    // ========== SEARCH OPERATIONS (delegated to MineralQueryDao) ==========

    fun searchFlow(query: String): Flow<List<MineralEntity>> = queryDao.searchFlow(query)

    // ========== FILTER OPERATIONS (delegated to MineralQueryDao) ==========

    fun filterFlow(
        group: String? = null,
        crystalSystem: String? = null,
        status: String? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null
    ): Flow<List<MineralEntity>> = queryDao.filterFlow(group, crystalSystem, status, mohsMin, mohsMax)

    fun filterAdvanced(
        groups: List<String>? = null,
        countries: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        statusTypes: List<String>? = null,
        qualityMin: Int? = null,
        qualityMax: Int? = null,
        hasPhotos: Boolean? = null,
        fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): Flow<List<MineralEntity>> = queryDao.filterAdvanced(
        groups, countries, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun getDistinctGroupsFlow(): Flow<List<String>> = queryDao.getDistinctGroupsFlow()

    fun getDistinctCrystalSystemsFlow(): Flow<List<String>> = queryDao.getDistinctCrystalSystemsFlow()

    suspend fun getAllTags(): List<String> = queryDao.getAllTags()

    // ========== PAGING OPERATIONS (delegated to MineralPagingDao) ==========

    fun getAllPaged(): PagingSource<Int, MineralEntity> = pagingDao.getAllPaged()

    fun getAllPagedSortedByNameAsc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByNameAsc()

    fun getAllPagedSortedByNameDesc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByNameDesc()

    fun getAllPagedSortedByDateDesc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByDateDesc()

    fun getAllPagedSortedByDateAsc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByDateAsc()

    fun getAllPagedSortedByGroup(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByGroup()

    fun getAllPagedSortedByHardnessAsc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByHardnessAsc()

    fun getAllPagedSortedByHardnessDesc(): PagingSource<Int, MineralEntity> =
        pagingDao.getAllPagedSortedByHardnessDesc()

    fun getMineralsByTypePaged(types: List<String>): PagingSource<Int, MineralEntity> =
        pagingDao.getMineralsByTypePaged(types)

    fun searchPaged(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPaged(query)

    fun searchPagedSortedByNameAsc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByNameAsc(query)

    fun searchPagedSortedByNameDesc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByNameDesc(query)

    fun searchPagedSortedByDateDesc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByDateDesc(query)

    fun searchPagedSortedByDateAsc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByDateAsc(query)

    fun searchPagedSortedByGroup(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByGroup(query)

    fun searchPagedSortedByHardnessAsc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByHardnessAsc(query)

    fun searchPagedSortedByHardnessDesc(query: String): PagingSource<Int, MineralEntity> =
        pagingDao.searchPagedSortedByHardnessDesc(query)

    fun filterAdvancedPaged(
        groups: List<String>? = null,
        countries: List<String>? = null,
        crystalSystems: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        statusTypes: List<String>? = null,
        qualityMin: Int? = null,
        qualityMax: Int? = null,
        hasPhotos: Boolean? = null,
        fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPaged(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByNameAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByNameAsc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByNameDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByNameDesc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByDateDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByDateDesc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByDateAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByDateAsc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByGroup(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByGroup(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByHardnessAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByHardnessAsc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    fun filterAdvancedPagedSortedByHardnessDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity> = pagingDao.filterAdvancedPagedSortedByHardnessDesc(
        groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes,
        qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
    )

    // ========== STATISTICS OPERATIONS (delegated to MineralStatisticsDao) ==========

    suspend fun getGroupDistribution(): Map<String, Int> = statisticsDao.getGroupDistribution()

    suspend fun getCountryDistribution(): Map<String, Int> = statisticsDao.getCountryDistribution()

    suspend fun getCrystalSystemDistribution(): Map<String, Int> = statisticsDao.getCrystalSystemDistribution()

    suspend fun getHardnessDistribution(): Map<String, Int> = statisticsDao.getHardnessDistribution()

    suspend fun getStatusDistribution(): Map<String, Int> = statisticsDao.getStatusDistribution()

    suspend fun getTypeDistribution(): Map<String, Int> = statisticsDao.getTypeDistribution()

    suspend fun getTotalValue(): Double = statisticsDao.getTotalValue()

    suspend fun getAverageValue(): Double = statisticsDao.getAverageValue()

    suspend fun getMostValuableSpecimen(): MineralValueInfo? = statisticsDao.getMostValuableSpecimen()

    suspend fun getAverageCompleteness(): Double = statisticsDao.getAverageCompleteness()

    suspend fun getFullyDocumentedCount(): Int = statisticsDao.getFullyDocumentedCount()

    suspend fun getAddedThisMonth(): Int = statisticsDao.getAddedThisMonth()

    suspend fun getAddedThisYear(): Int = statisticsDao.getAddedThisYear()

    suspend fun getAddedByMonthDistribution(): Map<String, Int> = statisticsDao.getAddedByMonthDistribution()

    suspend fun getMostCommonGroup(): String? = statisticsDao.getMostCommonGroup()

    suspend fun getMostCommonCountry(): String? = statisticsDao.getMostCommonCountry()

    suspend fun getMostFrequentComponents(): Map<String, Int> = statisticsDao.getMostFrequentComponents()

    suspend fun getAverageComponentCount(): Double? = statisticsDao.getAverageComponentCount()
}
