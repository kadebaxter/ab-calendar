package com.example.calendarnotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.calendarnotes.CalendarNotesApplication
import com.example.calendarnotes.data.AppPreferences
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.data.models.Category
import com.example.calendarnotes.data.models.ContactHistoryEntry
import com.example.calendarnotes.data.models.Note
import com.example.calendarnotes.data.models.Person
import com.example.calendarnotes.data.repository.CalendarNotesRepository
import com.example.calendarnotes.google.GoogleCalendarAuth
import com.example.calendarnotes.google.GoogleCalendarImporter
import com.example.calendarnotes.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GoogleCalendarSyncResult(
    val success: Boolean,
    val inserted: Int = 0,
    val updated: Int = 0,
    val removed: Int = 0,
    val message: String? = null
)

class CalendarNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarNotesApplication.from(application).repository
    private val preferences = AppPreferences(application)
    private val googleImporter = GoogleCalendarImporter(application)

    val categories: LiveData<List<Category>> = repository.categories.asLiveData()
    val notes: LiveData<List<Note>> = repository.notes.asLiveData()
    val people: LiveData<List<Person>> = repository.people.asLiveData()
    val lastEventByPersonId: LiveData<Map<Long, Long>> = repository.lastEventByPersonId.asLiveData()
    val calendarEvents: LiveData<List<CalendarEvent>> = repository.calendarEvents.asLiveData()

    init {
        // Ensure cache is warm even if Application warm-up hasn't finished yet.
        loadCategories()
        loadNotes()
        loadPeople()
        loadCalendarEvents()
    }

    fun loadCategories() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.refreshCategories()
            }
        }
    }

    fun addCategory(name: String, color: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertCategory(Category(name = name, color = color))
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateCategory(category)
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCategory(id)
            }
        }
    }

    fun loadNotes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.refreshNotes()
            }
        }
    }

    fun addNote(categoryId: Long?, title: String, content: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertNote(
                    Note(categoryId = categoryId, title = title, content = content)
                )
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateNote(note)
            }
        }
    }

    fun toggleNoteCompletion(note: Note) {
        updateNote(note.copy(isCompleted = !note.isCompleted))
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteNote(id)
            }
        }
    }

    fun loadCalendarEvents() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.refreshCalendarEvents()
                repository.refreshLastEventByPersonId()
            }
        }
    }

    fun loadPeople() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.refreshPeople()
                repository.refreshLastEventByPersonId()
            }
        }
    }

    fun loadLastEventTimesByPerson() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.refreshLastEventByPersonId()
            }
        }
    }

    fun addPerson(person: Person) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertPerson(person)
            }
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updatePerson(person)
            }
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deletePerson(id)
            }
        }
    }

    fun getPersonById(id: Long, callback: (Person?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getPersonById(id)
            }
            callback(result)
        }
    }

    fun getContactHistory(personId: Long, callback: (List<ContactHistoryEntry>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getContactHistoryForPerson(personId)
            }
            callback(result)
        }
    }

    fun addContactHistory(personId: Long, summary: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertContactHistory(
                    ContactHistoryEntry(personId = personId, summary = summary)
                )
            }
            onDone?.invoke()
        }
    }

    fun deleteContactHistory(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteContactHistory(id)
            }
        }
    }

    fun getEventsForPerson(personId: Long, callback: (List<CalendarEvent>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEventsForPerson(personId)
            }
            callback(result)
        }
    }

    fun getPeopleForEvent(eventId: Long, callback: (List<Person>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getPeopleForEvent(eventId)
            }
            callback(result)
        }
    }

    fun setPeopleForEvent(eventId: Long, personIds: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setPeopleForEvent(eventId, personIds)
            }
        }
    }

    fun addPersonToEvent(eventId: Long, personId: Long, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addPersonToEvent(eventId, personId)
            }
            onDone?.invoke()
        }
    }

    fun getNoteById(id: Long, callback: (Note?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getNoteById(id)
            }
            callback(result)
        }
    }

    fun getCalendarEventById(id: Long, callback: (CalendarEvent?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getCalendarEventById(id)
            }
            callback(result)
        }
    }

    fun addCalendarEvent(
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        categoryId: Long?,
        personIds: List<Long> = emptyList(),
        noteId: Long? = null,
        noteIds: List<Long> = emptyList(),
        onDone: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val eventId = withContext(Dispatchers.IO) {
                val linkedNotes = (noteIds + listOfNotNull(noteId)).distinct()
                val id = repository.insertCalendarEvent(
                    CalendarEvent(
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        categoryId = categoryId,
                        noteId = linkedNotes.firstOrNull(),
                        notificationEnabled = true,
                        notificationMinutesBefore = preferences.reminderMinutesBefore
                    )
                )
                if (id > 0 && personIds.isNotEmpty()) {
                    repository.setPeopleForEvent(id, personIds)
                }
                if (id > 0 && linkedNotes.isNotEmpty()) {
                    repository.setNotesForEvent(id, linkedNotes)
                }
                id
            }

            if (eventId > 0) {
                withContext(Dispatchers.IO) {
                    repository.getCalendarEventById(eventId)?.let {
                        NotificationScheduler.scheduleNotification(getApplication(), it)
                    }
                }
            }

            onDone?.invoke(eventId)
        }
    }

    fun createEventFromNote(noteId: Long, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            val reminderMinutes = preferences.reminderMinutesBefore
            val eventId = withContext(Dispatchers.IO) {
                repository.createEventFromNote(noteId, startTime, endTime, reminderMinutes)
            }

            if (eventId > 0) {
                withContext(Dispatchers.IO) {
                    repository.getCalendarEventById(eventId)?.let {
                        NotificationScheduler.scheduleNotification(getApplication(), it)
                    }
                }
            }
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            NotificationScheduler.cancelNotification(getApplication(), id)

            withContext(Dispatchers.IO) {
                repository.deleteCalendarEvent(id)
            }
        }
    }

    fun updateEventTime(id: Long, newStartTime: Long, newEndTime: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateEventTime(id, newStartTime, newEndTime)
                repository.getCalendarEventById(id)?.let {
                    NotificationScheduler.updateNotification(getApplication(), it)
                }
            }
        }
    }

    fun updateCalendarEvent(
        event: CalendarEvent,
        personIds: List<Long>? = null,
        noteIds: List<Long>? = null,
        onDone: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val withLegacyNote = if (noteIds != null) {
                    event.copy(noteId = noteIds.firstOrNull())
                } else {
                    event
                }
                repository.updateCalendarEvent(withLegacyNote)
                if (personIds != null) {
                    repository.setPeopleForEvent(event.id, personIds)
                }
                if (noteIds != null) {
                    repository.setNotesForEvent(event.id, noteIds)
                }
                NotificationScheduler.updateNotification(getApplication(), withLegacyNote)
            }
            onDone?.invoke()
        }
    }

    fun getNotesForEvent(eventId: Long, callback: (List<Note>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getNotesForEvent(eventId)
            }
            callback(result)
        }
    }

    fun setNotesForEvent(eventId: Long, noteIds: List<Long>, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setNotesForEvent(eventId, noteIds)
            }
            onDone?.invoke()
        }
    }

    fun getEventsForNote(noteId: Long, callback: (List<CalendarEvent>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEventsForNote(noteId)
            }
            callback(result)
        }
    }

    fun addNoteReturningId(
        categoryId: Long?,
        title: String,
        content: String,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                repository.insertNote(
                    Note(categoryId = categoryId, title = title, content = content)
                )
            }
            onDone(id)
        }
    }

    fun getEventsForDateRange(
        startTime: Long,
        endTime: Long,
        callback: (List<CalendarEvent>) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEventsByDateRange(startTime, endTime)
            }
            callback(result)
        }
    }

    fun isGoogleCalendarConnected(): Boolean {
        val account = GoogleCalendarAuth.lastSignedInAccount(getApplication())
        return GoogleCalendarAuth.hasCalendarAccess(account)
    }

    fun googleAccountEmail(): String? {
        return preferences.googleAccountEmail
            ?: GoogleCalendarAuth.lastSignedInAccount(getApplication())?.email
    }

    fun googleLastSyncMillis(): Long = preferences.googleLastSyncMillis

    fun googleAutoSyncEnabled(): Boolean = preferences.googleAutoSyncEnabled

    fun setGoogleAutoSyncEnabled(enabled: Boolean) {
        preferences.googleAutoSyncEnabled = enabled
    }

    fun onGoogleSignedIn(email: String?) {
        preferences.googleAccountEmail = email
    }

    fun syncGoogleCalendarIfDue(force: Boolean = false, onResult: ((GoogleCalendarSyncResult) -> Unit)? = null) {
        if (!isGoogleCalendarConnected()) {
            onResult?.invoke(
                GoogleCalendarSyncResult(success = false, message = "Not connected to Google Calendar")
            )
            return
        }
        if (!force && !preferences.googleAutoSyncEnabled) {
            onResult?.invoke(
                GoogleCalendarSyncResult(success = false, message = "Auto-sync is off")
            )
            return
        }
        val elapsed = System.currentTimeMillis() - preferences.googleLastSyncMillis
        if (!force && elapsed < AppPreferences.GOOGLE_AUTO_SYNC_INTERVAL_MS) {
            onResult?.invoke(
                GoogleCalendarSyncResult(success = true, message = "Recently synced")
            )
            return
        }
        syncGoogleCalendar(onResult)
    }

    fun syncGoogleCalendar(onResult: ((GoogleCalendarSyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            val account = GoogleCalendarAuth.lastSignedInAccount(getApplication())
            if (!GoogleCalendarAuth.hasCalendarAccess(account) || account == null) {
                onResult?.invoke(
                    GoogleCalendarSyncResult(
                        success = false,
                        message = "Sign in to Google Calendar in Settings first"
                    )
                )
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                val fetch = googleImporter.fetchEvents(account)
                if (fetch.errorMessage != null) {
                    return@withContext GoogleCalendarSyncResult(
                        success = false,
                        message = fetch.errorMessage
                    )
                }

                val categoryId = repository.getOrCreateGoogleCategoryId()
                var inserted = 0
                var updated = 0
                val keys = mutableSetOf<String>()

                fetch.events.forEach { imported ->
                    keys.add(imported.googleEventKey)
                    when (
                        repository.upsertGoogleEvent(
                            googleImporter.toLocalEvent(imported, categoryId)
                        )
                    ) {
                        is CalendarNotesRepository.UpsertResult.Inserted -> inserted++
                        is CalendarNotesRepository.UpsertResult.Updated -> updated++
                        CalendarNotesRepository.UpsertResult.Skipped -> Unit
                    }
                }

                val removed = repository.deleteGoogleEventsNotIn(keys)
                // Batch upserts skip per-row notify; publish once at the end.
                repository.refreshCalendarEvents()
                repository.refreshLastEventByPersonId()
                preferences.googleLastSyncMillis = System.currentTimeMillis()
                preferences.googleAccountEmail = account.email

                GoogleCalendarSyncResult(
                    success = true,
                    inserted = inserted,
                    updated = updated,
                    removed = removed
                )
            }

            onResult?.invoke(result)
        }
    }

    fun disconnectGoogleCalendar(removeImportedEvents: Boolean, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (removeImportedEvents) {
                    repository.deleteAllGoogleEvents()
                }
                preferences.clearGoogleAccount()
            }
            onDone?.invoke()
        }
    }
}
