package com.example.calendarnotes.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "calendar_notes.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_CATEGORIES = "categories"
        const val TABLE_SUB_CATEGORIES = "sub_categories"
        const val TABLE_TODO_ITEMS = "todo_items"
        const val TABLE_NOTES = "notes"
        const val TABLE_CALENDAR_EVENTS = "calendar_events"

        // Categories table
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_COLOR = "color"
        const val COL_CAT_CREATED_AT = "created_at"

        // Sub-categories table
        const val COL_SUBCAT_ID = "id"
        const val COL_SUBCAT_CATEGORY_ID = "category_id"
        const val COL_SUBCAT_NAME = "name"
        const val COL_SUBCAT_CREATED_AT = "created_at"

        // Todo items table
        const val COL_TODO_ID = "id"
        const val COL_TODO_SUB_CATEGORY_ID = "sub_category_id"
        const val COL_TODO_CATEGORY_ID = "category_id"
        const val COL_TODO_TITLE = "title"
        const val COL_TODO_DESCRIPTION = "description"
        const val COL_TODO_IS_COMPLETED = "is_completed"
        const val COL_TODO_PRIORITY = "priority"
        const val COL_TODO_DUE_DATE = "due_date"
        const val COL_TODO_CREATED_AT = "created_at"

        // Notes table
        const val COL_NOTE_ID = "id"
        const val COL_NOTE_CATEGORY_ID = "category_id"
        const val COL_NOTE_TITLE = "title"
        const val COL_NOTE_CONTENT = "content"
        const val COL_NOTE_CREATED_AT = "created_at"
        const val COL_NOTE_UPDATED_AT = "updated_at"

        // Calendar events table
        const val COL_EVENT_ID = "id"
        const val COL_EVENT_TITLE = "title"
        const val COL_EVENT_DESCRIPTION = "description"
        const val COL_EVENT_START_TIME = "start_time"
        const val COL_EVENT_END_TIME = "end_time"
        const val COL_EVENT_CATEGORY_ID = "category_id"
        const val COL_EVENT_TODO_ITEM_ID = "todo_item_id"
        const val COL_EVENT_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create categories table
        db.execSQL("""
            CREATE TABLE $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_COLOR TEXT NOT NULL,
                $COL_CAT_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent())

        // Create sub-categories table
        db.execSQL("""
            CREATE TABLE $TABLE_SUB_CATEGORIES (
                $COL_SUBCAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SUBCAT_CATEGORY_ID INTEGER NOT NULL,
                $COL_SUBCAT_NAME TEXT NOT NULL,
                $COL_SUBCAT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_SUBCAT_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create todo items table
        db.execSQL("""
            CREATE TABLE $TABLE_TODO_ITEMS (
                $COL_TODO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TODO_SUB_CATEGORY_ID INTEGER,
                $COL_TODO_CATEGORY_ID INTEGER NOT NULL,
                $COL_TODO_TITLE TEXT NOT NULL,
                $COL_TODO_DESCRIPTION TEXT,
                $COL_TODO_IS_COMPLETED INTEGER NOT NULL DEFAULT 0,
                $COL_TODO_PRIORITY INTEGER NOT NULL DEFAULT 0,
                $COL_TODO_DUE_DATE INTEGER,
                $COL_TODO_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_TODO_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE CASCADE,
                FOREIGN KEY($COL_TODO_SUB_CATEGORY_ID) REFERENCES $TABLE_SUB_CATEGORIES($COL_SUBCAT_ID) ON DELETE SET NULL
            )
        """.trimIndent())

        // Create notes table
        db.execSQL("""
            CREATE TABLE $TABLE_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_CATEGORY_ID INTEGER,
                $COL_NOTE_TITLE TEXT NOT NULL,
                $COL_NOTE_CONTENT TEXT,
                $COL_NOTE_CREATED_AT INTEGER NOT NULL,
                $COL_NOTE_UPDATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_NOTE_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE SET NULL
            )
        """.trimIndent())

        // Create calendar events table
        db.execSQL("""
            CREATE TABLE $TABLE_CALENDAR_EVENTS (
                $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EVENT_TITLE TEXT NOT NULL,
                $COL_EVENT_DESCRIPTION TEXT,
                $COL_EVENT_START_TIME INTEGER NOT NULL,
                $COL_EVENT_END_TIME INTEGER NOT NULL,
                $COL_EVENT_CATEGORY_ID INTEGER,
                $COL_EVENT_TODO_ITEM_ID INTEGER,
                $COL_EVENT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_EVENT_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID) ON DELETE SET NULL,
                FOREIGN KEY($COL_EVENT_TODO_ITEM_ID) REFERENCES $TABLE_TODO_ITEMS($COL_TODO_ID) ON DELETE SET NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CALENDAR_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TODO_ITEMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SUB_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }
}
