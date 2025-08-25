package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

data class Badge(
    @SerializedName("badge_id")
    val badgeId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("batik_pattern")
    val batikPattern: String,
    @SerializedName("pattern_color")
    val patternColor: String,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("tier")
    val tier: Int,
    @SerializedName("tier_display")
    val tierDisplay: String?
)

data class Achievement(
    @SerializedName("id")
    val id: Int,
    @SerializedName("badge")
    val badge: Badge,
    @SerializedName("earned_at")
    val earnedAt: String,
    @SerializedName("current_tier")
    val currentTier: Int
)
