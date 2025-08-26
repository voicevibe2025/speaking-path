package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // User profile state for top card
    private val _userName = mutableStateOf("Loading...")
    val userName: State<String> = _userName

    private val _userEmail = mutableStateOf("Loading...")
    val userEmail: State<String> = _userEmail

    private val _membershipStatus = mutableStateOf("Loading...")
    val membershipStatus: State<String> = _membershipStatus

    private val _userInitials = mutableStateOf("--")
    val userInitials: State<String> = _userInitials

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
                
                // Set user name
                val displayName = if (!userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank()) {
                    "${userProfile.firstName} ${userProfile.lastName}"
                } else {
                    userProfile.userName
                }
                _userName.value = displayName

                // Set user email
                _userEmail.value = userProfile.userEmail

                // Set membership status
                _membershipStatus.value = userProfile.membershipStatus ?: "New Member"

                // Generate user initials
                _userInitials.value = generateInitials(displayName)

            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
                _userName.value = "Network Error"
                _userEmail.value = "Please check connection"
                _membershipStatus.value = "---"
                _userInitials.value = "NE"
            } catch (e: HttpException) {
                _errorMessage.value = "Failed to load profile. Please try again."
                _userName.value = "Error Loading"
                _userEmail.value = "Please try again"
                _membershipStatus.value = "---"
                _userInitials.value = "ER"
            } finally {
                _isLoading.value = false
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

    /**
     * Logs out the user by calling the repository. Tokens and session data are
     * cleared in the repository regardless of API response.
     */
    suspend fun logout() {
        authRepository.logout()
    }
}
