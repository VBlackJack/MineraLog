package com.argumentor.data.repository

import com.argumentor.data.local.dao.FallacyDao
import com.argumentor.domain.model.Fallacy
import com.argumentor.domain.repository.FallacyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FallacyRepositoryImpl @Inject constructor(
    private val fallacyDao: FallacyDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FallacyRepository {
    override fun observeFallacies(): Flow<List<Fallacy>> =
        fallacyDao.observeFallacies()
            .map { fallacies -> fallacies.map { it.toDomain() } }
            .flowOn(dispatcher)
}
