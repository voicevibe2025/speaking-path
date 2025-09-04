package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.remote.dto.auth.UserDto
import com.example.voicevibe.data.remote.dto.auth.UserProfileDto
import com.example.voicevibe.data.remote.dto.auth.MessageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

/**
 * User API service interface
 */
interface UserApi {

    @GET("users/me/")
    suspend fun getCurrentUser(): Response<UserDto>

    @GET("users/{id}/")
    suspend fun getUser(@Path("id") userId: Int): Response<UserDto>

    @PUT("users/me/")
    suspend fun updateCurrentUser(
        @Body updates: Map<String, Any>
    ): Response<UserDto>

    @GET("users/profile/")
    suspend fun getUserProfile(): Response<UserProfileDto>

    @PUT("users/profile/")
    suspend fun updateUserProfile(
        @Body profile: UserProfileDto
    ): Response<UserProfileDto>

    @Multipart
    @PATCH("users/profile/")
    suspend fun updateAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<UserProfileDto>

    @DELETE("users/avatar/")
    suspend fun deleteAvatar(): Response<Unit>

    @POST("users/preferences/")
    suspend fun updatePreferences(
        @Body preferences: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("users/preferences/")
    suspend fun getPreferences(): Response<Map<String, Any>>

    @POST("users/change-password/")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<MessageResponse>

    @DELETE("users/delete-account/")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<Unit>
}

// Additional DTOs for User API
data class ChangePasswordRequest(
    @SerializedName("old_password")
    val oldPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)

data class DeleteAccountRequest(
    val password: String,
    val reason: String? = null
)
