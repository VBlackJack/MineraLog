package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query(
        """
        SELECT position AS label, COUNT(*) AS value
        FROM claims
        WHERE topicId = :topicId
        GROUP BY position
        """
    )
    fun observeStanceDistribution(topicId: Long): Flow<List<DistributionProjection>>

    @Query(
        """
        SELECT strength AS label, COUNT(*) AS value
        FROM claims
        WHERE topicId = :topicId
        GROUP BY strength
        """
    )
    fun observeStrengthDistribution(topicId: Long): Flow<List<DistributionProjection>>
}

data class DistributionProjection(
    val label: String,
    val value: Int
)
