package com.example.voicevibe.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.example.voicevibe.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Logs out the user by calling the repository. Tokens and session data are
     * cleared in the repository regardless of API response.
     */
    suspend fun logout() {
        authRepository.logout()
    }
}
