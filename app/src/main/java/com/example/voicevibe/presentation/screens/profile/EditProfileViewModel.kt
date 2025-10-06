package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    // Current profile data
    private val _currentEmail = mutableStateOf("")
    val currentEmail: State<String> = _currentEmail

    private val _currentDisplayName = mutableStateOf("")
    val currentDisplayName: State<String> = _currentDisplayName

    // Email update state
    private val _newEmail = mutableStateOf("")
    val newEmail: State<String> = _newEmail

    private val _isUpdatingEmail = mutableStateOf(false)
    val isUpdatingEmail: State<Boolean> = _isUpdatingEmail

    private val _emailUpdateSuccess = mutableStateOf(false)
    val emailUpdateSuccess: State<Boolean> = _emailUpdateSuccess

    private val _emailUpdateError = mutableStateOf<String?>(null)
    val emailUpdateError: State<String?> = _emailUpdateError

    // Display name update state
    private val _newDisplayName = mutableStateOf("")
    val newDisplayName: State<String> = _newDisplayName

    private val _isUpdatingDisplayName = mutableStateOf(false)
    val isUpdatingDisplayName: State<Boolean> = _isUpdatingDisplayName

    private val _displayNameUpdateSuccess = mutableStateOf(false)
    val displayNameUpdateSuccess: State<Boolean> = _displayNameUpdateSuccess

    private val _displayNameUpdateError = mutableStateOf<String?>(null)
    val displayNameUpdateError: State<String?> = _displayNameUpdateError

    // Password update state
    private val _currentPassword = mutableStateOf("")
    val currentPassword: State<String> = _currentPassword

    private val _newPassword = mutableStateOf("")
    val newPassword: State<String> = _newPassword

    private val _confirmPassword = mutableStateOf("")
    val confirmPassword: State<String> = _confirmPassword

    private val _isUpdatingPassword = mutableStateOf(false)
    val isUpdatingPassword: State<Boolean> = _isUpdatingPassword

    private val _passwordUpdateSuccess = mutableStateOf(false)
    val passwordUpdateSuccess: State<Boolean> = _passwordUpdateSuccess

    private val _passwordUpdateError = mutableStateOf<String?>(null)
    val passwordUpdateError: State<String?> = _passwordUpdateError

    // About me update state
    private val _aboutMe = mutableStateOf("")
    val aboutMe: State<String> = _aboutMe

    private val _isUpdatingAboutMe = mutableStateOf(false)
    val isUpdatingAboutMe: State<Boolean> = _isUpdatingAboutMe

    private val _aboutMeUpdateSuccess = mutableStateOf(false)
    val aboutMeUpdateSuccess: State<Boolean> = _aboutMeUpdateSuccess

    private val _aboutMeUpdateError = mutableStateOf<String?>(null)
    val aboutMeUpdateError: State<String?> = _aboutMeUpdateError

    // Delete account state
    private val _isDeletingAccount = mutableStateOf(false)
    val isDeletingAccount: State<Boolean> = _isDeletingAccount

    private val _deleteAccountSuccess = mutableStateOf(false)
    val deleteAccountSuccess: State<Boolean> = _deleteAccountSuccess

    private val _deleteAccountError = mutableStateOf<String?>(null)
    val deleteAccountError: State<String?> = _deleteAccountError

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                _currentEmail.value = profile.userEmail
                _currentDisplayName.value = profile.displayName ?: ""
                // Note: bio field is not in the current UserProfile data model
                // It will be added when backend returns it
            } catch (e: Exception) {
                // Handle error silently, fields will remain empty
            }
        }
    }

    fun onEmailChanged(email: String) {
        _newEmail.value = email
        _emailUpdateError.value = null
        _emailUpdateSuccess.value = false
    }

    fun onDisplayNameChanged(name: String) {
        _newDisplayName.value = name
        _displayNameUpdateError.value = null
        _displayNameUpdateSuccess.value = false
    }

    fun onCurrentPasswordChanged(password: String) {
        _currentPassword.value = password
        _passwordUpdateError.value = null
        _passwordUpdateSuccess.value = false
    }

    fun onNewPasswordChanged(password: String) {
        _newPassword.value = password
        _passwordUpdateError.value = null
        _passwordUpdateSuccess.value = false
    }

    fun onConfirmPasswordChanged(password: String) {
        _confirmPassword.value = password
        _passwordUpdateError.value = null
        _passwordUpdateSuccess.value = false
    }

    fun updateEmail() {
        val email = _newEmail.value.trim()
        
        if (email.isEmpty()) {
            _emailUpdateError.value = "Please enter an email address"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailUpdateError.value = "Please enter a valid email address"
            return
        }

        if (email == _currentEmail.value) {
            _emailUpdateError.value = "This is already your current email"
            return
        }

        viewModelScope.launch {
            _isUpdatingEmail.value = true
            _emailUpdateError.value = null
            _emailUpdateSuccess.value = false

            val fields = mapOf("email" to email)
            when (val result = userRepository.updateProfileFields(fields)) {
                is Resource.Success -> {
                    _currentEmail.value = email
                    _newEmail.value = ""
                    _emailUpdateSuccess.value = true
                }
                is Resource.Error -> {
                    _emailUpdateError.value = result.message ?: "Failed to update email"
                }
                is Resource.Loading -> {}
            }
            _isUpdatingEmail.value = false
        }
    }

    fun updateDisplayName() {
        val name = _newDisplayName.value.trim()
        
        if (name.isEmpty()) {
            _displayNameUpdateError.value = "Please enter a display name"
            return
        }

        if (name == _currentDisplayName.value) {
            _displayNameUpdateError.value = "This is already your current display name"
            return
        }

        viewModelScope.launch {
            _isUpdatingDisplayName.value = true
            _displayNameUpdateError.value = null
            _displayNameUpdateSuccess.value = false

            val fields = mapOf("display_name" to name)
            when (val result = userRepository.updateProfileFields(fields)) {
                is Resource.Success -> {
                    _currentDisplayName.value = name
                    _newDisplayName.value = ""
                    _displayNameUpdateSuccess.value = true
                }
                is Resource.Error -> {
                    _displayNameUpdateError.value = result.message ?: "Failed to update display name"
                }
                is Resource.Loading -> {}
            }
            _isUpdatingDisplayName.value = false
        }
    }

    fun updatePassword() {
        val current = _currentPassword.value
        val new = _newPassword.value
        val confirm = _confirmPassword.value

        if (current.isEmpty()) {
            _passwordUpdateError.value = "Please enter your current password"
            return
        }

        if (new.isEmpty()) {
            _passwordUpdateError.value = "Please enter a new password"
            return
        }

        if (new.length < 6) {
            _passwordUpdateError.value = "Password must be at least 6 characters"
            return
        }

        if (new != confirm) {
            _passwordUpdateError.value = "Passwords do not match"
            return
        }

        if (current == new) {
            _passwordUpdateError.value = "New password must be different from current password"
            return
        }

        viewModelScope.launch {
            _isUpdatingPassword.value = true
            _passwordUpdateError.value = null
            _passwordUpdateSuccess.value = false

            when (val result = userRepository.changePassword(current, new)) {
                is Resource.Success -> {
                    _currentPassword.value = ""
                    _newPassword.value = ""
                    _confirmPassword.value = ""
                    _passwordUpdateSuccess.value = true
                }
                is Resource.Error -> {
                    _passwordUpdateError.value = result.message ?: "Failed to change password"
                }
                is Resource.Loading -> {}
            }
            _isUpdatingPassword.value = false
        }
    }

    fun clearEmailStatus() {
        _emailUpdateSuccess.value = false
        _emailUpdateError.value = null
    }

    fun clearDisplayNameStatus() {
        _displayNameUpdateSuccess.value = false
        _displayNameUpdateError.value = null
    }

    fun clearPasswordStatus() {
        _passwordUpdateSuccess.value = false
        _passwordUpdateError.value = null
    }

    fun onAboutMeChanged(text: String) {
        if (text.length <= 500) {
            _aboutMe.value = text
            _aboutMeUpdateError.value = null
            _aboutMeUpdateSuccess.value = false
        }
    }

    fun updateAboutMe() {
        val text = _aboutMe.value.trim()

        viewModelScope.launch {
            _isUpdatingAboutMe.value = true
            _aboutMeUpdateError.value = null
            _aboutMeUpdateSuccess.value = false

            val fields = mapOf("bio" to text)
            when (val result = userRepository.updateProfileFields(fields)) {
                is Resource.Success -> {
                    _aboutMeUpdateSuccess.value = true
                }
                is Resource.Error -> {
                    _aboutMeUpdateError.value = result.message ?: "Failed to update about me"
                }
                is Resource.Loading -> {}
            }
            _isUpdatingAboutMe.value = false
        }
    }

    fun clearAboutMeStatus() {
        _aboutMeUpdateSuccess.value = false
        _aboutMeUpdateError.value = null
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteAccountError.value = null
            _deleteAccountSuccess.value = false

            when (val result = userRepository.deleteAccount()) {
                is Resource.Success -> {
                    _deleteAccountSuccess.value = true
                    // Clear all local data/cache if needed
                }
                is Resource.Error -> {
                    _deleteAccountError.value = result.message ?: "Failed to delete account"
                }
                is Resource.Loading -> {}
            }
            _isDeletingAccount.value = false
        }
    }

    fun clearDeleteAccountStatus() {
        _deleteAccountSuccess.value = false
        _deleteAccountError.value = null
    }
}
