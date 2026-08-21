package com.example.calendarnotes.google

import android.content.Context
import com.example.calendarnotes.data.models.CalendarEvent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import java.util.Calendar as JavaCalendar
import java.util.Collections

data class ImportedGoogleEvent(
    val googleEventKey: String,
    val googleCalendarId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val calendarName: String,
    val isAllDay: Boolean
)

data class GoogleSyncFetchResult(
    val events: List<ImportedGoogleEvent>,
    val errorMessage: String? = null
)

class GoogleCalendarImporter(private val context: Context) {

    fun fetchEvents(account: GoogleSignInAccount): GoogleSyncFetchResult {
        return try {
            val service = buildService(account)
            val calendars = service.calendarList().list().execute().items.orEmpty()
                .filter { it.selected != false && it.accessRole != "freeBusyReader" }

            val windowStart = System.currentTimeMillis() - PAST_WINDOW_MS
            val windowEnd = System.currentTimeMillis() + FUTURE_WINDOW_MS
            val imported = mutableListOf<ImportedGoogleEvent>()

            calendars.forEach { calendar ->
                val calendarId = calendar.id ?: return@forEach
                val calendarName = calendar.summary ?: calendarId
                var pageToken: String? = null
                do {
                    val response = service.events().list(calendarId)
                        .setTimeMin(DateTime(windowStart))
                        .setTimeMax(DateTime(windowEnd))
                        .setSingleEvents(true)
                        .setOrderBy("startTime")
                        .setMaxResults(250)
                        .setPageToken(pageToken)
                        .execute()

                    response.items.orEmpty().forEach { event ->
                        mapEvent(event, calendarId, calendarName)?.let { imported.add(it) }
                    }
                    pageToken = response.nextPageToken
                } while (!pageToken.isNullOrBlank())
            }

            GoogleSyncFetchResult(events = imported)
        } catch (e: Exception) {
            GoogleSyncFetchResult(
                events = emptyList(),
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }

    fun toLocalEvent(imported: ImportedGoogleEvent, categoryId: Long?): CalendarEvent {
        val description = buildString {
            if (imported.description.isNotBlank()) {
                append(imported.description.trim())
            }
            if (isNotEmpty()) append("\n\n")
            append("Imported from Google Calendar")
            if (imported.calendarName.isNotBlank()) {
                append(" · ")
                append(imported.calendarName)
            }
        }
        return CalendarEvent(
            title = imported.title.ifBlank { "(No title)" },
            description = description,
            startTime = imported.startTime,
            endTime = imported.endTime,
            categoryId = categoryId,
            notificationEnabled = false,
            googleEventKey = imported.googleEventKey,
            googleCalendarId = imported.googleCalendarId,
            isAllDay = imported.isAllDay
        )
    }

    private fun buildService(account: GoogleSignInAccount): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(CalendarScopes.CALENDAR_READONLY)
        )
        credential.selectedAccount = account.account
        return Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    private fun mapEvent(
        event: Event,
        calendarId: String,
        calendarName: String
    ): ImportedGoogleEvent? {
        if (event.status == "cancelled") return null
        val eventId = event.id ?: return null
        val times = eventTimes(event) ?: return null
        return ImportedGoogleEvent(
            googleEventKey = "$calendarId|$eventId",
            googleCalendarId = calendarId,
            title = event.summary.orEmpty(),
            description = event.description.orEmpty(),
            startTime = times.startMs,
            endTime = times.endMs,
            calendarName = calendarName,
            isAllDay = times.isAllDay
        )
    }

    private data class EventTimes(val startMs: Long, val endMs: Long, val isAllDay: Boolean)

    private fun eventTimes(event: Event): EventTimes? {
        val start = event.start ?: return null
        val end = event.end
        val startDateTime = start.dateTime
        if (startDateTime != null) {
            val startMs = startDateTime.value
            val endMs = end?.dateTime?.value ?: (startMs + DEFAULT_DURATION_MS)
            return EventTimes(
                startMs = startMs,
                endMs = endMs.coerceAtLeast(startMs + MIN_DURATION_MS),
                isAllDay = false
            )
        }

        val startDate = start.date ?: return null
        // Google all-day dates are calendar dates; map to local midnights (not UTC).
        val startMs = localMidnightFromGoogleDate(startDate)
        val endMs = end?.date?.let { localMidnightFromGoogleDate(it) } ?: (startMs + DAY_MS)
        return EventTimes(
            startMs = startMs,
            endMs = endMs.coerceAtLeast(startMs + DAY_MS),
            isAllDay = true
        )
    }

    private fun localMidnightFromGoogleDate(date: DateTime): Long {
        val raw = date.toStringRfc3339().take(10)
        val parts = raw.split("-")
        require(parts.size == 3) { "Unexpected Google date: $raw" }
        return JavaCalendar.getInstance().apply {
            set(JavaCalendar.YEAR, parts[0].toInt())
            set(JavaCalendar.MONTH, parts[1].toInt() - 1)
            set(JavaCalendar.DAY_OF_MONTH, parts[2].toInt())
            set(JavaCalendar.HOUR_OF_DAY, 0)
            set(JavaCalendar.MINUTE, 0)
            set(JavaCalendar.SECOND, 0)
            set(JavaCalendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val APP_NAME = "CalendarNotes"
        private const val PAST_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
        private const val FUTURE_WINDOW_MS = 365L * 24 * 60 * 60 * 1000
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val DEFAULT_DURATION_MS = 60L * 60 * 1000
        private const val MIN_DURATION_MS = 15L * 60 * 1000
    }
}
