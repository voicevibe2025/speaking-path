package com.example.voicevibe.data.network

import com.example.voicevibe.data.model.UserProfile
import retrofit2.http.GET

interface ProfileApiService {
    @GET("users/profile/")
    suspend fun getProfile(): UserProfile
}
