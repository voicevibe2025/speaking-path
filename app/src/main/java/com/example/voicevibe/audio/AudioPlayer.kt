package com.example.voicevibe.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AudioPlayer {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val queue = Channel<Pair<ByteArray, Int>>(capacity = Channel.UNLIMITED)

    @Volatile
    private var track: AudioTrack? = null
    @Volatile
    private var currentSampleRate: Int = 0
    private val trackMutex = Mutex()

    private var worker: Job? = scope.launch {
        for ((data, sampleRate) in queue) {
            if (!isActive) break
            ensureTrack(sampleRate)
            val t = track ?: continue
            // Write blocking on the audio thread, never on the WebSocket callback
            t.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) {
                t.play()
            }
        }
    }

    fun playPcm(data: ByteArray, sampleRate: Int = 24_000) {
        queue.trySend(data to sampleRate)
    }

    private suspend fun ensureTrack(sampleRate: Int) {
        trackMutex.withLock {
            if (track != null && currentSampleRate == sampleRate) return
            releaseInternal()
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuffer, sampleRate / 10 /* ~100ms */ * 2)
            track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setSampleRate(sampleRate)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            currentSampleRate = sampleRate
        }
    }

    fun stop() {
        if (trackMutex.tryLock()) {
            try {
                track?.let {
                    try { it.stop() } catch (_: Exception) {}
                    try { it.flush() } catch (_: Exception) {}
                }
            } finally {
                trackMutex.unlock()
            }
        }
    }

    fun release() {
        worker?.cancel()
        worker = null
        releaseInternal()
    }

    private fun releaseInternal() {
        track?.let {
            try { it.stop() } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        track = null
        currentSampleRate = 0
    }
}
