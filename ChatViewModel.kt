package com.example.cam

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<ChatMessage>()
    var isLoading = mutableStateOf(false)
        private set

    fun sendTextMessage(text: String, apiKey: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage("user", text))
        isLoading.value = true
        viewModelScope.launch {
            val reply = withContext(Dispatchers.IO) {
                if (apiKey.isBlank()) {
                    "Please add your Anthropic API key in Settings first."
                } else {
                    try {
                        ApiClient.sendChat(apiKey, messages.toList())
                    } catch (e: Exception) {
                        "Error reaching Cam: ${e.message}"
                    }
                }
            }
            messages.add(ChatMessage("assistant", reply))
            isLoading.value = false
        }
    }

    fun sendImageRequest(prompt: String) {
        if (prompt.isBlank()) return
        messages.add(ChatMessage("user", "Generate image: $prompt"))
        val url = ApiClient.imageUrlForPrompt(prompt)
        messages.add(ChatMessage("assistant", "Here's your image:", imageUrl = url))
    }
}
