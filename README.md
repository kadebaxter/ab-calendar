# Calendar & Notes App

A comprehensive Android app built with Kotlin that combines calendar, todo management, and note-taking capabilities.

## Features

### 1. Categories Management
- Create custom categories with color coding
- Add sub-categories to organize items better
- Delete categories (removes associated items)

### 2. Todo Items
- Create todos with title, description, and priority levels (Low, Medium, High)
- Organize todos by categories and sub-categories
- Mark todos as complete/incomplete
- Schedule todos to calendar events
- Delete todos

### 3. Notes
- Create and edit rich text notes
- Optional category association
- Timestamps for creation and last update
- Click to edit existing notes
- Delete notes

### 4. Calendar
- Visual calendar view
- Add events with start and end times
- View events for selected dates
- Color-coded events based on categories
- Create calendar events from todo items
- Delete events

## Technical Details

### Architecture
- **Language**: Kotlin
- **MVVM Pattern**: ViewModel separates UI from business logic
- **SQLite Database**: Local data persistence
- **Repository Pattern**: Clean data access layer
- **LiveData**: Reactive UI updates
- **ViewBinding**: Type-safe view access
- **Coroutines**: Asynchronous operations

### Database Schema
- **Categories**: id, name, color, created_at
- **Sub-Categories**: id, category_id, name, created_at
- **Todo Items**: id, category_id, sub_category_id, title, description, is_completed, priority, due_date, created_at
- **Notes**: id, category_id, title, content, created_at, updated_at
- **Calendar Events**: id, title, description, start_time, end_time, category_id, todo_item_id, created_at

### Key Components
- **MainActivity**: Tab-based navigation with ViewPager2
- **Fragments**: CalendarFragment, TodosFragment, NotesFragment, CategoriesFragment
- **Adapters**: RecyclerView adapters for each data type
- **ViewModel**: CalendarNotesViewModel manages all data operations
- **Repository**: CalendarNotesRepository handles database operations
- **DatabaseHelper**: SQLite database setup and management

## Usage

### Getting Started
1. Open the app
2. Navigate to the "Categories" tab
3. Create your first category (e.g., "Work", "Personal", "School")
4. Start creating todos, notes, and calendar events!

### Creating a Todo
1. Go to "Todos" tab
2. Tap the + button
3. Fill in title, description, select category, and set priority
4. Tap "Add"

### Scheduling a Todo
1. Find the todo in the "Todos" tab
2. Tap the calendar icon
3. Select date and time
4. The todo will appear on the calendar

### Creating a Note
1. Go to "Notes" tab
2. Tap the + button
3. Enter title and content
4. Optionally select a category
5. Tap "Add"

### Adding Calendar Events
1. Go to "Calendar" tab
2. Select a date on the calendar
3. Tap the + button
4. Fill in event details and times
5. Tap "Add"

## Future Enhancements (Not Yet Implemented)
- Canvas LMS integration for assignments
- Jira integration for work items
- Notifications and reminders
- Recurring events
- Export/import functionality
- Cloud sync

## Dependencies
- AndroidX Core KTX, AppCompat, Material Design
- ViewModel and LiveData (lifecycle-viewmodel-ktx, lifecycle-livedata-ktx)
- RecyclerView
- CardView
- ViewPager2
- ViewBinding
- Kotlin Coroutines for async operations

## Requirements
- Android SDK 24+ (Android 7.0 Nougat and above)
- Target SDK: 36 (Android 15)
- Kotlin 1.9.0
- Android Gradle Plugin 9.0.0
- Java 11 compatibility

## Building
1. Open project in Android Studio
2. Sync Gradle files
3. Run on emulator or physical device (Android 7.0+)

## Project Information
Educational project for CS 4270 - Mobile Application Development
Spring 2026
