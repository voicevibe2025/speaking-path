package com.example.voicevibe.data.remote.api

import com.example.voicevibe.domain.model.User
import com.example.voicevibe.domain.model.UserProgress
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for user operations
 */
interface UserApiService {
    
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<User>
    
    @PUT("api/users/me")
    suspend fun updateProfile(@Body user: User): Response<User>
    
    @GET("api/users/progress")
    suspend fun getUserProgress(): Response<UserProgress>
    
    @POST("api/users/profile-picture")
    suspend fun uploadProfilePicture(@Body imageData: ByteArray): Response<Map<String, String>>
    
    @PATCH("api/users/preferences")
    suspend fun updatePreferences(@Body preferences: Map<String, Any>): Response<User>
    
    @DELETE("api/users/me")
    suspend fun deleteAccount(): Response<Unit>
    
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<User>
    
    @GET("api/users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<User>>
    
    @POST("api/users/follow/{id}")
    suspend fun followUser(@Path("id") userId: String): Response<Unit>
    
    @DELETE("api/users/follow/{id}")
    suspend fun unfollowUser(@Path("id") userId: String): Response<Unit>
    
    @GET("api/users/followers")
    suspend fun getFollowers(): Response<List<User>>
    
    @GET("api/users/following")
    suspend fun getFollowing(): Response<List<User>>
    
    @POST("api/users/report/{id}")
    suspend fun reportUser(
        @Path("id") userId: String,
        @Body reason: Map<String, String>
    ): Response<Unit>
    
    @POST("api/users/block/{id}")
    suspend fun blockUser(@Path("id") userId: String): Response<Unit>
    
    @DELETE("api/users/block/{id}")
    suspend fun unblockUser(@Path("id") userId: String): Response<Unit>
    
    @GET("api/users/blocked")
    suspend fun getBlockedUsers(): Response<List<User>>
}
