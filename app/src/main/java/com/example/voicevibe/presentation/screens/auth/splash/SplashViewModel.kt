package com.example.voicevibe.presentation.screens.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Splash screen
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val splashScreenStateFlow: MutableStateFlow<Boolean>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SplashNavigationEvent>()
    val navigationEvent: SharedFlow<SplashNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        checkAuthenticationState()
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            // Show splash screen for minimum duration
            delay(1500)

            // Check if this is first time launch
            val hasCompletedOnboarding = tokenManager.hasCompletedOnboarding()

            if (!hasCompletedOnboarding) {
                // First time user - show onboarding
                _navigationEvent.emit(SplashNavigationEvent.NavigateToOnboarding)
                splashScreenStateFlow.value = false
                return@launch
            }

            // Check if user is logged in
            val accessToken = tokenManager.getAccessToken()

            if (accessToken.isNullOrEmpty()) {
                // Not logged in - go to login
                _navigationEvent.emit(SplashNavigationEvent.NavigateToLogin)
                splashScreenStateFlow.value = false
                return@launch
            }

            // Verify token is still valid
            verifyToken()
        }
    }

    private suspend fun verifyToken() {
        authRepository.verifyToken().collect { resource ->
            when (resource) {
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToHome)
                    splashScreenStateFlow.value = false
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // Token invalid or expired - attempt refresh
                    refreshToken()
                }
            }
        }
    }

    private suspend fun refreshToken() {
        val refreshToken = tokenManager.getRefreshToken()

        if (refreshToken.isNullOrEmpty()) {
            // No refresh token - go to login
            _navigationEvent.emit(SplashNavigationEvent.NavigateToLogin)
            splashScreenStateFlow.value = false
            return
        }

        authRepository.refreshToken(refreshToken).collect { resource ->
            when (resource) {
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToHome)
                    splashScreenStateFlow.value = false
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    // Refresh failed - go to login
                    tokenManager.clearSession()
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToLogin)
                    splashScreenStateFlow.value = false
                }
            }
        }
    }
}

/**
 * UI State for the Splash screen
 */
data class SplashUiState(
    val isLoading: Boolean = true
)

/**
 * Navigation events from Splash screen
 */
sealed class SplashNavigationEvent {
    object NavigateToOnboarding : SplashNavigationEvent()
    object NavigateToLogin : SplashNavigationEvent()
    object NavigateToHome : SplashNavigationEvent()
}
