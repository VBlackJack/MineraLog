package com.argumentor.ui.debate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.domain.model.Flashcard
import com.argumentor.domain.repository.TopicRepository
import com.argumentor.navigation.TOPIC_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class DebateUiState(
    val topicTitle: String = "",
    val cards: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0
)

@HiltViewModel
class DebateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    topicRepository: TopicRepository
) : ViewModel() {
    private val topicId: Long = savedStateHandle.get<String>(TOPIC_ID_ARG)?.toLongOrNull()
        ?: savedStateHandle.get<Long>(TOPIC_ID_ARG) ?: 0L
    private val index = MutableStateFlow(0)

    private val detailFlow = topicRepository.observeTopicDetail(topicId)
        .map { detail ->
            detail?.let {
                DebateUiState(
                    topicTitle = it.topic.title,
                    cards = it.claims.map { claim ->
                        Flashcard(
                            claim = claim.claim,
                            rebuttals = claim.rebuttals,
                            questions = claim.questions
                        )
                    }
                )
            } ?: DebateUiState()
        }

    val uiState: StateFlow<DebateUiState> = combine(detailFlow, index) { state, currentIndex ->
        state.copy(currentIndex = if (state.cards.isEmpty()) 0 else currentIndex % state.cards.size)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DebateUiState()
    )

    fun advance() {
        val state = uiState.value
        if (state.cards.isEmpty()) return
        index.value = (index.value + 1) % state.cards.size
    }
}
