package net.meshcore.mineralog.data.local.entity

/**
 * Enum representing the type of a mineral entry.
 *
 * @property SIMPLE A single mineral specimen with homogeneous properties (e.g., Quartz, Pyrite).
 * @property AGGREGATE A mineral aggregate or rock composed of multiple distinct minerals
 *                     (e.g., Granite, Gneiss). Aggregate minerals have components instead of
 *                     simple properties.
 * @property ROCK Reserved for future use to distinguish between mineral aggregates and
 *                true rock specimens.
 */
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
    ROCK
}
