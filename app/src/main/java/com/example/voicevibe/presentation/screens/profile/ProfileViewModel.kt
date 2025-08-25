package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _userName = mutableStateOf("Loading...")
    val userName: State<String> = _userName

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            try {
                val userProfile = repository.getProfile()
                _userName.value = userProfile.userName ?: "User"
            } catch (e: Exception) {
                _userName.value = "Error"
            }
        }
    }
}
