package com.example.calendarnotes.data.models

data class Category(
    val id: Long = 0,
    val name: String,
    val color: String = "#2196F3",
    val createdAt: Long = System.currentTimeMillis()
)
