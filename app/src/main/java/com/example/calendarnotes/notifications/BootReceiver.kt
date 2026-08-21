package com.example.calendarnotes.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.calendarnotes.CalendarNotesApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted - rescheduling notifications")
            
            // Reschedule all event notifications
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = CalendarNotesApplication.from(context).repository
                    val events = repository.getAllCalendarEvents()
                    NotificationScheduler.rescheduleAllNotifications(context.applicationContext, events)
                    Log.d("BootReceiver", "Successfully rescheduled ${events.size} event notifications")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule notifications: ${e.message}")
                }
            }
        }
    }
}
