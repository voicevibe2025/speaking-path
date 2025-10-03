package com.example.voicevibe.presentation.screens.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.ai.AiChatPrewarmManager
import com.example.voicevibe.data.repository.UserRepository
import com.example.voicevibe.data.repository.LearningPathRepository
import com.example.voicevibe.data.repository.GamificationRepository
import com.example.voicevibe.data.repository.SocialRepository
import com.example.voicevibe.data.repository.ProfileRepository
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.LearningPath
import com.example.voicevibe.domain.model.Post
import com.example.voicevibe.domain.model.PostComment
import com.example.voicevibe.domain.model.UserProgress
import com.example.voicevibe.domain.model.SocialNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * ViewModel for the Home Dashboard screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prewarmManager: AiChatPrewarmManager,
    private val userRepository: UserRepository,
    private val learningPathRepository: LearningPathRepository,
    private val gamificationRepository: GamificationRepository,
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    private val messagingRepository: com.example.voicevibe.data.repository.MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        loadUserProfileData()
        loadDashboardData()
        loadPosts()
        loadUnreadNotificationsCount()
        loadUnreadMessagesCount()
        // Pre-warm Vivi greeting in background so Free Practice opens instantly
        try {
            prewarmManager.prewarm()
        } catch (_: Throwable) { /* best-effort */ }
    }

    // Social posts
    fun loadPosts() {
        viewModelScope.launch {
            socialRepository.getPosts().collect { res ->
                when (res) {
                    is com.example.voicevibe.domain.model.Resource.Success -> {
                        _uiState.update { it.copy(posts = res.data ?: emptyList()) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun createTextPost(text: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            when (val res = socialRepository.createTextPost(text)) {
                is com.example.voicevibe.domain.model.Resource.Success -> {
                    res.data?.let { p -> _uiState.update { it.copy(posts = listOf(p) + it.posts) } }
                    onDone?.invoke()
                }
                else -> onDone?.invoke()
            }
        }
    }

    fun createLinkPost(link: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            when (val res = socialRepository.createLinkPost(link)) {
                is com.example.voicevibe.domain.model.Resource.Success -> {
                    res.data?.let { p -> _uiState.update { it.copy(posts = listOf(p) + it.posts) } }
                    onDone?.invoke()
                }
                else -> onDone?.invoke()
            }
        }
    }

    fun createImagePost(part: okhttp3.MultipartBody.Part, text: String? = null, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            when (val res = socialRepository.createImagePost(part, text)) {
                is com.example.voicevibe.domain.model.Resource.Success -> {
                    res.data?.let { p -> _uiState.update { it.copy(posts = listOf(p) + it.posts) } }
                    onDone?.invoke()
                }
                else -> onDone?.invoke()
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            val current = _uiState.value.posts
            socialRepository.likePost(postId)
            _uiState.update { state ->
                state.copy(posts = current.map { p -> if (p.id == postId) p.copy(likesCount = p.likesCount + (if (!p.isLikedByMe) 1 else 0), isLikedByMe = true) else p })
            }
        }
    }

    fun unlikePost(postId: Int) {
        viewModelScope.launch {
            val current = _uiState.value.posts
            socialRepository.unlikePost(postId)
            _uiState.update { state ->
                state.copy(posts = current.map { p -> if (p.id == postId) p.copy(likesCount = (if (p.isLikedByMe) p.likesCount - 1 else p.likesCount).coerceAtLeast(0), isLikedByMe = false) else p })
            }
        }
    }

    fun addComment(postId: Int, text: String, onDone: (() -> Unit)? = null) {
        addComment(postId, text, parentCommentId = null, onDone = onDone)
    }

    fun addComment(postId: Int, text: String, parentCommentId: Int?, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val res = socialRepository.addComment(postId, text, parent = parentCommentId)
            if (res is com.example.voicevibe.domain.model.Resource.Success) {
                val current = _uiState.value.posts
                _uiState.update { state ->
                    state.copy(posts = current.map { p -> if (p.id == postId) p.copy(commentsCount = p.commentsCount + 1) else p })
                }
            }
            onDone?.invoke()
        }
    }

    // Load comments for a post and deliver via callback
    fun fetchComments(postId: Int, onResult: (List<PostComment>) -> Unit) {
        viewModelScope.launch {
            val res = socialRepository.getComments(postId)
            if (res is com.example.voicevibe.domain.model.Resource.Success) {
                val list = res.data ?: emptyList()
                onResult(list.sortedByDescending { it.createdAt })
            } else {
                onResult(emptyList())
            }
        }
    }

    fun likeComment(commentId: Int) {
        viewModelScope.launch {
            socialRepository.likeComment(commentId)
        }
    }

    fun unlikeComment(commentId: Int) {
        viewModelScope.launch {
            socialRepository.unlikeComment(commentId)
        }
    }

    fun deletePost(postId: Int, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            // Optimistic removal
            val previous = _uiState.value.posts
            val updated = previous.filterNot { it.id == postId }
            _uiState.update { it.copy(posts = updated) }

            val result = socialRepository.deletePost(postId)
            val success = result is com.example.voicevibe.domain.model.Resource.Success
            if (!success) {
                // Restore on failure
                _uiState.update { it.copy(posts = previous) }
            }
            onDone?.let { it(success) }
        }
    }

    fun deleteComment(commentId: Int, postId: Int, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = socialRepository.deleteComment(commentId)
            val success = result is com.example.voicevibe.domain.model.Resource.Success
            // Optimistically decrement comments count for the post
            if (success) {
                _uiState.update { state ->
                    state.copy(posts = state.posts.map { p ->
                        if (p.id == postId) p.copy(commentsCount = (p.commentsCount - 1).coerceAtLeast(0)) else p
                    })
                }
            }
            onDone?.let { it(success) }
        }
    }

    // Notifications
    fun loadUnreadNotificationsCount() {
        viewModelScope.launch {
            when (val res = socialRepository.getUnreadNotificationCount()) {
                is Resource.Success -> _uiState.update { it.copy(unreadNotifications = res.data ?: 0) }
                else -> {}
            }
        }
    }

    // Messages
    fun loadUnreadMessagesCount() {
        viewModelScope.launch {
            when (val res = messagingRepository.getUnreadMessagesCount()) {
                is Resource.Success -> _uiState.update { it.copy(unreadMessages = res.data ?: 0) }
                else -> {}
            }
        }
    }

    fun loadNotifications(limit: Int? = 50, unreadOnly: Boolean? = null) {
        viewModelScope.launch {
            socialRepository.getNotifications(limit = limit, unread = unreadOnly).collect { res ->
                when (res) {
                    is Resource.Success -> _uiState.update { it.copy(notifications = res.data ?: emptyList()) }
                    else -> {}
                }
            }
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            socialRepository.markNotificationRead(id)
            _uiState.update { state ->
                val updated = state.notifications.map { if (it.id == id) it.copy(read = true) else it }
                val newCount = (state.unreadNotifications - 1).coerceAtLeast(0)
                state.copy(notifications = updated, unreadNotifications = newCount)
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            socialRepository.markAllNotificationsRead()
            _uiState.update { it.copy(unreadNotifications = 0, notifications = it.notifications.map { n -> n.copy(read = true) }) }
        }
    }

    fun ensurePostLoaded(postId: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val exists = _uiState.value.posts.any { it.id == postId }
            if (!exists) {
                val res = socialRepository.getPost(postId)
                if (res is Resource.Success) {
                    res.data?.let { post ->
                        _uiState.update { it.copy(posts = listOf(post) + it.posts) }
                    }
                }
            }
            onDone?.invoke()
        }
    }

    private fun loadUserProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userProfile = profileRepository.getProfile()
                
                // Normalize avatar URL if relative
                val avatarUrl = userProfile.avatarUrl?.let { normalizeUrl(it) }

                // Generate display name
                val displayName = if (!userProfile.firstName.isNullOrBlank() && !userProfile.lastName.isNullOrBlank()) {
                    "${userProfile.firstName} ${userProfile.lastName}"
                } else {
                    userProfile.userName
                }

                // Generate user initials
                val userInitials = generateInitials(displayName)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = displayName,
                        userLevel = userProfile.currentLevel ?: 1,
                        userInitials = userInitials,
                        avatarUrl = avatarUrl,
                        totalPoints = userProfile.totalPointsEarned ?: (userProfile.experiencePoints ?: 0),
                        currentStreak = userProfile.streakDays ?: 0,
                        // Completed should reflect Speaking Journey topics completed from backend profile
                        completedLessons = userProfile.lessonsCompleted ?: 0
                    )
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Network error. Please check your connection.",
                        userName = "Network Error",
                        userLevel = 1,
                        userInitials = "NE"
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load profile. Please try again.",
                        userName = "Error Loading",
                        userLevel = 1,
                        userInitials = "ER"
                    )
                }
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        // Constants.BASE_URL is like http://host:port/api/v1/
        val serverBase = com.example.voicevibe.utils.Constants.BASE_URL.substringBefore("/api/").trimEnd('/')
        val path = if (url.startsWith("/")) url else "/$url"
        return serverBase + path
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

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load learning paths
            learningPathRepository.getUserLearningPaths().collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                activeLearningPaths = resource.data?.filter { path ->
                                    path.progress < 100
                                } ?: emptyList()
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Skipping user progress API for now: backend exposes users/stats/ which doesn't match domain model.
        // Points and streak are loaded from ProfileRepository in loadUserProfileData().

        // Note: totalPoints and currentStreak are now loaded from ProfileRepository
        // in loadUserProfileData() method above
    }

    fun onStartPractice() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToPractice)
        }
    }

    fun onContinueLearning(pathId: String) {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToLearningPath(pathId))
        }
    }

    fun onViewAllPaths() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToLearningPaths)
        }
    }

    fun onViewAchievements() {
        viewModelScope.launch {
            _events.emit(HomeEvent.NavigateToAchievements)
        }
    }

    fun refresh() {
        loadUserProfileData()
        loadDashboardData()
        loadPosts()
        loadUnreadNotificationsCount()
        loadUnreadMessagesCount()
    }
}

/**
 * UI State for the Home Dashboard
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userLevel: Int = 1,
    val userInitials: String? = null,
    val avatarUrl: String? = null,
    val userProgress: UserProgress? = null,
    val activeLearningPaths: List<LearningPath> = emptyList(),
    val completedLessons: Int = 0,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val error: String? = null,
    val posts: List<Post> = emptyList(),
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val notifications: List<SocialNotification> = emptyList(),
)

/**
 * Events from Home Dashboard
 */
sealed class HomeEvent {
    object NavigateToPractice : HomeEvent()
    object NavigateToLearningPaths : HomeEvent()
    object NavigateToAchievements : HomeEvent()
    data class NavigateToLearningPath(val pathId: String) : HomeEvent()
}
