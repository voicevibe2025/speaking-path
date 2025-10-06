package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * Domain model for a message in a conversation.
 */
data class Message(
    val id: Int,
    val text: String,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val createdAt: LocalDateTime,
    val readAt: LocalDateTime?,
    val isRead: Boolean
)

/**
 * Domain model for a conversation preview.
 */
data class Conversation(
    val id: Int,
    val otherUser: ConversationUser,
    val lastMessage: Message?,
    val unreadCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Domain model for user info in conversation context.
 */
data class ConversationUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val isOnline: Boolean = false
)

/**
 * Domain model for a detailed conversation with all messages.
 */
data class ConversationDetail(
    val id: Int,
    val otherUser: ConversationUser,
    val messages: List<Message>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
