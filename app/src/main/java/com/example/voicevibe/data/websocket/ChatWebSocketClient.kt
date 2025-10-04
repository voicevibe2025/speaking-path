package com.example.voicevibe.data.websocket

import com.example.voicevibe.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.Gson
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket client for real-time chat messaging.
 */
@Singleton
class ChatWebSocketClient @Inject constructor(
    private val gson: Gson
) {
    private val TAG = "ChatWebSocketClient"
    
    private var webSocket: WebSocket? = null
    private var currentConversationId: Int? = null
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Incoming messages
    private val _messages = MutableSharedFlow<Message>()
    val messages: Flow<Message> = _messages.asSharedFlow()
    
    // Typing indicators
    private val _typingUsers = MutableSharedFlow<TypingEvent>()
    val typingUsers: Flow<TypingEvent> = _typingUsers.asSharedFlow()
    
    // Errors
    private val _errors = MutableSharedFlow<String>()
    val errors: Flow<String> = _errors.asSharedFlow()
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Connect to a conversation's WebSocket.
     */
    fun connect(conversationId: Int, token: String) {
        if (currentConversationId == conversationId && webSocket != null) {
            Log.d(TAG, "Already connected to conversation $conversationId")
            return
        }
        
        disconnect()
        
        currentConversationId = conversationId
        _connectionState.value = ConnectionState.Connecting
        
        // Build WebSocket URL with JWT token
        val wsUrl = "ws://10.0.2.2:8000/ws/messaging/conversation/$conversationId/?token=$token"
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = client.newWebSocket(request, createWebSocketListener())
    }
    
    /**
     * Disconnect from current WebSocket.
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        currentConversationId = null
        _connectionState.value = ConnectionState.Disconnected
    }
    
    /**
     * Send a text message.
     */
    fun sendMessage(text: String) {
        val payload = mapOf(
            "type" to "message",
            "text" to text
        )
        sendJson(payload)
    }
    
    /**
     * Send typing indicator.
     */
    fun sendTypingIndicator(isTyping: Boolean) {
        val payload = mapOf(
            "type" to "typing",
            "isTyping" to isTyping
        )
        sendJson(payload)
    }
    
    /**
     * Mark conversation as read.
     */
    fun markAsRead() {
        val payload = mapOf("type" to "mark_read")
        sendJson(payload)
    }
    
    private fun sendJson(payload: Map<String, Any>) {
        val json = gson.toJson(payload)
        val sent = webSocket?.send(json) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send message: $json")
        }
    }
    
    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            _connectionState.value = ConnectionState.Connected
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            try {
                val envelope = gson.fromJson(text, WebSocketEnvelope::class.java)
                when (envelope.type) {
                    "message" -> {
                        val message = gson.fromJson(
                            gson.toJson(envelope.message),
                            WebSocketMessage::class.java
                        )
                        _messages.tryEmit(message.toDomain())
                    }
                    "typing" -> {
                        val typingEvent = TypingEvent(
                            userId = envelope.userId ?: 0,
                            isTyping = envelope.isTyping ?: false
                        )
                        _typingUsers.tryEmit(typingEvent)
                    }
                    "error" -> {
                        val errorMsg = envelope.message as? String ?: "Unknown error"
                        _errors.tryEmit(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing WebSocket message", e)
                _errors.tryEmit("Failed to parse message: ${e.message}")
            }
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            _connectionState.value = ConnectionState.Disconnected
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            _connectionState.value = ConnectionState.Disconnected
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            _connectionState.value = ConnectionState.Error(t.message ?: "Connection failed")
            _errors.tryEmit(t.message ?: "Connection failed")
        }
    }
}

/**
 * Connection state sealed class.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Typing event data class.
 */
data class TypingEvent(
    val userId: Int,
    val isTyping: Boolean
)

/**
 * WebSocket envelope for incoming messages.
 */
private data class WebSocketEnvelope(
    val type: String,
    val message: Any? = null,
    val userId: Int? = null,
    val isTyping: Boolean? = null
)

/**
 * WebSocket message DTO.
 */
private data class WebSocketMessage(
    val id: Int,
    val text: String,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val createdAt: String,
    val readAt: String?,
    val isRead: Boolean
) {
    fun toDomain() = Message(
        id = id,
        text = text,
        senderId = senderId,
        senderName = senderName,
        senderAvatar = senderAvatar,
        createdAt = parseDateTime(createdAt),
        readAt = readAt?.let { parseDateTime(it) },
        isRead = isRead
    )
    
    private fun parseDateTime(dateString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
    }
}
