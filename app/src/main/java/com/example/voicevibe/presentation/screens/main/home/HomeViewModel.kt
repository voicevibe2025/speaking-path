package com.example.voicevibe.presentation.screens.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.LearningPathRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.domain.model.UserProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home Dashboard screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val learningPathRepository: LearningPathRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadUserData()
        loadDashboardData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = resource.data
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load learning paths
            learningPathRepository.getUserLearningPaths().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                activeLearningPaths = resource.data?.filter { path ->
                                    path.progress < 100
                                } ?: emptyList(),
                                completedLessons = resource.data?.sumOf { path ->
                                    path.completedLessons
                                } ?: 0
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            // Load user progress
            userRepository.getUserProgress().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(userProgress = resource.data)
                        }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            // Load gamification stats
            gamificationRepository.getUserStats().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.data?.let { stats ->
                            _uiState.update {
                                it.copy(
                                    totalPoints = stats.totalXp,
                                    currentStreak = stats.streakDays,
                                    badges = emptyList() // Will load badges separately
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun onStartPractice() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToPractice)
        }
    }

    fun onContinueLearning(pathId: String) {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToLearningPath(pathId))
        }
    }

    fun onViewAllPaths() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToLearningPaths)
        }
    }

    fun onViewAchievements() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToAchievements)
        }
    }

    fun refresh() {
        loadUserData()
        loadDashboardData()
    }
}

/**
 * UI State for the Home Dashboard
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val user: UserProfile? = null,
    val userProgress: UserProgress? = null,
    val activeLearningPaths: List<LearningPath> = emptyList(),
    val completedLessons: Int = 0,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Events from Home Dashboard
 */
sealed class HomeEvent {
    object NavigateToPractice : HomeEvent()
    object NavigateToLearningPaths : HomeEvent()
    object NavigateToAchievements : HomeEvent()
    data class NavigateToLearningPath(val pathId: String) : HomeEvent()
}
