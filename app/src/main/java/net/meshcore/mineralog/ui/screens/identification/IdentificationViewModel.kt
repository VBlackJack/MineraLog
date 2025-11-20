package net.meshcore.mineralog.ui.screens.identification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository

/**
 * Filter state for mineral identification.
 */
data class IdentificationFilter(
    val selectedColors: Set<String> = emptySet(),
    val mohsMin: Float? = null,
    val mohsMax: Float? = null,
    val selectedStreak: String? = null,
    val selectedLuster: String? = null,
    val isMagnetic: Boolean? = null
)

/**
 * Mineral result with relevance score.
 */
data class MineralResult(
    val mineral: ReferenceMineralEntity,
    val relevanceScore: Int
)

sealed class LoadingState {
    data object Loading : LoadingState()
    data object Success : LoadingState()
    data class Error(val message: String) : LoadingState()
}

class IdentificationViewModel(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModel() {

    // All reference minerals loaded in memory
    private val _allMinerals = MutableStateFlow<List<ReferenceMineralEntity>>(emptyList())

    // Current filter state
    private val _filter = MutableStateFlow(IdentificationFilter())
    val filter: StateFlow<IdentificationFilter> = _filter.asStateFlow()

    // Loading state
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Loading)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    // Filtered results with relevance scores
    val filteredResults: StateFlow<List<MineralResult>> = combine(
        _allMinerals,
        _filter
    ) { minerals, filter ->
        if (filter.isEmpty()) {
            // No filters applied, return empty list to encourage filtering
            emptyList()
        } else {
            filterAndScoreMinerals(minerals, filter)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadAllMinerals()
    }

    private fun loadAllMinerals() {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading
            try {
                val minerals = referenceMineralRepository.getAllFlow().firstOrNull() ?: emptyList()
                _allMinerals.value = minerals
                _loadingState.value = LoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error(e.message ?: "Failed to load minerals")
            }
        }
    }

    /**
     * Filter minerals and calculate relevance scores.
     */
    private fun filterAndScoreMinerals(
        minerals: List<ReferenceMineralEntity>,
        filter: IdentificationFilter
    ): List<MineralResult> {
        android.util.Log.d("IdentificationVM", "filterAndScoreMinerals: ${minerals.size} minerals, filter=$filter")

        // French collator for proper accent-aware and case-insensitive sorting
        // PRIMARY strength ignores case and accents (É = e = E)
        val collator = java.text.Collator.getInstance(java.util.Locale.FRENCH).apply {
            strength = java.text.Collator.PRIMARY
        }

        val results = minerals.mapNotNull { mineral ->
            val score = calculateRelevanceScore(mineral, filter)
            if (score > 0) {
                MineralResult(mineral, score)
            } else {
                null
            }
        }.sortedWith(
            // Pure alphabetical sorting with French collator (ignores scores)
            Comparator { a, b -> collator.compare(a.mineral.nameFr, b.mineral.nameFr) }
        )

        android.util.Log.d("IdentificationVM", "filterAndScoreMinerals: ${results.size} results")

        // Log first 10 results to verify sorting
        android.util.Log.d("IdentificationVM", "First 10 results (score, name):")
        results.take(10).forEach { result ->
            android.util.Log.d("IdentificationVM", "  ${result.relevanceScore} - ${result.mineral.nameFr}")
        }

        return results
    }

    /**
     * Calculate relevance score based on matching criteria.
     * Higher score = better match.
     */
    private fun calculateRelevanceScore(
        mineral: ReferenceMineralEntity,
        filter: IdentificationFilter
    ): Int {
        var score = 0

        // Color matching (most important criterion, weight: 3 points per match)
        if (filter.selectedColors.isNotEmpty()) {
            val mineralColors = mineral.colors?.split(",")?.map { it.trim() } ?: emptyList()

            // FIX: If mineral has no color data (empty or blank), don't exclude it
            // This handles incomplete reference data where color = "" or null
            if (mineralColors.isEmpty() || mineralColors.all { it.isBlank() }) {
                // No penalty, but no bonus points either (missing data = neutral)
            } else {
                val hasColorMatch = filter.selectedColors.any { selectedColor ->
                    mineralColors.any { mineralColor ->
                        mineralColor.equals(selectedColor, ignoreCase = true)
                    }
                }
                if (hasColorMatch) {
                    score += 3
                } else {
                    // No color match is a deal-breaker ONLY if color data exists
                    return 0
                }
            }
        }

        // Hardness matching (weight: 2 points)
        if (filter.mohsMin != null || filter.mohsMax != null) {
            val mineralMin = mineral.mohsMin ?: 0f
            val mineralMax = mineral.mohsMax ?: 0f

            // FIX: If hardness is 0.0 (missing data), don't exclude it
            // This handles incomplete reference data where mohsMin/mohsMax = 0.0
            if (mineralMin == 0f && mineralMax == 0f) {
                // No penalty, but no bonus points either (missing data = neutral)
            } else {
                val userMin = filter.mohsMin ?: 1f
                val userMax = filter.mohsMax ?: 10f

                // Check if ranges overlap
                val overlaps = (mineralMin <= userMax && mineralMax >= userMin)
                if (overlaps) {
                    score += 2
                } else {
                    // No hardness match is a deal-breaker ONLY if hardness data exists
                    return 0
                }
            }
        }

        // Streak matching (weight: 2 points)
        if (filter.selectedStreak != null) {
            val mineralStreak = mineral.streak?.trim() ?: ""
            if (mineralStreak.equals(filter.selectedStreak, ignoreCase = true) ||
                mineralStreak.contains(filter.selectedStreak, ignoreCase = true)) {
                score += 2
            }
            // Streak mismatch is not a deal-breaker (might be unknown)
        }

        // Luster matching (weight: 1 point)
        if (filter.selectedLuster != null) {
            val mineralLuster = mineral.luster?.trim() ?: ""
            if (mineralLuster.equals(filter.selectedLuster, ignoreCase = true) ||
                mineralLuster.contains(filter.selectedLuster, ignoreCase = true)) {
                score += 1
            }
            // Luster mismatch is not a deal-breaker
        }

        // Magnetism matching (weight: 2 points)
        if (filter.isMagnetic != null) {
            val mineralMagnetism = mineral.magnetism?.lowercase() ?: ""
            val mineralNotes = mineral.notes?.lowercase() ?: ""

            val isMineralMagnetic = mineralMagnetism.contains("magnetic") ||
                    mineralMagnetism.contains("magnétique") ||
                    mineralNotes.contains("magnetic") ||
                    mineralNotes.contains("magnétique")

            if (filter.isMagnetic == isMineralMagnetic) {
                score += 2
            } else if (filter.isMagnetic == true && !isMineralMagnetic) {
                // User selected magnetic but mineral is not: deal-breaker
                return 0
            }
        }

        // FIX: If score is 0, it means the mineral passed all filters but has missing data
        // Return at least 1 to include it in results (missing data = potential match)
        return score.coerceAtLeast(1)
    }

    // Filter update functions
    fun toggleColor(color: String) {
        _filter.value = _filter.value.copy(
            selectedColors = if (_filter.value.selectedColors.contains(color)) {
                _filter.value.selectedColors - color
            } else {
                _filter.value.selectedColors + color
            }
        )
    }

    fun setHardnessRange(min: Float?, max: Float?) {
        _filter.value = _filter.value.copy(
            mohsMin = min,
            mohsMax = max
        )
    }

    fun setStreak(streak: String?) {
        _filter.value = _filter.value.copy(selectedStreak = streak)
    }

    fun setLuster(luster: String?) {
        _filter.value = _filter.value.copy(selectedLuster = luster)
    }

    fun setMagnetic(magnetic: Boolean?) {
        _filter.value = _filter.value.copy(isMagnetic = magnetic)
    }

    fun clearFilters() {
        _filter.value = IdentificationFilter()
    }

    private fun IdentificationFilter.isEmpty(): Boolean {
        return selectedColors.isEmpty() &&
                mohsMin == null &&
                mohsMax == null &&
                selectedStreak == null &&
                selectedLuster == null &&
                isMagnetic == null
    }
}

class IdentificationViewModelFactory(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdentificationViewModel::class.java)) {
            return IdentificationViewModel(referenceMineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
