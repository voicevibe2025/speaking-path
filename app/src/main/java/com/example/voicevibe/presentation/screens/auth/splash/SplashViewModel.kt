package com.example.voicevibe.presentation.screens.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.data.repository.AiEvaluationRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import com.example.voicevibe.utils.WarmupUtils
import javax.inject.Inject

/**
 * ViewModel for the Splash screen
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val splashScreenStateFlow: MutableStateFlow<Boolean>,
    private val aiEvalRepo: AiEvaluationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<SplashNavigationEvent>()
    val navigationEvent: SharedFlow<SplashNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        checkAuthenticationState()
    }

    private var hasPrewarmed = false

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            // Show splash screen for minimum duration
            delay(1500)

            // Check if this is first time launch
            val hasCompletedOnboarding = tokenManager.isOnboardingCompletedFlow().first()

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
        // No explicit verify endpoint available. Treat token as valid and proceed,
        // or attempt a refresh to confirm validity.
        val isLoggedIn = tokenManager.isLoggedIn()
        if (isLoggedIn) {
            _uiState.update { it.copy(isLoading = false) }
            // Fire-and-forget ASR prewarm so first transcription is fast
            prewarmAsr()
            // Check if user has selected a group
            checkGroupStatus()
        } else {
            refreshToken()
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

        when (val result = authRepository.refreshToken()) {
            is Resource.Loading -> {
                _uiState.update { it.copy(isLoading = true) }
            }
            is Resource.Success -> {
                _uiState.update { it.copy(isLoading = false) }
                // Fire-and-forget ASR prewarm after successful refresh
                prewarmAsr()
                // Check if user has selected a group
                checkGroupStatus()
            }
            is Resource.Error -> {
                _uiState.update { it.copy(isLoading = false) }
                // Refresh failed - go to login
                tokenManager.clearAll()
                _navigationEvent.emit(SplashNavigationEvent.NavigateToLogin)
                splashScreenStateFlow.value = false
            }
        }
    }

    private suspend fun checkGroupStatus() {
        // Get user profile to check group status
        userRepository.getCurrentUser().collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    val hasGroup = resource.data?.hasGroup ?: false
                    if (hasGroup) {
                        _navigationEvent.emit(SplashNavigationEvent.NavigateToHome)
                    } else {
                        _navigationEvent.emit(SplashNavigationEvent.NavigateToGroupSelection)
                    }
                    splashScreenStateFlow.value = false
                }
                is Resource.Error -> {
                    // If profile fetch fails, still go to home
                    // User can select group later from settings
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToHome)
                    splashScreenStateFlow.value = false
                }
                is Resource.Loading -> {
                    // Keep loading
                }
            }
        }
    }

    private fun prewarmAsr() {
        if (hasPrewarmed) return
        hasPrewarmed = true
        viewModelScope.launch {
            try {
                val b64 = WarmupUtils.generateSilentWavBase64()
                // We don't care about the result; this primes server-side models and ffmpeg
                aiEvalRepo.transcribeBase64(b64)
                Timber.d("ASR prewarm request sent")
            } catch (t: Throwable) {
                // Ignore errors — do not block UI
                Timber.d(t, "ASR prewarm failed (ignored)")
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
    object NavigateToGroupSelection : SplashNavigationEvent()
}
