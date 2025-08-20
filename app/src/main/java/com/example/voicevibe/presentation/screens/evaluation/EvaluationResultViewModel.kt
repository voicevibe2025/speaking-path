package com.example.voicevibe.presentation.screens.evaluation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.SpeakingPracticeRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.SpeakingEvaluation
import com.example.voicevibe.domain.model.SpeakingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Evaluation Results screen
 */
@HiltViewModel
class EvaluationResultViewModel @Inject constructor(
    private val repository: SpeakingPracticeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = savedStateHandle.get<String>("sessionId") ?: ""

    private val _uiState = MutableStateFlow(EvaluationResultUiState())
    val uiState: StateFlow<EvaluationResultUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EvaluationResultEvent>()
    val events: SharedFlow<EvaluationResultEvent> = _events.asSharedFlow()

    init {
        loadEvaluationResult()
    }

    private fun loadEvaluationResult() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load session data
            val sessionResult = repository.getSession(sessionId)
            when (sessionResult) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(session = sessionResult.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = sessionResult.message
                        )
                    }
                    return@launch
                }
                else -> {}
            }

            // Load evaluation data
            val evaluationResult = repository.getEvaluation(sessionId)
            when (evaluationResult) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            evaluation = evaluationResult.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = evaluationResult.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun retryLoading() {
        loadEvaluationResult()
    }

    fun shareResults() {
        viewModelScope.launch {
            _uiState.value.evaluation?.let { evaluation ->
                _events.emit(
                    EvaluationResultEvent.ShareResults(
                        score = evaluation.overallScore,
                        feedback = evaluation.feedback
                    )
                )
            }
        }
    }

    fun practiceSimilar() {
        viewModelScope.launch {
            _events.emit(EvaluationResultEvent.NavigateToPractice)
        }
    }

    fun viewDetailedFeedback(category: String) {
        viewModelScope.launch {
            _events.emit(EvaluationResultEvent.ShowDetailedFeedback(category))
        }
    }

    fun saveToHistory() {
        // Session is automatically saved
        viewModelScope.launch {
            _events.emit(EvaluationResultEvent.ShowMessage("Results saved to history"))
        }
    }

    fun reportIssue() {
        viewModelScope.launch {
            _events.emit(EvaluationResultEvent.ShowReportDialog)
        }
    }
}

/**
 * UI State for Evaluation Results
 */
data class EvaluationResultUiState(
    val isLoading: Boolean = false,
    val session: SpeakingSession? = null,
    val evaluation: SpeakingEvaluation? = null,
    val error: String? = null,
    val isExpanded: Map<String, Boolean> = emptyMap() // For expandable sections
)

/**
 * Events from Evaluation Result screen
 */
sealed class EvaluationResultEvent {
    object NavigateToPractice : EvaluationResultEvent()
    data class ShareResults(val score: Float, val feedback: String) : EvaluationResultEvent()
    data class ShowDetailedFeedback(val category: String) : EvaluationResultEvent()
    data class ShowMessage(val message: String) : EvaluationResultEvent()
    object ShowReportDialog : EvaluationResultEvent()
}
