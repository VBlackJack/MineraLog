package net.meshcore.mineralog.ui.components

import android.content.Context
import net.meshcore.mineralog.R

/**
 * Quick Win #3: Predefined values for technical mineral fields
 * Reduces data entry errors and ensures consistency
 * Values are now loaded from string resources for internationalization
 */
object MineralFieldValues {

    fun getCrystalSystems(context: Context): List<String> {
        return context.resources.getStringArray(R.array.crystal_systems).toList()
    }

    fun getLusterTypes(context: Context): List<String> {
        return context.resources.getStringArray(R.array.luster_types).toList()
    }

    fun getDiaphaneityTypes(context: Context): List<String> {
        return context.resources.getStringArray(R.array.diaphaneity_types).toList()
    }

    fun getCleavageTypes(context: Context): List<String> {
        return context.resources.getStringArray(R.array.cleavage_types).toList()
    }

    fun getFractureTypes(context: Context): List<String> {
        return context.resources.getStringArray(R.array.fracture_types).toList()
    }

    fun getHabitTypes(context: Context): List<String> {
        return context.resources.getStringArray(R.array.habit_types).toList()
    }

    fun getStreakColors(context: Context): List<String> {
        return context.resources.getStringArray(R.array.streak_colors).toList()
    }

    // Legacy properties for backward compatibility (deprecated)
    @Deprecated("Use getCrystalSystems(context) instead", ReplaceWith("getCrystalSystems(context)"))
    val CRYSTAL_SYSTEMS = listOf(
        "Cubic", "Hexagonal", "Tetragonal", "Orthorhombic", "Monoclinic", "Triclinic", "Trigonal", "Other"
    )

    @Deprecated("Use getLusterTypes(context) instead", ReplaceWith("getLusterTypes(context)"))
    val LUSTER_TYPES = listOf(
        "Metallic", "Vitreous (Glassy)", "Pearly", "Silky", "Resinous", "Adamantine", "Greasy", "Dull/Earthy", "Other"
    )

    @Deprecated("Use getDiaphaneityTypes(context) instead", ReplaceWith("getDiaphaneityTypes(context)"))
    val DIAPHANEITY_TYPES = listOf(
        "Transparent", "Translucent", "Opaque", "Other"
    )

    @Deprecated("Use getCleavageTypes(context) instead", ReplaceWith("getCleavageTypes(context)"))
    val CLEAVAGE_TYPES = listOf(
        "Perfect", "Good", "Distinct", "Poor", "None", "Other"
    )

    @Deprecated("Use getFractureTypes(context) instead", ReplaceWith("getFractureTypes(context)"))
    val FRACTURE_TYPES = listOf(
        "Conchoidal", "Uneven", "Splintery", "Hackly", "Fibrous", "Brittle", "Other"
    )

    @Deprecated("Use getHabitTypes(context) instead", ReplaceWith("getHabitTypes(context)"))
    val HABIT_TYPES = listOf(
        "Prismatic", "Tabular", "Massive", "Granular", "Fibrous", "Acicular (Needle-like)", "Bladed", "Dendritic", "Botryoidal", "Stalactitic", "Cubic", "Octahedral", "Dodecahedral", "Other"
    )

    @Deprecated("Use getStreakColors(context) instead", ReplaceWith("getStreakColors(context)"))
    val STREAK_COLORS = listOf(
        "White", "Black", "Gray", "Red", "Brown", "Yellow", "Green", "Blue", "Colorless", "Other"
    )
}
