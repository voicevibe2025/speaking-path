package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.remote.api.CompleteListeningPracticeResponseDto
import com.example.voicevibe.data.remote.api.StartListeningPracticeResponseDto
import com.example.voicevibe.data.remote.api.SubmitListeningAnswerResponseDto
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListeningPracticeViewModel @Inject constructor(
    private val journeyRepo: SpeakingJourneyRepository,
    private val gamificationRepo: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListeningUiState())
    val uiState: StateFlow<ListeningUiState> = _uiState

    private var sessionId: String? = null
    private var topicId: String? = null
    private var rawQuestions: List<ListeningQuestionUi> = emptyList()

    private var xpFromAnswers: Int = 0
    private var correctCount: Int = 0

    fun start(topic: Topic) {
        if (topicId == topic.id && sessionId != null && !_uiState.value.showCongrats) return
        topicId = topic.id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val res = journeyRepo.startListeningPractice(topic.id)
            res.fold(
                onSuccess = { dto: StartListeningPracticeResponseDto ->
                    // Count as activity (idempotent server-side)
                    runCatching { gamificationRepo.updateStreak() }
                    sessionId = dto.sessionId
                    rawQuestions = dto.questions.map { ListeningQuestionUi(it.id, it.question, it.options) }
                    xpFromAnswers = 0
                    correctCount = 0
                    if (dto.questions.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, error = "No questions available for this topic.") }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                questionIndex = 0,
                                totalQuestions = dto.totalQuestions,
                                score = 0,
                                lastAwardedXp = 0,
                                totalXp = 0,
                                showCongrats = false,
                                isCompletionPending = false,
                                question = rawQuestions[0].question,
                                options = rawQuestions[0].options,
                                showQuestions = false,
                                error = null
                            )
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to start practice") }
                }
            )
        }
    }

    fun revealQuestions() {
        _uiState.update { it.copy(showQuestions = true) }
    }

    fun selectOption(option: String) {
        val sid = sessionId ?: return
        val tid = topicId ?: return
        val idx = _uiState.value.questionIndex
        if (idx !in rawQuestions.indices) return
        val q = rawQuestions[idx]
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, selectedOption = option, revealedAnswer = false, answerCorrect = null) }
            val res = journeyRepo.submitListeningAnswer(
                topicId = tid,
                sessionId = sid,
                questionId = q.id,
                selected = option
            )
            res.fold(
                onSuccess = { body: SubmitListeningAnswerResponseDto ->
                    runCatching { gamificationRepo.updateStreak() }
                    handleAnswerResponse(body)
                },
                onFailure = { e: Throwable ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to submit answer") }
                }
            )
        }
    }

    private fun handleAnswerResponse(body: SubmitListeningAnswerResponseDto) {
        val idx = _uiState.value.questionIndex
        if (body.correct) {
            correctCount += 1
        }
        val newScore = body.totalScore
        xpFromAnswers += body.xpAwarded

        _uiState.update {
            it.copy(
                isSubmitting = false,
                revealedAnswer = true,
                answerCorrect = body.correct,
                score = newScore,
                lastAwardedXp = body.xpAwarded,
                totalXp = xpFromAnswers,
                isCompletionPending = body.completed
            )
        }

        val nextIdx = body.nextIndex ?: (idx + 1)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            if (body.completed) {
                finalizeSession()
            } else {
                val q = rawQuestions.getOrNull(nextIdx)
                _uiState.update {
                    it.copy(
                        questionIndex = nextIdx,
                        question = q?.question ?: "",
                        options = q?.options ?: emptyList(),
                        selectedOption = null,
                        revealedAnswer = false,
                        answerCorrect = null,
                        isCompletionPending = false
                    )
                }
            }
        }
    }

    private fun finalizeSession() {
        val sid = sessionId ?: return
        val tid = topicId ?: return
        viewModelScope.launch {
            val res = journeyRepo.completeListeningPractice(tid, sid)
            res.fold(
                onSuccess = { dto: CompleteListeningPracticeResponseDto ->
                    runCatching { gamificationRepo.updateStreak() }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showCongrats = true,
                            isCompletionPending = false,
                            score = dto.totalScore,
                            totalQuestions = dto.totalQuestions,
                            questionIndex = dto.totalQuestions,
                            completionXp = dto.xpAwarded,
                            totalXp = xpFromAnswers + dto.xpAwarded,
                            correctCount = dto.correctCount
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to complete practice") }
                }
            )
        }
    }

    fun dismissCongrats() {
        sessionId = null
        rawQuestions = emptyList()
        _uiState.update { it.copy(showCongrats = false) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun restart(topic: Topic) {
        sessionId = null
        rawQuestions = emptyList()
        xpFromAnswers = 0
        correctCount = 0
        _uiState.update { ListeningUiState(isLoading = true) }
        start(topic)
    }
}

// --- UI State ---

data class ListeningQuestionUi(
    val id: String,
    val question: String,
    val options: List<String>
)

data class ListeningUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val question: String = "",
    val options: List<String> = emptyList(),
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val score: Int = 0,
    val lastAwardedXp: Int = 0,
    val totalXp: Int = 0,
    val completionXp: Int = 0,
    val correctCount: Int = 0,
    val showCongrats: Boolean = false,
    val showQuestions: Boolean = false,
    val error: String? = null,
    val selectedOption: String? = null,
    val revealedAnswer: Boolean = false,
    val answerCorrect: Boolean? = null,
    val isCompletionPending: Boolean = false,
)
