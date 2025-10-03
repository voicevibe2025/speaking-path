package com.example.voicevibe.presentation.screens.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.MessagingRepository
import com.example.voicevibe.domain.model.Conversation
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the messaging list screen.
 */
data class MessagingUiState(
    val conversations: List<Conversation> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Events emitted by the messaging screen.
 */
sealed class MessagingEvent {
    data class NavigateToConversation(val conversationId: Int, val otherUserId: Int, val otherUserName: String) : MessagingEvent()
    data class ShowMessage(val message: String) : MessagingEvent()
}

/**
 * ViewModel for the Messaging (conversations list) screen.
 */
@HiltViewModel
class MessagingViewModel @Inject constructor(
    private val repository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MessagingEvent>()
    val events: SharedFlow<MessagingEvent> = _events.asSharedFlow()

    init {
        loadConversations()
        loadUnreadCount()
    }

    fun loadConversations() {
        viewModelScope.launch {
            repository.getConversations().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                conversations = result.data ?: emptyList(),
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            when (val result = repository.getUnreadMessagesCount()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(unreadCount = result.data ?: 0) }
                }
                is Resource.Error -> {
                    // Silently fail for unread count
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun onConversationClick(conversation: Conversation) {
        viewModelScope.launch {
            _events.emit(
                MessagingEvent.NavigateToConversation(
                    conversationId = conversation.id,
                    otherUserId = conversation.otherUser.id,
                    otherUserName = conversation.otherUser.displayName
                )
            )
        }
    }

    fun deleteConversation(conversationId: Int) {
        viewModelScope.launch {
            when (repository.deleteConversation(conversationId)) {
                is Resource.Success -> {
                    // Reload conversations after deletion
                    loadConversations()
                    _events.emit(MessagingEvent.ShowMessage("Conversation deleted"))
                }
                is Resource.Error -> {
                    _events.emit(MessagingEvent.ShowMessage("Failed to delete conversation"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun refresh() {
        loadConversations()
        loadUnreadCount()
    }
}
