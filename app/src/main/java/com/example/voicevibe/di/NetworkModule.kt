package com.example.voicevibe.di

import com.example.voicevibe.BuildConfig
import com.example.voicevibe.data.remote.api.*
import com.example.voicevibe.data.remote.interceptor.AuthInterceptor
import com.example.voicevibe.data.network.ProfileApiService
import com.example.voicevibe.utils.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java,
            object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
                override fun serialize(
                    src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?
                ): JsonElement {
                    if (src == null) return com.google.gson.JsonNull.INSTANCE
                    val instant = src.atOffset(ZoneOffset.UTC).toInstant()
                    return com.google.gson.JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(instant))
                }

                override fun deserialize(
                    json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
                ): LocalDateTime {
                    if (json == null || json.isJsonNull) throw JsonParseException("Null date")
                    val s = json.asString.trim()
                    // If string contains timezone info (Z or an explicit offset), parse with OffsetDateTime first
                    val hasOffsetOrZ = s.endsWith("Z", ignoreCase = true) || s.indexOf('+', 10) != -1 || s.indexOf('-', 10) != -1
                    if (hasOffsetOrZ) {
                        val text = if (s.endsWith("z")) s.dropLast(1) + "Z" else s
                        return try {
                            val odt = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                        } catch (e: Exception) {
                            // Fallback: try Instant for strict Z cases
                            try {
                                val normalized = text.replace("+00:00", "Z").replace("+0000", "Z")
                                val instant = Instant.parse(normalized)
                                LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                            } catch (e2: Exception) {
                                throw JsonParseException("Failed to parse LocalDateTime: $s", e2)
                            }
                        }
                    }
                    // Local date-time without zone info
                    return try {
                        LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (e: Exception) {
                        throw JsonParseException("Failed to parse LocalDateTime: $s", e)
                    }
                }

                private fun normalizeIso8601(input: String): String {
                    // Ensure 'Z' is uppercase and fractional seconds are supported by Instant.parse
                    // DRF may return microseconds; Instant.parse supports up to nanoseconds already, so return as-is
                    return if (input.endsWith("z")) input.dropLast(1) + "Z" else input
                }
            }
        )
        .create()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // API Services

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileApiService =
        retrofit.create(ProfileApiService::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApiService =
        retrofit.create(UserApiService::class.java)

    @Provides
    @Singleton
    fun provideLearningPathApi(retrofit: Retrofit): LearningPathApiService =
        retrofit.create(LearningPathApiService::class.java)

    @Provides
    @Singleton
    fun provideLearningApiService(retrofit: Retrofit): LearningApiService =
        retrofit.create(LearningApiService::class.java)

    @Provides
    @Singleton
    fun provideSpeakingPracticeApi(retrofit: Retrofit): SpeakingPracticeApiService =
        retrofit.create(SpeakingPracticeApiService::class.java)

    @Provides
    @Singleton
    fun provideGamificationApi(retrofit: Retrofit): GamificationApiService =
        retrofit.create(GamificationApiService::class.java)

    @Provides
    @Singleton
    fun provideSpeakingJourneyApi(retrofit: Retrofit): com.example.voicevibe.data.remote.api.SpeakingJourneyApiService =
        retrofit.create(com.example.voicevibe.data.remote.api.SpeakingJourneyApiService::class.java)

    @Provides
    @Singleton
    fun provideAiEvaluationApi(retrofit: Retrofit): com.example.voicevibe.data.remote.api.AiEvaluationApiService =
        retrofit.create(com.example.voicevibe.data.remote.api.AiEvaluationApiService::class.java)

    @Provides
    @Singleton
    fun provideSocialApi(retrofit: Retrofit): com.example.voicevibe.data.remote.api.SocialApiService =
        retrofit.create(com.example.voicevibe.data.remote.api.SocialApiService::class.java)

    @Provides
    @Singleton
    fun provideCoachApi(retrofit: Retrofit): com.example.voicevibe.data.remote.api.CoachApiService =
        retrofit.create(com.example.voicevibe.data.remote.api.CoachApiService::class.java)

}
