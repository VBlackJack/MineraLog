package com.argumentor.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.Claim
import com.argumentor.domain.model.ClaimDetail
import com.argumentor.domain.model.ClaimPosition
import com.argumentor.domain.model.Evidence
import com.argumentor.domain.model.EvidenceQuality
import com.argumentor.domain.model.EvidenceType
import com.argumentor.domain.model.Question
import com.argumentor.domain.model.Rebuttal
import com.argumentor.domain.model.RebuttalStyle
import com.argumentor.domain.model.Topic
import com.argumentor.domain.model.TopicDetail
import com.argumentor.domain.model.TopicStance
import com.argumentor.domain.repository.TopicRepository
import com.argumentor.navigation.TOPIC_ID_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TopicEditorUiState(
    val topicId: Long = 0,
    val title: String = "",
    val summary: String = "",
    val stance: TopicStance = TopicStance.NEUTRAL,
    val color: Long = 0xFF0D47A1,
    val createdAt: Long = System.currentTimeMillis(),
    val claims: List<ClaimDetail> = emptyList(),
    val isExisting: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class TopicEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val topicRepository: TopicRepository
) : ViewModel() {
    private val initialId: Long = savedStateHandle.get<String>(TOPIC_ID_ARG)?.toLongOrNull()
        ?: savedStateHandle.get<Long>(TOPIC_ID_ARG) ?: 0L
    private val draft = MutableStateFlow(TopicEditorUiState(topicId = initialId, isExisting = initialId != 0L))

    private val detailFlow = if (initialId == 0L) MutableStateFlow<TopicDetail?>(null) else
        topicRepository.observeTopicDetail(initialId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val uiState: StateFlow<TopicEditorUiState> = combine(draft, detailFlow) { draftState, detail ->
        detail?.let {
            draftState.copy(
                topicId = it.topic.id,
                title = it.topic.title,
                summary = it.topic.summary,
                stance = it.topic.stance,
                color = it.topic.color,
                createdAt = it.topic.createdAt,
                claims = it.claims,
                isExisting = true,
                isSaving = false
            )
        } else {
            draftState
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        draft.value
    )

    fun updateTitle(value: String) {
        draft.value = draft.value.copy(title = value)
    }

    fun updateSummary(value: String) {
        draft.value = draft.value.copy(summary = value)
    }

    fun updateStance(stance: TopicStance) {
        draft.value = draft.value.copy(stance = stance)
    }

    fun saveTopic(onSaved: (Long) -> Unit = {}) {
        val current = draft.value
        if (current.title.isBlank()) return
        viewModelScope.launch {
            draft.value = current.copy(isSaving = true)
            val id = if (current.topicId == 0L) {
                topicRepository.createTopic(current.title, current.summary, current.stance, current.color)
            } else {
                topicRepository.updateTopic(
                    Topic(
                        id = current.topicId,
                        title = current.title,
                        summary = current.summary,
                        stance = current.stance,
                        color = current.color,
                        createdAt = current.createdAt
                    )
                )
                current.topicId
            }
            draft.value = draft.value.copy(topicId = id, isExisting = true, isSaving = false)
            onSaved(id)
        }
    }

    fun addClaim(text: String, position: ClaimPosition, strength: ArgumentStrength) {
        val topicId = uiState.value.topicId
        if (topicId == 0L) return
        viewModelScope.launch {
            topicRepository.upsertClaim(
                Claim(
                    topicId = topicId,
                    text = text,
                    position = position,
                    strength = strength
                )
            )
        }
    }

    fun addEvidence(claimId: Long, type: EvidenceType, content: String, quality: EvidenceQuality) {
        viewModelScope.launch {
            topicRepository.upsertEvidence(
                Evidence(
                    claimId = claimId,
                    type = type,
                    content = content,
                    sourceId = null,
                    quality = quality
                )
            )
        }
    }

    fun addQuestion(claimId: Long, prompt: String, answer: String) {
        viewModelScope.launch {
            topicRepository.upsertQuestion(
                Question(
                    claimId = claimId,
                    prompt = prompt,
                    expectedAnswer = answer
                )
            )
        }
    }

    fun addRebuttal(claimId: Long, text: String, style: RebuttalStyle) {
        viewModelScope.launch {
            topicRepository.upsertRebuttal(
                Rebuttal(
                    claimId = claimId,
                    text = text,
                    style = style
                )
            )
        }
    }
}
