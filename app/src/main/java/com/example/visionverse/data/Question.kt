package com.example.visionverse.data

data class Question(
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0
)