package com.example.dom_tantsa_bosikom.models

data class Child(
    val id: String = "",
    val name: String = "",
    val birthDate: String = "",
    val parentId: String = "",
    val parentName: String = "",
    val groupId: String = "",
    val notes: String = "",
    val createdAt: Long = 0L
)