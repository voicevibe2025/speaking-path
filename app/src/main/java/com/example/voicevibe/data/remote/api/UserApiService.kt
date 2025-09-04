package com.example.voicevibe.data.remote.api

import com.example.voicevibe.domain.model.UserProfile
import com.example.voicevibe.domain.model.UserProgress
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for user operations
 */
interface UserApiService {
    
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserProfile>
    
    @PUT("users/me")
    suspend fun updateProfile(@Body user: UserProfile): Response<UserProfile>
    
    @GET("users/progress")
    suspend fun getUserProgress(): Response<UserProgress>
    
    @Multipart
    @PATCH("users/profile/")
    suspend fun updateAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<UserProfile>
    
    @PATCH("users/preferences")
    suspend fun updatePreferences(@Body preferences: Map<String, Any>): Response<UserProfile>
    
    @DELETE("users/me")
    suspend fun deleteAccount(): Response<Unit>
    
    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<UserProfile>
    
    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<UserProfile>>
    
    @POST("users/follow/{id}")
    suspend fun followUser(@Path("id") userId: String): Response<Unit>
    
    @DELETE("users/follow/{id}")
    suspend fun unfollowUser(@Path("id") userId: String): Response<Unit>
    
    @GET("users/followers")
    suspend fun getFollowers(): Response<List<UserProfile>>
    
    @GET("users/following")
    suspend fun getFollowing(): Response<List<UserProfile>>
    
    @POST("users/report/{id}")
    suspend fun reportUser(
        @Path("id") userId: String,
        @Body reason: Map<String, String>
    ): Response<Unit>
    
    @POST("users/block/{id}")
    suspend fun blockUser(@Path("id") userId: String): Response<Unit>
    
    @DELETE("users/block/{id}")
    suspend fun unblockUser(@Path("id") userId: String): Response<Unit>
    
    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<List<UserProfile>>
}
