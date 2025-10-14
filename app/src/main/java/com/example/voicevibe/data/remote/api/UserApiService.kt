package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.model.UserProfile
import com.example.voicevibe.domain.model.UserProgress
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

/**
 * API service for user operations
 */
interface UserApiService {
    
    @GET("users/profile/")
    suspend fun getCurrentUser(): Response<UserProfile>
    
    @PUT("users/profile/")
    suspend fun updateProfile(@Body user: UserProfile): Response<UserProfile>
    
    @GET("users/progress")
    suspend fun getUserProgress(): Response<UserProgress>
    
    @Multipart
    @PATCH("users/profile/")
    suspend fun updateAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<UserProfile>
    
    @PATCH("users/preferences/")
    suspend fun updatePreferences(@Body preferences: Map<String, Any>): Response<UserProfile>
    
    @DELETE("users/delete-account/")
    suspend fun deleteAccount(): Response<Unit>
    
    @GET("users/{id}/")
    suspend fun getUserById(@Path("id") userId: String): Response<UserProfile>
    
    @GET("users/search/")
    suspend fun searchUsers(@Query("query") query: String): Response<List<UserProfile>>
    
    @GET("users/search/unified/")
    suspend fun unifiedSearch(@Query("query") query: String): Response<UnifiedSearchResponse>
    
    @POST("users/follow/{id}/")
    suspend fun followUser(@Path("id") userId: String): Response<Unit>
    
    @DELETE("users/follow/{id}/")
    suspend fun unfollowUser(@Path("id") userId: String): Response<Unit>
    
    @GET("users/followers/")
    suspend fun getFollowers(): Response<List<UserProfile>>
    
    @GET("users/followers/{id}/")
    suspend fun getFollowersByUserId(@Path("id") userId: String): Response<List<UserProfile>>
    
    @GET("users/following/")
    suspend fun getFollowing(): Response<List<UserProfile>>
    
    @GET("users/following/{id}/")
    suspend fun getFollowingByUserId(@Path("id") userId: String): Response<List<UserProfile>>
    
    // Privacy Settings
    @GET("users/privacy-settings/")
    suspend fun getPrivacySettings(): Response<PrivacySettings>
    
    @PATCH("users/privacy-settings/")
    suspend fun updatePrivacySettings(@Body settings: PrivacySettings): Response<PrivacySettings>
    
    // Blocking
    @POST("users/block/{id}/")
    suspend fun blockUser(
        @Path("id") userId: String,
        @Body reason: Map<String, String>
    ): Response<BlockResponse>
    
    @DELETE("users/block/{id}/")
    suspend fun unblockUser(@Path("id") userId: String): Response<BlockResponse>
    
    @GET("users/blocked/")
    suspend fun getBlockedUsers(): Response<List<BlockedUser>>
    
    // Reporting
    @POST("users/reports/")
    suspend fun createReport(@Body report: CreateReportRequest): Response<ReportResponse>
    
    @GET("users/reports/my/")
    suspend fun getMyReports(): Response<List<ReportItem>>
    
    @POST("users/change-password/")
    suspend fun changePassword(@Body request: com.example.voicevibe.data.remote.api.ChangePasswordRequest): Response<ChangePasswordResponse>
    
    @PATCH("users/profile/")
    suspend fun updateProfileFields(@Body fields: Map<String, String>): Response<UserProfile>
    
    // Group endpoints
    @GET("users/groups/")
    suspend fun getGroups(): Response<List<GroupDto>>
    
    @GET("users/groups/check/")
    suspend fun checkGroupStatus(): Response<GroupStatusDto>
    
    @POST("users/groups/{id}/join/")
    suspend fun joinGroup(@Path("id") groupId: Int): Response<JoinGroupResponse>
    
    @GET("users/groups/members/")
    suspend fun getMyGroupMembers(): Response<GroupMembersDto>
    
