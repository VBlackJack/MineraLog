package com.argumentor.core.i18n

import androidx.annotation.StringRes
import com.argumentor.R
import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.ClaimPosition
import com.argumentor.domain.model.EvidenceQuality
import com.argumentor.domain.model.EvidenceType
import com.argumentor.domain.model.RebuttalStyle
import com.argumentor.domain.model.TopicStance

@StringRes
fun TopicStance.labelRes(): Int = when (this) {
    TopicStance.PRO -> R.string.stance_pro
    TopicStance.CON -> R.string.stance_con
    TopicStance.NEUTRAL -> R.string.stance_neutral
}

@StringRes
fun ClaimPosition.labelRes(): Int = when (this) {
    ClaimPosition.SUPPORT -> R.string.claim_position_support
    ClaimPosition.CHALLENGE -> R.string.claim_position_challenge
}

@StringRes
fun ArgumentStrength.labelRes(): Int = when (this) {
    ArgumentStrength.WEAK -> R.string.strength_weak
    ArgumentStrength.MODERATE -> R.string.strength_moderate
    ArgumentStrength.STRONG -> R.string.strength_strong
}

@StringRes
fun EvidenceType.labelRes(): Int = when (this) {
    EvidenceType.CITATION -> R.string.evidence_type_citation
    EvidenceType.STATISTIC -> R.string.evidence_type_statistic
    EvidenceType.EXPERIMENT -> R.string.evidence_type_experiment
    EvidenceType.ANECDOTE -> R.string.evidence_type_anecdote
    EvidenceType.ANALOGY -> R.string.evidence_type_analogy
    EvidenceType.EXPERT_OPINION -> R.string.evidence_type_expert_opinion
}

@StringRes
fun EvidenceQuality.labelRes(): Int = when (this) {
    EvidenceQuality.LOW -> R.string.evidence_quality_low
    EvidenceQuality.MEDIUM -> R.string.evidence_quality_medium
    EvidenceQuality.HIGH -> R.string.evidence_quality_high
}

@StringRes
fun RebuttalStyle.labelRes(): Int = when (this) {
    RebuttalStyle.CLARIFICATION -> R.string.rebuttal_style_clarification
    RebuttalStyle.COUNTER_EXAMPLE -> R.string.rebuttal_style_counter_example
    RebuttalStyle.ALTERNATIVE_CAUSE -> R.string.rebuttal_style_alternative_cause
}

fun claimPositionLabelRes(value: String): Int? = runCatching {
    ClaimPosition.valueOf(value)
}.getOrNull()?.labelRes()

fun argumentStrengthLabelRes(value: String): Int? = runCatching {
    ArgumentStrength.valueOf(value)
}.getOrNull()?.labelRes()
