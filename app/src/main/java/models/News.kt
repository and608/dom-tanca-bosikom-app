package com.example.dom_tantsa_bosikom.models

data class News(
    var id: String = "",
    var title: String = "",
    var text: String = "",
    var author: String = "",
    var date: String = "",
    var timestamp: Long = 0L
)