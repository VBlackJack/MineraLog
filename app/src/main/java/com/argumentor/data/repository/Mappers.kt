package com.argumentor.data.repository

import com.argumentor.data.local.dao.TopicOverviewProjection
import com.argumentor.data.local.entity.ClaimEntity
import com.argumentor.data.local.entity.EvidenceEntity
import com.argumentor.data.local.entity.FallacyEntity
import com.argumentor.data.local.entity.QuestionEntity
import com.argumentor.data.local.entity.RebuttalEntity
import com.argumentor.data.local.entity.TopicEntity
import com.argumentor.domain.model.Claim
import com.argumentor.domain.model.Evidence
import com.argumentor.domain.model.Fallacy
import com.argumentor.domain.model.Question
import com.argumentor.domain.model.Rebuttal
import com.argumentor.domain.model.Topic
import com.argumentor.domain.model.TopicOverview
import com.argumentor.domain.model.TopicStance

fun TopicEntity.toDomain() = Topic(
    id = id,
    title = title,
    summary = summary,
    stance = stance,
    color = color,
    createdAt = createdAt
)

fun Topic.toEntity() = TopicEntity(
    id = id,
    title = title,
    summary = summary,
    stance = stance,
    color = color,
    createdAt = createdAt
)

fun TopicOverviewProjection.toDomain(): TopicOverview = TopicOverview(
    topic = Topic(
        id = id,
        title = title,
        summary = summary,
        stance = TopicStance.valueOf(stance),
        color = color,
        createdAt = createdAt
    ),
    claimCount = claimCount,
    supportCount = supportCount ?: 0,
    challengeCount = challengeCount ?: 0
)

fun ClaimEntity.toDomain() = Claim(
    id = id,
    topicId = topicId,
    text = text,
    position = position,
    strength = strength
)

fun Claim.toEntity() = ClaimEntity(
    id = id,
    topicId = topicId,
    text = text,
    position = position,
    strength = strength
)

fun EvidenceEntity.toDomain() = Evidence(
    id = id,
    claimId = claimId,
    type = type,
    content = content,
    sourceId = sourceId,
    quality = quality
)

fun Evidence.toEntity() = EvidenceEntity(
    id = id,
    claimId = claimId,
    type = type,
    content = content,
    sourceId = sourceId,
    quality = quality
)

fun QuestionEntity.toDomain() = Question(
    id = id,
    claimId = claimId,
    prompt = prompt,
    expectedAnswer = expectedAnswer
)

fun Question.toEntity() = QuestionEntity(
    id = id,
    claimId = claimId,
    prompt = prompt,
    expectedAnswer = expectedAnswer
)

fun RebuttalEntity.toDomain() = Rebuttal(
    id = id,
    claimId = claimId,
    text = text,
    style = style
)

fun Rebuttal.toEntity() = RebuttalEntity(
    id = id,
    claimId = claimId,
    text = text,
    style = style
)

fun FallacyEntity.toDomain() = Fallacy(
    id = id,
    name = name,
    description = description,
    example = example
)
