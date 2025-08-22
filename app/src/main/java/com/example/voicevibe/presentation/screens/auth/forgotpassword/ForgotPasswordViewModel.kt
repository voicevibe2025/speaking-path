package com.example.voicevibe.presentation.screens.auth.forgotpassword

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.voicevibe.data.remote.dto.auth.MessageResponse

/**
 * ViewModel for the Forgot Password screen
 */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ForgotPasswordEvent>()
    val events: SharedFlow<ForgotPasswordEvent> = _events.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
    }

    fun sendResetLink() {
        if (!validateEmail()) return

        viewModelScope.launch {
            authRepository.requestPasswordReset(_uiState.value.email)
                .collect { resource: Resource<MessageResponse> ->
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isEmailSent = true
                                )
                            }
                            _events.emit(ForgotPasswordEvent.EmailSentSuccess)
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message
                                )
                            }
                            _events.emit(ForgotPasswordEvent.ShowError(resource.message ?: "Failed to send reset email"))
                        }
                    }
                }
        }
    }

    fun resendEmail() {
        sendResetLink()
    }

    private fun validateEmail(): Boolean {
        val email = _uiState.value.email

        return when {
            email.isBlank() -> {
                _uiState.update { it.copy(emailError = "Email is required") }
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.update { it.copy(emailError = "Enter a valid email address") }
                false
            }
            else -> true
        }
    }
}

/**
 * UI State for the Forgot Password screen
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Events from Forgot Password screen
 */
sealed class ForgotPasswordEvent {
    object EmailSentSuccess : ForgotPasswordEvent()
    data class ShowError(val message: String) : ForgotPasswordEvent()
}
