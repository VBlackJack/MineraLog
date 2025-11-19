package com.argumentor.domain.repository

import com.argumentor.domain.model.DistributionSlice
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeStanceDistribution(topicId: Long): Flow<List<DistributionSlice>>
    fun observeStrengthDistribution(topicId: Long): Flow<List<DistributionSlice>>
}
