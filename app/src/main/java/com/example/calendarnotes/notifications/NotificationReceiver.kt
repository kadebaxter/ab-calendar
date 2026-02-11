package com.example.calendarnotes.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.calendarnotes.MainActivity
import com.example.calendarnotes.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1)
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Event Reminder"
        val eventDescription = intent.getStringExtra(EXTRA_EVENT_DESCRIPTION) ?: ""
        val eventStartTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, 0)

        if (eventId == -1L) {
            return
        }

        showNotification(context, eventId, eventTitle, eventDescription, eventStartTime)
    }

    private fun showNotification(
        context: Context,
        eventId: Long,
        title: String,
        description: String,
        startTime: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create an intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format the time
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val timeString = timeFormat.format(java.util.Date(startTime))

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Starting at $timeString")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$description\n\nStarting at $timeString"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(eventId.toInt(), notificationBuilder.build())
    }

    companion object {
        const val CHANNEL_ID = "calendar_events_channel"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_DESCRIPTION = "event_description"
        const val EXTRA_EVENT_START_TIME = "event_start_time"
    }
}
