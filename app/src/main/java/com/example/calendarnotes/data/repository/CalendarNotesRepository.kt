package com.example.calendarnotes.data.repository

import android.content.ContentValues
import android.content.Context
import com.example.calendarnotes.data.database.DatabaseHelper
import com.example.calendarnotes.data.models.*

class CalendarNotesRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // Category operations
    fun insertCategory(category: Category): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CAT_NAME, category.name)
            put(DatabaseHelper.COL_CAT_COLOR, category.color)
            put(DatabaseHelper.COL_CAT_CREATED_AT, category.createdAt)
        }
        return db.insert(DatabaseHelper.TABLE_CATEGORIES, null, values)
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
        return db.delete(DatabaseHelper.TABLE_CATEGORIES, "${DatabaseHelper.COL_CAT_ID} = ?", arrayOf(id.toString()))
    }

    fun updateCategory(category: Category): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CAT_NAME, category.name)
            put(DatabaseHelper.COL_CAT_COLOR, category.color)
        }
        return db.update(DatabaseHelper.TABLE_CATEGORIES, values, "${DatabaseHelper.COL_CAT_ID} = ?", arrayOf(category.id.toString()))
    }

    // SubCategory operations
    fun insertSubCategory(subCategory: SubCategory): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SUBCAT_CATEGORY_ID, subCategory.categoryId)
            put(DatabaseHelper.COL_SUBCAT_NAME, subCategory.name)
            put(DatabaseHelper.COL_SUBCAT_CREATED_AT, subCategory.createdAt)
        }
        return db.insert(DatabaseHelper.TABLE_SUB_CATEGORIES, null, values)
    }

    fun getSubCategoriesByCategory(categoryId: Long): List<SubCategory> {
        val subCategories = mutableListOf<SubCategory>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_SUB_CATEGORIES,
            null,
            "${DatabaseHelper.COL_SUBCAT_CATEGORY_ID} = ?",
            arrayOf(categoryId.toString()),
            null, null,
            "${DatabaseHelper.COL_SUBCAT_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                subCategories.add(
                    SubCategory(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_SUBCAT_ID)),
                        categoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_SUBCAT_CATEGORY_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_SUBCAT_NAME)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_SUBCAT_CREATED_AT))
                    )
                )
            }
        }
        return subCategories
    }

    fun deleteSubCategory(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_SUB_CATEGORIES, "${DatabaseHelper.COL_SUBCAT_ID} = ?", arrayOf(id.toString()))
    }

    // TodoItem operations
    fun insertTodoItem(todoItem: TodoItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TODO_SUB_CATEGORY_ID, todoItem.subCategoryId)
            put(DatabaseHelper.COL_TODO_CATEGORY_ID, todoItem.categoryId)
            put(DatabaseHelper.COL_TODO_TITLE, todoItem.title)
            put(DatabaseHelper.COL_TODO_DESCRIPTION, todoItem.description)
            put(DatabaseHelper.COL_TODO_IS_COMPLETED, if (todoItem.isCompleted) 1 else 0)
            put(DatabaseHelper.COL_TODO_PRIORITY, todoItem.priority)
            put(DatabaseHelper.COL_TODO_DUE_DATE, todoItem.dueDate)
            put(DatabaseHelper.COL_TODO_CREATED_AT, todoItem.createdAt)
        }
        return db.insert(DatabaseHelper.TABLE_TODO_ITEMS, null, values)
    }

    fun getAllTodoItems(): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TODO_ITEMS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_TODO_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                todos.add(
                    TodoItem(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_ID)),
                        subCategoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_SUB_CATEGORY_ID)),
                        categoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_CATEGORY_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_TITLE)),
                        description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DESCRIPTION)) ?: "",
                        isCompleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_IS_COMPLETED)) == 1,
                        priority = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_PRIORITY)),
                        dueDate = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DUE_DATE))) null 
                                  else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DUE_DATE)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_CREATED_AT))
                    )
                )
            }
        }
        return todos
    }

    fun getTodoItemsByCategory(categoryId: Long): List<TodoItem> {
        val todos = mutableListOf<TodoItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TODO_ITEMS,
            null,
            "${DatabaseHelper.COL_TODO_CATEGORY_ID} = ?",
            arrayOf(categoryId.toString()),
            null, null,
            "${DatabaseHelper.COL_TODO_CREATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                todos.add(
                    TodoItem(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_ID)),
                        subCategoryId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_SUB_CATEGORY_ID))) null
                                        else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_SUB_CATEGORY_ID)),
                        categoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_CATEGORY_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_TITLE)),
                        description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DESCRIPTION)) ?: "",
                        isCompleted = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_IS_COMPLETED)) == 1,
                        priority = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_PRIORITY)),
                        dueDate = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DUE_DATE))) null 
                                  else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DUE_DATE)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_CREATED_AT))
                    )
                )
            }
        }
        return todos
    }

    fun updateTodoItem(todoItem: TodoItem): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TODO_TITLE, todoItem.title)
            put(DatabaseHelper.COL_TODO_DESCRIPTION, todoItem.description)
            put(DatabaseHelper.COL_TODO_IS_COMPLETED, if (todoItem.isCompleted) 1 else 0)
            put(DatabaseHelper.COL_TODO_PRIORITY, todoItem.priority)
            put(DatabaseHelper.COL_TODO_DUE_DATE, todoItem.dueDate)
        }
        return db.update(DatabaseHelper.TABLE_TODO_ITEMS, values, "${DatabaseHelper.COL_TODO_ID} = ?", arrayOf(todoItem.id.toString()))
    }

    fun deleteTodoItem(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_TODO_ITEMS, "${DatabaseHelper.COL_TODO_ID} = ?", arrayOf(id.toString()))
    }

    // Note operations
    fun insertNote(note: Note): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTE_CATEGORY_ID, note.categoryId)
            put(DatabaseHelper.COL_NOTE_TITLE, note.title)
            put(DatabaseHelper.COL_NOTE_CONTENT, note.content)
            put(DatabaseHelper.COL_NOTE_CREATED_AT, note.createdAt)
            put(DatabaseHelper.COL_NOTE_UPDATED_AT, note.updatedAt)
        }
        return db.insert(DatabaseHelper.TABLE_NOTES, null, values)
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_NOTES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_NOTE_UPDATED_AT} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                notes.add(
                    Note(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_ID)),
                        categoryId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY_ID))) null
                                     else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE)),
                        content = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT)) ?: "",
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CREATED_AT)),
                        updatedAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_UPDATED_AT))
                    )
                )
            }
        }
        return notes
    }

    fun updateNote(note: Note): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOTE_TITLE, note.title)
            put(DatabaseHelper.COL_NOTE_CONTENT, note.content)
            put(DatabaseHelper.COL_NOTE_CATEGORY_ID, note.categoryId)
            put(DatabaseHelper.COL_NOTE_UPDATED_AT, System.currentTimeMillis())
        }
        return db.update(DatabaseHelper.TABLE_NOTES, values, "${DatabaseHelper.COL_NOTE_ID} = ?", arrayOf(note.id.toString()))
    }

    fun deleteNote(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_NOTES, "${DatabaseHelper.COL_NOTE_ID} = ?", arrayOf(id.toString()))
    }

    // Calendar Event operations
    fun insertCalendarEvent(event: CalendarEvent): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EVENT_TITLE, event.title)
            put(DatabaseHelper.COL_EVENT_DESCRIPTION, event.description)
            put(DatabaseHelper.COL_EVENT_START_TIME, event.startTime)
            put(DatabaseHelper.COL_EVENT_END_TIME, event.endTime)
            put(DatabaseHelper.COL_EVENT_CATEGORY_ID, event.categoryId)
            put(DatabaseHelper.COL_EVENT_TODO_ITEM_ID, event.todoItemId)
            put(DatabaseHelper.COL_EVENT_CREATED_AT, event.createdAt)
        }
        return db.insert(DatabaseHelper.TABLE_CALENDAR_EVENTS, null, values)
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
                events.add(
                    CalendarEvent(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TITLE)),
                        description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_DESCRIPTION)) ?: "",
                        startTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_START_TIME)),
                        endTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_END_TIME)),
                        categoryId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID))) null
                                     else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID)),
                        todoItemId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TODO_ITEM_ID))) null
                                     else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TODO_ITEM_ID)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CREATED_AT))
                    )
                )
            }
        }
        return events
    }

    fun getEventsByDateRange(startTime: Long, endTime: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            null,
            "${DatabaseHelper.COL_EVENT_START_TIME} >= ? AND ${DatabaseHelper.COL_EVENT_START_TIME} <= ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null, null,
            "${DatabaseHelper.COL_EVENT_START_TIME} ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                events.add(
                    CalendarEvent(
                        id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TITLE)),
                        description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_DESCRIPTION)) ?: "",
                        startTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_START_TIME)),
                        endTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_END_TIME)),
                        categoryId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID))) null
                                     else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CATEGORY_ID)),
                        todoItemId = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TODO_ITEM_ID))) null
                                     else it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_TODO_ITEM_ID)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_EVENT_CREATED_AT))
                    )
                )
            }
        }
        return events
    }

    fun updateCalendarEvent(event: CalendarEvent): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EVENT_TITLE, event.title)
            put(DatabaseHelper.COL_EVENT_DESCRIPTION, event.description)
            put(DatabaseHelper.COL_EVENT_START_TIME, event.startTime)
            put(DatabaseHelper.COL_EVENT_END_TIME, event.endTime)
            put(DatabaseHelper.COL_EVENT_CATEGORY_ID, event.categoryId)
        }
        return db.update(DatabaseHelper.TABLE_CALENDAR_EVENTS, values, "${DatabaseHelper.COL_EVENT_ID} = ?", arrayOf(event.id.toString()))
    }

    fun deleteCalendarEvent(id: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(DatabaseHelper.TABLE_CALENDAR_EVENTS, "${DatabaseHelper.COL_EVENT_ID} = ?", arrayOf(id.toString()))
    }
    
    fun updateEventTime(id: Long, newStartTime: Long, newEndTime: Long): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_EVENT_START_TIME, newStartTime)
            put(DatabaseHelper.COL_EVENT_END_TIME, newEndTime)
        }
        return db.update(
            DatabaseHelper.TABLE_CALENDAR_EVENTS,
            values,
            "${DatabaseHelper.COL_EVENT_ID} = ?",
            arrayOf(id.toString())
        )
    }

    // Create calendar event from todo item
    fun createEventFromTodo(todoId: Long, startTime: Long, endTime: Long): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TODO_ITEMS,
            null,
            "${DatabaseHelper.COL_TODO_ID} = ?",
            arrayOf(todoId.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val event = CalendarEvent(
                    title = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_DESCRIPTION)) ?: "",
                    startTime = startTime,
                    endTime = endTime,
                    categoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_TODO_CATEGORY_ID)),
                    todoItemId = todoId
                )
                return insertCalendarEvent(event)
            }
        }
        return -1
    }
}
