package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

data class Activity(
    @SerializedName("type")
    val type: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("relative_time")
    val relativeTime: String,
    @SerializedName("icon")
    val icon: String,
    @SerializedName("color")
    val color: String
)
