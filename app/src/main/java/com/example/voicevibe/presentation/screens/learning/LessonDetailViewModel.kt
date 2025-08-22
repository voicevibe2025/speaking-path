package com.example.voicevibe.presentation.screens.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.LearningRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LessonDetailUiState(
    val isLoading: Boolean = false,
    val lesson: LessonDetail? = null,
    val currentSectionIndex: Int = 0,
    val quizAnswers: Map<String, Int> = emptyMap(),
    val practiceCompleted: Set<String> = emptySet(),
    val isLessonCompleted: Boolean = false,
    val xpEarned: Int = 0,
    val nextLesson: NextLessonInfo? = null,
    val completionMessage: String? = null,
    val error: String? = null
)

data class NextLessonInfo(
    val pathId: String,
    val moduleId: String,
    val lessonId: String,
    val title: String
)

data class LessonDetail(
    val id: String,
    val title: String,
    val description: String,
    val sections: List<LessonSection>,
    val xpReward: Int,
    val estimatedDuration: Int
)

data class LessonSection(
    val id: String,
    val title: String,
    val content: String,
    val type: LessonSectionType,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val quizOptions: List<String>? = null,
    val correctAnswer: Int? = null,
    val explanation: String? = null,
    val practicePrompt: String? = null,
    val keyPoints: List<String>? = null
)

enum class LessonSectionType {
    VIDEO,
    TEXT,
    QUIZ,
    PRACTICE,
    AUDIO
}

@HiltViewModel
class LessonDetailViewModel @Inject constructor(
    private val repository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonDetailUiState())
    val uiState: StateFlow<LessonDetailUiState> = _uiState.asStateFlow()

    fun loadLesson(pathId: String, moduleId: String, lessonId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val lesson = createMockLesson(lessonId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lesson = lesson,
                        nextLesson = NextLessonInfo(pathId, moduleId, "next_lesson", "Next Lesson")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load lesson")
                }
            }
        }
    }

    fun navigateToNextSection() {
        val current = _uiState.value.currentSectionIndex
        val total = _uiState.value.lesson?.sections?.size ?: 0
        if (current < total - 1) {
            _uiState.update { it.copy(currentSectionIndex = current + 1, xpEarned = it.xpEarned + 10) }
        }
    }

    fun navigateToPreviousSection() {
        val current = _uiState.value.currentSectionIndex
        if (current > 0) {
            _uiState.update { it.copy(currentSectionIndex = current - 1) }
        }
    }

    fun selectQuizAnswer(sectionId: String, answerIndex: Int) {
        _uiState.update { state ->
            val answers = state.quizAnswers + (sectionId to answerIndex)
            val xp = if (state.lesson?.sections?.find { it.id == sectionId }?.correctAnswer == answerIndex) 20 else 0
            state.copy(quizAnswers = answers, xpEarned = state.xpEarned + xp)
        }
    }

    fun completePractice(sectionId: String) {
        _uiState.update { state ->
            state.copy(
                practiceCompleted = state.practiceCompleted + sectionId,
                xpEarned = state.xpEarned + 30
            )
        }
    }

    fun completeLesson() {
        val totalXp = _uiState.value.xpEarned + (_uiState.value.lesson?.xpReward ?: 0)
        _uiState.update {
            it.copy(
                isLessonCompleted = true,
                xpEarned = totalXp,
                completionMessage = "Congratulations! You've earned $totalXp XP!"
            )
        }
    }

    fun clearCompletionMessage() {
        _uiState.update { it.copy(completionMessage = null) }
    }

    private fun createMockLesson(lessonId: String) = LessonDetail(
        id = lessonId,
        title = "Introduction to Pronunciation",
        description = "Learn the basics of English pronunciation",
        xpReward = 100,
        estimatedDuration = 15,
        sections = listOf(
            LessonSection(
                id = "s1",
                title = "Welcome",
                content = "In this lesson, we'll explore English pronunciation basics.",
                type = LessonSectionType.VIDEO,
                videoUrl = "https://example.com/video.mp4"
            ),
            LessonSection(
                id = "s2",
                title = "Key Concepts",
                content = "English pronunciation involves vowels, consonants, stress, and intonation.",
                type = LessonSectionType.TEXT,
                keyPoints = listOf("Vowel sounds", "Consonant clusters", "Word stress", "Intonation")
            ),
            LessonSection(
                id = "s3",
                title = "Quick Quiz",
                content = "Which is most important for clear pronunciation?",
                type = LessonSectionType.QUIZ,
                quizOptions = listOf("Speaking fast", "Correct stress patterns", "Complex vocabulary", "Speaking loudly"),
                correctAnswer = 1,
                explanation = "Correct stress patterns are crucial for clear pronunciation."
            ),
            LessonSection(
                id = "s4",
                title = "Practice Time",
                content = "Practice saying this phrase with correct stress.",
                type = LessonSectionType.PRACTICE,
                practicePrompt = "Hello, how are you today?"
            ),
            LessonSection(
                id = "s5",
                title = "Listen and Learn",
                content = "Listen to native pronunciation examples.",
                type = LessonSectionType.AUDIO,
                audioUrl = "https://example.com/audio.mp3"
            )
        )
    )
}
