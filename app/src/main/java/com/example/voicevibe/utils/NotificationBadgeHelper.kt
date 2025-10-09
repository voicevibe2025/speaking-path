package com.example.voicevibe.utils

import android.content.Context
import me.leolin.shortcutbadger.ShortcutBadger
import timber.log.Timber

/**
 * Helper class for managing app icon notification badges
 * Shows the unread notification count on the app icon in the phone's home screen
 */
object NotificationBadgeHelper {
    
    /**
     * Update the app icon badge with the specified count
     * @param context Application context
     * @param count Number to display on the badge (0 will remove the badge)
     */
    fun updateBadge(context: Context, count: Int) {
        try {
            val badgeCount = count.coerceAtLeast(0) // Ensure count is not negative
            val success = ShortcutBadger.applyCount(context, badgeCount)
            
            if (success) {
                Timber.d("App icon badge updated successfully: $badgeCount")
            } else {
                Timber.w("Failed to update app icon badge. Device may not support badges.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating app icon badge")
        }
    }
    
    /**
     * Remove the badge from the app icon
     * @param context Application context
     */
    fun removeBadge(context: Context) {
        try {
            val success = ShortcutBadger.removeCount(context)
            if (success) {
                Timber.d("App icon badge removed successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing app icon badge")
        }
    }
    
    /**
     * Check if the current device supports notification badges
     * @param context Application context
     * @return true if badges are supported, false otherwise
     */
    fun isBadgeSupported(context: Context): Boolean {
        return ShortcutBadger.isBadgeCounterSupported(context)
    }
}
