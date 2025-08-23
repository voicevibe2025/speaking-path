package com.example.voicevibe.presentation.screens.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.LearningRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class LearningPathDetailUiState(
    val isLoading: Boolean = false,
    val learningPath: LearningPath? = null,
    val reviews: List<PathReview> = emptyList(),
    val isBookmarked: Boolean = false,
    val error: String? = null,
    val enrollmentInProgress: Boolean = false,
    val successMessage: String? = null
)

@HiltViewModel
class LearningPathDetailViewModel @Inject constructor(
    private val repository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningPathDetailUiState())
    val uiState: StateFlow<LearningPathDetailUiState> = _uiState.asStateFlow()

    private var currentPathId: String? = null

    fun loadPathDetails(pathId: String) {
        if (currentPathId == pathId && _uiState.value.learningPath != null) {
            return // Already loaded
        }

        currentPathId = pathId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val path = repository.getPathById(pathId)
                if (path != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            learningPath = path,
                            reviews = generateMockReviews(), // In real app, fetch from repository
                            isBookmarked = false // In real app, check bookmark status
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Learning path not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load learning path"
                    )
                }
            }
        }
    }

    fun enrollInPath() {
        val pathId = currentPathId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(enrollmentInProgress = true, error = null) }

            try {
                repository.enrollInPath(pathId)

                // Reload path to get updated enrollment status
                val currentPath = _uiState.value.learningPath
                val updatedPath = currentPath?.copy(
                    isEnrolled = true,
                    enrolledCount = currentPath.enrolledCount + 1
                )

                _uiState.update {
                    it.copy(
                        enrollmentInProgress = false,
                        learningPath = updatedPath,
                        successMessage = "Successfully enrolled in the learning path!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        enrollmentInProgress = false,
                        error = e.message ?: "Failed to enroll in path"
                    )
                }
            }
        }
    }

    fun unenrollFromPath() {
        val pathId = currentPathId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(enrollmentInProgress = true, error = null) }

            try {
                repository.unenrollFromPath(pathId)

                // Update path enrollment status
                val currentPath = _uiState.value.learningPath
                val updatedPath = currentPath?.copy(
                    isEnrolled = false,
                    enrolledCount = (currentPath.enrolledCount - 1).coerceAtLeast(0),
                    progress = 0f,
                    completedLessons = 0,
                    nextLesson = null
                )

                _uiState.update {
                    it.copy(
                        enrollmentInProgress = false,
                        learningPath = updatedPath,
                        successMessage = "Unenrolled from the learning path"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        enrollmentInProgress = false,
                        error = e.message ?: "Failed to unenroll from path"
                    )
                }
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            try {
                val currentBookmarkStatus = _uiState.value.isBookmarked
                // In real app, call repository to save bookmark
                _uiState.update {
                    it.copy(
                        isBookmarked = !currentBookmarkStatus,
                        successMessage = if (!currentBookmarkStatus) "Added to bookmarks" else "Removed from bookmarks"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to update bookmark")
                }
            }
        }
    }

    fun sharePathDetails() {
        val path = _uiState.value.learningPath ?: return
        // In real app, implement share functionality
        _uiState.update {
            it.copy(successMessage = "Share feature coming soon!")
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun generateMockReviews(): List<PathReview> {
        return listOf(
            PathReview(
                id = "review_1",
                userName = "John Doe",
                userAvatar = null,
                rating = 5,
                comment = "Excellent course! The instructor explains everything clearly and the practice exercises are very helpful. I've improved my pronunciation significantly.",
                date = LocalDateTime.now().minusDays(5),
                helpfulCount = 12,
                isVerifiedPurchase = true
            ),
            PathReview(
                id = "review_2",
                userName = "Sarah Smith",
                userAvatar = null,
                rating = 4,
                comment = "Great content overall. The modules are well-structured and progressive. I would have liked more interactive exercises in some lessons.",
                date = LocalDateTime.now().minusDays(10),
                helpfulCount = 8,
                isVerifiedPurchase = true
            ),
            PathReview(
                id = "review_3",
                userName = "Michael Chen",
                userAvatar = null,
                rating = 5,
                comment = "This learning path exceeded my expectations! The combination of theory and practice is perfect. Highly recommended for anyone serious about improving their English.",
                date = LocalDateTime.now().minusDays(15),
                helpfulCount = 20,
                isVerifiedPurchase = true
            ),
            PathReview(
                id = "review_4",
                userName = "Emma Wilson",
                userAvatar = null,
                rating = 5,
                comment = "The best pronunciation course I've taken online. The instructor's teaching style is engaging and the lessons are easy to follow.",
                date = LocalDateTime.now().minusDays(20),
                helpfulCount = 15,
                isVerifiedPurchase = false
            ),
            PathReview(
                id = "review_5",
                userName = "David Brown",
                userAvatar = null,
                rating = 4,
                comment = "Good course with valuable content. The assessment modules really help track progress. Some technical issues with video playback occasionally.",
                date = LocalDateTime.now().minusDays(25),
                helpfulCount = 5,
                isVerifiedPurchase = true
            )
        )
    }
}
