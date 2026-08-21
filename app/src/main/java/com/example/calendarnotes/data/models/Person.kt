package com.example.calendarnotes.data.models

data class Person(
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val status: PersonStatus = PersonStatus.NOT_IN_CONTACT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
