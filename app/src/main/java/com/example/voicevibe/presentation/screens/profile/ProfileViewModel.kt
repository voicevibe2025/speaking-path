package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _userName = mutableStateOf("Loading...")
    val userName: State<String> = _userName

    private val _proficiency = mutableStateOf("...")
    val proficiency: State<String> = _proficiency

    private val _xp = mutableStateOf(0)
    val xp: State<Int> = _xp

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

    // Recent Achievements state
    private val _recentAchievements = mutableStateOf<List<com.example.voicevibe.data.model.Achievement>>(emptyList())
    val recentAchievements: State<List<com.example.voicevibe.data.model.Achievement>> = _recentAchievements

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

    private val _vocabularyScore = mutableStateOf(0f)
    val vocabularyScore: State<Float> = _vocabularyScore

    private val _pronunciationScore = mutableStateOf(0f)
    val pronunciationScore: State<Float> = _pronunciationScore

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userProfile = profileRepository.getProfile()
                val displayName = if (!userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank()) {
                    "${userProfile.firstName} ${userProfile.lastName}"
                } else {
                    userProfile.userName
                }
                _userName.value = displayName
                _proficiency.value = userProfile.currentProficiency?.replaceFirstChar { it.titlecase() } ?: "N/A"
                _xp.value = userProfile.experiencePoints ?: 0
                _streak.value = userProfile.streakDays ?: 0

                // Update Quick Stats
                _practiceHours.value = userProfile.totalPracticeHours ?: 0f
                _lessonsCompleted.value = userProfile.lessonsCompleted ?: 0
                _recordingsCount.value = userProfile.recordingsCount ?: 0
                _avgScore.value = userProfile.avgScore ?: 0f

                // Update Recent Achievements
                _recentAchievements.value = userProfile.recentAchievements ?: emptyList()

                // Update Learning Preferences
                _dailyPracticeGoal.value = userProfile.dailyPracticeGoal ?: 15
                _learningGoal.value = formatLearningGoal(userProfile.learningGoal)
                _targetLanguage.value = formatTargetLanguage(userProfile.targetLanguage)

                // Update Skill Progress (convert from 0-100 to 0.0-1.0 for progress bars)
                _speakingScore.value = (userProfile.speakingScore ?: 0f) / 100f
                _listeningScore.value = (userProfile.listeningScore ?: 0f) / 100f
                _grammarScore.value = (userProfile.grammarScore ?: 0f) / 100f
                _vocabularyScore.value = (userProfile.vocabularyScore ?: 0f) / 100f
                _pronunciationScore.value = (userProfile.pronunciationScore ?: 0f) / 100f

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
}
