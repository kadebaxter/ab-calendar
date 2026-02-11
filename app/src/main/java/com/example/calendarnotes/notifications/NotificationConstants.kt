package com.example.calendarnotes.notifications

/**
 * Helper class for managing event notification settings
 */
object NotificationConstants {
    // Default notification time before event (in minutes)
    const val DEFAULT_NOTIFICATION_MINUTES = 30
    
    // Predefined notification time options (in minutes)
    const val NOTIFY_5_MINUTES = 5
    const val NOTIFY_15_MINUTES = 15
    const val NOTIFY_30_MINUTES = 30
    const val NOTIFY_1_HOUR = 60
    const val NOTIFY_2_HOURS = 120
    const val NOTIFY_1_DAY = 1440
    
    /**
     * Get a list of common notification time options
     * @return List of pairs (label, minutes)
     */
    fun getNotificationTimeOptions(): List<Pair<String, Int>> {
        return listOf(
            "5 minutes before" to NOTIFY_5_MINUTES,
            "15 minutes before" to NOTIFY_15_MINUTES,
            "30 minutes before" to NOTIFY_30_MINUTES,
            "1 hour before" to NOTIFY_1_HOUR,
            "2 hours before" to NOTIFY_2_HOURS,
            "1 day before" to NOTIFY_1_DAY
        )
    }
    
    /**
     * Format minutes into human-readable text
     */
    fun formatMinutesToText(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes minutes"
            minutes < 1440 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""}"
            else -> "${minutes / 1440} day${if (minutes / 1440 > 1) "s" else ""}"
        }
    }
}
