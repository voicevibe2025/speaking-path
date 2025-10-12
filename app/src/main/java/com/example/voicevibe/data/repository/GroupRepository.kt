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
}
