package com.example.voicevibe.data.repository

import com.example.voicevibe.data.model.UserProfile
import com.example.voicevibe.data.network.ProfileApiService
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val apiService: ProfileApiService
) {
    suspend fun getProfile(): UserProfile {
        return apiService.getProfile()
    }
}
