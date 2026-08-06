package com.example.cam

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val text: String,
    val imageUrl: String? = null
)
