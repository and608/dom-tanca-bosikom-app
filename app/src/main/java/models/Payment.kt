package com.example.dom_tantsa_bosikom.models

data class Payment(
    val id: String = "",
    val childId: String = "",
    val childName: String = "",
    val parentId: String = "",
    val parentName: String = "",
    val month: String = "",
    val status: String = "unpaid"
)