package com.example.fewstep.util.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class GeneratedHabit(
    val name: String,
    val description: String,
    val time: String
)

object AiGoalGenerator {
    private val client = OkHttpClient()

    suspend fun generateHabitsForGoal(goal: String): List<GeneratedHabit> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = "You are a habit coach. The user wants to achieve this goal: '$goal'. " +
                    "Return EXACTLY a JSON array of 3 daily habits to achieve this. " +
                    "Each habit MUST include a 'name', 'description', and a suggested 'time' (24-hour format HH:mm, e.g. 07:30 or 21:00). " +
                    "Do NOT use markdown block ticks. Return ONLY raw JSON. " +
                    "Example: [{\"name\": \"Morning Run\", \"description\": \"Run for 20 mins\", \"time\": \"06:30\"}]"

            val encodedPrompt = URLEncoder.encode(systemPrompt, "UTF-8")
            val url = "https://text.pollinations.ai/$encodedPrompt"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val cleanedJson = responseBody.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val habitsList = mutableListOf<GeneratedHabit>()
            val jsonArray = JSONArray(cleanedJson)
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                habitsList.add(
                    GeneratedHabit(
                        name = obj.getString("name"),
                        description = obj.getString("description"),
                        time = obj.optString("time", "09:00")
                    )
                )
            }
            
            return@withContext habitsList
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext listOf(
                GeneratedHabit("Research $goal", "Spend 10 minutes learning about your goal", "09:00"),
                GeneratedHabit("Take action on $goal", "Do one small task related to your goal", "12:00"),
                GeneratedHabit("Review $goal", "Reflect on your progress today", "20:00")
            )
        }
    }
}
