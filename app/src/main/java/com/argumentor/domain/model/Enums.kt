package com.argumentor.domain.model

enum class TopicStance {
    PRO,
    CON,
    NEUTRAL
}

enum class ClaimPosition {
    SUPPORT,
    CHALLENGE
}

enum class ArgumentStrength {
    WEAK,
    MODERATE,
    STRONG
}

enum class EvidenceType {
    CITATION,
    STATISTIC,
    EXPERIMENT,
    ANECDOTE,
    ANALOGY,
    EXPERT_OPINION
}

enum class EvidenceQuality {
    LOW,
    MEDIUM,
    HIGH
}

enum class RebuttalStyle {
    CLARIFICATION,
    COUNTER_EXAMPLE,
    ALTERNATIVE_CAUSE
}
