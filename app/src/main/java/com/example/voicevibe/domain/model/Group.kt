package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/**
 * Batam cultural group
 */
data class Group(
    val id: Int,
    val name: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val color: String,
    val memberCount: Int,
    val createdAt: LocalDateTime
)

/**
 * Group member (simplified user info)
 */
data class GroupMember(
    val userId: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val level: Int,
    val xp: Int,
    val streakDays: Int
)

/**
 * Group chat message
 */
data class GroupMessage(
    val id: Int,
    val groupId: Int,
    val groupName: String,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val timestamp: LocalDateTime
)

/**
 * Group status response
 */
data class GroupStatus(
    val hasGroup: Boolean,
    val group: Group?
)

/**
 * Group members response
 */
data class GroupMembersResponse(
    val group: Group,
    val members: List<GroupMember>,
    val totalMembers: Int
)

/**
 * Group messages response
 */
data class GroupMessagesResponse(
    val group: Group,
    val messages: List<GroupMessage>,
    val hasMore: Boolean
)
