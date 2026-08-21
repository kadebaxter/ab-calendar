package com.example.calendarnotes.data.models

data class ContactHistoryEntry(
    val id: Long = 0,
    val personId: Long,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)
