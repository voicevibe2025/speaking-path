package com.example.voicevibe.presentation.screens.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Login screen
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Login result events
    private val _loginEvent = MutableSharedFlow<LoginEvent>()
    val loginEvent: SharedFlow<LoginEvent> = _loginEvent.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onRememberMeChanged(rememberMe: Boolean) {
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    fun login() {
        if (!validateInputs()) return

        viewModelScope.launch {
            authRepository.login(
                email = _uiState.value.email,
                password = _uiState.value.password
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, generalError = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        checkGroupStatusAndNavigate()
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generalError = resource.message ?: "Login failed"
                            )
                        }

                        // Check for specific error types
                        when {
                            resource.message?.contains("email", ignoreCase = true) == true ||
                            resource.message?.contains("password", ignoreCase = true) == true -> {
                                _uiState.update {
                                    it.copy(generalError = "Invalid email or password")
                                }
                            }
                            resource.message?.contains("network", ignoreCase = true) == true -> {
                                _loginEvent.emit(LoginEvent.NetworkError)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = _uiState.value.email
        val password = _uiState.value.password
        var isValid = true

        // Validate email
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }

        // Validate password
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            isValid = false
        } else if (password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }

        return isValid
    }

    fun clearError() {
        _uiState.update { it.copy(generalError = null) }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, generalError = null) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        checkGroupStatusAndNavigate()
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                generalError = resource.message ?: "Google sign-in failed"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkGroupStatusAndNavigate() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val hasGroup = resource.data?.hasGroup ?: false
                        if (hasGroup) {
                            _loginEvent.emit(LoginEvent.NavigateToHome)
                        } else {
                            _loginEvent.emit(LoginEvent.NavigateToGroupSelection)
                        }
                    }
                    is Resource.Error -> {
                        // If we can't check, default to home
                        _loginEvent.emit(LoginEvent.NavigateToHome)
                    }
                    is Resource.Loading -> {
                        // Keep loading
                    }
                }
            }
        }
    }
}

/**
 * UI State for the Login screen
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

/**
 * Login events
 */
sealed class LoginEvent {
    object Success : LoginEvent()
    object NavigateToHome : LoginEvent()
    object NavigateToGroupSelection : LoginEvent()
    object NetworkError : LoginEvent()
}
