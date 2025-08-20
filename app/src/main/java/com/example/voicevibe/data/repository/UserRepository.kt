package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.UserApiService
import com.example.voicevibe.domain.model.User
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
    fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()))
            } else {
                emit(Resource.Error("Failed to load user data"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(user: User): Resource<User> {
        return try {
            val response = apiService.updateProfile(user)
            if (response.isSuccessful) {
                Resource.Success(response.body())
            } else {
                Resource.Error("Failed to update profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Get user progress
     */
    fun getUserProgress(): Flow<Resource<UserProgress>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getUserProgress()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()))
            } else {
                emit(Resource.Error("Failed to load user progress"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    /**
     * Upload profile picture
     */
    suspend fun uploadProfilePicture(imageData: ByteArray): Resource<String> {
        return try {
            val response = apiService.uploadProfilePicture(imageData)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.get("url") ?: "")
            } else {
                Resource.Error("Failed to upload profile picture")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updatePreferences(preferences: Map<String, Any>): Resource<User> {
        return try {
            val response = apiService.updatePreferences(preferences)
            if (response.isSuccessful) {
                Resource.Success(response.body())
            } else {
                Resource.Error("Failed to update preferences")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
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
                Resource.Error("Failed to delete account")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}
