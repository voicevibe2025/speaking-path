package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.UserApiService
import com.example.voicevibe.data.remote.api.SendGroupMessageRequest
import com.example.voicevibe.data.mapper.toDomain
import com.example.voicevibe.domain.model.Group
import com.example.voicevibe.domain.model.GroupMember
import com.example.voicevibe.domain.model.GroupMessage
import com.example.voicevibe.domain.model.GroupStatus
import com.example.voicevibe.domain.model.Resource
import com.example.voicevibe.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing group data and operations
 */
@Singleton
class GroupRepository @Inject constructor(
    private val apiService: UserApiService
) {
    
    /**
     * Get all available groups
     */
    suspend fun getGroups(): Resource<List<Group>> {
        return try {
            val response = apiService.getGroups()
            if (response.isSuccessful) {
                val groups = response.body()?.map { it.toDomain() } ?: emptyList()
                Resource.Success(groups)
            } else {
                Resource.Error("Failed to load groups")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Check if current user has a group
     */
    suspend fun checkGroupStatus(): Resource<GroupStatus> {
        return try {
            val response = apiService.checkGroupStatus()
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val status = GroupStatus(
                        hasGroup = dto.hasGroup,
                        group = dto.group?.toDomain()
                    )
                    Resource.Success(status)
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to check group status")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Join a group (one-time selection)
     */
    suspend fun joinGroup(groupId: Int): Resource<UserProfile> {
        return try {
            val response = apiService.joinGroup(groupId)
            if (response.isSuccessful) {
                response.body()?.profile?.let { dataProfile ->
                    Resource.Success(dataProfile.toDomain())
                } ?: Resource.Error("Failed to join group")
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error(errorBody ?: "Failed to join group")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get members of current user's group
     */
    suspend fun getMyGroupMembers(): Resource<Pair<Group, List<GroupMember>>> {
        return try {
            val response = apiService.getMyGroupMembers()
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val group = dto.group.toDomain()
                    val members = dto.members.map { it.toDomain() }
                    Resource.Success(Pair(group, members))
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load group members")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get members of a specific group
     */
    suspend fun getGroupMembers(groupId: Int): Resource<Pair<Group, List<GroupMember>>> {
        return try {
            val response = apiService.getGroupMembers(groupId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val group = dto.group.toDomain()
                    val members = dto.members.map { it.toDomain() }
                    Resource.Success(Pair(group, members))
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load group members")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get messages from current user's group
     */
    suspend fun getMyGroupMessages(limit: Int = 50, offset: Int = 0): Resource<Triple<Group, List<GroupMessage>, Boolean>> {
        return try {
            val response = apiService.getMyGroupMessages(limit, offset)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val group = dto.group.toDomain()
                    val messages = dto.messages.map { it.toDomain() }
                    Resource.Success(Triple(group, messages, dto.hasMore))
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get messages from a specific group
     */
    suspend fun getGroupMessages(groupId: Int, limit: Int = 50, offset: Int = 0): Resource<Triple<Group, List<GroupMessage>, Boolean>> {
        return try {
            val response = apiService.getGroupMessages(groupId, limit, offset)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val group = dto.group.toDomain()
                    val messages = dto.messages.map { it.toDomain() }
                    Resource.Success(Triple(group, messages, dto.hasMore))
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load messages")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Send a message to current user's group
     */
    suspend fun sendMessage(message: String): Resource<GroupMessage> {
        return try {
            val request = SendGroupMessageRequest(message)
            val response = apiService.sendGroupMessage(request)
            if (response.isSuccessful) {
                response.body()?.message?.let { dto ->
                    Resource.Success(dto.toDomain())
                } ?: Resource.Error("Empty response")
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error(errorBody ?: "Failed to send message")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Delete a message from the group chat
     */
    suspend fun deleteMessage(messageId: Int): Resource<Boolean> {
        return try {
            // Prefer plain resource DELETE (/) first to avoid 404 from '/delete/'
            val plain = apiService.deleteGroupMessagePlain(messageId)
            if (plain.isSuccessful) {
                return Resource.Success(true)
            }

            // Fallback: try '/delete/' endpoint
            val response = apiService.deleteGroupMessage(messageId)
            if (response.isSuccessful) {
                return Resource.Success(response.body()?.success ?: true)
            }

            // Prefer friendly message for 404s
            if (plain.code() == 404 && response.code() == 404) {
                return Resource.Error("Server doesn't support deleting group messages yet")
            }

            val errorBody = plain.errorBody()?.string() ?: response.errorBody()?.string()
            Resource.Error(errorBody ?: "Failed to delete message")
        } catch (e: Exception) {
            // Final fallback: try plain endpoint inside catch in case primary threw
            return try {
                val fallback = apiService.deleteGroupMessagePlain(messageId)
                if (fallback.isSuccessful) Resource.Success(true)
                else {
                    if (fallback.code() == 404) Resource.Error("Server doesn't support deleting group messages yet")
                    else Resource.Error(fallback.errorBody()?.string() ?: "Failed to delete message")
                }
            } catch (e2: Exception) {
                Resource.Error(e2.message ?: "Unknown error occurred")
            }
        }
    }
}
