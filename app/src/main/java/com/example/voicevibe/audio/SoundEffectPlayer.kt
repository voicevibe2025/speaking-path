package com.example.voicevibe.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundEffectPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Play a sound effect from res/raw.
     * Multiple rapid calls will interrupt the previous sound.
     */
    fun play(@RawRes soundRes: Int) {
        try {
            // Release any existing player
            mediaPlayer?.release()
            
            // Create and start new player
            mediaPlayer = MediaPlayer.create(context, soundRes)?.apply {
                setOnCompletionListener { mp ->
                    mp.release()
                }
                start()
            }
        } catch (e: Exception) {
            // Silently fail - sound effects are non-critical
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
