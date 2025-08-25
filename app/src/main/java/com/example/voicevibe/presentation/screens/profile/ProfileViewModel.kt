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
