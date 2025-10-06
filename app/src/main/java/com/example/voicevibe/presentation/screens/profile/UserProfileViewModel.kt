package com.example.voicevibe.presentation.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
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
    private val speakingJourneyRepository: SpeakingJourneyRepository,
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
        loadSpeakingOverview()
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
            val result = speakingJourneyRepository.getActivities(limit = 50, userId = userId)
            result.onSuccess { list ->
                _uiState.update { it.copy(activities = list) }
            }.onFailure {
                _uiState.update { it.copy(activities = emptyList()) }
            }
        }
    }

    private fun loadSpeakingOverview() {
        viewModelScope.launch {
            _uiState.update { it.copy(speakingOverviewLoading = true, speakingOverviewError = null) }
            
            if (userId == null) {
                // Current user: calculate from topics
                val result = speakingJourneyRepository.getTopics()
                result.onSuccess { resp ->
                    val topics = resp.topics
                    // Normalize per-topic practice scores to 0..100 where possible
                    var pronSum = 0f
                    var pronCount = 0
                    var fluencySum = 0f
                    var fluencyCount = 0
                    var vocabSum = 0f
                    var vocabCount = 0

                    val perTopicAverages = mutableListOf<Float>()

                    topics.forEach { t ->
                        val ps = t.practiceScores
                        if (ps != null) {
                            // Pronunciation
                            if (ps.maxPronunciation > 0) {
                                val v = (ps.pronunciation.toFloat() / ps.maxPronunciation.toFloat()) * 100f
                                pronSum += v
                                pronCount++
                            }
                            // Fluency
                            if (ps.maxFluency > 0) {
                                val v = (ps.fluency.toFloat() / ps.maxFluency.toFloat()) * 100f
                                fluencySum += v
                                fluencyCount++
                            }
                            // Vocabulary
                            if (ps.maxVocabulary > 0) {
                                val v = (ps.vocabulary.toFloat() / ps.maxVocabulary.toFloat()) * 100f
                                vocabSum += v
                                vocabCount++
                            }

                            // Per-topic overall average (only include available metrics)
                            val metrics = buildList<Float> {
                                if (ps.maxPronunciation > 0) add((ps.pronunciation.toFloat() / ps.maxPronunciation.toFloat()) * 100f)
                                if (ps.maxFluency > 0) add((ps.fluency.toFloat() / ps.maxFluency.toFloat()) * 100f)
                                if (ps.maxVocabulary > 0) add((ps.vocabulary.toFloat() / ps.maxVocabulary.toFloat()) * 100f)
                            }
                            if (metrics.isNotEmpty()) {
                                perTopicAverages.add(metrics.average().toFloat())
                            }
                        }
                    }

                    val avgPron = if (pronCount > 0) pronSum / pronCount else 0f
                    val avgFlu = if (fluencyCount > 0) fluencySum / fluencyCount else 0f
                    val avgVocab = if (vocabCount > 0) vocabSum / vocabCount else 0f

                    // Improvement rate: recent avg (last 3) - early avg (first 3)
                    val window = kotlin.math.max(1, kotlin.math.min(3, perTopicAverages.size / 2))
                    val earlyAvg = if (perTopicAverages.size >= window) perTopicAverages.take(window).average().toFloat() else 0f
                    val recentAvg = if (perTopicAverages.size >= window) perTopicAverages.takeLast(window).average().toFloat() else 0f
                    val improvement = (recentAvg - earlyAvg)

                    val completedTopics = topics.count { it.completed }
                    val totalWords = topics.filter { it.completed }.flatMap { it.vocabulary }.toSet().size

                    val overview = SpeakingOverview(
                        averagePronunciation = avgPron.coerceIn(0f, 100f),
                        averageFluency = avgFlu.coerceIn(0f, 100f),
                        averageVocabulary = avgVocab.coerceIn(0f, 100f),
                        improvementRate = improvement,
                        completedTopics = completedTopics,
                        totalPracticeMinutes = 0, // Not available from speaking_journey endpoints yet
                        totalWordsLearned = totalWords
                    )

                    _uiState.update { it.copy(speakingOverviewLoading = false, speakingOverview = overview) }
                }.onFailure { err ->
                    _uiState.update { it.copy(speakingOverviewLoading = false, speakingOverviewError = err.message) }
                }
            } else {
                // Target user: use data from profile stats
                _uiState.value.userProfile?.let { profile ->
                    val overview = SpeakingOverview(
                        averagePronunciation = profile.stats.averageAccuracy,
                        averageFluency = profile.stats.averageFluency,
                        averageVocabulary = 0f, // Not available in stats, set to 0
                        improvementRate = profile.stats.improvementRate,
                        completedTopics = profile.stats.completedLessons,
                        totalPracticeMinutes = profile.stats.totalPracticeMinutes,
                        totalWordsLearned = profile.stats.totalWords
                    )
                    _uiState.update { it.copy(speakingOverviewLoading = false, speakingOverview = overview) }
                } ?: run {
                    // If profile not loaded yet, wait for it and retry
                    _uiState.update { it.copy(speakingOverviewLoading = false) }
                }
            }
        }
    }

    fun followUser() {
        viewModelScope.launch {
            userId?.let { id ->
                when (val result = repository.followUser(id)) {
                    is Resource.Success -> {
                        _uiState.update { currentState ->
                            val current = currentState.userProfile
                            val newStats = current?.stats?.copy(
                                followersCount = (current.stats.followersCount + 1).coerceAtLeast(0)
                            )
                            currentState.copy(
                                userProfile = current?.copy(
                                    isFollowing = true,
                                    stats = newStats ?: current.stats
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
                            val current = currentState.userProfile
                            val newStats = current?.stats?.copy(
                                followersCount = (current.stats.followersCount - 1).coerceAtLeast(0)
                            )
                            currentState.copy(
                                userProfile = current?.copy(
                                    isFollowing = false,
                                    stats = newStats ?: current.stats
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
            val targetId = (userId ?: _uiState.value.userProfile?.id)?.toString()
            targetId?.let { id ->
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

    fun reportUser(reason: String, description: String = "") {
        viewModelScope.launch {
            // Get the actual user ID from the loaded profile
            val reportedUserId = (userId ?: _uiState.value.userProfile?.id)?.toIntOrNull()
            if (reportedUserId != null) {
                // Map human-readable reason from UI to backend enum codes
                val backendReason = when (reason.trim().lowercase()) {
                    "spam" -> "spam"
                    "harassment or bullying", "harassment" -> "harassment"
                    "hate speech" -> "hate_speech"
                    "inappropriate content", "inappropriate" -> "inappropriate"
                    "impersonation" -> "impersonation"
                    else -> "other"
                }
                when (val result = repository.createReport(
                    reportType = "user",
                    reason = backendReason,
                    description = description,
                    reportedUserId = reportedUserId
                )) {
                    is Resource.Success<*> -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(result.data?.toString() ?: "User reported. We'll review this soon.")
                        )
                    }
                    is Resource.Error<*> -> {
                        _events.emit(
                            UserProfileEvent.ShowMessage(
                                result.message ?: "Failed to report user"
                            )
                        )
                    }
                    else -> {}
                }
            } else {
                _events.emit(UserProfileEvent.ShowMessage("Unable to report user"))
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
        loadSpeakingOverview()
    }

    fun loadFollowers(userId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(followersLoading = true) }
            val result = repository.getFollowers(userId)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(followers = result.data ?: emptyList(), followersLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(followers = emptyList(), followersLoading = false) }
                }
                else -> {}
            }
        }
    }

    fun loadFollowing(userId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(followingLoading = true) }
            val result = repository.getFollowing(userId)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(following = result.data ?: emptyList(), followingLoading = false) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(following = emptyList(), followingLoading = false) }
                }
                else -> {}
            }
        }
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
    val error: String? = null,
    // Speaking Journey derived overview metrics for Overview tab
    val speakingOverview: SpeakingOverview? = null,
    val speakingOverviewLoading: Boolean = false,
    val speakingOverviewError: String? = null,
    // Followers and following lists
    val followers: List<UserProfile> = emptyList(),
    val following: List<UserProfile> = emptyList(),
    val followersLoading: Boolean = false,
    val followingLoading: Boolean = false
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
