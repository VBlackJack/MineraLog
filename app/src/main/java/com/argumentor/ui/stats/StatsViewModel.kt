package com.argumentor.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.domain.model.DistributionSlice
import com.argumentor.domain.repository.StatisticsRepository
import com.argumentor.domain.repository.TopicRepository
import com.argumentor.navigation.TOPIC_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class StatsUiState(
    val topicTitle: String = "",
    val stance: List<DistributionSlice> = emptyList(),
    val strength: List<DistributionSlice> = emptyList()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    statisticsRepository: StatisticsRepository,
    topicRepository: TopicRepository
) : ViewModel() {
    private val topicId: Long = savedStateHandle.get<String>(TOPIC_ID_ARG)?.toLongOrNull()
        ?: savedStateHandle.get<Long>(TOPIC_ID_ARG) ?: 0L

    private val titleFlow = topicRepository.observeTopicDetail(topicId)
        .map { it?.topic?.title ?: "" }

    private val stanceFlow = statisticsRepository.observeStanceDistribution(topicId)
    private val strengthFlow = statisticsRepository.observeStrengthDistribution(topicId)

    val uiState: StateFlow<StatsUiState> = combine(titleFlow, stanceFlow, strengthFlow) { title, stance, strength ->
        StatsUiState(topicTitle = title, stance = stance, strength = strength)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState()
    )
}
