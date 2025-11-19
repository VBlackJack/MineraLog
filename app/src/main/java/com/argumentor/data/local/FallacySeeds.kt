package com.argumentor.data.local

import android.content.Context
import androidx.annotation.StringRes
import com.argumentor.R
import com.argumentor.data.local.entity.FallacyEntity

data class FallacySeed(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val exampleRes: Int
)

object FallacySeeds {
    private val seeds = listOf(
        FallacySeed(R.string.fallacy_ad_hominem_name, R.string.fallacy_ad_hominem_description, R.string.fallacy_ad_hominem_example),
        FallacySeed(R.string.fallacy_appeal_authority_name, R.string.fallacy_appeal_authority_description, R.string.fallacy_appeal_authority_example),
        FallacySeed(R.string.fallacy_appeal_popularity_name, R.string.fallacy_appeal_popularity_description, R.string.fallacy_appeal_popularity_example),
        FallacySeed(R.string.fallacy_appeal_emotion_name, R.string.fallacy_appeal_emotion_description, R.string.fallacy_appeal_emotion_example),
        FallacySeed(R.string.fallacy_straw_man_name, R.string.fallacy_straw_man_description, R.string.fallacy_straw_man_example),
        FallacySeed(R.string.fallacy_slippery_slope_name, R.string.fallacy_slippery_slope_description, R.string.fallacy_slippery_slope_example),
        FallacySeed(R.string.fallacy_false_dilemma_name, R.string.fallacy_false_dilemma_description, R.string.fallacy_false_dilemma_example),
        FallacySeed(R.string.fallacy_circular_reasoning_name, R.string.fallacy_circular_reasoning_description, R.string.fallacy_circular_reasoning_example),
        FallacySeed(R.string.fallacy_hasty_generalization_name, R.string.fallacy_hasty_generalization_description, R.string.fallacy_hasty_generalization_example),
        FallacySeed(R.string.fallacy_red_herring_name, R.string.fallacy_red_herring_description, R.string.fallacy_red_herring_example),
        FallacySeed(R.string.fallacy_post_hoc_name, R.string.fallacy_post_hoc_description, R.string.fallacy_post_hoc_example),
        FallacySeed(R.string.fallacy_false_cause_name, R.string.fallacy_false_cause_description, R.string.fallacy_false_cause_example),
        FallacySeed(R.string.fallacy_appeal_ignorance_name, R.string.fallacy_appeal_ignorance_description, R.string.fallacy_appeal_ignorance_example),
        FallacySeed(R.string.fallacy_cherry_picking_name, R.string.fallacy_cherry_picking_description, R.string.fallacy_cherry_picking_example),
        FallacySeed(R.string.fallacy_equivocation_name, R.string.fallacy_equivocation_description, R.string.fallacy_equivocation_example),
        FallacySeed(R.string.fallacy_false_analogy_name, R.string.fallacy_false_analogy_description, R.string.fallacy_false_analogy_example),
        FallacySeed(R.string.fallacy_no_true_scotsman_name, R.string.fallacy_no_true_scotsman_description, R.string.fallacy_no_true_scotsman_example),
        FallacySeed(R.string.fallacy_gamblers_name, R.string.fallacy_gamblers_description, R.string.fallacy_gamblers_example),
        FallacySeed(R.string.fallacy_tu_quoque_name, R.string.fallacy_tu_quoque_description, R.string.fallacy_tu_quoque_example),
        FallacySeed(R.string.fallacy_appeal_tradition_name, R.string.fallacy_appeal_tradition_description, R.string.fallacy_appeal_tradition_example),
        FallacySeed(R.string.fallacy_appeal_nature_name, R.string.fallacy_appeal_nature_description, R.string.fallacy_appeal_nature_example),
        FallacySeed(R.string.fallacy_middle_ground_name, R.string.fallacy_middle_ground_description, R.string.fallacy_middle_ground_example),
        FallacySeed(R.string.fallacy_loaded_question_name, R.string.fallacy_loaded_question_description, R.string.fallacy_loaded_question_example),
        FallacySeed(R.string.fallacy_composition_name, R.string.fallacy_composition_description, R.string.fallacy_composition_example),
        FallacySeed(R.string.fallacy_division_name, R.string.fallacy_division_description, R.string.fallacy_division_example),
        FallacySeed(R.string.fallacy_appeal_fear_name, R.string.fallacy_appeal_fear_description, R.string.fallacy_appeal_fear_example),
        FallacySeed(R.string.fallacy_appeal_pity_name, R.string.fallacy_appeal_pity_description, R.string.fallacy_appeal_pity_example),
        FallacySeed(R.string.fallacy_sunk_cost_name, R.string.fallacy_sunk_cost_description, R.string.fallacy_sunk_cost_example),
        FallacySeed(R.string.fallacy_false_balance_name, R.string.fallacy_false_balance_description, R.string.fallacy_false_balance_example),
        FallacySeed(R.string.fallacy_personal_incredulity_name, R.string.fallacy_personal_incredulity_description, R.string.fallacy_personal_incredulity_example),
        FallacySeed(R.string.fallacy_appeal_novelty_name, R.string.fallacy_appeal_novelty_description, R.string.fallacy_appeal_novelty_example)
    )

    fun build(context: Context): List<FallacyEntity> = seeds.map {
        FallacyEntity(
            name = context.getString(it.nameRes),
            description = context.getString(it.descriptionRes),
            example = context.getString(it.exampleRes)
        )
    }
}
