package com.argumentor.data.repository

import com.argumentor.data.local.dao.ClaimDao
import com.argumentor.data.local.dao.EvidenceDao
import com.argumentor.data.local.dao.QuestionDao
import com.argumentor.data.local.dao.RebuttalDao
import com.argumentor.data.local.dao.TopicDao
import com.argumentor.data.local.entity.TopicEntity
import com.argumentor.domain.model.Claim
import com.argumentor.domain.model.ClaimDetail
import com.argumentor.domain.model.Evidence
import com.argumentor.domain.model.Question
import com.argumentor.domain.model.Rebuttal
import com.argumentor.domain.model.Topic
import com.argumentor.domain.model.TopicDetail
import com.argumentor.domain.model.TopicOverview
import com.argumentor.domain.model.TopicStance
import com.argumentor.domain.repository.TopicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TopicRepositoryImpl @Inject constructor(
    private val topicDao: TopicDao,
    private val claimDao: ClaimDao,
    private val evidenceDao: EvidenceDao,
    private val questionDao: QuestionDao,
    private val rebuttalDao: RebuttalDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TopicRepository {

    override fun observeTopics(searchQuery: String): Flow<List<TopicOverview>> {
        val query = searchQuery.trim()
        val source = if (query.isEmpty()) {
            topicDao.observeTopicOverview()
        } else {
            val tokenized = query.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(separator = " ") { "$it*" }
            topicDao.searchTopicOverview(tokenized)
        }
        return source.map { list -> list.map { it.toDomain() } }.flowOn(dispatcher)
    }

    override fun observeTopicDetail(topicId: Long): Flow<TopicDetail?> {
        val topicFlow = topicDao.observeTopic(topicId).map { it?.toDomain() }
        val claimsFlow = claimDao.observeClaimsByTopic(topicId).map { list -> list.map { it.toDomain() } }
        val evidencesFlow = evidenceDao.observeEvidencesByTopic(topicId).map { list -> list.map { it.toDomain() } }
        val questionsFlow = questionDao.observeQuestionsByTopic(topicId).map { list -> list.map { it.toDomain() } }
        val rebuttalsFlow = rebuttalDao.observeRebuttalsByTopic(topicId).map { list -> list.map { it.toDomain() } }

        return combine(topicFlow, claimsFlow, evidencesFlow, questionsFlow, rebuttalsFlow) { topic, claims, evidences, questions, rebuttals ->
            topic ?: return@combine null
            val evidenceMap = evidences.groupBy(Evidence::claimId)
            val questionMap = questions.groupBy(Question::claimId)
            val rebuttalMap = rebuttals.groupBy(Rebuttal::claimId)
            TopicDetail(
                topic = topic,
                claims = claims.map { claim ->
                    ClaimDetail(
                        claim = claim,
                        evidences = evidenceMap[claim.id].orEmpty(),
                        questions = questionMap[claim.id].orEmpty(),
                        rebuttals = rebuttalMap[claim.id].orEmpty()
                    )
                }
            )
        }.flowOn(dispatcher)
    }

    override suspend fun createTopic(title: String, summary: String, stance: TopicStance, color: Long): Long =
        withContext(dispatcher) {
            val entity = TopicEntity(
                title = title,
                summary = summary,
                stance = stance,
                color = color,
                createdAt = System.currentTimeMillis()
            )
            topicDao.upsert(entity)
        }

    override suspend fun updateTopic(topic: Topic) = withContext(dispatcher) {
        topicDao.upsert(topic.toEntity())
    }

    override suspend fun deleteTopic(topicId: Long) = withContext(dispatcher) {
        topicDao.deleteById(topicId)
    }

    override suspend fun upsertClaim(claim: Claim): Long = withContext(dispatcher) {
        claimDao.upsert(claim.toEntity())
    }

    override suspend fun removeClaim(claimId: Long) = withContext(dispatcher) {
        claimDao.deleteById(claimId)
    }

    override suspend fun upsertEvidence(evidence: Evidence): Long = withContext(dispatcher) {
        evidenceDao.upsert(evidence.toEntity())
    }

    override suspend fun removeEvidence(evidenceId: Long) = withContext(dispatcher) {
        evidenceDao.deleteById(evidenceId)
    }

    override suspend fun upsertQuestion(question: Question): Long = withContext(dispatcher) {
        questionDao.upsert(question.toEntity())
    }

    override suspend fun removeQuestion(questionId: Long) = withContext(dispatcher) {
        questionDao.deleteById(questionId)
    }

    override suspend fun upsertRebuttal(rebuttal: Rebuttal): Long = withContext(dispatcher) {
        rebuttalDao.upsert(rebuttal.toEntity())
    }

    override suspend fun removeRebuttal(rebuttalId: Long) = withContext(dispatcher) {
        rebuttalDao.deleteById(rebuttalId)
    }
}
