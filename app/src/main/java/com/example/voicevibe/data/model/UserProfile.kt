package com.example.voicevibe.data.model

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("user_email")
    val userEmail: String,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?
)
