package com.example.calendarnotes.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calendarnotes.data.models.CalendarEvent

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

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

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(NotificationReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(NotificationReceiver.EXTRA_EVENT_DESCRIPTION, event.description)
            putExtra(NotificationReceiver.EXTRA_EVENT_START_TIME, event.startTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
     * Cancel a scheduled notification for an event
     * @param context Application context
     * @param eventId The ID of the event whose notification should be cancelled
     */
    fun cancelNotification(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        
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
}
