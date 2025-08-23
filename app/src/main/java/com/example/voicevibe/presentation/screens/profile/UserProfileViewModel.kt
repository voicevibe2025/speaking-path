package com.example.voicevibe.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for User Profile screen
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get userId from navigation args, null means current user
    private val userId: String? = savedStateHandle.get<String>("userId")

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UserProfileEvent>()
    val events: SharedFlow<UserProfileEvent> = _events.asSharedFlow()

    init {
        loadUserProfile()
        loadUserActivities()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            if (userId == null) {
                // getCurrentUser returns Flow<Resource<UserProfile>>
                repository.getCurrentUser().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    userProfile = result.data,
                                    isOwnProfile = true,
                                    error = null
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
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } else {
                // getUserById returns Resource<UserProfile> directly
                val result = repository.getUserById(userId)
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userProfile = result.data,
                                isOwnProfile = false,
                                error = null
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
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadUserActivities() {
        viewModelScope.launch {
            val targetUserId = userId ?: repository.getCurrentUserId()

            when (val result = repository.getUserActivities(targetUserId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(activities = result.data ?: emptyList())
                    }
                }
                else -> {}
            }
        }
    }

    fun followUser() {
        viewModelScope.launch {
            userId?.let { id ->
                when (val result = repository.followUser(id)) {
                    is Resource.Success -> {
                        _uiState.update { currentState ->
                            currentState.copy(
                                userProfile = currentState.userProfile?.copy(
                                    isFollowing = true
                                )
                            )
                        }
                        _events.emit(
                            UserProfileEvent.ShowMessage("Following user")
                        )
                    }
                    is Resource.Error -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(
                                result.message ?: "Failed to follow user"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun unfollowUser() {
        viewModelScope.launch {
            userId?.let { id ->
                when (val result = repository.unfollowUser(id)) {
                    is Resource.Success -> {
                        _uiState.update { currentState ->
                            currentState.copy(
                                userProfile = currentState.userProfile?.copy(
                                    isFollowing = false
                                )
                            )
                        }
                        _events.emit(
                            UserProfileEvent.ShowMessage("Unfollowed user")
                        )
                    }
                    is Resource.Error -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(
                                result.message ?: "Failed to unfollow user"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun blockUser() {
        viewModelScope.launch {
            userId?.let { id ->
                when (val result = repository.blockUser(id)) {
                    is Resource.Success -> {
                        _events.emit(UserProfileEvent.NavigateBack)
                        _events.emit(
                            UserProfileEvent.ShowMessage("User blocked")
                        )
                    }
                    is Resource.Error -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(
                                result.message ?: "Failed to block user"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun reportUser(reason: String) {
        viewModelScope.launch {
            userId?.let { id ->
                when (val result = repository.reportUser(id, reason)) {
                    is Resource.Success -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage("User reported. We'll review this soon.")
                        )
                    }
                    is Resource.Error -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(
                                result.message ?: "Failed to report user"
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun challengeUser() {
        viewModelScope.launch {
            userId?.let { id ->
                _events.emit(UserProfileEvent.ShowChallengeDialog(id))
            }
        }
    }

    fun editProfile() {
        viewModelScope.launch {
            _events.emit(UserProfileEvent.NavigateToEditProfile)
        }
    }

    fun shareProfile() {
        viewModelScope.launch {
            _uiState.value.userProfile?.let { profile ->
                _events.emit(
                    UserProfileEvent.ShareProfile(
                        username = profile.username,
                        level = profile.level,
                        achievements = profile.stats.achievementsUnlocked
                    )
                )
            }
        }
    }

    fun viewAchievements() {
        viewModelScope.launch {
            val targetUserId = userId ?: repository.getCurrentUserId()
            _events.emit(UserProfileEvent.NavigateToAchievements(targetUserId))
        }
    }

    fun viewFollowers() {
        viewModelScope.launch {
            val targetUserId = userId ?: repository.getCurrentUserId()
            _events.emit(UserProfileEvent.NavigateToFollowers(targetUserId))
        }
    }

    fun viewFollowing() {
        viewModelScope.launch {
            val targetUserId = userId ?: repository.getCurrentUserId()
            _events.emit(UserProfileEvent.NavigateToFollowing(targetUserId))
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refreshProfile() {
        loadUserProfile()
        loadUserActivities()
    }
}

/**
 * UI State for User Profile screen
 */
data class UserProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val activities: List<UserActivity> = emptyList(),
    val isOwnProfile: Boolean = false,
    val selectedTab: ProfileTab = ProfileTab.OVERVIEW,
    val error: String? = null
)

/**
 * Profile tabs
 */
enum class ProfileTab {
    OVERVIEW,
    ACTIVITY,
    ACHIEVEMENTS,
    STATISTICS
}

/**
 * Events from User Profile screen
 */
sealed class UserProfileEvent {
    object NavigateBack : UserProfileEvent()
    object NavigateToEditProfile : UserProfileEvent()
    object NavigateToSettings : UserProfileEvent()
    data class NavigateToAchievements(val userId: String) : UserProfileEvent()
    data class NavigateToFollowers(val userId: String) : UserProfileEvent()
    data class NavigateToFollowing(val userId: String) : UserProfileEvent()
    data class ShowChallengeDialog(val userId: String) : UserProfileEvent()
    data class ShareProfile(
        val username: String,
        val level: Int,
        val achievements: Int
    ) : UserProfileEvent()
    data class ShowMessage(val message: String) : UserProfileEvent()
}
