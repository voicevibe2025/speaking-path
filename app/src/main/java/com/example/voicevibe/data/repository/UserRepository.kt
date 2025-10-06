package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.UserApiService
import com.example.voicevibe.data.mapper.toData
import com.example.voicevibe.data.mapper.toDomain
import com.example.voicevibe.domain.model.UserProfile as DomainUserProfile
import com.example.voicevibe.domain.model.UserProgress
import com.example.voicevibe.domain.model.UserActivity
import com.example.voicevibe.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import com.example.voicevibe.utils.Constants
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user data
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: UserApiService
) {
    
    /**
     * Get current user data
     */
    fun getCurrentUser(): Flow<Resource<DomainUserProfile>> = flow {
        emit(Resource.Loading<DomainUserProfile>())
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    // Normalize avatar URL if present
                                        val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    emit(Resource.Success(domainProfile))
                } ?: emit(Resource.Error<DomainUserProfile>("Failed to load user data: empty body"))
            } else {
                emit(Resource.Error<DomainUserProfile>("Failed to load user data"))
            }
        } catch (e: Exception) {
            emit(Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred"))
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Resource<DomainUserProfile> {
        return try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    Resource.Success(domainProfile)
                } ?: Resource.Error<DomainUserProfile>("Failed to load user data: empty body")
            } else {
                Resource.Error<DomainUserProfile>("Failed to load user data")
            }
        } catch (e: Exception) {
            Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Search users by name or username
     */
    suspend fun searchUsers(query: String): Resource<List<DomainUserProfile>> {
        return try {
            val response = apiService.searchUsers(query)
            if (response.isSuccessful) {
                val list = response.body()?.map { data ->
                    data.toDomain().copy(
                        avatarUrl = data.avatarUrl?.let { normalizeUrl(it) }
                    )
                } ?: emptyList()
                Resource.Success(list)
            } else {
                Resource.Error("Failed to search users")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): String {
        // This would typically be stored in preferences or obtained from auth token
        return "current_user_id" // Mock implementation
    }

    /**
     * Get user activities
     */
    suspend fun getUserActivities(userId: String): Resource<List<UserActivity>> {
        // Mock implementation since API doesn't exist yet
        return try {
            // Return mock empty list for now
            Resource.Success(emptyList<UserActivity>())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Follow a user
     */
    suspend fun followUser(userId: String): Resource<Unit> {
        return try {
            val response = apiService.followUser(userId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to follow user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(userId: String): Resource<Unit> {
        return try {
            val response = apiService.unfollowUser(userId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to unfollow user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Block a user
     */
    suspend fun blockUser(userId: String): Resource<Unit> {
        return try {
            val response = apiService.blockUser(userId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to block user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Report a user
     */
    suspend fun reportUser(userId: String, reason: String): Resource<Unit> {
        return try {
            val reasonMap = mapOf("reason" to reason)
            val response = apiService.reportUser(userId, reasonMap)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to report user")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(user: DomainUserProfile): Resource<DomainUserProfile> {
        return try {
            val response = apiService.updateProfile(user.toData())
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    Resource.Success(domainProfile)
                } ?: Resource.Error<DomainUserProfile>("Failed to update profile: empty body")
            } else {
                Resource.Error<DomainUserProfile>("Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get user progress
     */
    fun getUserProgress(): Flow<Resource<UserProgress>> = flow {
        emit(Resource.Loading<UserProgress>())
        try {
            val response = apiService.getUserProgress()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error<UserProgress>("Failed to load user progress: empty body"))
            } else {
                emit(Resource.Error<UserProgress>("Failed to load user progress"))
            }
        } catch (e: Exception) {
            emit(Resource.Error<UserProgress>(e.message ?: "Unknown error occurred"))
        }
    }
    
    /**
     * Upload profile picture
     */
    suspend fun uploadProfilePicture(imageData: ByteArray): Resource<String> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = imageData.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData(
                name = "avatar",
                filename = "avatar.jpg",
                body = requestBody
            )
            val response = apiService.updateAvatar(part)
            if (response.isSuccessful) {
                val profile = response.body()
                val url = profile?.avatarUrl
                if (url.isNullOrBlank()) {
                    Resource.Error<String>("Server did not return an avatar URL")
                } else {
                    Resource.Success(normalizeUrl(url))
                }
            } else {
                Resource.Error<String>("Failed to upload profile picture")
            }
        } catch (e: Exception) {
            Resource.Error<String>(e.message ?: "Unknown error occurred")
        }
    }

    private fun normalizeUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        // Convert BASE_URL like http://host:port/api/v1/ -> http://host:port
        val serverBase = Constants.BASE_URL.substringBefore("/api/").trimEnd('/')
        val path = if (url.startsWith("/")) url else "/$url"
        return serverBase + path
    }

    /**
     * Upload avatar image via multipart PATCH users/profile/
     */
    suspend fun uploadAvatar(file: File): Resource<DomainUserProfile> {
        return try {
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = file.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData(
                name = "avatar",
                filename = file.name,
                body = requestBody
            )
            val response = apiService.updateAvatar(part)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    Resource.Success(domainProfile)
                } ?: Resource.Error<DomainUserProfile>("Failed to upload avatar: empty body")
            } else {
                Resource.Error<DomainUserProfile>("Failed to upload avatar")
            }
        } catch (e: Exception) {
            Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updatePreferences(preferences: Map<String, Any>): Resource<DomainUserProfile> {
        return try {
            val response = apiService.updatePreferences(preferences)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    Resource.Success(domainProfile)
                } ?: Resource.Error<DomainUserProfile>("Failed to update preferences: empty body")
            } else {
                Resource.Error<DomainUserProfile>("Failed to update preferences")
            }
        } catch (e: Exception) {
            Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Delete user account
     */
    suspend fun deleteAccount(): Resource<Boolean> {
        return try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) {
                Resource.Success(true)
            } else {
                Resource.Error<Boolean>("Failed to delete account")
            }
        } catch (e: Exception) {
            Resource.Error<Boolean>(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get followers for current user or specified user
     */
    suspend fun getFollowers(userId: String? = null): Resource<List<DomainUserProfile>> {
        return try {
            val response = if (userId != null) {
                apiService.getFollowersByUserId(userId)
            } else {
                apiService.getFollowers()
            }
            if (response.isSuccessful) {
                val list = response.body()?.map { data ->
                    data.toDomain().copy(
                        avatarUrl = data.avatarUrl?.let { normalizeUrl(it) }
                    )
                } ?: emptyList()
                Resource.Success(list)
            } else {
                Resource.Error("Failed to load followers")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Get following for current user or specified user
     */
    suspend fun getFollowing(userId: String? = null): Resource<List<DomainUserProfile>> {
        return try {
            val response = if (userId != null) {
                apiService.getFollowingByUserId(userId)
            } else {
                apiService.getFollowing()
            }
            if (response.isSuccessful) {
                val list = response.body()?.map { data ->
                    data.toDomain().copy(
                        avatarUrl = data.avatarUrl?.let { normalizeUrl(it) }
                    )
                } ?: emptyList()
                Resource.Success(list)
            } else {
                Resource.Error("Failed to load following")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Change user password
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Resource<String> {
        return try {
            val request = com.example.voicevibe.data.remote.api.ChangePasswordRequest(
                oldPassword = oldPassword,
                newPassword = newPassword
            )
            val response = apiService.changePassword(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Resource.Success(body.message ?: "Password changed successfully")
                } else {
                    Resource.Error(body?.error ?: "Failed to change password")
                }
            } else {
                Resource.Error("Failed to change password")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Update profile fields (email, displayName, etc.)
     */
    suspend fun updateProfileFields(fields: Map<String, String>): Resource<DomainUserProfile> {
        return try {
            val response = apiService.updateProfileFields(fields)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    val domainProfile = body.toDomain().copy(
                        avatarUrl = body.avatarUrl?.let { normalizeUrl(it) }
                    )
                    Resource.Success(domainProfile)
                } ?: Resource.Error<DomainUserProfile>("Failed to update profile: empty body")
            } else {
                Resource.Error<DomainUserProfile>("Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error<DomainUserProfile>(e.message ?: "Unknown error occurred")
        }
    }
}
