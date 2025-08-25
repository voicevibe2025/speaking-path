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

            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
            } catch (e: HttpException) {
                _errorMessage.value = "Error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
