package com.example.calendarnotes.data.models

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val categoryId: Long?,
    val todoItemId: Long? = null, // Link to todo if created from todo
    val createdAt: Long = System.currentTimeMillis(),
    val notificationEnabled: Boolean = true,
    val notificationMinutesBefore: Int = 30
)
