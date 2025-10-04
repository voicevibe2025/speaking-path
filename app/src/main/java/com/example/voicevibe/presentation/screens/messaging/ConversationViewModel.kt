package com.example.voicevibe.presentation.screens.messaging

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.repository.MessagingRepository
import com.example.voicevibe.data.websocket.ChatWebSocketClient
import com.example.voicevibe.data.websocket.ConnectionState
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
    val error: String? = null,
    val isConnected: Boolean = false,
    val otherUserTyping: Boolean = false
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
    private val webSocketClient: ChatWebSocketClient,
    private val tokenManager: TokenManager,
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
            connectToWebSocket(conversationId)
        } else if (otherUserId != null) {
            // Create new conversation with user
            createConversationWithUser(otherUserId)
        }
        
        // Observe WebSocket connection state
        observeWebSocketState()
        
        // Observe incoming messages
        observeIncomingMessages()
        
        // Observe typing indicators
        observeTypingIndicators()
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
    
    private fun connectToWebSocket(convId: Int) {
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()
            if (!token.isNullOrEmpty()) {
                webSocketClient.connect(convId, token)
            }
        }
    }
    
    private fun observeWebSocketState() {
        viewModelScope.launch {
            webSocketClient.connectionState.collect { state ->
                _uiState.update { 
                    it.copy(isConnected = state is ConnectionState.Connected) 
                }
            }
        }
    }
    
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            webSocketClient.messages.collect { message ->
                // Add message to list if not already present
                _uiState.update { state ->
                    if (state.messages.none { it.id == message.id }) {
                        state.copy(messages = state.messages + message)
                    } else {
                        state
                    }
                }
            }
        }
    }
    
    private fun observeTypingIndicators() {
        viewModelScope.launch {
            webSocketClient.typingUsers.collect { typingEvent ->
                _uiState.update { 
                    it.copy(otherUserTyping = typingEvent.isTyping) 
                }
            }
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
                    
                    // Connect to WebSocket for new conversation
                    conversation?.id?.let { convId ->
                        connectToWebSocket(convId)
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
        
        // Send typing indicator when user starts/stops typing
        val isTyping = text.isNotEmpty()
        if (_uiState.value.isConnected) {
            webSocketClient.sendTypingIndicator(isTyping)
        }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.messageText.trim()

        if (text.isEmpty()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            
            if (currentState.isConnected) {
                // Send via WebSocket for instant delivery
                webSocketClient.sendMessage(text)
                
                // Clear message text immediately for better UX
                _uiState.update {
                    it.copy(
                        messageText = "",
                        isSending = false
                    )
                }
                
                _events.emit(ConversationEvent.MessageSent)
            } else {
                // Fallback to HTTP if WebSocket is not connected
                val recipientId = currentState.otherUser?.id
                if (recipientId == null) {
                    _uiState.update { it.copy(isSending = false) }
                    return@launch
                }
                
                when (val result = repository.sendMessage(recipientId, text)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                messageText = "",
                                isSending = false
                            )
                        }
                        
                        result.data?.let { newMessage ->
                            _uiState.update {
                                it.copy(messages = it.messages + newMessage)
                            }
                        }
                        
                        _events.emit(ConversationEvent.MessageSent)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isSending = false) }
                        _events.emit(ConversationEvent.ShowMessage(result.message ?: "Failed to send message"))
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun markAsRead() {
        val conversationId = _uiState.value.conversationId ?: return
        
        viewModelScope.launch {
            if (_uiState.value.isConnected) {
                // Use WebSocket for instant marking
                webSocketClient.markAsRead()
            } else {
                // Fallback to HTTP
                repository.markConversationAsRead(conversationId)
            }
        }
    }

    fun refresh() {
        val id = _uiState.value.conversationId
        if (id != null) {
            loadConversation(id)
        }
    }
}
