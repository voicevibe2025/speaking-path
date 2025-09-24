package com.example.voicevibe.data.ai

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
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

    fun connect(token: String, model: String?, listener: Listener) {
        close()
        currentModel = model?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL

        val endpoint = "wss://generativelanguage.googleapis.com/v1beta/models/$currentModel:streamGenerateContent?access_token=$token"
        val request = Request.Builder()
            .url(endpoint)
            .build()

        webSocket = webSocketClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener.onMessage(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                listener.onFailure(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }
        })
    }

    fun sendUserMessage(message: String) {
        val payload = mapOf(
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
        val json = gson.toJson(payload)
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
