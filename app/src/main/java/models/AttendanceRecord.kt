package com.example.dom_tantsa_bosikom.models

data class AttendanceRecord(
    val id: String = "",
    val childId: String = "",
    val childName: String = "",
    val dateDisplay: String = "",
    val status: String = "",      // present / absent / sick
    val notes: String = "",
    val markedAt: Long = 0L
)