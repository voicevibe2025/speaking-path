package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("user_name")
    val userName: String?,
    @SerializedName("user_email")
    val userEmail: String?
)
