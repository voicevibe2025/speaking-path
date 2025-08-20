package com.example.voicevibe.domain.model

/**
 * Sealed class to represent different states of a resource/API call
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Represents a successful resource fetch
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents a failed resource fetch
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Represents a loading state
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
