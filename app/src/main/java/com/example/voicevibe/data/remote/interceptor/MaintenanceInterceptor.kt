package com.example.voicevibe.data.remote.interceptor

import com.example.voicevibe.data.system.MaintenanceManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class MaintenanceInterceptor @Inject constructor(
    private val maintenanceManager: MaintenanceManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            val response = chain.proceed(request)

            when (response.code) {
                502, 503, 504 -> {
                    // Try to read message and retryAfterSeconds from JSON body if provided
                    var message: String? = null
                    var retryAfterSeconds: Long? = null
                    try {
                        val peek = response.peekBody(8_192).string()
                        if (peek.isNotBlank()) {
                            val json = JSONObject(peek)
                            message = json.optString("message").takeIf { it.isNotBlank() }
                            retryAfterSeconds = json.optLong("retryAfterSeconds", -1L).let { if (it >= 0) it else null }
                        }
                    } catch (_: Exception) {
                        // ignore parse errors
                    }
                    if (retryAfterSeconds == null) {
                        val retryAfterHeader = response.header("Retry-After")
                        retryAfterSeconds = retryAfterHeader?.toLongOrNull()
                    }
                    maintenanceManager.setMaintenance(
                        message = message ?: "Server maintenance in progress. Some features may be unavailable.",
                        retryAfterSeconds = retryAfterSeconds
                    )
                }
                else -> {
                    // Clear banner if previously active and we received a successful response
                    if (maintenanceManager.state.value.active && response.isSuccessful) {
                        maintenanceManager.clearIfResolved()
                    }
                }
            }
            response
        } catch (ioe: IOException) {
            // Network issue (server reset, DNS, timeout, etc.)
            maintenanceManager.setNetworkIssue()
            throw ioe
        }
    }
}
