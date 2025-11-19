package com.argumentor.ui.fallacies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.domain.model.Fallacy
import com.argumentor.domain.repository.FallacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


data class FallacyUiState(
    val items: List<Fallacy> = emptyList()
)

@HiltViewModel
class FallacyViewModel @Inject constructor(
    fallacyRepository: FallacyRepository
) : ViewModel() {
    val uiState: StateFlow<FallacyUiState> = fallacyRepository.observeFallacies()
        .map { FallacyUiState(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            FallacyUiState()
        )
}
