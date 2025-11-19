package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.argumentor.data.local.entity.FallacyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FallacyDao {
    @Query("SELECT * FROM fallacies ORDER BY name ASC")
    fun observeFallacies(): Flow<List<FallacyEntity>>

    @Query("SELECT COUNT(*) FROM fallacies")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FallacyEntity>)
}
