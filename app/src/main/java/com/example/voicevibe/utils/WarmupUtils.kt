package com.example.voicevibe.utils

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utilities to prewarm the backend ASR by sending a tiny silent WAV.
 */
object WarmupUtils {
    /**
     * Generate a small 16-bit PCM mono WAV of silence and return as Base64 (no wrap).
     */
    @JvmStatic
    fun generateSilentWavBase64(durationSec: Double = 0.2, sampleRate: Int = 16000): String {
        val numChannels = 1
        val bitsPerSample = 16
        val bytesPerSample = bitsPerSample / 8
        val numSamples = (durationSec * sampleRate).toInt().coerceAtLeast(1)
        val dataSize = numSamples * numChannels * bytesPerSample
        val byteRate = sampleRate * numChannels * bytesPerSample
        val blockAlign = numChannels * bytesPerSample
        val totalDataLen = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // Audio format: PCM
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }

        val out = ByteArrayOutputStream(44 + dataSize)
        out.write(header.array())
        // Data chunk: silence (zeros)
        out.write(ByteArray(dataSize))

        val bytes = out.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
