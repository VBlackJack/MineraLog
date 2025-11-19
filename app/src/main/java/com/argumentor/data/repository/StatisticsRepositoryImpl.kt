package com.argumentor.data.repository

import com.argumentor.data.local.dao.StatisticsDao
import com.argumentor.domain.model.DistributionSlice
import com.argumentor.domain.repository.StatisticsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StatisticsRepository {
    override fun observeStanceDistribution(topicId: Long): Flow<List<DistributionSlice>> =
        statisticsDao.observeStanceDistribution(topicId)
            .map { rows -> rows.map { DistributionSlice(label = it.label, count = it.value) } }
            .flowOn(dispatcher)

    override fun observeStrengthDistribution(topicId: Long): Flow<List<DistributionSlice>> =
        statisticsDao.observeStrengthDistribution(topicId)
            .map { rows -> rows.map { DistributionSlice(label = it.label, count = it.value) } }
            .flowOn(dispatcher)
}
