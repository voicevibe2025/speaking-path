package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * DTO for message from API.
 */
data class MessageDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("text")
    val text: String,
    @SerializedName("senderId")
    val senderId: Int,
    @SerializedName("senderName")
    val senderName: String,
    @SerializedName("senderAvatar")
    val senderAvatar: String?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("readAt")
    val readAt: String?,
    @SerializedName("isRead")
    val isRead: Boolean
)

/**
 * DTO for conversation user.
 */
data class ConversationUserDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

/**
 * DTO for conversation preview.
 */
data class ConversationDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("otherUser")
    val otherUser: ConversationUserDto,
    @SerializedName("lastMessage")
    val lastMessage: MessageDto?,
    @SerializedName("unreadCount")
    val unreadCount: Int,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * DTO for conversation detail.
 */
data class ConversationDetailDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("otherUser")
    val otherUser: ConversationUserDto,
    @SerializedName("messages")
    val messages: List<MessageDto>,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * Request DTO for sending a message.
 */
data class SendMessageRequest(
    @SerializedName("recipientId")
    val recipientId: Int,
    @SerializedName("text")
    val text: String
)

/**
 * Response DTO for unread message count.
 */
data class UnreadMessagesCountDto(
    @SerializedName("unreadCount")
    val unreadCount: Int
)
