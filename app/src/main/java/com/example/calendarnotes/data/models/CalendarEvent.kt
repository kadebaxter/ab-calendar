package com.example.calendarnotes.data.models

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val categoryId: Long?,
    val noteId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val notificationEnabled: Boolean = true,
    val notificationMinutesBefore: Int = 30,
    /** Stable key: "calendarId|eventId" for Google imports; null for local events. */
    val googleEventKey: String? = null,
    val googleCalendarId: String? = null,
    val isAllDay: Boolean = false
) {
    val isFromGoogle: Boolean get() = !googleEventKey.isNullOrBlank()

    /**
     * True for marked all-day events, or legacy imports that span ~a full day
     * (e.g. Google birthdays stored as UTC midnights).
     */
    fun displaysAsAllDay(): Boolean {
        if (isAllDay) return true
        val duration = endTime - startTime
        return duration >= ALL_DAY_DURATION_THRESHOLD_MS
    }

    companion object {
        private const val ALL_DAY_DURATION_THRESHOLD_MS = 20L * 60 * 60 * 1000
    }
}
