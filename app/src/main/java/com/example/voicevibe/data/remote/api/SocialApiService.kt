package com.example.voicevibe.data.remote.api

import com.example.voicevibe.domain.model.Post
import com.example.voicevibe.domain.model.PostComment
import com.example.voicevibe.domain.model.SocialNotification
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for social posts
 */
interface SocialApiService {

    @GET("social/posts/")
    suspend fun getPosts(): Response<List<Post>>

    @GET("social/posts/{id}/")
    suspend fun getPost(@Path("id") postId: Int): Response<Post>

    @FormUrlEncoded
    @POST("social/posts/")
    suspend fun createTextPost(
        @Field("text") text: String
    ): Response<Post>

    @FormUrlEncoded
    @POST("social/posts/")
    suspend fun createLinkPost(
        @Field("link_url") linkUrl: String
    ): Response<Post>

    @Multipart
    @POST("social/posts/")
    suspend fun createImagePost(
        @Part image: MultipartBody.Part
    ): Response<Post>

    @POST("social/posts/{id}/like/")
    suspend fun likePost(@Path("id") postId: Int): Response<Map<String, Any>>

    @DELETE("social/posts/{id}/like/")
    suspend fun unlikePost(@Path("id") postId: Int): Response<Map<String, Any>>

    @DELETE("social/posts/{id}/")
    suspend fun deletePost(@Path("id") postId: Int): Response<Void>

    @GET("social/posts/{id}/comments/")
    suspend fun getComments(@Path("id") postId: Int): Response<List<PostComment>>

    @FormUrlEncoded
    @POST("social/posts/{id}/comments/")
    suspend fun addComment(
        @Path("id") postId: Int,
        @Field("text") text: String
    ): Response<PostComment>

    @FormUrlEncoded
    @POST("social/posts/{id}/comments/")
    suspend fun replyToComment(
        @Path("id") postId: Int,
        @Field("text") text: String,
        @Field("parent") parent: Int
    ): Response<PostComment>

    @POST("social/comments/{id}/like/")
    suspend fun likeComment(@Path("id") commentId: Int): Response<Map<String, Any>>

    @DELETE("social/comments/{id}/like/")
    suspend fun unlikeComment(@Path("id") commentId: Int): Response<Map<String, Any>>

    @DELETE("social/comments/{id}/")
    suspend fun deleteComment(@Path("id") commentId: Int): Response<Void>

    // Notifications
    @GET("social/notifications/")
    suspend fun getNotifications(@Query("limit") limit: Int? = null, @Query("unread") unread: Boolean? = null): Response<List<SocialNotification>>

    @GET("social/notifications/unread-count/")
    suspend fun getUnreadNotificationCount(): Response<Map<String, Int>>

    @POST("social/notifications/{id}/read/")
    suspend fun markNotificationRead(@Path("id") notificationId: Int): Response<Map<String, Any>>

    @POST("social/notifications/mark-all-read/")
    suspend fun markAllNotificationsRead(): Response<Map<String, Any>>
}
