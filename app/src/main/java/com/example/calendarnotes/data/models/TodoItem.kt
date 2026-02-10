package com.example.calendarnotes.data.models

data class TodoItem(
    val id: Long = 0,
    val subCategoryId: Long?,
    val categoryId: Long,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0, // 0=low, 1=medium, 2=high
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
