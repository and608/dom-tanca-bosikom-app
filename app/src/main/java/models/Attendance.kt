package com.example.dom_tantsa_bosikom.models

data class Attendance(
    val id: String = "",
    val childId: String = "",
    val childName: String = "",
    val groupId: String = "",
    val date: String = "",
    val isPresent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String {
        return if (isPresent) "Присутствовал" else "Отсутствовал"
    }
}