package com.argumentor.domain.model

data class Topic(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val stance: TopicStance,
    val color: Long,
    val createdAt: Long
)

data class TopicOverview(
    val topic: Topic,
    val claimCount: Int,
    val supportCount: Int,
    val challengeCount: Int
)

data class Claim(
    val id: Long = 0,
    val topicId: Long,
    val text: String,
    val position: ClaimPosition,
    val strength: ArgumentStrength
)

data class Evidence(
    val id: Long = 0,
    val claimId: Long,
    val type: EvidenceType,
    val content: String,
    val sourceId: Long?,
    val quality: EvidenceQuality
)

data class Source(
    val id: Long = 0,
    val title: String,
    val author: String,
    val url: String,
    val year: Int
)

data class Question(
    val id: Long = 0,
    val claimId: Long,
    val prompt: String,
    val expectedAnswer: String
)

data class Rebuttal(
    val id: Long = 0,
    val claimId: Long,
    val text: String,
    val style: RebuttalStyle
)

data class ClaimDetail(
    val claim: Claim,
    val evidences: List<Evidence>,
    val questions: List<Question>,
    val rebuttals: List<Rebuttal>
)

data class TopicDetail(
    val topic: Topic,
    val claims: List<ClaimDetail>
)

data class DistributionSlice(
    val label: String,
    val count: Int
)

data class Flashcard(
    val claim: Claim,
    val rebuttals: List<Rebuttal>,
    val questions: List<Question>
)

data class Fallacy(
    val id: Long = 0,
    val name: String,
    val description: String,
    val example: String
)
