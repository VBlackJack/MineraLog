package com.argumentor.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseDump(
    val topics: List<TopicPayload>,
    val claims: List<ClaimPayload>,
    val evidences: List<EvidencePayload>,
    val sources: List<SourcePayload>,
    val questions: List<QuestionPayload>,
    val rebuttals: List<RebuttalPayload>
)

@Serializable
data class TopicPayload(
    val id: Long,
    val title: String,
    val summary: String,
    val stance: String,
    val color: Long,
    val createdAt: Long
)

@Serializable
data class ClaimPayload(
    val id: Long,
    val topicId: Long,
    val text: String,
    val position: String,
    val strength: String
)

@Serializable
data class EvidencePayload(
    val id: Long,
    val claimId: Long,
    val type: String,
    val content: String,
    val sourceId: Long?,
    val quality: String
)

@Serializable
data class SourcePayload(
    val id: Long,
    val title: String,
    val author: String,
    val url: String,
    val year: Int
)

@Serializable
data class QuestionPayload(
    val id: Long,
    val claimId: Long,
    val prompt: String,
    val expectedAnswer: String
)

@Serializable
data class RebuttalPayload(
    val id: Long,
    val claimId: Long,
    val text: String,
    val style: String
)
