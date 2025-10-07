package com.example.voicevibe.presentation.screens.profile

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.AuthRepository
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository
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

    // Avatar state
    private val _avatarUrl = mutableStateOf<String?>(null)
    val avatarUrl: State<String?> = _avatarUrl

    private val _isUploadingAvatar = mutableStateOf(false)
    val isUploadingAvatar: State<Boolean> = _isUploadingAvatar

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    // Blocked users state
    private val _blockedUsers = mutableStateOf<List<com.example.voicevibe.data.remote.api.BlockedUser>>(emptyList())
    val blockedUsers: State<List<com.example.voicevibe.data.remote.api.BlockedUser>> = _blockedUsers
    private val _blockedUsersLoading = mutableStateOf(false)
    val blockedUsersLoading: State<Boolean> = _blockedUsersLoading
    private val _blockedUsersError = mutableStateOf<String?>(null)
    val blockedUsersError: State<String?> = _blockedUsersError

    // Feature flags / preferences
    private val _speakingOnlyEnabled = mutableStateOf(true)
    val speakingOnlyEnabled: State<Boolean> = _speakingOnlyEnabled

    private val _ttsVoiceId = mutableStateOf<String?>(null)
    val ttsVoiceId: State<String?> = _ttsVoiceId

    // Learning preference: voice accent
    private val _voiceAccent = mutableStateOf<String?>(null)
    val voiceAccent: State<String?> = _voiceAccent

    // Account preferences
    private val _showEmailOnProfile = mutableStateOf(true)
    val showEmailOnProfile: State<Boolean> = _showEmailOnProfile

    // About Me / Bio
    private val _aboutMe = mutableStateOf("")
    val aboutMe: State<String> = _aboutMe

    // Privacy settings
    private val _privacyLoading = mutableStateOf(false)
    val privacyLoading: State<Boolean> = _privacyLoading

    private val _privacyError = mutableStateOf<String?>(null)
    val privacyError: State<String?> = _privacyError

    private val _hideAvatar = mutableStateOf(false)
    val hideAvatar: State<Boolean> = _hideAvatar

    private val _hideOnlineStatus = mutableStateOf(false)
    val hideOnlineStatus: State<Boolean> = _hideOnlineStatus

    private val _allowMessagesFromStrangers = mutableStateOf(true)
    val allowMessagesFromStrangers: State<Boolean> = _allowMessagesFromStrangers

    private val _isSavingPrivacy = mutableStateOf(false)
    val isSavingPrivacy: State<Boolean> = _isSavingPrivacy

    init {
        fetchUserProfile()
        observeSettings()
        // Ensure preference is persisted as ON while lock is active
        if (com.example.voicevibe.utils.Constants.LOCK_SPEAKING_ONLY_ON) {
            viewModelScope.launch {
                tokenManager.setSpeakingOnlyFlowEnabled(true)
            }
        }
    }

    // Load blocked users from backend
    fun loadBlockedUsers() {
        viewModelScope.launch {
            _blockedUsersLoading.value = true
            _blockedUsersError.value = null
            when (val result = userRepository.getBlockedUsers()) {
                is Resource.Success -> {
                    _blockedUsers.value = result.data ?: emptyList()
                }
                is Resource.Error -> {
                    _blockedUsersError.value = result.message ?: "Failed to load blocked users"
                    _blockedUsers.value = emptyList()
                }
                else -> {}
            }
            _blockedUsersLoading.value = false
        }
    }

    // Unblock a specific user and update local list
    fun unblockUser(userId: Int, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            when (val result = userRepository.unblockUser(userId.toString())) {
                is Resource.Success -> {
                    _blockedUsers.value = _blockedUsers.value.filter { it.userId != userId }
                    onSuccess?.invoke()
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message ?: "Failed to unblock user"
                }
                else -> {}
            }
        }
    }

    private fun fetchUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userProfile = profileRepository.getProfile()
                
                // Set user name
                val displayName = when {
                    !userProfile.displayName.isNullOrBlank() -> userProfile.displayName
                    !userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank() ->
                        "${userProfile.firstName} ${userProfile.lastName}"
                    !userProfile.firstName.isNullOrBlank() -> userProfile.firstName
                    !userProfile.lastName.isNullOrBlank() -> userProfile.lastName
                    else -> userProfile.userName
                }
                _userName.value = displayName

                // Set user email
                _userEmail.value = userProfile.userEmail

                // Set membership status
                _membershipStatus.value = userProfile.membershipStatus ?: "New Member"

                // Generate user initials
                _userInitials.value = generateInitials(displayName)

                // Set avatar URL (normalize if relative)
                _avatarUrl.value = userProfile.avatarUrl?.let { normalizeUrl(it) }

                // Set bio
                _aboutMe.value = userProfile.bio ?: ""

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

    private fun observeSettings() {
        // Speaking-only feature flag
        viewModelScope.launch {
            tokenManager.speakingOnlyFlowEnabledFlow().collect { enabled ->
                _speakingOnlyEnabled.value = enabled
            }
        }
        // Preferred TTS voice id
        viewModelScope.launch {
            tokenManager.ttsVoiceIdFlow().collect { id ->
                _ttsVoiceId.value = id
            }
        }
        // Preferred accent
        viewModelScope.launch {
            tokenManager.voiceAccentFlow().collect { accent ->
                _voiceAccent.value = accent
            }
        }
        // Show email on profile/header
        viewModelScope.launch {
            tokenManager.showEmailOnProfileFlow().collect { show ->
                _showEmailOnProfile.value = show
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

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val serverBase = com.example.voicevibe.utils.Constants.BASE_URL.substringBefore("/api/").trimEnd('/')
        val path = if (url.startsWith("/")) url else "/$url"
        return serverBase + path
    }

    /**
     * Logs out the user by calling the repository. Tokens and session data are
     * cleared in the repository regardless of API response.
     */
    suspend fun logout() {
        authRepository.logout()
    }

    // UI actions for settings
    fun onToggleSpeakingOnly(enabled: Boolean) {
        viewModelScope.launch {
            if (com.example.voicevibe.utils.Constants.LOCK_SPEAKING_ONLY_ON) {
                // Ignore UI toggle when locked; force state to ON
                _speakingOnlyEnabled.value = true
                tokenManager.setSpeakingOnlyFlowEnabled(true)
            } else {
                tokenManager.setSpeakingOnlyFlowEnabled(enabled)
            }
        }
    }

    fun setPreferredTtsVoice(voiceId: String?) {
        viewModelScope.launch {
            tokenManager.setTtsVoiceId(voiceId)
        }
    }

    fun setVoiceAccent(accent: String?) {
        viewModelScope.launch {
            tokenManager.setVoiceAccent(accent)
        }
    }

    fun setShowEmailOnProfile(value: Boolean) {
        viewModelScope.launch {
            tokenManager.setShowEmailOnProfile(value)
        }
    }

    // region Privacy settings
    fun loadPrivacySettings() {
        viewModelScope.launch {
            _privacyLoading.value = true
            _privacyError.value = null
            when (val result = userRepository.getPrivacySettings()) {
                is Resource.Success -> {
                    val s = result.data
                    _hideAvatar.value = s?.hideAvatar ?: false
                    _hideOnlineStatus.value = s?.hideOnlineStatus ?: false
                    _allowMessagesFromStrangers.value = s?.allowMessagesFromStrangers ?: true
                }
                is Resource.Error -> {
                    _privacyError.value = result.message ?: "Failed to load privacy settings"
                }
                else -> {}
            }
            _privacyLoading.value = false
        }
    }

    fun savePrivacySettings(
        hideAvatar: Boolean = _hideAvatar.value,
        hideOnlineStatus: Boolean = _hideOnlineStatus.value,
        allowMessagesFromStrangers: Boolean = _allowMessagesFromStrangers.value,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSavingPrivacy.value = true
            _privacyError.value = null
            val payload = com.example.voicevibe.data.remote.api.PrivacySettings(
                hideAvatar = hideAvatar,
                hideOnlineStatus = hideOnlineStatus,
                allowMessagesFromStrangers = allowMessagesFromStrangers
            )
            when (val result = userRepository.updatePrivacySettings(payload)) {
                is Resource.Success -> {
                    val s = result.data
                    _hideAvatar.value = s?.hideAvatar ?: hideAvatar
                    _hideOnlineStatus.value = s?.hideOnlineStatus ?: hideOnlineStatus
                    _allowMessagesFromStrangers.value = s?.allowMessagesFromStrangers ?: allowMessagesFromStrangers
                    onSuccess?.invoke()
                }
                is Resource.Error -> {
                    _privacyError.value = result.message ?: "Failed to update privacy settings"
                }
                else -> {}
            }
            _isSavingPrivacy.value = false
        }
    }

    fun setHideAvatar(value: Boolean) { _hideAvatar.value = value }
    fun setHideOnlineStatus(value: Boolean) { _hideOnlineStatus.value = value }
    fun setAllowMessagesFromStrangers(value: Boolean) { _allowMessagesFromStrangers.value = value }
    // endregion

    // Avatar upload handler - accepts raw image bytes from UI layer
    fun uploadAvatar(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _errorMessage.value = ""
            when (val result = userRepository.uploadProfilePicture(imageBytes)) {
                is Resource.Success -> {
                    _avatarUrl.value = result.data
                    // Reload the full profile to ensure all data is in sync
                    fetchUserProfile()
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message ?: "Failed to upload avatar"
                }
                is Resource.Loading -> {
                    // no-op
                }
            }
            _isUploadingAvatar.value = false
        }
    }
    
    // Public method to refresh profile (can be called from UI on resume)
    fun refreshProfile() {
        fetchUserProfile()
    }
}
