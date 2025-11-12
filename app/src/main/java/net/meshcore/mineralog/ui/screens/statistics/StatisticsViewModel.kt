package net.meshcore.mineralog.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.model.CollectionStatistics
import net.meshcore.mineralog.data.repository.StatisticsRepository

/**
 * ViewModel for Statistics screen.
 * Manages collection statistics computation and caching.
 */
class StatisticsViewModel(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading
            try {
                val stats = statisticsRepository.getStatistics()
                _uiState.value = StatisticsUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshStatistics() {
        viewModelScope.launch {
            try {
                val stats = statisticsRepository.refreshStatistics()
                _uiState.value = StatisticsUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI state for Statistics screen.
 */
sealed class StatisticsUiState {
    data object Loading : StatisticsUiState()
    data class Success(val statistics: CollectionStatistics) : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}
