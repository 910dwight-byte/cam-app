package com.example.cam

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Calls the Anthropic API to get a chat reply. Requires the user's own API key.
    fun sendChat(apiKey: String, history: List<ChatMessage>): String {
        val messagesArray = JSONArray()
        for (m in history) {
            if (m.imageUrl != null) continue // skip image-generation turns in the chat history
            val obj = JSONObject()
            obj.put("role", m.role)
            obj.put("content", m.text)
            messagesArray.put(obj)
        }

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 1024)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: return "No response from server."
            if (!response.isSuccessful) {
                return "Error ${response.code}: $responseBody"
            }
            val json = JSONObject(responseBody)
            val content = json.optJSONArray("content") ?: return "Error: unexpected response format."
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") {
                    sb.append(block.optString("text"))
                }
            }
            return sb.toString().ifBlank { "(empty response)" }
        }
    }

    // Free, no-key image generation via Pollinations.
    fun imageUrlForPrompt(prompt: String): String {
        val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        return "https://image.pollinations.ai/prompt/$encoded"
    }
}
