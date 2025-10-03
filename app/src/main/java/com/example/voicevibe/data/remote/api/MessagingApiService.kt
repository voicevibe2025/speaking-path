package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for messaging between users.
 */
interface MessagingApiService {

    /**
     * Get all conversations for the current user.
     */
    @GET("messaging/conversations/")
    suspend fun getConversations(): Response<List<ConversationDto>>

    /**
     * Get a specific conversation with all messages.
     */
    @GET("messaging/conversations/{id}/")
    suspend fun getConversationDetail(
        @Path("id") conversationId: Int
    ): Response<ConversationDetailDto>

    /**
     * Get or create a conversation with a specific user.
     */
    @GET("messaging/conversations/with-user/{userId}/")
    suspend fun getOrCreateConversationWithUser(
        @Path("userId") userId: Int
    ): Response<ConversationDetailDto>

    /**
     * Send a message.
     */
    @POST("messaging/messages/send/")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<MessageDto>

    /**
     * Get unread message count.
     */
    @GET("messaging/messages/unread-count/")
    suspend fun getUnreadMessagesCount(): Response<UnreadMessagesCountDto>

    /**
     * Mark conversation as read.
     */
    @POST("messaging/conversations/{id}/mark-read/")
    suspend fun markConversationAsRead(
        @Path("id") conversationId: Int
    ): Response<Map<String, Int>>

    /**
     * Delete a conversation.
     */
    @DELETE("messaging/conversations/{id}/delete/")
    suspend fun deleteConversation(
        @Path("id") conversationId: Int
    ): Response<Unit>
}
