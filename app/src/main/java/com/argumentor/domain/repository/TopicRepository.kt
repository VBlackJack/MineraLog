package com.argumentor.domain.repository

import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.Claim
import com.argumentor.domain.model.Evidence
import com.argumentor.domain.model.Question
import com.argumentor.domain.model.Rebuttal
import com.argumentor.domain.model.Topic
import com.argumentor.domain.model.TopicDetail
import com.argumentor.domain.model.TopicOverview
import com.argumentor.domain.model.TopicStance
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    fun observeTopics(searchQuery: String): Flow<List<TopicOverview>>
    fun observeTopicDetail(topicId: Long): Flow<TopicDetail?>
    suspend fun createTopic(
        title: String,
        summary: String,
        stance: TopicStance,
        color: Long
    ): Long

    suspend fun updateTopic(topic: Topic)
    suspend fun deleteTopic(topicId: Long)

    suspend fun upsertClaim(claim: Claim): Long
    suspend fun removeClaim(claimId: Long)

    suspend fun upsertEvidence(evidence: Evidence): Long
    suspend fun removeEvidence(evidenceId: Long)

    suspend fun upsertQuestion(question: Question): Long
    suspend fun removeQuestion(questionId: Long)

    suspend fun upsertRebuttal(rebuttal: Rebuttal): Long
    suspend fun removeRebuttal(rebuttalId: Long)
}
