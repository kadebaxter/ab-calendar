package com.example.calendarnotes.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.calendarnotes.data.database.DatabaseHelper
import com.example.calendarnotes.data.models.CalendarEvent
import com.example.calendarnotes.data.models.Category
import com.example.calendarnotes.data.models.ContactHistoryEntry
import com.example.calendarnotes.data.models.Note
import com.example.calendarnotes.data.models.Person
import com.example.calendarnotes.data.models.PersonStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped data layer. List StateFlows are the single source of truth for UI;
 * every mutating call refreshes the relevant flows so all screens stay in sync.
 */
class CalendarNotesRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _people = MutableStateFlow<List<Person>>(emptyList())
    val people: StateFlow<List<Person>> = _people.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _lastEventByPersonId = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val lastEventByPersonId: StateFlow<Map<Long, Long>> = _lastEventByPersonId.asStateFlow()

    fun refreshAll() {
        refreshCategories()
        refreshNotes()
        refreshPeople()
        refreshCalendarEvents()
        refreshLastEventByPersonId()
    }

    fun refreshCategories() {
        _categories.value = getAllCategories()
    }

    fun refreshNotes() {
        _notes.value = getAllNotes()
    }

    fun refreshPeople() {
        _people.value = getAllPeople()
    }

    fun refreshCalendarEvents() {
        _calendarEvents.value = getAllCalendarEvents()
    }

    fun refreshLastEventByPersonId() {
        _lastEventByPersonId.value = getLastEventStartTimesByPersonId()
    }

    // Category operations
    fun insertCategory(category: Category): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CAT_NAME, category.name)
            put(DatabaseHelper.COL_CAT_COLOR, category.color)
            put(DatabaseHelper.COL_CAT_CREATED_AT, category.createdAt)
        }
        val id = db.insert(DatabaseHelper.TABLE_CATEGORIES, null, values)
        if (id > 0) refreshCategories()
        return id
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CATEGORIES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_CAT_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                categories.add(
                    Category(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_NAME)),
                        color = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_COLOR)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_CAT_CREATED_AT))
                    )
                )
            }
        }
        return categories
    }

    fun deleteCategory(id: Long): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_CATEGORIES,
            "${DatabaseHelper.COL_CAT_ID} = ?",
            arrayOf(id.toString())
        )
        if (rows > 0) {
            refreshCategories()
            refreshNotes()
        }
        return rows
    }

    fun updateCategory(category: Category): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CAT_NAME, category.name)
            put(DatabaseHelper.COL_CAT_COLOR, category.color)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_CATEGORIES,
            values,
            "${DatabaseHelper.COL_CAT_ID} = ?",
            arrayOf(category.id.toString())
        )
        if (rows > 0) {
            refreshCategories()
            refreshNotes()
        }
        return rows
    }

    // Note operations
    fun insertNote(note: Note): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTE_CATEGORY_ID, note.categoryId)
            put(DatabaseHelper.COL_NOTE_TITLE, note.title)
            put(DatabaseHelper.COL_NOTE_CONTENT, note.content)
            put(DatabaseHelper.COL_NOTE_IS_COMPLETED, if (note.isCompleted) 1 else 0)
            put(DatabaseHelper.COL_NOTE_CREATED_AT, note.createdAt)
            put(DatabaseHelper.COL_NOTE_UPDATED_AT, note.updatedAt)
        }
        val id = db.insert(DatabaseHelper.TABLE_NOTES, null, values)
        if (id > 0) refreshNotes()
        return id
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_NOTE_IS_COMPLETED} ASC, ${DatabaseHelper.COL_NOTE_UPDATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                notes.add(cursorToNote(it))
            }
        }
        return notes
    }

    fun getNoteById(id: Long): Note? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTES,
            null,
            "${DatabaseHelper.COL_NOTE_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToNote(it)
            }
        }
        return null
    }

    fun updateNote(note: Note): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTE_TITLE, note.title)
            put(DatabaseHelper.COL_NOTE_CONTENT, note.content)
            put(DatabaseHelper.COL_NOTE_CATEGORY_ID, note.categoryId)
            put(DatabaseHelper.COL_NOTE_IS_COMPLETED, if (note.isCompleted) 1 else 0)
            put(DatabaseHelper.COL_NOTE_UPDATED_AT, System.currentTimeMillis())
        }
        val rows = db.update(
            DatabaseHelper.TABLE_NOTES,
            values,
            "${DatabaseHelper.COL_NOTE_ID} = ?",
            arrayOf(note.id.toString())
        )
        if (rows > 0) refreshNotes()
        return rows
    }

    fun deleteNote(id: Long): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_NOTES,
            "${DatabaseHelper.COL_NOTE_ID} = ?",
            arrayOf(id.toString())
        )
        if (rows > 0) {
            refreshNotes()
            refreshCalendarEvents()
        }
        return rows
    }

    private fun cursorToNote(cursor: Cursor): Note {
        return Note(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_ID)),
            categoryId = if (cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY_ID))) null
            else cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT)) ?: "",
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_IS_COMPLETED)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_UPDATED_AT))
        )
    }

    // Calendar Event operations
    fun insertCalendarEvent(event: CalendarEvent): Long {
        val id = insertCalendarEventRow(event)
        if (id > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return id
    }

    /** Insert without notifying observers — for batch Google sync. */
    private fun insertCalendarEventRow(event: CalendarEvent): Long {
        val db = dbHelper.writableDatabase
        return db.insert(DatabaseHelper.TABLE_CALENDAR_EVENTS, null, eventValues(event))
    }

    fun getAllCalendarEvents(): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_EVENT_START_TIME} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    fun getEventsByDateRange(startTime: Long, endTime: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = dbHelper.readableDatabase
        // Overlap: event starts before range end and ends after range start.
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            null,
            "${DatabaseHelper.COL_EVENT_START_TIME} < ? AND ${DatabaseHelper.COL_EVENT_END_TIME} > ?",
            arrayOf(endTime.toString(), startTime.toString()),
            null, null,
            "${DatabaseHelper.COL_EVENT_START_TIME} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    fun updateCalendarEvent(event: CalendarEvent): Int {
        val rows = updateCalendarEventRow(event)
        if (rows > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return rows
    }

    /** Update without notifying observers — for batch Google sync. */
    private fun updateCalendarEventRow(event: CalendarEvent): Int {
        val db = dbHelper.writableDatabase
        return db.update(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            eventValues(event, includeCreatedAt = false),
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(event.id.toString())
        )
    }

    fun getCalendarEventByGoogleKey(googleEventKey: String): CalendarEvent? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            null,
            "${DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY} = ?",
            arrayOf(googleEventKey),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) return cursorToEvent(it)
        }
        return null
    }

    /**
     * Insert or update a Google-imported event. Preserves local id, category, note link,
     * people, and notification prefs on update.
     */
    fun upsertGoogleEvent(event: CalendarEvent): UpsertResult {
        val key = event.googleEventKey ?: return UpsertResult.Skipped
        val existing = getCalendarEventByGoogleKey(key)
        return if (existing == null) {
            val id = insertCalendarEventRow(event)
            if (id > 0) UpsertResult.Inserted(id) else UpsertResult.Skipped
        } else {
            val merged = event.copy(
                id = existing.id,
                categoryId = existing.categoryId,
                noteId = existing.noteId,
                createdAt = existing.createdAt,
                notificationEnabled = existing.notificationEnabled,
                notificationMinutesBefore = existing.notificationMinutesBefore
            )
            updateCalendarEventRow(merged)
            UpsertResult.Updated(existing.id)
        }
    }

    fun getGoogleEventKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            arrayOf(DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY),
            "${DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY} IS NOT NULL",
            null, null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                val key = it.getString(0)
                if (!key.isNullOrBlank()) keys.add(key)
            }
        }
        return keys
    }

    fun deleteGoogleEventsNotIn(keysToKeep: Set<String>): Int {
        val db = dbHelper.writableDatabase
        val existing = getGoogleEventKeys()
        val toDelete = existing - keysToKeep
        var deleted = 0
        toDelete.forEach { key ->
            deleted += db.delete(
                DatabaseHelper.TABLE_CALENDAR_EVENTS,
                "${DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY} = ?",
                arrayOf(key)
            )
        }
        if (deleted > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return deleted
    }

    fun deleteAllGoogleEvents(): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            "${DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY} IS NOT NULL",
            null
        )
        if (rows > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return rows
    }

    fun getOrCreateGoogleCategoryId(): Long {
        val existing = getAllCategories().firstOrNull { it.name.equals(GOOGLE_CATEGORY_NAME, ignoreCase = true) }
        if (existing != null) return existing.id
        return insertCategory(
            Category(name = GOOGLE_CATEGORY_NAME, color = GOOGLE_CATEGORY_COLOR)
        )
    }

    private fun eventValues(event: CalendarEvent, includeCreatedAt: Boolean = true): ContentValues {
        return ContentValues().apply {
            put(DatabaseHelper.COL_EVENT_TITLE, event.title)
            put(DatabaseHelper.COL_EVENT_DESCRIPTION, event.description)
            put(DatabaseHelper.COL_EVENT_START_TIME, event.startTime)
            put(DatabaseHelper.COL_EVENT_END_TIME, event.endTime)
            put(DatabaseHelper.COL_EVENT_CATEGORY_ID, event.categoryId)
            put(DatabaseHelper.COL_EVENT_NOTE_ID, event.noteId)
            if (includeCreatedAt) {
                put(DatabaseHelper.COL_EVENT_CREATED_AT, event.createdAt)
            }
            put(DatabaseHelper.COL_EVENT_NOTIFICATION_ENABLED, if (event.notificationEnabled) 1 else 0)
            put(DatabaseHelper.COL_EVENT_NOTIFICATION_MINUTES_BEFORE, event.notificationMinutesBefore)
            if (event.googleEventKey.isNullOrBlank()) {
                putNull(DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY)
            } else {
                put(DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY, event.googleEventKey)
            }
            if (event.googleCalendarId.isNullOrBlank()) {
                putNull(DatabaseHelper.COL_EVENT_GOOGLE_CALENDAR_ID)
            } else {
                put(DatabaseHelper.COL_EVENT_GOOGLE_CALENDAR_ID, event.googleCalendarId)
            }
            put(DatabaseHelper.COL_EVENT_IS_ALL_DAY, if (event.isAllDay) 1 else 0)
        }
    }

    sealed class UpsertResult {
        data class Inserted(val id: Long) : UpsertResult()
        data class Updated(val id: Long) : UpsertResult()
        data object Skipped : UpsertResult()
    }

    companion object {
        private const val GOOGLE_CATEGORY_NAME = "Google"
        private const val GOOGLE_CATEGORY_COLOR = "#4285F4"
    }

    fun deleteCalendarEvent(id: Long): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(id.toString())
        )
        if (rows > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return rows
    }

    fun getCalendarEventById(id: Long): CalendarEvent? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            null,
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return cursorToEvent(it)
            }
        }
        return null
    }

    fun updateEventTime(id: Long, newStartTime: Long, newEndTime: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EVENT_START_TIME, newStartTime)
            put(DatabaseHelper.COL_EVENT_END_TIME, newEndTime)
        }
        val rows = db.update(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            values,
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(id.toString())
        )
        if (rows > 0) {
            refreshCalendarEvents()
            refreshLastEventByPersonId()
        }
        return rows
    }

    fun createEventFromNote(
        noteId: Long,
        startTime: Long,
        endTime: Long,
        notificationMinutesBefore: Int = 30
    ): Long {
        val note = getNoteById(noteId) ?: return -1
        return insertCalendarEvent(
            CalendarEvent(
                title = note.title,
                description = note.content,
                startTime = startTime,
                endTime = endTime,
                categoryId = note.categoryId,
                noteId = noteId,
                notificationMinutesBefore = notificationMinutesBefore
            )
        )
    }

    private fun cursorToEvent(cursor: Cursor): CalendarEvent {
        val googleKeyIndex = cursor.getColumnIndex(DatabaseHelper.COL_EVENT_GOOGLE_EVENT_KEY)
        val googleCalIndex = cursor.getColumnIndex(DatabaseHelper.COL_EVENT_GOOGLE_CALENDAR_ID)
        return CalendarEvent(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_DESCRIPTION)) ?: "",
            startTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_START_TIME)),
            endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_END_TIME)),
            categoryId = if (cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID))) null
            else cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID)),
            noteId = if (cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NOTE_ID))) null
            else cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NOTE_ID)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CREATED_AT)),
            notificationEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NOTIFICATION_ENABLED)) == 1,
            notificationMinutesBefore = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_NOTIFICATION_MINUTES_BEFORE)),
            googleEventKey = if (googleKeyIndex >= 0 && !cursor.isNull(googleKeyIndex)) {
                cursor.getString(googleKeyIndex)
            } else {
                null
            },
            googleCalendarId = if (googleCalIndex >= 0 && !cursor.isNull(googleCalIndex)) {
                cursor.getString(googleCalIndex)
            } else {
                null
            },
            isAllDay = run {
                val index = cursor.getColumnIndex(DatabaseHelper.COL_EVENT_IS_ALL_DAY)
                index >= 0 && !cursor.isNull(index) && cursor.getInt(index) == 1
            }
        )
    }

    // People
    fun insertPerson(person: Person): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PERSON_NAME, person.name)
            put(DatabaseHelper.COL_PERSON_PHONE, person.phone)
            put(DatabaseHelper.COL_PERSON_EMAIL, person.email)
            put(DatabaseHelper.COL_PERSON_ADDRESS, person.address)
            put(DatabaseHelper.COL_PERSON_NOTES, person.notes)
            put(DatabaseHelper.COL_PERSON_STATUS, person.status.key)
            put(DatabaseHelper.COL_PERSON_CREATED_AT, person.createdAt)
            put(DatabaseHelper.COL_PERSON_UPDATED_AT, person.updatedAt)
        }
        val id = db.insert(DatabaseHelper.TABLE_PEOPLE, null, values)
        if (id > 0) refreshPeople()
        return id
    }

    fun getAllPeople(): List<Person> {
        val people = mutableListOf<Person>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PEOPLE,
            null, null, null, null, null,
            "${DatabaseHelper.COL_PERSON_NAME} COLLATE NOCASE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                people.add(cursorToPerson(it))
            }
        }
        return people
    }

    fun getPersonById(id: Long): Person? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_PEOPLE,
            null,
            "${DatabaseHelper.COL_PERSON_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) return cursorToPerson(it)
        }
        return null
    }

    fun updatePerson(person: Person): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_PERSON_NAME, person.name)
            put(DatabaseHelper.COL_PERSON_PHONE, person.phone)
            put(DatabaseHelper.COL_PERSON_EMAIL, person.email)
            put(DatabaseHelper.COL_PERSON_ADDRESS, person.address)
            put(DatabaseHelper.COL_PERSON_NOTES, person.notes)
            put(DatabaseHelper.COL_PERSON_STATUS, person.status.key)
            put(DatabaseHelper.COL_PERSON_UPDATED_AT, System.currentTimeMillis())
        }
        val rows = db.update(
            DatabaseHelper.TABLE_PEOPLE,
            values,
            "${DatabaseHelper.COL_PERSON_ID} = ?",
            arrayOf(person.id.toString())
        )
        if (rows > 0) refreshPeople()
        return rows
    }

    fun deletePerson(id: Long): Int {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_PEOPLE,
            "${DatabaseHelper.COL_PERSON_ID} = ?",
            arrayOf(id.toString())
        )
        if (rows > 0) {
            refreshPeople()
            refreshLastEventByPersonId()
        }
        return rows
    }

    private fun cursorToPerson(cursor: Cursor): Person {
        return Person(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_NAME)),
            phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_PHONE)) ?: "",
            email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_EMAIL)) ?: "",
            address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_ADDRESS)) ?: "",
            notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_NOTES)) ?: "",
            status = PersonStatus.fromKey(
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_STATUS))
            ),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PERSON_UPDATED_AT))
        )
    }

    // Contact history
    fun insertContactHistory(entry: ContactHistoryEntry): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_HISTORY_PERSON_ID, entry.personId)
            put(DatabaseHelper.COL_HISTORY_SUMMARY, entry.summary)
            put(DatabaseHelper.COL_HISTORY_TIMESTAMP, entry.timestamp)
        }
        return db.insert(DatabaseHelper.TABLE_CONTACT_HISTORY, null, values)
    }

    fun getContactHistoryForPerson(personId: Long): List<ContactHistoryEntry> {
        val entries = mutableListOf<ContactHistoryEntry>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CONTACT_HISTORY,
            null,
            "${DatabaseHelper.COL_HISTORY_PERSON_ID} = ?",
            arrayOf(personId.toString()),
            null, null,
            "${DatabaseHelper.COL_HISTORY_TIMESTAMP} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                entries.add(
                    ContactHistoryEntry(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_HISTORY_ID)),
                        personId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_HISTORY_PERSON_ID)),
                        summary = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_HISTORY_SUMMARY)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_HISTORY_TIMESTAMP))
                    )
                )
            }
        }
        return entries
    }

    fun deleteContactHistory(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DatabaseHelper.TABLE_CONTACT_HISTORY,
            "${DatabaseHelper.COL_HISTORY_ID} = ?",
            arrayOf(id.toString())
        )
    }

    // Event <-> people
    fun setPeopleForEvent(eventId: Long, personIds: List<Long>) {
        val db = dbHelper.writableDatabase
        db.delete(
            DatabaseHelper.TABLE_EVENT_PEOPLE,
            "${DatabaseHelper.COL_EP_EVENT_ID} = ?",
            arrayOf(eventId.toString())
        )
        for (personId in personIds.distinct()) {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_EP_EVENT_ID, eventId)
                put(DatabaseHelper.COL_EP_PERSON_ID, personId)
            }
            db.insert(DatabaseHelper.TABLE_EVENT_PEOPLE, null, values)
        }
        refreshLastEventByPersonId()
    }

    fun getPeopleForEvent(eventId: Long): List<Person> {
        val people = mutableListOf<Person>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT p.* FROM ${DatabaseHelper.TABLE_PEOPLE} p
            INNER JOIN ${DatabaseHelper.TABLE_EVENT_PEOPLE} ep
                ON p.${DatabaseHelper.COL_PERSON_ID} = ep.${DatabaseHelper.COL_EP_PERSON_ID}
            WHERE ep.${DatabaseHelper.COL_EP_EVENT_ID} = ?
            ORDER BY p.${DatabaseHelper.COL_PERSON_NAME} COLLATE NOCASE ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(eventId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                people.add(cursorToPerson(it))
            }
        }
        return people
    }

    fun getEventsForPerson(personId: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT e.* FROM ${DatabaseHelper.TABLE_CALENDAR_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_EVENT_PEOPLE} ep
                ON e.${DatabaseHelper.COL_EVENT_ID} = ep.${DatabaseHelper.COL_EP_EVENT_ID}
            WHERE ep.${DatabaseHelper.COL_EP_PERSON_ID} = ?
            ORDER BY e.${DatabaseHelper.COL_EVENT_START_TIME} DESC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(personId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    /** Most recent linked event start time per person id. */
    fun getLastEventStartTimesByPersonId(): Map<Long, Long> {
        val result = mutableMapOf<Long, Long>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT ep.${DatabaseHelper.COL_EP_PERSON_ID},
                   MAX(e.${DatabaseHelper.COL_EVENT_START_TIME}) AS last_start
            FROM ${DatabaseHelper.TABLE_EVENT_PEOPLE} ep
            INNER JOIN ${DatabaseHelper.TABLE_CALENDAR_EVENTS} e
                ON e.${DatabaseHelper.COL_EVENT_ID} = ep.${DatabaseHelper.COL_EP_EVENT_ID}
            GROUP BY ep.${DatabaseHelper.COL_EP_PERSON_ID}
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        cursor.use {
            while (it.moveToNext()) {
                val personId = it.getLong(0)
                val lastStart = it.getLong(1)
                result[personId] = lastStart
            }
        }
        return result
    }

    fun addPersonToEvent(eventId: Long, personId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EP_EVENT_ID, eventId)
            put(DatabaseHelper.COL_EP_PERSON_ID, personId)
        }
        db.insertWithOnConflict(
            DatabaseHelper.TABLE_EVENT_PEOPLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        refreshLastEventByPersonId()
    }

    fun setNotesForEvent(eventId: Long, noteIds: List<Long>) {
        val db = dbHelper.writableDatabase
        db.delete(
            DatabaseHelper.TABLE_EVENT_NOTES,
            "${DatabaseHelper.COL_EN_EVENT_ID} = ?",
            arrayOf(eventId.toString())
        )
        for (noteId in noteIds.distinct()) {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_EN_EVENT_ID, eventId)
                put(DatabaseHelper.COL_EN_NOTE_ID, noteId)
            }
            db.insert(DatabaseHelper.TABLE_EVENT_NOTES, null, values)
        }
        // Keep legacy note_id in sync with the first linked task (if any).
        val legacyNoteId = noteIds.firstOrNull()
        val values = ContentValues().apply {
            if (legacyNoteId == null) {
                putNull(DatabaseHelper.COL_EVENT_NOTE_ID)
            } else {
                put(DatabaseHelper.COL_EVENT_NOTE_ID, legacyNoteId)
            }
        }
        db.update(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            values,
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(eventId.toString())
        )
        refreshCalendarEvents()
    }

    fun getNotesForEvent(eventId: Long): List<Note> {
        val notes = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT n.* FROM ${DatabaseHelper.TABLE_NOTES} n
            INNER JOIN ${DatabaseHelper.TABLE_EVENT_NOTES} en
                ON n.${DatabaseHelper.COL_NOTE_ID} = en.${DatabaseHelper.COL_EN_NOTE_ID}
            WHERE en.${DatabaseHelper.COL_EN_EVENT_ID} = ?
            ORDER BY n.${DatabaseHelper.COL_NOTE_IS_COMPLETED} ASC,
                     n.${DatabaseHelper.COL_NOTE_UPDATED_AT} DESC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(eventId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                notes.add(cursorToNote(it))
            }
        }
        return notes
    }

    fun getEventsForNote(noteId: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT e.* FROM ${DatabaseHelper.TABLE_CALENDAR_EVENTS} e
            INNER JOIN ${DatabaseHelper.TABLE_EVENT_NOTES} en
                ON e.${DatabaseHelper.COL_EVENT_ID} = en.${DatabaseHelper.COL_EN_EVENT_ID}
            WHERE en.${DatabaseHelper.COL_EN_NOTE_ID} = ?
            ORDER BY e.${DatabaseHelper.COL_EVENT_START_TIME} ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(noteId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    fun addNoteToEvent(eventId: Long, noteId: Long) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EN_EVENT_ID, eventId)
            put(DatabaseHelper.COL_EN_NOTE_ID, noteId)
        }
        db.insertWithOnConflict(
            DatabaseHelper.TABLE_EVENT_NOTES,
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )
        refreshCalendarEvents()
    }
}
