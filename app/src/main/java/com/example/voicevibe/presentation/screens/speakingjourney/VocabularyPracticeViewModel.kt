package com.example.voicevibe.presentation.screens.speakingjourney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.remote.api.StartVocabularyPracticeResponseDto
import com.example.voicevibe.data.remote.api.SubmitVocabularyAnswerResponseDto
import com.example.voicevibe.data.remote.api.VocabularyQuestionDto
import com.example.voicevibe.data.remote.api.CompleteVocabularyPracticeResponseDto
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class VocabularyPracticeViewModel @Inject constructor(
    private val journeyRepo: SpeakingJourneyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabUiState())
    val uiState: StateFlow<VocabUiState> = _uiState

    private var sessionId: String? = null
    private var topicId: String? = null
    private var rawQuestions: List<VocabularyQuestionDto> = emptyList()

    private var xpFromAnswers: Int = 0
    private var correctCount: Int = 0

    fun start(topic: Topic) {
        if (topicId == topic.id && sessionId != null) return
        topicId = topic.id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val res = journeyRepo.startVocabularyPractice(topic.id)
            res.fold(
                onSuccess = { dto: StartVocabularyPracticeResponseDto ->
                    sessionId = dto.sessionId
                    rawQuestions = dto.questions
                    xpFromAnswers = 0
                    correctCount = 0
                    if (dto.questions.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, error = "No questions available for this topic.") }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            questionIndex = 0,
                            totalQuestions = dto.totalQuestions,
                            score = 0,
                            lastAwardedXp = 0,
                            totalXp = 0,
                            showCongrats = false,
                            definition = dto.questions[0].definition,
                            options = dto.questions[0].options,
                            error = null
                        ) }
                    }
                },
                onFailure = { e: Throwable ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to start practice") }
                }
            )
        }
    }

    fun selectOption(option: String) {
        val sid = sessionId ?: return
        val tid = topicId ?: return
        val idx = _uiState.value.questionIndex
        if (idx !in rawQuestions.indices) return
        val q = rawQuestions[idx]
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, selectedOption = option, revealedAnswer = false, answerCorrect = null) }
            val res = journeyRepo.submitVocabularyAnswer(
                topicId = tid,
                sessionId = sid,
                questionId = q.id,
                selected = option
            )
            res.fold(
                onSuccess = { body: SubmitVocabularyAnswerResponseDto ->
                    handleAnswerResponse(body)
                },
                onFailure = { e: Throwable ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to submit answer") }
                }
            )
        }
    }

    private fun handleAnswerResponse(body: SubmitVocabularyAnswerResponseDto) {
        val idx = _uiState.value.questionIndex
        if (body.correct) {
            correctCount += 1
        }
        val newScore = body.totalScore
        xpFromAnswers += body.xpAwarded

        // Show feedback highlight first
        _uiState.update { it.copy(
            isSubmitting = false,
            revealedAnswer = true,
            answerCorrect = body.correct,
            score = newScore,
            lastAwardedXp = body.xpAwarded,
            totalXp = xpFromAnswers
        ) }

        val nextIdx = body.nextIndex ?: (idx + 1)
        viewModelScope.launch {
            delay(1000)
            if (body.completed) {
                finalizeSession()
            } else {
                val def = rawQuestions.getOrNull(nextIdx)?.definition ?: ""
                val opts = rawQuestions.getOrNull(nextIdx)?.options ?: emptyList()
                _uiState.update { it.copy(
                    // advance to next question
                    questionIndex = nextIdx,
                    definition = def,
                    options = opts,
                    // reset feedback state
                    selectedOption = null,
                    revealedAnswer = false,
                    answerCorrect = null,
                ) }
            }
        }
    }

    private fun finalizeSession() {
        val sid = sessionId ?: return
        val tid = topicId ?: return
        viewModelScope.launch {
            val res = journeyRepo.completeVocabularyPractice(tid, sid)
            res.fold(
                onSuccess = { dto: CompleteVocabularyPracticeResponseDto ->
                    _uiState.update { it.copy(
                        isSubmitting = false,
                        showCongrats = true,
                        score = dto.totalScore,
                        totalQuestions = dto.totalQuestions,
                        questionIndex = dto.totalQuestions,
                        completionXp = dto.xpAwarded,
                        totalXp = xpFromAnswers + dto.xpAwarded,
                        correctCount = dto.correctCount
                    ) }
                },
                onFailure = { e: Throwable ->
                    _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to complete practice") }
                }
            )
        }
    }

    fun dismissCongrats() {
        _uiState.update { it.copy(showCongrats = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// --- UI State ---

data class VocabUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val definition: String = "",
    val options: List<String> = emptyList(),
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val score: Int = 0,
    val lastAwardedXp: Int = 0,
    val totalXp: Int = 0,
    val completionXp: Int = 0,
    val correctCount: Int = 0,
    val showCongrats: Boolean = false,
    val error: String? = null,
    // feedback highlight state
    val selectedOption: String? = null,
    val revealedAnswer: Boolean = false,
    val answerCorrect: Boolean? = null,
)
