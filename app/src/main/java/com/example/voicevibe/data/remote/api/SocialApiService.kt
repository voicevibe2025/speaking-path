package com.example.voicevibe.data.remote.api

import com.example.voicevibe.domain.model.Post
import com.example.voicevibe.domain.model.PostComment
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API service for social posts
 */
interface SocialApiService {

    @GET("social/posts/")
    suspend fun getPosts(): Response<List<Post>>

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
}
