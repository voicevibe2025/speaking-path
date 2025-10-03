package com.example.voicevibe.presentation.screens.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.repository.MessagingRepository
import com.example.voicevibe.domain.model.ConversationUser
import com.example.voicevibe.domain.model.Message
import com.example.voicevibe.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the conversation screen.
 */
data class ConversationUiState(
    val conversationId: Int? = null,
    val otherUser: ConversationUser? = null,
    val messages: List<Message> = emptyList(),
    val messageText: String = "",
    val isSending: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Events emitted by the conversation screen.
 */
sealed class ConversationEvent {
    data class ShowMessage(val message: String) : ConversationEvent()
    object MessageSent : ConversationEvent()
    object NavigateBack : ConversationEvent()
}

/**
 * ViewModel for the Conversation (chat) screen.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val repository: MessagingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: Int? = savedStateHandle.get<String>("conversationId")?.toIntOrNull()
    private val otherUserId: Int? = savedStateHandle.get<String>("userId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConversationEvent>()
    val events: SharedFlow<ConversationEvent> = _events.asSharedFlow()

    init {
        if (conversationId != null) {
            loadConversation(conversationId)
        } else if (otherUserId != null) {
            // Create new conversation with user
            createConversationWithUser(otherUserId)
        }
    }

    private fun loadConversation(id: Int) {
        viewModelScope.launch {
            repository.getConversationDetail(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val conversation = result.data
                        _uiState.update {
                            it.copy(
                                conversationId = conversation?.id,
                                otherUser = conversation?.otherUser,
                                messages = conversation?.messages ?: emptyList(),
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

    private fun createConversationWithUser(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = repository.getOrCreateConversationWithUser(userId)) {
                is Resource.Success -> {
                    val conversation = result.data
                    _uiState.update {
                        it.copy(
                            conversationId = conversation?.id,
                            otherUser = conversation?.otherUser,
                            messages = conversation?.messages ?: emptyList(),
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
                    _events.emit(ConversationEvent.ShowMessage(result.message ?: "Failed to start conversation"))
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onMessageTextChanged(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.messageText.trim()
        val recipientId = currentState.otherUser?.id

        if (text.isEmpty() || recipientId == null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            when (val result = repository.sendMessage(recipientId, text)) {
                is Resource.Success -> {
                    // Clear message text
                    _uiState.update {
                        it.copy(
                            messageText = "",
                            isSending = false
                        )
                    }
                    
                    // Add new message to list optimistically
                    result.data?.let { newMessage ->
                        _uiState.update {
                            it.copy(messages = it.messages + newMessage)
                        }
                    }
                    
                    _events.emit(ConversationEvent.MessageSent)
                    
                    // Reload conversation to get any updates
                    currentState.conversationId?.let { loadConversation(it) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSending = false) }
                    _events.emit(ConversationEvent.ShowMessage(result.message ?: "Failed to send message"))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun markAsRead() {
        val conversationId = _uiState.value.conversationId ?: return
        
        viewModelScope.launch {
            repository.markConversationAsRead(conversationId)
        }
    }

    fun refresh() {
        val id = _uiState.value.conversationId
        if (id != null) {
            loadConversation(id)
        }
    }
}
