package com.example.voicevibe

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main Application class for VoiceVibe
 * Initializes Hilt dependency injection and Timber logging
 */
@HiltAndroidApp
class VoiceVibeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant a production tree that sends logs to crash reporting service
            Timber.plant(ProductionTree())
        }

        Timber.d("VoiceVibe Application started")
    }

    /**
     * Production logging tree that filters logs
     */
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings and errors in production
            if (priority >= android.util.Log.WARN) {
                // Send to crash reporting service like Sentry
                // For now, just use default logging
                android.util.Log.println(priority, tag, message)
            }
        }
    }
}
