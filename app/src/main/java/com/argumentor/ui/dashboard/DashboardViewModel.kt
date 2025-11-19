package com.argumentor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.domain.model.TopicOverview
import com.argumentor.domain.model.TopicStance
import com.argumentor.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val palette = listOf(
    0xFF0D47A1,
    0xFF00897B,
    0xFFFF7043,
    0xFF6D4C41
)

data class DashboardUiState(
    val searchQuery: String = "",
    val topics: List<TopicOverview> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val topicRepository: TopicRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _events = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val events: SharedFlow<Long> = _events

    val uiState: StateFlow<DashboardUiState> = query
        .flatMapLatest { topicRepository.observeTopics(it) }
        .map { DashboardUiState(searchQuery = query.value, topics = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    fun onSearchChange(value: String) {
        query.value = value
    }

    fun quickCreateTopic(title: String, summary: String, stance: TopicStance) {
        viewModelScope.launch {
            val color = palette.random()
            val topicId = topicRepository.createTopic(title, summary, stance, color)
            _events.emit(topicId)
        }
    }
}
