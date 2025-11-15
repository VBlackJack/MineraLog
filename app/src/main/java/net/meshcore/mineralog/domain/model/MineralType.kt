package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain enum representing the type of a mineral entry.
 */
@Serializable
enum class MineralType {
    /**
     * Single mineral with uniform properties.
     * Example: Quartz (SiO₂), Pyrite (FeS₂), Amethyst
     */
    SIMPLE,

    /**
     * Mineral aggregate or rock composed of multiple minerals.
     * Example: Granite (Quartz + Feldspath + Mica), Gneiss, Basalt
     */
    AGGREGATE,

    /**
     * Reserved for future extension to distinguish between aggregates and true rocks.
     * Not currently used in v2.0.
     */
    ROCK;

    companion object {
        /**
         * Convert from entity layer string representation.
         */
        fun fromString(value: String): MineralType {
            return valueOf(value.uppercase())
        }
    }
}
