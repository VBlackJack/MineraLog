package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain enum representing the role/importance of a mineral component in an aggregate.
 *
 * This classification helps understand the composition and structure of mineral aggregates
 * by categorizing components based on their volumetric or mass percentage.
 */
@Serializable
enum class ComponentRole {
    /**
     * Principal component (> 20% of the aggregate).
     * These are the major minerals that define the aggregate's primary composition.
     */
    PRINCIPAL,

    /**
     * Accessory component (5-20% of the aggregate).
     * Secondary minerals that contribute significantly but are not dominant.
     */
    ACCESSORY,

    /**
     * Trace component (< 5% of the aggregate).
     * Minor minerals present in small amounts.
     */
    TRACE;

    companion object {
        /**
         * Convert from entity layer string representation.
         */
        fun fromString(value: String): ComponentRole {
            return valueOf(value.uppercase())
        }

        /**
         * Determine role based on percentage.
         */
        fun fromPercentage(percentage: Float?): ComponentRole {
            return when {
                percentage == null -> TRACE
                percentage > 20f -> PRINCIPAL
                percentage >= 5f -> ACCESSORY
                else -> TRACE
            }
        }
    }
}
