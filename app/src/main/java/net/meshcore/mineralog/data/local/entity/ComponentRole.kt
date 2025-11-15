package net.meshcore.mineralog.data.local.entity

/**
 * Enum representing the role/importance of a mineral component in an aggregate.
 *
 * This classification helps understand the composition and structure of mineral aggregates
 * by categorizing components based on their volumetric or mass percentage.
 *
 * @property PRINCIPAL Major component constituting more than 20% of the aggregate.
 *                     Example: Quartz (35%) in Granite.
 * @property ACCESSORY Secondary component constituting between 5% and 20% of the aggregate.
 *                     Example: Mica (15%) in Granite.
 * @property TRACE Minor component constituting less than 5% of the aggregate.
 *                Example: Biotite (3%) in Granite.
 */
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
    TRACE
}
