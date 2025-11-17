package net.meshcore.mineralog.domain.sorting

import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.ui.screens.home.SortOption

/**
 * Strategy Pattern implementation for mineral sorting.
 *
 * Eliminates code duplication of sorting logic across MineralRepository.
 * Previously duplicated 3 times (getAllFlow, searchFlow, filterAdvancedFlow).
 *
 * Sprint 2: Architecture Refactoring - Open/Closed Principle (OCP)
 * Target: Remove 70+ lines of duplicated sorting logic
 *
 * @see net.meshcore.mineralog.data.repository.MineralRepository
 */
object MineralSortStrategy {

    /**
     * Sort a list of minerals according to the specified sort option.
     *
     * @param minerals The list of minerals to sort
     * @param sortOption The sort option to apply
     * @return Sorted list of minerals
     */
    fun sort(minerals: List<Mineral>, sortOption: SortOption): List<Mineral> {
        return when (sortOption) {
            SortOption.NAME_ASC -> minerals.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> minerals.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> minerals.sortedByDescending { it.updatedAt }
            SortOption.DATE_OLDEST -> minerals.sortedBy { it.updatedAt }
            SortOption.GROUP -> minerals.sortedWith(
                compareBy({ it.group }, { it.name.lowercase() })
            )
            SortOption.HARDNESS_LOW -> minerals.sortedWith(
                compareBy({ it.mohsMin }, { it.name.lowercase() })
            )
            SortOption.HARDNESS_HIGH -> minerals.sortedWith(
                compareByDescending<Mineral> { it.mohsMax }.thenBy { it.name.lowercase() }
            )
        }
    }

    /**
     * Get a comparator for the specified sort option.
     * Useful when sorting needs to be performed multiple times with the same option.
     *
     * @param sortOption The sort option to create a comparator for
     * @return Comparator for the specified sort option
     */
    fun comparator(sortOption: SortOption): Comparator<Mineral> {
        return when (sortOption) {
            SortOption.NAME_ASC -> compareBy { it.name.lowercase() }
            SortOption.NAME_DESC -> compareByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> compareByDescending { it.updatedAt }
            SortOption.DATE_OLDEST -> compareBy { it.updatedAt }
            SortOption.GROUP -> compareBy<Mineral> { it.group }.thenBy { it.name.lowercase() }
            SortOption.HARDNESS_LOW -> compareBy<Mineral> { it.mohsMin }.thenBy { it.name.lowercase() }
            SortOption.HARDNESS_HIGH -> compareByDescending<Mineral> { it.mohsMax }.thenBy { it.name.lowercase() }
        }
    }
}
