package com.example.voicevibe.domain.model

import java.time.LocalDateTime

/** Author info for posts */
data class PostAuthor(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
)

/** A social post: exactly one of text, imageUrl, or linkUrl will be present */
data class Post(
    val id: Int,
    val author: PostAuthor,
    val text: String? = null,
    val imageUrl: String? = null,
    val linkUrl: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByMe: Boolean,
    val canInteract: Boolean,
    val canDelete: Boolean = false,
)

/** Comment on a post */
data class PostComment(
    val id: Int,
    val post: Int,
    val author: PostAuthor,
    val text: String,
    val parent: Int? = null,
    val createdAt: LocalDateTime,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val canDelete: Boolean = false,
)

/** In-app social notification */
data class SocialNotification(
    val id: Int,
    val type: String,
    val actor: PostAuthor,
    val postId: Int,
    val commentId: Int? = null,
    val createdAt: LocalDateTime,
    val read: Boolean = false,
)
