package com.example.fewstep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.Habit
import com.example.fewstep.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

class AiCoachViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Hello! I'm your FewStep AI Coach. How can I help you reach your goals today? 🚀", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(text: String, user: User?, habits: List<Habit>) {
        if (text.isBlank()) return
        
        // Safety Filter check
        if (isUnsafe(text)) {
            _messages.value += ChatMessage(text, true)
            _messages.value += ChatMessage("I'm sorry, but I can't discuss that. I'm here to help you with your habits and productivity! ✨", false)
            return
        }

        val userMessage = ChatMessage(text, true)
        _messages.value += userMessage
        _isTyping.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = fetchAiResponse(text, user, habits)
                if (response.startsWith("ERROR_MSG:")) {
                    _messages.value += ChatMessage(response.removePrefix("ERROR_MSG:"), false, isError = true)
                } else {
                    _messages.value += ChatMessage(response, false)
                }
            } catch (e: Exception) {
                _messages.value += ChatMessage("I am so sorry, my dear champion 🥺❤️ Currently this service is not available. Please come back later, I promise I'll be right here waiting for you! ✨💪", false, isError = true)
            } finally {
                _isTyping.value = false
            }
        }
    }

    private fun isUnsafe(text: String): Boolean {
        val unsafeKeywords = listOf("abuse", "vulgar", "fuck", "shit", "porn", "sexy", "violence", "hate")
        return unsafeKeywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun fetchAiResponse(userPrompt: String, user: User?, habits: List<Habit>): String {
        // Limit names and habits to avoid URL overflow
        val shortName = user?.name?.split(" ")?.firstOrNull() ?: "Champion"
        val habitContext = habits.take(5).joinToString(", ") { it.title }
        
        val prompt = """
            Instruction: You are 'FewStep Coach'. A deeply empathetic, purely emotional, and highly motivational life coach. 
            User: $shortName. Rank: ${user?.rankTitle ?: "Novice"}.
            Habits: $habitContext.
            Goal: Reply to '$userPrompt' with intense positive energy and encouragement.
            Rule: IMPORTANT! You MUST match the language of the user's prompt! If the user speaks English, reply ONLY in English. If the user speaks Hindi, reply ONLY in Hindi (Devanagari script). DO NOT write Hindi words using English letters. 1-2 short sentences, max 45 words. Include 1-2 emojis.
        """.trimIndent()

        // Clean prompt for path: replace special chars and encode
        val cleanPrompt = prompt.replace("\n", " ").replace("\r", " ").trim()
        val encodedPrompt = java.net.URLEncoder.encode(cleanPrompt, "UTF-8").replace("+", "%20")
        
        // Priority endpoints
        // 1. Pollinations (Must use path for direct text, query returns HTML docs)
        val endpoints = listOf(
            "https://text.pollinations.ai/$encodedPrompt"
        )
        
        for (url in endpoints) {
            var retryCount = 0
            val maxRetries = 1
            
            while (retryCount <= maxRetries) {
                var shouldRetry = false
                var lastErrorCode = 0
                
                try {
                    val requestBuilder = Request.Builder()
                        .url(url)
                    
                    // Pollinations: MUST be GET and PATH-based for plain text
                    requestBuilder.get().addHeader("Accept", "text/plain")

                    val request = requestBuilder.build()
                    var moveToFallback = false
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyText = response.body?.string()?.trim()
                            if (!bodyText.isNullOrEmpty()) {
                                // Don't return if it's HTML (Pollinations sometimes returns error pages as 200)
                                if (bodyText.contains("<!DOCTYPE html>") || bodyText.startsWith("<html")) {
                                     // This is an error page, try fallback
                                     lastErrorCode = 404 // Treat as not found
                                     moveToFallback = true
                                } else {
                                    return if (bodyText.startsWith("{")) {
                                        val jsonResp = JSONObject(bodyText)
                                        jsonResp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                                    } else {
                                        bodyText
                                    }
                                }
                            }
                        }
                        
                        lastErrorCode = response.code
                        if (lastErrorCode == 429) {
                             if (url == endpoints.last()) return "ERROR_MSG:I am so sorry, my dear champion 🥺❤️ Currently my servers are taking a tiny break. Please come back later, I promise I'll be here for you! ✨💪"
                             else moveToFallback = true 
                        }
                        if (lastErrorCode in listOf(301, 302, 404, 502, 503, 504)) {
                            shouldRetry = true
                        }
                    }
                    if (moveToFallback) break
                } catch (e: Exception) {
                    android.util.Log.e("AiCoach", "Network Error on $url: ${e.message}")
                    shouldRetry = true
                }

                if (shouldRetry) {
                    retryCount++
                    if (retryCount <= maxRetries) {
                        Thread.sleep(500L * retryCount)
                        continue
                    }
                }
                break // No more retries for this url
            }
        }
        
        return "ERROR_MSG:I am so sorry, my dear champion 🥺❤️ Currently this service is not available. Please come back later, I promise I'll be right here waiting for you! ✨💪"
    }
}
