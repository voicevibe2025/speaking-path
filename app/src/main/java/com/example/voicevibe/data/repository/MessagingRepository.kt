package com.example.voicevibe.data.repository

import com.example.voicevibe.data.mapper.toDomain
import com.example.voicevibe.data.model.SendMessageRequest
import com.example.voicevibe.data.remote.api.MessagingApiService
import com.example.voicevibe.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for messaging operations.
 */
@Singleton
class MessagingRepository @Inject constructor(
    private val api: MessagingApiService
) {

    /**
     * Get all conversations for the current user.
     */
    fun getConversations(): Flow<Resource<List<Conversation>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getConversations()
            if (response.isSuccessful) {
                val conversations = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(Resource.Success(conversations))
            } else {
                emit(Resource.Error("Failed to load conversations: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get a specific conversation with all messages.
     */
    fun getConversationDetail(conversationId: Int): Flow<Resource<ConversationDetail>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getConversationDetail(conversationId)
            if (response.isSuccessful) {
                val conversation = response.body()?.toDomain()
                if (conversation != null) {
                    emit(Resource.Success(conversation))
                } else {
                    emit(Resource.Error("Conversation not found"))
                }
            } else {
                emit(Resource.Error("Failed to load conversation: ${response.message()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Get or create a conversation with a specific user.
     */
    suspend fun getOrCreateConversationWithUser(userId: Int): Resource<ConversationDetail> {
        return try {
            val response = api.getOrCreateConversationWithUser(userId)
            if (response.isSuccessful) {
                val conversation = response.body()?.toDomain()
                if (conversation != null) {
                    Resource.Success(conversation)
                } else {
                    Resource.Error("Failed to create conversation")
                }
            } else {
                Resource.Error("Failed to create conversation: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Send a message to a user.
     */
    suspend fun sendMessage(recipientId: Int, text: String): Resource<Message> {
        return try {
            val request = SendMessageRequest(recipientId = recipientId, text = text)
            val response = api.sendMessage(request)
            if (response.isSuccessful) {
                val message = response.body()?.toDomain()
                if (message != null) {
                    Resource.Success(message)
                } else {
                    Resource.Error("Failed to send message")
                }
            } else {
                Resource.Error("Failed to send message: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get unread message count.
     */
    suspend fun getUnreadMessagesCount(): Resource<Int> {
        return try {
            val response = api.getUnreadMessagesCount()
            if (response.isSuccessful) {
                val count = response.body()?.unreadCount ?: 0
                Resource.Success(count)
            } else {
                Resource.Error("Failed to get unread count: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Mark conversation as read.
     */
    suspend fun markConversationAsRead(conversationId: Int): Resource<Unit> {
        return try {
            val response = api.markConversationAsRead(conversationId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to mark as read: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Delete a conversation.
     */
    suspend fun deleteConversation(conversationId: Int): Resource<Unit> {
        return try {
            val response = api.deleteConversation(conversationId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to delete conversation: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}
