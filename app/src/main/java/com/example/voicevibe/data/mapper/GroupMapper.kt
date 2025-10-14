package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.remote.api.GroupDto
import com.example.voicevibe.data.remote.api.GroupMemberDto
import com.example.voicevibe.data.remote.api.GroupMessageDto
import com.example.voicevibe.data.remote.api.MaterialDto
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.GroupMember
import com.example.voicevibe.domain.model.GroupMessage
import com.example.voicevibe.domain.model.Material
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Map GroupDto to domain Group
 */
fun GroupDto.toDomain(): Group {
    return Group(
        id = this.id,
        name = this.name,
        displayName = this.displayName,
        description = this.description,
        icon = this.icon,
        color = this.color,
        memberCount = this.memberCount,
        createdAt = parseToLocalDateTime(this.createdAt)
    )
}

/**
 * Map GroupMemberDto to domain GroupMember
 */
fun GroupMemberDto.toDomain(): GroupMember {
    return GroupMember(
        userId = this.userId,
        username = this.username,
        displayName = this.displayName,
        avatarUrl = this.avatarUrl,
        level = this.level,
        xp = this.xp,
        streakDays = this.streakDays
    )
}

/**
 * Map GroupMessageDto to domain GroupMessage
 */
fun GroupMessageDto.toDomain(): GroupMessage {
    return GroupMessage(
        id = this.id,
        groupId = this.groupId,
        groupName = this.groupName,
        senderId = this.senderId,
        senderName = this.senderName,
        senderAvatar = this.senderAvatar,
        message = this.message,
        timestamp = parseToLocalDateTime(this.timestamp)
    )
}

/**
 * Map MaterialDto to domain Material
 */
fun MaterialDto.toDomain(): Material {
    return Material(
        id = this.id,
        title = this.title,
        description = this.description,
        sequence = this.sequence,
        type = this.type
    )
}

private fun parseToLocalDateTime(input: String?): LocalDateTime {
    if (input.isNullOrBlank()) return LocalDateTime.now()
    return try {
        LocalDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        try {
            // Try alternate formats if needed
            LocalDateTime.parse(input)
        } catch (e2: Exception) {
            LocalDateTime.now()
        }
    }
}
