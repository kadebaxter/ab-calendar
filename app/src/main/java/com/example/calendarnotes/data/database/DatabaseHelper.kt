package com.example.calendarnotes.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "calendar_notes.db"
        private const val DATABASE_VERSION = 7

        const val TABLE_CATEGORIES = "categories"
        const val TABLE_NOTES = "notes"
        const val TABLE_CALENDAR_EVENTS = "calendar_events"
        const val TABLE_PEOPLE = "people"
        const val TABLE_CONTACT_HISTORY = "contact_history"
        const val TABLE_EVENT_PEOPLE = "event_people"
        const val TABLE_EVENT_NOTES = "event_notes"

        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_COLOR = "color"
        const val COL_CAT_CREATED_AT = "created_at"

        const val COL_NOTE_ID = "id"
        const val COL_NOTE_CATEGORY_ID = "category_id"
        const val COL_NOTE_TITLE = "title"
        const val COL_NOTE_CONTENT = "content"
        const val COL_NOTE_IS_COMPLETED = "is_completed"
        const val COL_NOTE_CREATED_AT = "created_at"
        const val COL_NOTE_UPDATED_AT = "updated_at"

        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TITLE = "title"
        const val COL_EVENT_DESCRIPTION = "description"
        const val COL_EVENT_START_TIME = "start_time"
        const val COL_EVENT_END_TIME = "end_time"
        const val COL_EVENT_CATEGORY_ID = "category_id"
        const val COL_EVENT_NOTE_ID = "note_id"
        const val COL_EVENT_CREATED_AT = "created_at"
        const val COL_EVENT_NOTIFICATION_ENABLED = "notification_enabled"
        const val COL_EVENT_NOTIFICATION_MINUTES_BEFORE = "notification_minutes_before"
        const val COL_EVENT_GOOGLE_EVENT_KEY = "google_event_key"
        const val COL_EVENT_GOOGLE_CALENDAR_ID = "google_calendar_id"
        const val COL_EVENT_IS_ALL_DAY = "is_all_day"

        const val COL_PERSON_ID = "id"
        const val COL_PERSON_NAME = "name"
        const val COL_PERSON_PHONE = "phone"
        const val COL_PERSON_EMAIL = "email"
        const val COL_PERSON_ADDRESS = "address"
        const val COL_PERSON_NOTES = "notes"
        const val COL_PERSON_STATUS = "status"
        const val COL_PERSON_CREATED_AT = "created_at"
        const val COL_PERSON_UPDATED_AT = "updated_at"

        const val COL_HISTORY_ID = "id"
        const val COL_HISTORY_PERSON_ID = "person_id"
        const val COL_HISTORY_SUMMARY = "summary"
        const val COL_HISTORY_TIMESTAMP = "timestamp"

        const val COL_EP_EVENT_ID = "event_id"
        const val COL_EP_PERSON_ID = "person_id"

        const val COL_EN_EVENT_ID = "event_id"
        const val COL_EN_NOTE_ID = "note_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_COLOR TEXT NOT NULL,
                $COL_CAT_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_CATEGORY_ID INTEGER,
                $COL_NOTE_TITLE TEXT NOT NULL,
                $COL_NOTE_CONTENT TEXT,
                $COL_NOTE_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COL_NOTE_CREATED_AT INTEGER NOT NULL,
                $COL_NOTE_UPDATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_NOTE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CALENDAR_EVENTS (
                $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVENT_TITLE TEXT NOT NULL,
                $COL_EVENT_DESCRIPTION TEXT,
                $COL_EVENT_START_TIME INTEGER NOT NULL,
                $COL_EVENT_END_TIME INTEGER NOT NULL,
                $COL_EVENT_CATEGORY_ID INTEGER,
                $COL_EVENT_NOTE_ID INTEGER,
                $COL_EVENT_CREATED_AT INTEGER NOT NULL,
                $COL_EVENT_NOTIFICATION_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COL_EVENT_NOTIFICATION_MINUTES_BEFORE INTEGER NOT NULL DEFAULT 30,
                $COL_EVENT_GOOGLE_EVENT_KEY TEXT UNIQUE,
                $COL_EVENT_GOOGLE_CALENDAR_ID TEXT,
                $COL_EVENT_IS_ALL_DAY INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY($COL_EVENT_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE SET NULL,
                FOREIGN KEY($COL_EVENT_NOTE_ID) REFERENCES $TABLE_NOTES($COL_NOTE_ID) ON DELETE SET NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PEOPLE (
                $COL_PERSON_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PERSON_NAME TEXT NOT NULL,
                $COL_PERSON_PHONE TEXT,
                $COL_PERSON_EMAIL TEXT,
                $COL_PERSON_ADDRESS TEXT,
                $COL_PERSON_NOTES TEXT,
                $COL_PERSON_STATUS TEXT NOT NULL,
                $COL_PERSON_CREATED_AT INTEGER NOT NULL,
                $COL_PERSON_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTACT_HISTORY (
                $COL_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HISTORY_PERSON_ID INTEGER NOT NULL,
                $COL_HISTORY_SUMMARY TEXT NOT NULL,
                $COL_HISTORY_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY($COL_HISTORY_PERSON_ID) REFERENCES $TABLE_PEOPLE($COL_PERSON_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENT_PEOPLE (
                $COL_EP_EVENT_ID INTEGER NOT NULL,
                $COL_EP_PERSON_ID INTEGER NOT NULL,
                PRIMARY KEY ($COL_EP_EVENT_ID, $COL_EP_PERSON_ID),
                FOREIGN KEY($COL_EP_EVENT_ID) REFERENCES $TABLE_CALENDAR_EVENTS($COL_EVENT_ID) ON DELETE CASCADE,
                FOREIGN KEY($COL_EP_PERSON_ID) REFERENCES $TABLE_PEOPLE($COL_PERSON_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENT_NOTES (
                $COL_EN_EVENT_ID INTEGER NOT NULL,
                $COL_EN_NOTE_ID INTEGER NOT NULL,
                PRIMARY KEY ($COL_EN_EVENT_ID, $COL_EN_NOTE_ID),
                FOREIGN KEY($COL_EN_EVENT_ID) REFERENCES $TABLE_CALENDAR_EVENTS($COL_EVENT_ID) ON DELETE CASCADE,
                FOREIGN KEY($COL_EN_NOTE_ID) REFERENCES $TABLE_NOTES($COL_NOTE_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENT_PEOPLE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACT_HISTORY")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PEOPLE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CALENDAR_EVENTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            db.execSQL("DROP TABLE IF EXISTS todo_items")
            db.execSQL("DROP TABLE IF EXISTS sub_categories")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
            onCreate(db)
            return
        }
        if (oldVersion < 5) {
            db.execSQL(
                "ALTER TABLE $TABLE_CALENDAR_EVENTS ADD COLUMN $COL_EVENT_GOOGLE_EVENT_KEY TEXT"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_CALENDAR_EVENTS ADD COLUMN $COL_EVENT_GOOGLE_CALENDAR_ID TEXT"
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_calendar_events_google_key
                ON $TABLE_CALENDAR_EVENTS($COL_EVENT_GOOGLE_EVENT_KEY)
                """.trimIndent()
            )
        }
        if (oldVersion < 6) {
            db.execSQL(
                "ALTER TABLE $TABLE_CALENDAR_EVENTS ADD COLUMN $COL_EVENT_IS_ALL_DAY INTEGER NOT NULL DEFAULT 0"
            )
        }
        if (oldVersion < 7) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_EVENT_NOTES (
                    $COL_EN_EVENT_ID INTEGER NOT NULL,
                    $COL_EN_NOTE_ID INTEGER NOT NULL,
                    PRIMARY KEY ($COL_EN_EVENT_ID, $COL_EN_NOTE_ID),
                    FOREIGN KEY($COL_EN_EVENT_ID) REFERENCES $TABLE_CALENDAR_EVENTS($COL_EVENT_ID) ON DELETE CASCADE,
                    FOREIGN KEY($COL_EN_NOTE_ID) REFERENCES $TABLE_NOTES($COL_NOTE_ID) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            // Preserve legacy one-note-per-event links.
            db.execSQL(
                """
                INSERT OR IGNORE INTO $TABLE_EVENT_NOTES ($COL_EN_EVENT_ID, $COL_EN_NOTE_ID)
                SELECT $COL_EVENT_ID, $COL_EVENT_NOTE_ID
                FROM $TABLE_CALENDAR_EVENTS
                WHERE $COL_EVENT_NOTE_ID IS NOT NULL
                """.trimIndent()
            )
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
