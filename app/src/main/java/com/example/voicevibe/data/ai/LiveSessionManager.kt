package com.example.voicevibe.data.ai

import android.util.Base64
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionManager @Inject constructor(
    okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    interface Listener {
        fun onOpen()
        fun onMessage(text: String)
        fun onFailure(t: Throwable)
        fun onClosed()
    }

    private val webSocketClient: OkHttpClient = okHttpClient.newBuilder()
        .apply {
            interceptors().clear()
            networkInterceptors().clear()
        }
        .build()

    private var webSocket: WebSocket? = null
    private var currentModel: String = DEFAULT_MODEL
    private var currentResponseModalities: List<String> = listOf("TEXT")
    private var ready: Boolean = false
    private val pendingMessages = mutableListOf<String>()
    private var loggedFirstAudio = false

    fun connect(
        token: String,
        model: String?,
        responseModalities: List<String>?,
        listener: Listener
    ) {
        close()
        currentModel = model?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        currentResponseModalities = responseModalities?.takeIf { it.isNotEmpty() } ?: listOf("TEXT")
        ready = false
        pendingMessages.clear()

        // Fallback strategy: try v1beta with Authorization header, then access_token param,
        // then v1alpha with Authorization header, then access_token param.
        data class Attempt(val url: String, val useHeader: Boolean)

        val attempts = listOf(
            Attempt(
                url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained",
                useHeader = true
            ),
            Attempt(
                url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained?access_token=$token",
                useHeader = false
            ),
            Attempt(
                url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContentConstrained",
                useHeader = true
            ),
            Attempt(
                url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContentConstrained?access_token=$token",
                useHeader = false
            )
        )

        fun tryConnect(index: Int) {
            if (index >= attempts.size) {
                listener.onFailure(IllegalStateException("All Live API connection attempts failed"))
                return
            }

            val attempt = attempts[index]
            val builder = Request.Builder().url(attempt.url)
            if (attempt.useHeader) {
                builder.header("Authorization", "Token $token")
            }
            val request = builder.build()
            Timber.tag("LiveSession").d("Connecting (attempt=%d) url=%s header=%s", index, attempt.url, attempt.useHeader)

            webSocket = webSocketClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    // Send initial setup so the session knows the model and modalities
                    sendSetup()
                    Timber.tag("LiveSession").d("WebSocket opened")
                    listener.onOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Detect setupComplete and mark ready, flushing any queued messages
                    try {
                        val root = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                        if (root.has("setupComplete")) {
                            ready = true
                            Timber.tag("LiveSession").d("setupComplete received; flushing %d queued messages", pendingMessages.size)
                            if (pendingMessages.isNotEmpty()) {
                                pendingMessages.forEach { msg -> this@LiveSessionManager.webSocket?.send(msg) }
                                pendingMessages.clear()
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore parse errors; pass through to listener
                    }
                    listener.onMessage(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val text = bytes.utf8()
                    // Detect setupComplete in binary frames too
                    try {
                        val root = gson.fromJson(text, com.google.gson.JsonObject::class.java)
                        if (root.has("setupComplete")) {
                            ready = true
                            Timber.tag("LiveSession").d("setupComplete (binary) received; flushing %d queued messages", pendingMessages.size)
                            if (pendingMessages.isNotEmpty()) {
                                pendingMessages.forEach { msg -> this@LiveSessionManager.webSocket?.send(msg) }
                                pendingMessages.clear()
                            }
                        }
                    } catch (_: Exception) {
                        // ignore
                    }
                    listener.onMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    // Try next attempt on failure (e.g., 404 on handshake)
                    Timber.tag("LiveSession").e(t, "WebSocket failure (attempt=%d) code=%s message=%s", index, response?.code, response?.message)
                    tryConnect(index + 1)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosed()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosed()
                }
            })
        }

        tryConnect(0)
    }

    fun sendUserMessage(message: String) {
        val clientContentPayload = mapOf(
            "clientContent" to mapOf(
                "turns" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to message))
                    )
                ),
                "turnComplete" to true
            )
        )
        val realtimeInputPayload = mapOf(
            "realtimeInput" to mapOf(
                "text" to message
            )
        )

        val json1 = gson.toJson(clientContentPayload)
        val json2 = gson.toJson(realtimeInputPayload)
        if (ready) {
            webSocket?.send(json1)
            webSocket?.send(json2)
        } else {
            pendingMessages.add(json1)
            pendingMessages.add(json2)
        }
        Timber.tag("LiveSession").d("Sent user text (ready=%s)", ready)
    }

    fun sendAudioChunk(pcm16le: ByteArray, sampleRate: Int = 16_000) {
        if (!ready) return
        val base64 = Base64.encodeToString(pcm16le, Base64.NO_WRAP)
        val payload = mapOf(
            "realtimeInput" to mapOf(
                "audio" to mapOf(
                    "data" to base64,
                    "mimeType" to "audio/pcm;rate=$sampleRate"
                )
            )
        )
        val json = gson.toJson(payload)
        if (!loggedFirstAudio) {
            Timber.tag("LiveSession").d("Sending first audio chunk bytes=%d sr=%d", pcm16le.size, sampleRate)
            loggedFirstAudio = true
        }
        webSocket?.send(json)
    }

    fun sendAudioStreamEnd() {
        if (!ready) return
        val payload = mapOf(
            "realtimeInput" to mapOf(
                "audioStreamEnd" to true
            )
        )
        val json = gson.toJson(payload)
        webSocket?.send(json)
    }

    fun sendRawMessage(json: String) {
        if (ready) {
            webSocket?.send(json)
        } else {
            pendingMessages.add(json)
        }
        Timber.tag("LiveSession").d("Sent raw message (ready=%s)", ready)
    }

    private fun sendSetup() {
        val modelName = if (currentModel.startsWith("models/")) currentModel else "models/$currentModel"
        val setup = mutableMapOf<String, Any>(
            "model" to modelName,
            "generationConfig" to mapOf(
                "responseModalities" to currentResponseModalities
            )
        )
        if (currentResponseModalities.any { it.equals("AUDIO", ignoreCase = true) }) {
            // Enable input/output transcription so we can show text captions
            setup["inputAudioTranscription"] = emptyMap<String, Any>()
            setup["outputAudioTranscription"] = emptyMap<String, Any>()
        }
        val payload = mapOf("setup" to setup)
        val json = gson.toJson(payload)
        Timber.tag("LiveSession").d("Sending setup: %s", json)
        webSocket?.send(json)
    }

    fun close() {
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Closing")
        webSocket = null
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val DEFAULT_MODEL = "gemini-live-2.5-flash-preview"
    }
}
