package com.example.calendarnotes.data.models

data class Note(
    val id: Long = 0,
    val categoryId: Long?,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
