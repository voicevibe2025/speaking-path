package com.example.voicevibe.data.remote.interceptor

import com.example.voicevibe.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds JWT authentication token to API requests
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for login and register endpoints
        val isAuthEndpoint = originalRequest.url.pathSegments.any {
            it in listOf("login", "register", "refresh")
        }

        if (isAuthEndpoint) {
            return chain.proceed(originalRequest)
        }

        // Get the access token
        val accessToken = tokenManager.getAccessToken()

        // If no token available, proceed without auth header
        if (accessToken.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
