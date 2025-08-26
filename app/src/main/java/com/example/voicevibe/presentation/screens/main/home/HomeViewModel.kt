package com.example.voicevibe.presentation.screens.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.LearningPathRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.domain.model.UserProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the Home Dashboard screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val learningPathRepository: LearningPathRepository,
    private val gamificationRepository: GamificationRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadUserProfileData()
        loadDashboardData()
    }

    private fun loadUserProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userProfile = profileRepository.getProfile()
                
                // Generate display name
                val displayName = if (!userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank()) {
                    "${userProfile.firstName} ${userProfile.lastName}"
                } else {
                    userProfile.userName
                }

                // Generate user initials
                val userInitials = generateInitials(displayName)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = displayName,
                        userLevel = userProfile.currentLevel ?: 1,
                        userInitials = userInitials,
                        totalPoints = userProfile.experiencePoints ?: 0,
                        currentStreak = userProfile.streakDays ?: 0
                    )
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Network error. Please check your connection.",
                        userName = "Network Error",
                        userLevel = 1,
                        userInitials = "NE"
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load profile. Please try again.",
                        userName = "Error Loading",
                        userLevel = 1,
                        userInitials = "ER"
                    )
                }
            }
        }
    }

    private fun generateInitials(displayName: String): String {
        val parts = displayName.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> "${parts[0][0].uppercaseChar()}${parts[0][1].uppercaseChar()}"
            parts.size == 1 -> "${parts[0].first().uppercaseChar()}${parts[0].first().uppercaseChar()}"
            else -> "VV" // VoiceVibe default
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

        // Note: totalPoints and currentStreak are now loaded from ProfileRepository
        // in loadUserProfileData() method above
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
        loadUserProfileData()
        loadDashboardData()
    }
}

/**
 * UI State for the Home Dashboard
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userLevel: Int = 1,
    val userInitials: String? = null,
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
