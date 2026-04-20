package com.example.dom_tantsa_bosikom.models

data class Schedule(
    val id: String = "",
    val groupId: String = "",
    val dayOfWeek: Int = 1,
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val notes: String = "",
    val isActive: Boolean = true

) {
    fun getDayName(): String {
        return when (dayOfWeek) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> "Неизвестно"
        }
    }

    fun getTimeRange(): String {
        return "$startTime - $endTime"
    }
}