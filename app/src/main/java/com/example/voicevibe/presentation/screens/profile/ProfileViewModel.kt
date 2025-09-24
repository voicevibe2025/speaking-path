package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.data.repository.SpeakingJourneyRepository
import com.example.voicevibe.domain.model.UserActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val speakingJourneyRepository: SpeakingJourneyRepository
) : ViewModel() {

    private val _userName = mutableStateOf("Loading...")
    val userName: State<String> = _userName

    private val _proficiency = mutableStateOf("...")
    val proficiency: State<String> = _proficiency

    private val _xp = mutableStateOf(0)
    val xp: State<Int> = _xp

    // Next level XP threshold (denominator for progress bar)
    private val _nextLevelXp = mutableStateOf(100)
    val nextLevelXp: State<Int> = _nextLevelXp

    // Lifetime XP (never decreases): total_points_earned
    private val _totalXp = mutableStateOf(0)
    val totalXp: State<Int> = _totalXp

    private val _streak = mutableStateOf(0)
    val streak: State<Int> = _streak

    // Quick Stats state
    private val _practiceHours = mutableStateOf(0f)
    val practiceHours: State<Float> = _practiceHours

    private val _lessonsCompleted = mutableStateOf(0)
    val lessonsCompleted: State<Int> = _lessonsCompleted

    private val _recordingsCount = mutableStateOf(0)
    val recordingsCount: State<Int> = _recordingsCount

    private val _avgScore = mutableStateOf(0f)
    val avgScore: State<Float> = _avgScore

    // Social counts
    private val _followersCount = mutableStateOf(0)
    val followersCount: State<Int> = _followersCount

    private val _followingCount = mutableStateOf(0)
    val followingCount: State<Int> = _followingCount

    // Avatar and initials state
    private val _avatarUrl = mutableStateOf<String?>(null)
    val avatarUrl: State<String?> = _avatarUrl

    private val _userInitials = mutableStateOf("VV")
    val userInitials: State<String> = _userInitials

    // Recent Achievements state
    private val _recentAchievements = mutableStateOf<List<com.example.voicevibe.data.model.Achievement>>(emptyList())
    val recentAchievements: State<List<com.example.voicevibe.data.model.Achievement>> = _recentAchievements

    // Activities feed (current user)
    private val _activities = mutableStateOf<List<UserActivity>>(emptyList())
    val activities: State<List<UserActivity>> = _activities

    // Learning Preferences state
    private val _dailyPracticeGoal = mutableStateOf(15)
    val dailyPracticeGoal: State<Int> = _dailyPracticeGoal

    private val _learningGoal = mutableStateOf("Conversational")
    val learningGoal: State<String> = _learningGoal

    private val _targetLanguage = mutableStateOf("English")
    val targetLanguage: State<String> = _targetLanguage

    // Skill Progress state
    private val _speakingScore = mutableStateOf(0f)
    val speakingScore: State<Float> = _speakingScore

    private val _listeningScore = mutableStateOf(0f)
    val listeningScore: State<Float> = _listeningScore

    private val _grammarScore = mutableStateOf(0f)
    val grammarScore: State<Float> = _grammarScore

    private val _fluencyScore = mutableStateOf(0f)
    val fluencyScore: State<Float> = _fluencyScore

    private val _vocabularyScore = mutableStateOf(0f)
    val vocabularyScore: State<Float> = _vocabularyScore

    private val _pronunciationScore = mutableStateOf(0f)
    val pronunciationScore: State<Float> = _pronunciationScore

    // Monthly Progress state
    private val _monthlyDaysActive = mutableStateOf(0)
    val monthlyDaysActive: State<Int> = _monthlyDaysActive

    private val _monthlyXpEarned = mutableStateOf(0)
    val monthlyXpEarned: State<Int> = _monthlyXpEarned

    private val _monthlyLessonsCompleted = mutableStateOf(0)
    val monthlyLessonsCompleted: State<Int> = _monthlyLessonsCompleted

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    init {
        fetchUserProfile()
    }

    fun refresh() {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userProfile = profileRepository.getProfile()
                val displayName = when {
                    !userProfile.displayName.isNullOrBlank() -> userProfile.displayName
                    !userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank() ->
                        "${userProfile.firstName} ${userProfile.lastName}"
                    !userProfile.firstName.isNullOrBlank() -> userProfile.firstName
                    !userProfile.lastName.isNullOrBlank() -> userProfile.lastName
                    else -> userProfile.userName
                }
                _userName.value = displayName
                _userInitials.value = generateInitials(displayName)
                _proficiency.value = userProfile.currentProficiency?.replaceFirstChar { it.titlecase() } ?: "N/A"
                val currentXp = userProfile.experiencePoints ?: 0
                _xp.value = currentXp
                _totalXp.value = userProfile.totalPointsEarned ?: currentXp
                // Compute threshold = currentXp + remaining (xpToNextLevel). If backend didn't provide, use Option A default
                val currentLevel = userProfile.currentLevel ?: 1
                val remaining = userProfile.xpToNextLevel ?: kotlin.math.max(1, 100 + 25 * (currentLevel - 1) - currentXp)
                _nextLevelXp.value = (currentXp + remaining).coerceAtLeast(1)
                _streak.value = userProfile.streakDays ?: 0

                // Avatar URL (normalize if relative)
                _avatarUrl.value = userProfile.avatarUrl?.let { normalizeUrl(it) }

                // Update Quick Stats
                _practiceHours.value = userProfile.totalPracticeHours ?: 0f
                _lessonsCompleted.value = userProfile.lessonsCompleted ?: 0
                _recordingsCount.value = userProfile.recordingsCount ?: 0
                _avgScore.value = userProfile.avgScore ?: 0f

                // Social counts
                _followersCount.value = userProfile.followersCount ?: 0
                _followingCount.value = userProfile.followingCount ?: 0

                // Update Recent Achievements
                _recentAchievements.value = userProfile.recentAchievements ?: emptyList()

                // Load Speaking Journey activities for current user (domain model)
                speakingJourneyRepository.getActivities(limit = 50, userId = null)
                    .onSuccess { list -> _activities.value = list }
                    .onFailure { /* keep empty on failure */ }

                // Update Learning Preferences
                _dailyPracticeGoal.value = userProfile.dailyPracticeGoal ?: 15
                _learningGoal.value = formatLearningGoal(userProfile.learningGoal)
                _targetLanguage.value = formatTargetLanguage(userProfile.targetLanguage)

                // Update Skill Progress (convert from 0-100 to 0.0-1.0 for progress bars)
                _speakingScore.value = (userProfile.speakingScore ?: 0f) / 100f
                _fluencyScore.value = (userProfile.fluencyScore ?: 0f) / 100f
                _listeningScore.value = (userProfile.listeningScore ?: 0f) / 100f
                _grammarScore.value = 0f // Grammar not implemented yet
                _vocabularyScore.value = (userProfile.vocabularyScore ?: 0f) / 100f
                _pronunciationScore.value = (userProfile.pronunciationScore ?: 0f) / 100f

                // Update Monthly Progress
                _monthlyDaysActive.value = userProfile.monthlyDaysActive ?: 0
                _monthlyXpEarned.value = userProfile.monthlyXpEarned ?: 0
                _monthlyLessonsCompleted.value = userProfile.monthlyLessonsCompleted ?: 0

            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
            } catch (e: HttpException) {
                _errorMessage.value = "Failed to load profile. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatLearningGoal(goal: String?): String {
        return when (goal?.lowercase()) {
            "business" -> "Business English"
            "conversational" -> "Conversational"
            "academic" -> "Academic English"
            "travel" -> "Travel English"
            "professional" -> "Professional English"
            else -> "General English"
        }
    }

    private fun formatTargetLanguage(language: String?): String {
        return when (language?.lowercase()) {
            "en", "english" -> "American English"
            "en-us" -> "American English"
            "en-gb" -> "British English"
            "en-au" -> "Australian English"
            else -> "English"
        }
    }

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val serverBase = com.example.voicevibe.utils.Constants.BASE_URL.substringBefore("/api/").trimEnd('/')
        val path = if (url.startsWith("/")) url else "/$url"
        return serverBase + path
    }

    private fun generateInitials(displayName: String): String {
        val parts = displayName.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> "${parts[0][0].uppercaseChar()}${parts[0][1].uppercaseChar()}"
            parts.size == 1 -> "${parts[0].first().uppercaseChar()}${parts[0].first().uppercaseChar()}"
            else -> "VV"
        }
    }
}

