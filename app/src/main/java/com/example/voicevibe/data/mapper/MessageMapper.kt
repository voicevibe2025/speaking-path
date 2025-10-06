package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.model.*
import com.example.voicevibe.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Maps MessageDto to domain Message.
 */
fun MessageDto.toDomain(): Message {
    return Message(
        id = this.id,
        text = this.text,
        senderId = this.senderId,
        senderName = this.senderName,
        senderAvatar = this.senderAvatar,
        createdAt = parseToLocalDateTime(this.createdAt),
        readAt = this.readAt?.let { parseToLocalDateTime(it) },
        isRead = this.isRead
    )
}

/**
 * Maps ConversationUserDto to domain ConversationUser.
 */
fun ConversationUserDto.toDomain(): ConversationUser {
    return ConversationUser(
        id = this.id,
        username = this.username,
        displayName = this.displayName,
        avatarUrl = this.avatarUrl,
        isOnline = this.isOnline == true
    )
}

/**
 * Maps ConversationDto to domain Conversation.
 */
fun ConversationDto.toDomain(): Conversation {
    return Conversation(
        id = this.id,
        otherUser = this.otherUser.toDomain(),
        lastMessage = this.lastMessage?.toDomain(),
        unreadCount = this.unreadCount,
        createdAt = parseToLocalDateTime(this.createdAt),
        updatedAt = parseToLocalDateTime(this.updatedAt)
    )
}

/**
 * Maps ConversationDetailDto to domain ConversationDetail.
 */
fun ConversationDetailDto.toDomain(): ConversationDetail {
    return ConversationDetail(
        id = this.id,
        otherUser = this.otherUser.toDomain(),
        messages = this.messages.map { it.toDomain() },
        createdAt = parseToLocalDateTime(this.createdAt),
        updatedAt = parseToLocalDateTime(this.updatedAt)
    )
}

/**
 * Parse ISO 8601 date string to LocalDateTime.
 */
private fun parseToLocalDateTime(input: String): LocalDateTime {
    return try {
        // Try ISO instant (e.g., 2025-09-22T13:00:00Z)
        val ins = Instant.parse(input)
        LocalDateTime.ofInstant(ins, ZoneOffset.UTC)
    } catch (_: Exception) {
        try {
            // Try offset datetime (e.g., 2025-09-22T13:00:00+00:00)
            val odt = OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            odt.toLocalDateTime()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
            } catch (_: Exception) {
                LocalDateTime.now()
            }
        }
    }
}
