package com.example.calendarnotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.calendarnotes.data.models.*
import com.example.calendarnotes.data.repository.CalendarNotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarNotesRepository(application)
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _todoItems = MutableLiveData<List<TodoItem>>()
    val todoItems: LiveData<List<TodoItem>> = _todoItems

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    private val _calendarEvents = MutableLiveData<List<CalendarEvent>>()
    val calendarEvents: LiveData<List<CalendarEvent>> = _calendarEvents

    init {
        loadCategories()
        loadTodoItems()
        loadNotes()
        loadCalendarEvents()
    }

    // Category methods
    fun loadCategories() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllCategories()
            }
            _categories.value = result
        }
    }

    fun addCategory(name: String, color: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertCategory(Category(name = name, color = color))
            }
            loadCategories()
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCategory(id)
            }
            loadCategories()
            loadTodoItems()
        }
    }

    // SubCategory methods
    fun addSubCategory(categoryId: Long, name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertSubCategory(SubCategory(categoryId = categoryId, name = name))
            }
        }
    }

    fun getSubCategories(categoryId: Long, callback: (List<SubCategory>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getSubCategoriesByCategory(categoryId)
            }
            callback(result)
        }
    }

    // TodoItem methods
    fun loadTodoItems() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllTodoItems()
            }
            _todoItems.value = result
        }
    }

    fun addTodoItem(categoryId: Long, subCategoryId: Long?, title: String, description: String, priority: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertTodoItem(
                    TodoItem(
                        categoryId = categoryId,
                        subCategoryId = subCategoryId,
                        title = title,
                        description = description,
                        priority = priority
                    )
                )
            }
            loadTodoItems()
        }
    }

    fun updateTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateTodoItem(todoItem)
            }
            loadTodoItems()
        }
    }

    fun toggleTodoCompletion(todoItem: TodoItem) {
        updateTodoItem(todoItem.copy(isCompleted = !todoItem.isCompleted))
    }

    fun deleteTodoItem(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTodoItem(id)
            }
            loadTodoItems()
        }
    }

    // Note methods
    fun loadNotes() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllNotes()
            }
            _notes.value = result
        }
    }

    fun addNote(categoryId: Long?, title: String, content: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertNote(Note(categoryId = categoryId, title = title, content = content))
            }
            loadNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateNote(note)
            }
            loadNotes()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteNote(id)
            }
            loadNotes()
        }
    }

    // Calendar Event methods
    fun loadCalendarEvents() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getAllCalendarEvents()
            }
            _calendarEvents.value = result
        }
    }

    fun addCalendarEvent(title: String, description: String, startTime: Long, endTime: Long, categoryId: Long?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertCalendarEvent(
                    CalendarEvent(
                        title = title,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        categoryId = categoryId
                    )
                )
            }
            loadCalendarEvents()
        }
    }

    fun createEventFromTodo(todoId: Long, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.createEventFromTodo(todoId, startTime, endTime)
            }
            loadCalendarEvents()
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteCalendarEvent(id)
            }
            loadCalendarEvents()
        }
    }
    
    fun updateEventTime(id: Long, newStartTime: Long, newEndTime: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateEventTime(id, newStartTime, newEndTime)
            }
            loadCalendarEvents()
        }
    }

    fun getEventsForDateRange(startTime: Long, endTime: Long, callback: (List<CalendarEvent>) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEventsByDateRange(startTime, endTime)
            }
            callback(result)
        }
    }
}