    @GET("users/groups/{id}/members/")
    suspend fun getGroupMembers(@Path("id") groupId: Int): Response<GroupMembersDto>
    
    @GET("users/groups/messages/")
    suspend fun getMyGroupMessages(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<GroupMessagesDto>
    
    @GET("users/groups/{id}/messages/")
    suspend fun getGroupMessages(
        @Path("id") groupId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<GroupMessagesDto>
    
    @POST("users/groups/messages/send/")
    suspend fun sendGroupMessage(@Body request: SendGroupMessageRequest): Response<SendGroupMessageResponse>
    
    @DELETE("users/groups/messages/{id}/delete/")
    suspend fun deleteGroupMessage(@Path("id") messageId: Int): Response<DeleteMessageResponse>

    // Fallback: some environments may expose delete at the resource URL without a trailing 'delete/'
    @DELETE("users/groups/messages/{id}/")
    suspend fun deleteGroupMessagePlain(@Path("id") messageId: Int): Response<Unit>
}

data class ChangePasswordResponse(
    val success: Boolean,
    val message: String?,
    val error: String?
)

data class PrivacySettings(
    val id: Int? = null,
    @SerializedName("hide_avatar")
    val hideAvatar: Boolean = false,
    @SerializedName("hide_online_status")
    val hideOnlineStatus: Boolean = false,
    @SerializedName("allow_messages_from_strangers")
    val allowMessagesFromStrangers: Boolean = true
)

data class BlockedUser(
    val id: Int,
    val userId: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val reason: String?,
    val blockedAt: String
)

data class BlockResponse(
    val success: Boolean,
    val message: String,
    val isBlocked: Boolean
)

data class CreateReportRequest(
    @SerializedName("report_type")
    val reportType: String, // "user", "post", "comment"
    val reason: String, // "spam", "harassment", "hate_speech", "inappropriate", "impersonation", "other"
    val description: String,
    @SerializedName("reported_user")
    val reportedUser: Int? = null,
    @SerializedName("reported_post_id")
    val reportedPostId: Int? = null,
    @SerializedName("reported_comment_id")
    val reportedCommentId: Int? = null
)

data class ReportResponse(
    val success: Boolean,
    val message: String,
    val report: ReportItem?
)

data class ReportItem(
    val id: Int,
    @SerializedName("report_type")
    val reportType: String,
    val reason: String,
    val description: String,
    @SerializedName("reporter_name")
    val reporterName: String?,
    @SerializedName("reported_user_name")
    val reportedUserName: String?,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String
)

// Group DTOs
data class GroupDto(
    val id: Int,
    val name: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val color: String,
    val memberCount: Int,
    @SerializedName("created_at")
    val createdAt: String
)

data class GroupMemberDto(
    val userId: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val level: Int,
    val xp: Int,
    val streakDays: Int
)

data class GroupMessageDto(
    val id: Int,
    val groupId: Int,
    val groupName: String,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val timestamp: String
)

data class GroupStatusDto(
    val hasGroup: Boolean,
    val group: GroupDto?
)

data class GroupMembersDto(
    val group: GroupDto,
    val members: List<GroupMemberDto>,
    val totalMembers: Int
)

data class GroupMessagesDto(
    val group: GroupDto,
    val messages: List<GroupMessageDto>,
    val hasMore: Boolean
)

data class JoinGroupResponse(
    val success: Boolean,
    val message: String,
    val profile: UserProfile?
)

data class SendGroupMessageRequest(
    val message: String
)

data class SendGroupMessageResponse(
    val success: Boolean,
    val message: GroupMessageDto
)

data class DeleteMessageResponse(
    val success: Boolean,
    val message: String? = null
)

// Unified Search DTOs
data class UnifiedSearchResponse(
    val users: List<UserProfile>,
    val groups: List<GroupDto>,
    val materials: List<MaterialDto>
)

data class MaterialDto(
    val id: String,
    val title: String,
    val description: String,
    val sequence: Int,
    val type: String
)
