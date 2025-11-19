package com.argumentor.domain.repository

import com.argumentor.domain.model.Fallacy
import kotlinx.coroutines.flow.Flow

interface FallacyRepository {
    fun observeFallacies(): Flow<List<Fallacy>>
}
