package com.example.calendarnotes.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calendarnotes.data.models.CalendarEvent

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    const val ACTION_EVENT_REMINDER = "com.example.calendarnotes.ACTION_EVENT_REMINDER"

    /**
     * Schedule a notification for an event
     * @param context Application context
     * @param event The calendar event to schedule notification for
     */
    fun scheduleNotification(context: Context, event: CalendarEvent) {
        if (!event.notificationEnabled) {
            Log.d(TAG, "Notification disabled for event: ${event.id}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate the notification trigger time
        val notificationTime = event.startTime - (event.notificationMinutesBefore * 60 * 1000)

        // Don't schedule if the notification time has already passed
        if (notificationTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Notification time has passed for event: ${event.id}")
            return
        }

        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            eventId = event.id,
            title = event.title,
            description = event.description,
            startTime = event.startTime,
            includeAction = true,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        try {
            // Use setExactAndAllowWhileIdle for precise timing even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime,
                    pendingIntent
                )
            }

            val timeFormat = java.text.SimpleDateFormat("MMM dd, yyyy h:mm a", java.util.Locale.getDefault())
            Log.d(TAG, "Scheduled notification for event '${event.title}' at ${timeFormat.format(java.util.Date(notificationTime))}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule notification: ${e.message}")
        }
    }

    /**
     * Cancel a scheduled notification for an event and dismiss any shown notification.
     */
    fun cancelNotification(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel current-style alarms (with explicit action) and legacy ones (no action).
        cancelAlarmVariant(context, alarmManager, eventId, includeAction = true)
        cancelAlarmVariant(context, alarmManager, eventId, includeAction = false)

        // Also clear any notification already posted to the shade.
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(requestCodeFor(eventId))

        Log.d(TAG, "Cancelled notification for event: $eventId")
    }

    /**
     * Reschedule all notifications (useful after boot)
     * @param context Application context
     * @param events List of all calendar events
     */
    fun rescheduleAllNotifications(context: Context, events: List<CalendarEvent>) {
        Log.d(TAG, "Rescheduling all notifications for ${events.size} events")
        events.forEach { event ->
            scheduleNotification(context, event)
        }
    }

    /**
     * Update a notification by cancelling the old one and scheduling a new one
     * @param context Application context
     * @param event The updated calendar event
     */
    fun updateNotification(context: Context, event: CalendarEvent) {
        cancelNotification(context, event.id)
        scheduleNotification(context, event)
    }

    fun requestCodeFor(eventId: Long): Int = eventId.toInt()

    private fun cancelAlarmVariant(
        context: Context,
        alarmManager: AlarmManager,
        eventId: Long,
        includeAction: Boolean
    ) {
        val existing = buildAlarmPendingIntent(
            context = context,
            eventId = eventId,
            title = "",
            description = "",
            startTime = 0L,
            includeAction = includeAction,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
            return
        }

        val pendingIntent = buildAlarmPendingIntent(
            context = context,
            eventId = eventId,
            title = "",
            description = "",
            startTime = 0L,
            includeAction = includeAction,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        eventId: Long,
        title: String,
        description: String,
        startTime: Long,
        includeAction: Boolean,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            if (includeAction) {
                action = ACTION_EVENT_REMINDER
            }
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationReceiver.EXTRA_EVENT_TITLE, title)
            putExtra(NotificationReceiver.EXTRA_EVENT_DESCRIPTION, description)
            putExtra(NotificationReceiver.EXTRA_EVENT_START_TIME, startTime)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            intent,
            flags
        )
    }
}
