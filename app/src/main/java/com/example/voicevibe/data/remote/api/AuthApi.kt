package com.example.voicevibe.data.remote.api

import com.example.voicevibe.data.remote.dto.auth.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Authentication API service interface
 */
interface AuthApi {

    @POST("auth/register/")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/login/google/")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @POST("auth/token/refresh/")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<TokenResponse>

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    @POST("auth/verify-email/")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): Response<MessageResponse>

    @POST("auth/resend-verification/")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): Response<MessageResponse>

    @POST("auth/password-reset/")
    suspend fun requestPasswordReset(
        @Body request: PasswordResetRequest
    ): Response<MessageResponse>

    @POST("auth/password-reset-confirm/")
    suspend fun confirmPasswordReset(
        @Body request: PasswordResetConfirmRequest
    ): Response<MessageResponse>
}
