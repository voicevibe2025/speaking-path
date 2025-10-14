package com.example.voicevibe.data.model

import com.example.voicevibe.data.remote.api.GroupDto
import com.google.gson.annotations.SerializedName

/**
 * DTO for search material (topic) from backend
 */
data class SearchMaterialDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("sequence") val sequence: Int,
    @SerializedName("unlocked") val unlocked: Boolean,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("type") val type: String? = null
)

/**
 * DTO for unified search response from backend
 */
data class UnifiedSearchResponseDto(
    @SerializedName("users") val users: List<UserProfile>,
    @SerializedName("groups") val groups: List<GroupDto>,
    @SerializedName("materials") val materials: List<SearchMaterialDto>
)
