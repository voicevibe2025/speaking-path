package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.UserApiService
import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.UserProgress
import com.example.voicevibe.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    fun getCurrentUser(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading<UserProfile>())
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(Resource.Success(body))
                } ?: emit(Resource.Error<UserProfile>("Failed to load user data: empty body"))
            } else {
                emit(Resource.Error<UserProfile>("Failed to load user data"))
            }
        } catch (e: Exception) {
            emit(Resource.Error<UserProfile>(e.message ?: "Unknown error occurred"))
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(user: UserProfile): Resource<UserProfile> {
        return try {
            val response = apiService.updateProfile(user)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error<UserProfile>("Failed to update profile: empty body")
            } else {
                Resource.Error<UserProfile>("Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error<UserProfile>(e.message ?: "Unknown error occurred")
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
            val response = apiService.uploadProfilePicture(imageData)
            if (response.isSuccessful) {
                val url = response.body()?.let { map -> map["url"] } ?: ""
                Resource.Success(url)
            } else {
                Resource.Error<String>("Failed to upload profile picture")
            }
        } catch (e: Exception) {
            Resource.Error<String>(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updatePreferences(preferences: Map<String, Any>): Resource<UserProfile> {
        return try {
            val response = apiService.updatePreferences(preferences)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    Resource.Success(body)
                } ?: Resource.Error<UserProfile>("Failed to update preferences: empty body")
            } else {
                Resource.Error<UserProfile>("Failed to update preferences")
            }
        } catch (e: Exception) {
            Resource.Error<UserProfile>(e.message ?: "Unknown error occurred")
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
}
