package com.example.calendarnotes.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var reminderMinutesBefore: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTES, value).apply()

    /** Calendar.SUNDAY or Calendar.MONDAY */
    var weekStartDay: Int
        get() = prefs.getInt(KEY_WEEK_START, Calendar.SUNDAY)
        set(value) = prefs.edit().putInt(KEY_WEEK_START, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var googleAccountEmail: String?
        get() = prefs.getString(KEY_GOOGLE_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_GOOGLE_EMAIL, value).apply()

    var googleLastSyncMillis: Long
        get() = prefs.getLong(KEY_GOOGLE_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_GOOGLE_LAST_SYNC, value).apply()

    var googleAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_AUTO_SYNC, true)
        set(value) = prefs.edit().putBoolean(KEY_GOOGLE_AUTO_SYNC, value).apply()

    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    fun clearGoogleAccount() {
        prefs.edit()
            .remove(KEY_GOOGLE_EMAIL)
            .remove(KEY_GOOGLE_LAST_SYNC)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "calendar_notes_prefs"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
        private const val KEY_WEEK_START = "week_start_day"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GOOGLE_EMAIL = "google_account_email"
        private const val KEY_GOOGLE_LAST_SYNC = "google_last_sync_millis"
        private const val KEY_GOOGLE_AUTO_SYNC = "google_auto_sync_enabled"
        const val DEFAULT_REMINDER_MINUTES = 30
        const val GOOGLE_AUTO_SYNC_INTERVAL_MS = 15 * 60 * 1000L
    }
}
