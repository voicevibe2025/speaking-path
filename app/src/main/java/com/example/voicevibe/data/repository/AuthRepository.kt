package com.example.voicevibe.data.repository

import android.content.Context
import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.local.TokenManager
import com.example.voicevibe.data.remote.api.AuthApi
import com.example.voicevibe.data.remote.dto.auth.*
import com.example.voicevibe.domain.model.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) {

    /**
     * Register a new user
     */
    fun register(
        email: String,
        password: String,
        passwordConfirm: String,
        firstName: String,
        lastName: String,
        nativeLanguage: String = "Indonesian",
        targetLanguage: String = "English",
        proficiencyLevel: String = "A1"
    ): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading())

        try {
            val request = RegisterRequest(
                email = email,
                password = password,
                passwordConfirm = passwordConfirm,
                firstName = firstName,
                lastName = lastName,
                nativeLanguage = nativeLanguage,
                targetLanguage = targetLanguage,
                proficiencyLevel = proficiencyLevel
            )

            val response = authApi.register(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save tokens and user data
                tokenManager.saveTokens(authResponse.access, authResponse.refresh)
                tokenManager.saveUserData(
                    userId = authResponse.user.id.toString(),
                    email = authResponse.user.email,
                    name = "${authResponse.user.firstName} ${authResponse.user.lastName}"
                )
                emit(Resource.Success(authResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                emit(Resource.Error(errorMessage))
            }
        } catch (e: HttpException) {
            Timber.e(e, "Registration HTTP exception")
            emit(Resource.Error("Network error: ${e.message()}"))
        } catch (e: IOException) {
            Timber.e(e, "Registration IO exception")
            emit(Resource.Error("Connection error. Please check your internet connection."))
        } catch (e: Exception) {
            Timber.e(e, "Registration unexpected exception")
            emit(Resource.Error("An unexpected error occurred: ${e.localizedMessage}"))
        }
    }

    /**
     * Login or register via Google using a Firebase ID token
     */
    fun loginWithGoogle(idToken: String): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading())

        try {
            val request = GoogleLoginRequest(idToken = idToken)
            val response = authApi.googleLogin(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveTokens(authResponse.access, authResponse.refresh)
                tokenManager.saveUserData(
                    userId = authResponse.user.id.toString(),
                    email = authResponse.user.email,
                    name = "${authResponse.user.firstName} ${authResponse.user.lastName}"
                )
                emit(Resource.Success(authResponse))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Google sign-in failed"
                emit(Resource.Error(errorMessage))
            }
        } catch (e: HttpException) {
            Timber.e(e, "Google login HTTP exception")
            emit(Resource.Error("Network error: ${e.message()}"))
        } catch (e: IOException) {
            Timber.e(e, "Google login IO exception")
            emit(Resource.Error("Connection error. Please check your internet connection."))
        } catch (e: Exception) {
            Timber.e(e, "Google login unexpected exception")
            emit(Resource.Error("An unexpected error occurred: ${e.localizedMessage}"))
        }
    }

    /**
     * Login user
     */
    fun login(email: String, password: String): Flow<Resource<AuthResponse>> = flow {
        emit(Resource.Loading())

        try {
            val request = LoginRequest(email = email, password = password)
            val response = authApi.login(request)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save tokens and user data
                tokenManager.saveTokens(authResponse.access, authResponse.refresh)
                tokenManager.saveUserData(
                    userId = authResponse.user.id.toString(),
                    email = authResponse.user.email,
                    name = "${authResponse.user.firstName} ${authResponse.user.lastName}"
                )
                emit(Resource.Success(authResponse))
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Invalid email or password"
                    404 -> "User not found"
                    else -> response.errorBody()?.string() ?: "Login failed"
                }
                emit(Resource.Error(errorMessage))
            }
        } catch (e: HttpException) {
            Timber.e(e, "Login HTTP exception")
            emit(Resource.Error("Network error: ${e.message()}"))
        } catch (e: IOException) {
            Timber.e(e, "Login IO exception")
            emit(Resource.Error("Connection error. Please check your internet connection."))
        } catch (e: Exception) {
            Timber.e(e, "Login unexpected exception")
            emit(Resource.Error("An unexpected error occurred: ${e.localizedMessage}"))
        }
    }

    /**
     * Refresh access token
     */
    suspend fun refreshToken(): Resource<TokenResponse> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()

            if (refreshToken.isNullOrEmpty()) {
                return Resource.Error("No refresh token available")
            }

            val request = RefreshTokenRequest(refresh = refreshToken)
            val response = authApi.refreshToken(request)

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                // Update access token
                tokenManager.updateAccessToken(tokenResponse.access)
                // Update refresh token if provided
                tokenResponse.refresh?.let {
                    tokenManager.saveTokens(tokenResponse.access, it)
                }
                Resource.Success(tokenResponse)
            } else {
                Resource.Error("Token refresh failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh exception")
            Resource.Error("Failed to refresh token: ${e.localizedMessage}")
        }
    }

    /**
     * Logout user
     */
    suspend fun logout(): Resource<Unit> {
        return try {
            val response = authApi.logout()

            // Clear local data regardless of API response
            tokenManager.clearAll()

            // Sign out from Google Sign-In to force account selection on next login
            try {
                val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
            } catch (e: Exception) {
                Timber.e(e, "Google Sign-Out failed")
                // Continue with logout even if Google sign-out fails
            }

            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                // Still consider it successful since local data is cleared
                Resource.Success(Unit)
            }
        } catch (e: Exception) {
            // Clear local data even if API call fails
            tokenManager.clearAll()
            Resource.Success(Unit)
        }
    }

    /**
     * Request password reset
     */
    fun requestPasswordReset(email: String): Flow<Resource<MessageResponse>> = flow {
        emit(Resource.Loading())

        try {
            val request = PasswordResetRequest(email = email)
            val response = authApi.requestPasswordReset(request)

            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to send reset email"
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "Password reset request exception")
            emit(Resource.Error("Failed to request password reset: ${e.localizedMessage}"))
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Flow<Boolean> = tokenManager.isLoggedInFlow()

    /**
     * Check if onboarding is completed
     */
    fun isOnboardingCompleted(): Flow<Boolean> = tokenManager.isOnboardingCompletedFlow()

    /**
     * Mark onboarding as completed
     */
    suspend fun setOnboardingCompleted() {
        tokenManager.setOnboardingCompleted()
    }
}
