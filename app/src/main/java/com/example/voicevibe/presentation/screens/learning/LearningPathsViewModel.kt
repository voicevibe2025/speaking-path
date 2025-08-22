package com.example.voicevibe.presentation.screens.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.domain.model.*
import com.example.voicevibe.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningPathsViewModel @Inject constructor(
    private val learningRepository: LearningRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningPathsUiState())
    val uiState: StateFlow<LearningPathsUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<PathCategory?>(null)
    val selectedCategory: StateFlow<PathCategory?> = _selectedCategory.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow<DifficultyLevel?>(null)
    val selectedDifficulty: StateFlow<DifficultyLevel?> = _selectedDifficulty.asStateFlow()

    private val _sortOption = MutableStateFlow(PathSortOption.RECOMMENDED)
    val sortOption: StateFlow<PathSortOption> = _sortOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFilters = MutableStateFlow(false)
    val showFilters: StateFlow<Boolean> = _showFilters.asStateFlow()

    init {
        loadLearningPaths()
        loadRecommendations()
        loadUserProgress()
    }

    private fun loadLearningPaths() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val paths = learningRepository.getAllLearningPaths()
                val enrolledPaths = learningRepository.getEnrolledPaths()

                _uiState.update {
                    it.copy(
                        allPaths = paths,
                        enrolledPaths = enrolledPaths,
                        filteredPaths = paths,
                        isLoading = false
                    )
                }
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val recommendations = learningRepository.getRecommendedPaths()
                _uiState.update {
                    it.copy(recommendedPaths = recommendations)
                }
            } catch (e: Exception) {
                // Handle error silently for recommendations
            }
        }
    }

    private fun loadUserProgress() {
        viewModelScope.launch {
            try {
                val streak = learningRepository.getLearningStreak()
                val continueLesson = learningRepository.getContinueLesson()

                _uiState.update {
                    it.copy(
                        learningStreak = streak,
                        continueLesson = continueLesson
                    )
                }
            } catch (e: Exception) {
                // Handle error silently for progress
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFiltersAndSort()
    }

    fun selectCategory(category: PathCategory?) {
        _selectedCategory.value = category
        applyFiltersAndSort()
    }

    fun selectDifficulty(difficulty: DifficultyLevel?) {
        _selectedDifficulty.value = difficulty
        applyFiltersAndSort()
    }

    fun updateSortOption(option: PathSortOption) {
        _sortOption.value = option
        applyFiltersAndSort()
    }

    fun toggleFilters() {
        _showFilters.value = !_showFilters.value
    }

    fun applyFilters(filters: PathFilters) {
        _uiState.update { it.copy(activeFilters = filters) }
        applyFiltersAndSort()
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedDifficulty.value = null
        _uiState.update { it.copy(activeFilters = PathFilters()) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val allPaths = _uiState.value.allPaths
        val query = _searchQuery.value
        val category = _selectedCategory.value
        val difficulty = _selectedDifficulty.value
        val filters = _uiState.value.activeFilters
        val sort = _sortOption.value

        var filtered = allPaths

        // Apply search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { path ->
                path.title.contains(query, ignoreCase = true) ||
                path.description.contains(query, ignoreCase = true) ||
                path.tags.any { it.contains(query, ignoreCase = true) }
            }
        }

        // Apply category filter
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        // Apply difficulty filter
        if (difficulty != null) {
            filtered = filtered.filter { it.difficulty == difficulty }
        }

        // Apply additional filters
        filters.isPremium?.let { isPremium ->
            filtered = filtered.filter { it.isPremium == isPremium }
        }

        filters.isEnrolled?.let { isEnrolled ->
            filtered = filtered.filter { it.isEnrolled == isEnrolled }
        }

        filters.hasCertificate?.let { hasCertificate ->
            filtered = filtered.filter { it.certificateAvailable == hasCertificate }
        }

        filters.duration?.let { duration ->
            filtered = filtered.filter {
                it.duration in duration.min..duration.max
            }
        }

        // Apply sorting
        filtered = when (sort) {
            PathSortOption.RECOMMENDED -> filtered.sortedByDescending { it.isRecommended }
            PathSortOption.POPULARITY -> filtered.sortedByDescending { it.enrolledCount }
            PathSortOption.RATING -> filtered.sortedByDescending { it.rating }
            PathSortOption.NEWEST -> filtered.sortedByDescending { it.lastAccessedAt }
            PathSortOption.DURATION_SHORT -> filtered.sortedBy { it.duration }
            PathSortOption.DURATION_LONG -> filtered.sortedByDescending { it.duration }
            PathSortOption.DIFFICULTY_LOW -> filtered.sortedBy { it.difficulty.ordinal }
            PathSortOption.DIFFICULTY_HIGH -> filtered.sortedByDescending { it.difficulty.ordinal }
        }

        _uiState.update {
            it.copy(filteredPaths = filtered)
        }
    }

    fun enrollInPath(pathId: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(enrollingPathId = pathId)
                }

                learningRepository.enrollInPath(pathId)

                // Update the path in the list
                _uiState.update { state ->
                    state.copy(
                        allPaths = state.allPaths.map { path ->
                            if (path.id == pathId) {
                                path.copy(
                                    isEnrolled = true,
                                    enrolledCount = path.enrolledCount + 1
                                )
                            } else path
                        },
                        enrollingPathId = null
                    )
                }

                // Refresh enrolled paths
                loadLearningPaths()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        enrollingPathId = null,
                        error = "Failed to enroll: ${e.message}"
                    )
                }
            }
        }
    }

    fun unenrollFromPath(pathId: String) {
        viewModelScope.launch {
            try {
                learningRepository.unenrollFromPath(pathId)

                // Update the path in the list
                _uiState.update { state ->
                    state.copy(
                        allPaths = state.allPaths.map { path ->
                            if (path.id == pathId) {
                                path.copy(
                                    isEnrolled = false,
                                    enrolledCount = (path.enrolledCount - 1).coerceAtLeast(0)
                                )
                            } else path
                        }
                    )
                }

                // Refresh enrolled paths
                loadLearningPaths()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to unenroll: ${e.message}")
                }
            }
        }
    }

    fun markLessonComplete(lessonId: String) {
        viewModelScope.launch {
            try {
                learningRepository.markLessonComplete(lessonId)
                loadUserProgress()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to mark lesson complete: ${e.message}")
                }
            }
        }
    }

    fun bookmarkLesson(lessonId: String) {
        viewModelScope.launch {
            try {
                learningRepository.bookmarkLesson(lessonId)
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun refresh() {
        loadLearningPaths()
        loadRecommendations()
        loadUserProgress()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class LearningPathsUiState(
    val allPaths: List<LearningPath> = emptyList(),
    val filteredPaths: List<LearningPath> = emptyList(),
    val enrolledPaths: List<LearningPath> = emptyList(),
    val recommendedPaths: List<PathRecommendation> = emptyList(),
    val learningStreak: LearningStreak? = null,
    val continueLesson: LessonInfo? = null,
    val activeFilters: PathFilters = PathFilters(),
    val enrollingPathId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
