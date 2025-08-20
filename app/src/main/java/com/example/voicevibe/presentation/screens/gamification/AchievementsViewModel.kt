package com.example.voicevibe.presentation.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Achievements screen
 */
@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val repository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AchievementsEvent>()
    val events: SharedFlow<AchievementsEvent> = _events.asSharedFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load user achievements
            when (val result = repository.getUserAchievements()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            achievements = result.data,
                            filteredAchievements = filterAchievements(
                                result.data,
                                it.selectedCategory,
                                it.showOnlyUnlocked
                            )
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> {}
            }

            // Load achievement stats
            when (val statsResult = repository.getAchievementStats()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(stats = statsResult.data)
                    }
                }
                else -> {}
            }
        }
    }

    fun filterByCategory(category: AchievementCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredAchievements = filterAchievements(
                    state.achievements,
                    category,
                    state.showOnlyUnlocked
                )
            )
        }
    }

    fun toggleUnlockedFilter() {
        _uiState.update { state ->
            state.copy(
                showOnlyUnlocked = !state.showOnlyUnlocked,
                filteredAchievements = filterAchievements(
                    state.achievements,
                    state.selectedCategory,
                    !state.showOnlyUnlocked
                )
            )
        }
    }

    fun sortAchievements(sortBy: AchievementSortBy) {
        _uiState.update { state ->
            state.copy(
                sortBy = sortBy,
                filteredAchievements = sortAchievements(
                    state.filteredAchievements,
                    sortBy
                )
            )
        }
    }

    fun viewAchievementDetails(achievement: Achievement) {
        viewModelScope.launch {
            _events.emit(AchievementsEvent.ShowAchievementDetails(achievement))
        }
    }

    fun shareAchievement(achievement: Achievement) {
        viewModelScope.launch {
            _events.emit(
                AchievementsEvent.ShareAchievement(
                    title = achievement.title,
                    description = achievement.description,
                    points = achievement.points
                )
            )
        }
    }

    fun claimReward(achievement: Achievement) {
        viewModelScope.launch {
            achievement.reward?.let { reward ->
                when (val result = repository.claimAchievementReward(achievement.id)) {
                    is Resource.Success -> {
                        _events.emit(
                            AchievementsEvent.ShowMessage(
                                "Reward claimed: ${reward.description}"
                            )
                        )
                        loadAchievements() // Refresh
                    }
                    is Resource.Error -> {
                        _events.emit(
                            AchievementsEvent.ShowMessage(
                                result.message ?: "Failed to claim reward"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun retryLoading() {
        loadAchievements()
    }

    private fun filterAchievements(
        achievements: List<Achievement>,
        category: AchievementCategory?,
        showOnlyUnlocked: Boolean
    ): List<Achievement> {
        return achievements.filter { achievement ->
            val categoryMatch = category == null || achievement.category == category
            val unlockedMatch = !showOnlyUnlocked || achievement.isUnlocked
            categoryMatch && unlockedMatch
        }
    }

    private fun sortAchievements(
        achievements: List<Achievement>,
        sortBy: AchievementSortBy
    ): List<Achievement> {
        return when (sortBy) {
            AchievementSortBy.POINTS -> achievements.sortedByDescending { it.points }
            AchievementSortBy.PROGRESS -> achievements.sortedByDescending { it.progress }
            AchievementSortBy.RARITY -> achievements.sortedByDescending { it.rarity.ordinal }
            AchievementSortBy.RECENT -> achievements.sortedByDescending { it.unlockedAt }
            AchievementSortBy.NAME -> achievements.sortedBy { it.title }
        }
    }
}

/**
 * UI State for Achievements screen
 */
data class AchievementsUiState(
    val isLoading: Boolean = false,
    val achievements: List<Achievement> = emptyList(),
    val filteredAchievements: List<Achievement> = emptyList(),
    val stats: AchievementStats? = null,
    val selectedCategory: AchievementCategory? = null,
    val showOnlyUnlocked: Boolean = false,
    val sortBy: AchievementSortBy = AchievementSortBy.POINTS,
    val error: String? = null
)

/**
 * Sort options for achievements
 */
enum class AchievementSortBy {
    POINTS,
    PROGRESS,
    RARITY,
    RECENT,
    NAME
}

/**
 * Events from Achievements screen
 */
sealed class AchievementsEvent {
    data class ShowAchievementDetails(val achievement: Achievement) : AchievementsEvent()
    data class ShareAchievement(
        val title: String,
        val description: String,
        val points: Int
    ) : AchievementsEvent()
    data class ShowMessage(val message: String) : AchievementsEvent()
}
