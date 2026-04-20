package com.example.dom_tantsa_bosikom.models

data class GroupStats(
    val totalChildren: Int = 0,
    val totalLessons: Int = 0,
    val totalMarks: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val attendancePercent: Double = 0.0
)