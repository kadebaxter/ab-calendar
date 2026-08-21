package com.example.calendarnotes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.calendarnotes.data.AppPreferences
import com.example.calendarnotes.data.repository.CalendarNotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarNotesApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: CalendarNotesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = CalendarNotesRepository(this)
        applicationScope.launch {
            repository.refreshAll()
        }
        AppPreferences(this).applyTheme()
        createNotificationChannel()
    }

    companion object {
        fun from(context: Context): CalendarNotesApplication {
            return context.applicationContext as CalendarNotesApplication
        }
    }

    private fun createNotificationChannel() {
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "calendar_events_channel"
            val channelName = "Event Reminders"
            val channelDescription = "Notifications for upcoming calendar events"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
