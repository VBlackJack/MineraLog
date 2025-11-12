package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable

/**
 * Lifecycle status of a mineral specimen in the collection.
 *
 * v1.1.0: Added for tracking specimen lifecycle and display status.
 * Default for legacy imports: IN_COLLECTION
 */
@Serializable
enum class MineralStatus(val value: String) {
    /**
     * Specimen is in the collection, stored away.
     * Default status for newly added or imported minerals.
     */
    IN_COLLECTION("in_collection"),

    /**
     * Specimen is currently on display in a showcase, exhibition, or visible location.
     * StatusDetails may include displayLocation field.
     */
    ON_DISPLAY("on_display"),

    /**
     * Specimen has been loaned to another person, institution, or exhibition.
     * StatusDetails should include loanedTo, loanedDate, and expectedReturn fields.
     */
    LOANED("loaned"),

    /**
     * Specimen needs restoration, cleaning, or repair work.
     * StatusDetails may include restorationNotes field.
     */
    NEEDS_RESTORATION("needs_restoration"),

    /**
     * Specimen is marked for sale or trade.
     * StatusDetails may include askingPrice field.
     */
    FOR_SALE("for_sale");

    companion object {
        /**
         * Parse from string value, defaulting to IN_COLLECTION if unknown.
         * Ensures backward compatibility with v1.0.0 data.
         */
        fun fromValue(value: String): MineralStatus {
            return entries.firstOrNull { it.value == value } ?: IN_COLLECTION
        }
    }
}

/**
 * Extended status details for minerals.
 * Stored as JSON in statusDetails field for flexibility.
 *
 * Fields are nullable to support various status types.
 */
@Serializable
data class MineralStatusDetails(
    // For ON_DISPLAY status
    val displayLocation: String? = null,
    val displaySince: String? = null, // ISO-8601 date

    // For LOANED status
    val loanedTo: String? = null,
    val loanedDate: String? = null, // ISO-8601 date
    val expectedReturn: String? = null, // ISO-8601 date
    val loanAgreementNotes: String? = null,

    // For NEEDS_RESTORATION status
    val restorationNotes: String? = null,
    val restorationPriority: Int? = null, // 1-5, 5 being highest

    // For FOR_SALE status
    val askingPrice: Double? = null,
    val currency: String? = null,
    val listedDate: String? = null, // ISO-8601 date
    val saleNotes: String? = null
)
