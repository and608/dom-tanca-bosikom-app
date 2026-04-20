package com.example.dom_tantsa_bosikom

data class ChildStatistics(
    val childId: String,
    val childName: String,
    val totalClasses: Int,      // Всего занятий за месяц
    val attendedClasses: Int,   // Посещено
    val percentage: Int         // Процент посещаемости
)