package com.example.calendarnotes.data.models

data class SubCategory(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
