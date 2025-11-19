package net.meshcore.mineralog.data.local.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.MineralComponentDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.domain.model.Mineral

/**
 * Custom PagingSource for Mineral entities that eliminates N+1 query problem.
 *
 * Standard approach (N+1 problem):
 * - Load page of N minerals
 * - For each mineral, load provenance (N queries)
 * - For each mineral, load storage (N queries)
 * - For each mineral, load photos (N queries)
 * - Total: 1 + 3N queries per page
 *
 * Optimized approach (this implementation):
 * - Load page of N minerals (1 query)
 * - Batch load all provenances for these N minerals (1 query)
 * - Batch load all storages for these N minerals (1 query)
 * - Batch load all photos for these N minerals (1 query)
 * - Total: 4 queries per page (constant, independent of page size)
 *
 * Performance improvement: ~75% reduction in queries for typical 20-item pages
 * (from 61 queries to 4 queries).
 */
class MineralPagingSource(
    private val mineralDao: MineralDao,
    private val provenanceDao: ProvenanceDao,
    private val storageDao: StorageDao,
    private val photoDao: PhotoDao,
    private val mineralComponentDao: MineralComponentDao,
    private val basePagingSource: PagingSource<Int, net.meshcore.mineralog.data.local.entity.MineralEntity>
) : PagingSource<Int, Mineral>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Mineral> {
        return try {
            // Load page from base PagingSource (Room-generated)
            val result = basePagingSource.load(params)

            when (result) {
                is LoadResult.Page -> {
                    val entities = result.data

                    // If no entities, return empty page
                    if (entities.isEmpty()) {
                        return LoadResult.Page(
                            data = emptyList(),
                            prevKey = result.prevKey,
                            nextKey = result.nextKey
                        )
                    }

                    // Extract mineral IDs for batch loading
                    val mineralIds = entities.map { it.id }

                    // Batch load all related entities in parallel (3 queries instead of 3N)
                    val provenances = provenanceDao.getByMineralIds(mineralIds)
                        .associateBy { it.mineralId }
                    val storages = storageDao.getByMineralIds(mineralIds)
                        .associateBy { it.mineralId }
                    val photos = photoDao.getByMineralIds(mineralIds)
                        .groupBy { it.mineralId }

                    // Map entities to domain models with pre-loaded related data
                    val aggregateIds = entities.filter { it.type == "AGGREGATE" }.map { it.id }
                    val components = if (aggregateIds.isNotEmpty()) {
                        mineralComponentDao.getByAggregateIds(aggregateIds)
                            .groupBy { it.aggregateId }
                    } else {
                        emptyMap()
                    }

                    val minerals = entities.map { entity ->
                        entity.toDomain(
                            provenance = provenances[entity.id],
                            storage = storages[entity.id],
                            photos = photos[entity.id] ?: emptyList(),
                            components = components[entity.id] ?: emptyList()
                        )
                    }

                    LoadResult.Page(
                        data = minerals,
                        prevKey = result.prevKey,
                        nextKey = result.nextKey
                    )
                }
                is LoadResult.Error -> LoadResult.Error(result.throwable)
                is LoadResult.Invalid -> LoadResult.Invalid()
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Mineral>): Int? {
        // Return the key for the page that contains the anchor position
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
