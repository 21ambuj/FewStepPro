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
    val timestamp: Long = System.currentTimeMillis()
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
                _messages.value += ChatMessage(response, false)
            } catch (e: Exception) {
                _messages.value += ChatMessage("Oops! I’m having trouble connecting right now. Let's try again in a moment. 🛠️", false)
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
        val habitContext = habits.joinToString(", ") { it.title }
        val prompt = """
            FewStep Coach. 
            User: ${user?.name ?: "Champion"}, Level: ${user?.level ?: 1}, Streak: ${user?.currentStreak ?: 0}. 
            Habits: $habitContext. 
            
            Strict Guidelines:
            1. Answer only in short points using simple dashes (-).
            2. NO special characters like *, #, or bold markdown.
            3. Use 1-2 emojis per point based on context.
            4. Keep responses very short and professional.
            
            User says: $userPrompt
        """.trimIndent()

        val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
        val url = "https://text.pollinations.ai/$encodedPrompt"
        
        var retryCount = 0
        val maxRetries = 2
        
        while (retryCount <= maxRetries) {
            var shouldRetry = false
            var lastErrorCode = 0
            
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string() ?: "I couldn't hear you clearly... 🎤"
                    }
                    
                    lastErrorCode = response.code
                    if (lastErrorCode == 429) return "You're too fast! Let's slow down and focus on your habits for a moment. 🧘‍♂️☕"
                    if (lastErrorCode in listOf(502, 503, 504)) {
                        shouldRetry = true
                    }
                }
            } catch (e: Exception) {
                shouldRetry = true
            }

            if (shouldRetry) {
                retryCount++
                if (retryCount <= maxRetries) {
                    Thread.sleep(1000L * retryCount)
                    continue
                }
            }
            
            if (lastErrorCode != 0) return "Error ($lastErrorCode). I'm resetting, please try again! ☕"
            return "Connection trouble! 🌐"
        }
        return "I'm having trouble connecting. Let's try again in a bit! 🛠️"
    }
}
