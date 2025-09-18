package com.example.voicevibe.data.repository

import com.example.voicevibe.data.remote.api.SocialApiService
import com.example.voicevibe.domain.model.Post
import com.example.voicevibe.domain.model.PostComment
import com.example.voicevibe.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val api: SocialApiService
) {
    fun getPosts(): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val resp = api.getPosts()
            if (resp.isSuccessful) {
                emit(Resource.Success(resp.body() ?: emptyList()))
            } else {
                emit(Resource.Error("Failed to load posts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    suspend fun createTextPost(text: String): Resource<Post> {
        return try {
            val resp = api.createTextPost(text)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty response")
            } else Resource.Error("Failed to create post")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun createLinkPost(linkUrl: String): Resource<Post> {
        return try {
            val resp = api.createLinkPost(linkUrl)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty response")
            } else Resource.Error("Failed to create link post")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun createImagePost(image: MultipartBody.Part): Resource<Post> {
        return try {
            val resp = api.createImagePost(image)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty response")
            } else Resource.Error("Failed to upload image post")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun likePost(id: Int): Resource<Unit> {
        return try {
            val resp = api.likePost(id)
            if (resp.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to like")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun unlikePost(id: Int): Resource<Unit> {
        return try {
            val resp = api.unlikePost(id)
            if (resp.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to unlike")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun getComments(id: Int): Resource<List<PostComment>> {
        return try {
            val resp = api.getComments(id)
            if (resp.isSuccessful) Resource.Success(resp.body() ?: emptyList()) else Resource.Error("Failed to load comments")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun addComment(id: Int, text: String, parent: Int? = null): Resource<PostComment> {
        return try {
            val resp = if (parent != null) api.replyToComment(id, text, parent) else api.addComment(id, text)
            if (resp.isSuccessful) {
                resp.body()?.let { Resource.Success(it) } ?: Resource.Error("Empty response")
            } else Resource.Error("Failed to add comment")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun likeComment(id: Int): Resource<Unit> {
        return try {
            val resp = api.likeComment(id)
            if (resp.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to like comment")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }

    suspend fun unlikeComment(id: Int): Resource<Unit> {
        return try {
            val resp = api.unlikeComment(id)
            if (resp.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to unlike comment")
        } catch (e: Exception) { Resource.Error(e.message ?: "Unknown error") }
    }
}
