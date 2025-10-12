package com.example.voicevibe.presentation.screens.auth.register

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
 * ViewModel for the Register screen
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // Register result events
    private val _registerEvent = MutableSharedFlow<RegisterEvent>()
    val registerEvent: SharedFlow<RegisterEvent> = _registerEvent.asSharedFlow()

    fun onFirstNameChanged(firstName: String) {
        _uiState.update { it.copy(firstName = firstName, firstNameError = null) }
    }

    fun onLastNameChanged(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, lastNameError = null) }
    }

    fun onGenderChanged(gender: String) {
        _uiState.update { it.copy(gender = gender, genderError = null) }
    }

    fun onProvinceChanged(province: String) {
        _uiState.update { it.copy(province = province, provinceError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
        validatePasswordStrength(password)
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun onAgreeToTermsChanged(agreeToTerms: Boolean) {
        _uiState.update { it.copy(agreeToTerms = agreeToTerms) }
    }

    fun register() {
        if (!validateInputs()) return

        viewModelScope.launch {
            authRepository.register(
                firstName = _uiState.value.firstName,
                lastName = _uiState.value.lastName,
                email = _uiState.value.email,
                password = _uiState.value.password,
                passwordConfirm = _uiState.value.confirmPassword,
                gender = _uiState.value.gender,
                province = _uiState.value.province
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
                                generalError = resource.message ?: "Registration failed"
                            )
                        }

                        // Check for specific error types
                        when {
                            resource.message?.contains("email", ignoreCase = true) == true -> {
                                if (resource.message.contains("already", ignoreCase = true)) {
                                    _uiState.update {
                                        it.copy(emailError = "Email already registered")
                                    }
                                }
                            }
                            resource.message?.contains("network", ignoreCase = true) == true -> {
                                _registerEvent.emit(RegisterEvent.NetworkError)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val firstName = _uiState.value.firstName
        val lastName = _uiState.value.lastName
        val email = _uiState.value.email
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword
        val gender = _uiState.value.gender
        val province = _uiState.value.province
        val agreeToTerms = _uiState.value.agreeToTerms
        var isValid = true

        // Validate first name
        if (firstName.isBlank()) {
            _uiState.update { it.copy(firstNameError = "First name is required") }
            isValid = false
        }

        // Validate last name
        if (lastName.isBlank()) {
            _uiState.update { it.copy(lastNameError = "Last name is required") }
            isValid = false
        }

        // Validate gender
        if (gender.isBlank()) {
            _uiState.update { it.copy(genderError = "Gender is required") }
            isValid = false
        }

        // Validate province
        if (province.isBlank()) {
            _uiState.update { it.copy(provinceError = "Province is required") }
            isValid = false
        }

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
        } else if (password.length < 8) {
            _uiState.update { it.copy(passwordError = "Password must be at least 8 characters") }
            isValid = false
        }

        // Validate confirm password
        if (confirmPassword.isBlank()) {
            _uiState.update { it.copy(confirmPasswordError = "Please confirm your password") }
            isValid = false
        } else if (password != confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }

        // Validate terms agreement
        if (!agreeToTerms) {
            _uiState.update { it.copy(generalError = "Please agree to the terms and conditions") }
            isValid = false
        }

        return isValid
    }

    private fun validatePasswordStrength(password: String) {
        val strength = when {
            password.length < 8 -> PasswordStrength.WEAK
            password.length < 12 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() } -> PasswordStrength.MEDIUM
            password.length >= 12 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() } -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
        _uiState.update { it.copy(passwordStrength = strength) }
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
                            _registerEvent.emit(RegisterEvent.NavigateToHome)
                        } else {
                            _registerEvent.emit(RegisterEvent.NavigateToGroupSelection)
                        }
                    }
                    is Resource.Error -> {
                        // If we can't check, default to home
                        _registerEvent.emit(RegisterEvent.NavigateToHome)
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
 * UI State for the Register screen
 */
data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val gender: String = "",
    val province: String = "",
    val agreeToTerms: Boolean = false,
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val genderError: String? = null,
    val provinceError: String? = null,
    val generalError: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK
)

/**
 * Password strength levels
 */
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

/**
 * Register events
 */
sealed class RegisterEvent {
    object Success : RegisterEvent()
    object NavigateToHome : RegisterEvent()
    object NavigateToGroupSelection : RegisterEvent()
    object NetworkError : RegisterEvent()
}
