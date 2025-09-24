package com.example.voicevibe.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class AudioRecorder(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    @Volatile
    private var recordingJob: Job? = null

    @Volatile
    var isRecording: Boolean = false
        private set

    private var audioRecord: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    fun start(
        sampleRateHz: Int = 16_000,
        onChunk: (ByteArray) -> Unit
    ) {
        if (isRecording) return

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Aim for ~100ms chunks (16k * 2 bytes * 0.1 = 3200 bytes)
        val chunkSize = maxOf(minBuffer, 3200)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            chunkSize
        )
        audioRecord = recorder
        // Enable hardware AEC/NS/AGC when available
        try {
            val sessionId = recorder.audioSessionId
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)
                aec?.enabled = true
                Timber.tag("AudioRecorder").d("AEC enabled")
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId)
                ns?.enabled = true
                Timber.tag("AudioRecorder").d("NoiseSuppressor enabled")
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)
                agc?.enabled = true
                Timber.tag("AudioRecorder").d("AGC enabled")
            }
        } catch (e: Exception) {
            Timber.tag("AudioRecorder").w(e, "Failed to init audio effects")
        }
        try {
            recorder.startRecording()
            isRecording = true
            Timber.tag("AudioRecorder").d("Recording started @%d Hz, chunk=%d", sampleRateHz, chunkSize)
        } catch (e: Exception) {
            Timber.tag("AudioRecorder").e(e, "Failed to start recording")
            isRecording = false
            return
        }

        // Always run capture loop on IO, even if provided scope is Main
        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(chunkSize)
            while (isActive && isRecording) {
                val read = try { recorder.read(buffer, 0, buffer.size) } catch (e: Exception) {
                    Timber.tag("AudioRecorder").e(e, "read() failed")
                    break
                }
                if (read > 0) {
                    val copy = buffer.copyOf(read)
                    onChunk(copy)
                }
            }
            Timber.tag("AudioRecorder").d("Recording loop ended")
        }
    }

    suspend fun stop() {
        if (!isRecording) return
        isRecording = false
        recordingJob?.let {
            try { it.cancelAndJoin() } catch (_: Exception) {}
        }
        recordingJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        Timber.tag("AudioRecorder").d("Recording stopped")
        try { aec?.release() } catch (_: Exception) {}
        try { ns?.release() } catch (_: Exception) {}
        try { agc?.release() } catch (_: Exception) {}
        aec = null
        ns = null
        agc = null
        audioRecord?.release()
        audioRecord = null
    }
}
