package com.example.dom_tantsa_bosikom.models

data class ScheduleItem(
    val id: String = "",
    val groupId: String = "",
    val date: String = "",
    val time: String = "",
    val topic: String = "",
    val createdAt: Long = 0L
)