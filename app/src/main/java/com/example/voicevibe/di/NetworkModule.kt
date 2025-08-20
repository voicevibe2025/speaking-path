package com.example.voicevibe.di

import com.example.voicevibe.data.remote.api.*
import com.example.voicevibe.data.remote.interceptor.AuthInterceptor
import com.example.voicevibe.utils.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideLearningApi(retrofit: Retrofit): LearningApi =
        retrofit.create(LearningApi::class.java)

    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi =
        retrofit.create(SessionApi::class.java)

    @Provides
    @Singleton
    fun provideEvaluationApi(retrofit: Retrofit): EvaluationApi =
        retrofit.create(EvaluationApi::class.java)

    @Provides
    @Singleton
    fun provideGamificationApi(retrofit: Retrofit): GamificationApi =
        retrofit.create(GamificationApi::class.java)

    @Provides
    @Singleton
    fun provideCulturalApi(retrofit: Retrofit): CulturalApi =
        retrofit.create(CulturalApi::class.java)

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi =
        retrofit.create(AnalyticsApi::class.java)
}
