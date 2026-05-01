package com.example.fewstep.util.ai

import com.example.fewstep.data.model.Habit
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object AiNotificationEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private fun getStudyReminders(user: String, rank: String) = listOf(
        "अरे भविष्य के लीडर $user! 🧠 %s आपकी मंजिल का रास्ता है... हार मत मानो!",
        "सफलता मेहनत मांगती है $rank! 👀 %s पूरा करो! ज्ञान ही आपकी असली ताकत है! 🎯🔥",
        "ओये विजेता $user! 💡 %s के लिए सिर्फ 10 मिनट... अपने सपनों से समझौता मत करो! 🚀📖",
        "मेहनत कभी बेकार नहीं जाती! %s निपटाओ और साबित करो कि आप $rank से बेहतर हैं! 🏆🎓"
    )

    private fun getGymReminders(user: String, rank: String) = listOf(
        "जीत का जुनून चाहिए $rank! 💪 %s पूरा करो और अपने शरीर को मजबूत बनाओ!",
        "ओये चैंपियन $user! 🏋️‍♂️ %s पूरा करो और साबित करो कि आप रुकने वाले नहीं हैं!",
        "अनुशासन ही आपकी पहचान है... %s कर लो, आज का दर्द कल की जीत बनेगा! 🥛🎖️",
        "उठो! %s पूरा करो! पसीना बहाओ और अपनी ताकत को महसूस करो! 😤🥊"
    )

    private fun getWorkReminders(user: String, rank: String) = listOf(
        "सपनों के सौदागर $user! 💼 %s आपका भविष्य बदल सकता है... पूरे जोश से लग जाओ! 📈🔥",
        "वक्त की कदर करो! %s खत्म करो और $rank का ताज हासिल करो! 👑💻",
        "आपका जुनून ही आपकी ताकत है... %s में अपना सौ प्रतिशत दो $user! ⚡🏢",
        "महानता काम करने से मिलती है! 💪 %s निपटाओ और आगे बढ़ो! 🥇🚀"
    )

    private fun getHealthReminders(user: String, rank: String) = listOf(
        "स्वास्थ्य ही असली दौलत है! 🍏 %s को मत छोड़ो, आपका शरीर आपका मंदिर है $user! 😤",
        "लंबी और खुशहाल जिंदगी चाहिए $rank? 🏃‍♂️ तो %s पूरा करना ही पड़ेगा... फिट रहो!",
        "अरे ऊर्जावान $user! ✨ %s के बिना आपका दिन अधूरा है। चलो इसे जल्दी पूरा करो!",
        "खुद से प्यार करो! 🙏 %s पूरा करो और अपने जीवन में नई ताजगी महसूस करो! 🌱🌟"
    )

    private fun getMorningReminders(user: String, rank: String) = listOf(
        "सुप्रभात $user! ☀️ %s आपका इंतजार कर रहा है... चलो दिन की शुरुआत एक जीत से करें! 🍳",
        "अरे ओ $rank... 👀 सूरज निकल आया है और आपकी मंजिल भी! उठो और %s पूरा करो! 🐯",
        "गुड मॉर्निंग! 🌅 %s पूरा करोगे तभी तो आपका दिन शानदार बनेगा! जोश दिखाओ!",
        "आज का पहला कदम उठाओ $user! 🔥 %s से शुरुआत करो, जीत आपकी होगी! 🌟✨"
    )

    private fun getNoonReminders(user: String, rank: String) = listOf(
        "दिन का आधा सफर तय हो गया $user! 🍱 %s के लिए 5 मिनट निकालो और रफ्तार पकड़ो! 😉",
        "अरे कर्मवीर $rank... %s करने का यही सही समय है! हम आपके साथ हैं... ❤️",
        "ओये $user! 🕶️ %s बाकी है... अपना अनुशासन दिखाओ और सबको हैरान कर दो! 😎🔥",
        "याद रखो... %s पूरा करोगे तभी सुकून मिलेगा। चलो, अभी शुरू करो! ⚡"
    )

    private fun getEveningReminders(user: String, rank: String) = listOf(
        "शाम हो गई है! %s मत छोड़ो $user... अपनी मंजिल की तरफ कदम बढ़ाओ!",
        "अरे $user... %s बाकी है! आपका कल आज की मेहनत पर निर्भर करता है! 😤📱",
        "सुनो $rank... %s कर लो, अपनी खुद की नजरों में हीरो बनो! ☕💓",
        "फिटनेस और अनुशासन ही जीवन है... 💪 %s पूरा करो और रिकॉर्ड तोड़ो! 🏆👟"
    )

    private fun getMidnightReminders(user: String, rank: String) = listOf(
        "रात का सफर $user! 🌌 %s खत्म करो और एक विजेता की तरह चैन की नींद सो जाओ। 🛌💤",
        "दिन खत्म होने वाला है, पर आपका जज्बा नहीं! %s जल्दी पूरा करो! 🌙",
        "सोने से पहले %s कर लो $rank... कल सुबह गर्व से उठना! 👻😂",
        "ओये $user... वक्त कम है, पर आपकी हिम्मत ज्यादा! लक्ष्य पूरा करो! 😴💤"
    )

    private fun getStreakProtector(user: String, rank: String) = listOf(
        "ओये $user! %d दिनों का स्ट्रिक खतरे में है! %s जल्दी करो, अपनी मेहनत को बेकार मत जाने दो! 💣🔥",
        "आपका %d दिनों का संघर्ष जाया नहीं जाना चाहिए। %s पूरा करो और इतिहास रचो! 🚀📈",
        "अरे $rank, %d दिनों का स्ट्रिक किसी रिकॉर्ड से कम नहीं! इसे टूटने मत देना! 🥺✨",
        "निरंतरता ही सफलता की कुंजी है $user! अपना %d दिनों का स्ट्रिक बचाओ और %s पूरा करो! 🔑🤩"
    )

    suspend fun getMessage(habitName: String, hour: Int, streak: Int, userName: String = "Champion", rank: String = "Novice"): String {
        return withContext(Dispatchers.IO) {
            try {
                val timeOfDayStr = when(hour) {
                    in 5..11 -> "Morning (Greet with Good Morning)"
                    in 12..16 -> "Afternoon (Greet with Good Afternoon)"
                    in 17..20 -> "Evening (Greet with Good Evening. NEVER say Good Night)"
                    else -> "Night (Greet with Good Night)"
                }

                val prompt = """
                    Write a short, deeply emotional and highly motivational 1-sentence push notification in Simple Native Hindi (Devanagari script) for a user named $userName (Rank: $rank) who needs to do their habit: '$habitName'. 
                    Current time context: $timeOfDayStr. Current streak: $streak days. 
                    Be highly encouraging, energetic, and inspiring. Use simple, conversational Hindi words spoken in daily life. Avoid complex or overly formal words. Keep it under 15 words. Include 1-2 emojis. 
                    CRITICAL: Write the entire notification in HINDI script (Devanagari). DO NOT use English letters for Hindi words.
                    Example style: 'आज रुकना नहीं है, %s पूरा करो और अपने सपनों को सच करो! 🔥💪'
                    No hashtag, no intro. Pure 1-sentence push notification.
                """.trimIndent()
                
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val url = "https://text.pollinations.ai/$encodedPrompt"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val aiText = response.body?.string()?.trim()
                    if (!aiText.isNullOrEmpty() && aiText.length < 150) {
                        return@withContext aiText
                    }
                }
            } catch (e: Exception) {
                // Silent catch, let it drop down to fallback
            }
            
            // Critical Fallback logic guarantees delivery
            getLocalMessage(habitName, hour, streak, userName, rank)
        }
    }

    private fun getLocalMessage(habitName: String, hour: Int, streak: Int, userName: String, rank: String): String {
        // High priority: Streak protection if user has 3+ days
        if (streak >= 3 && Random.nextFloat() > 0.4f) {
            val template = getStreakProtector(userName, rank).random()
            return String.format(template, streak, habitName)
        }

        // Mid priority: Categorized personalization
        val activityPool = detectActivityPool(habitName, userName, rank)
        if (activityPool != null && Random.nextFloat() > 0.3f) {
            return String.format(activityPool.random(), habitName)
        }

        // Default: Phase based messages
        val list = when (hour) {
            in 5..10 -> getMorningReminders(userName, rank)
            in 11..15 -> getNoonReminders(userName, rank)
            in 16..21 -> getEveningReminders(userName, rank)
            else -> getMidnightReminders(userName, rank)
        }
        
        val template = list.random()
        return String.format(template, habitName)
    }

    private fun detectActivityPool(habitName: String, userName: String, rank: String): List<String>? {
        val name = habitName.lowercase()
        return when {
            name.contains("study") || name.contains("padho") || name.contains("math") || name.contains("coding") || name.contains("read") || name.contains("book") -> getStudyReminders(userName, rank)
            name.contains("gym") || name.contains("workout") || name.contains("exercise") || name.contains("body") || name.contains("weight") || name.contains("pushup") -> getGymReminders(userName, rank)
            name.contains("work") || name.contains("office") || name.contains("project") || name.contains("client") || name.contains("email") || name.contains("call") -> getWorkReminders(userName, rank)
            name.contains("water") || name.contains("health") || name.contains("yoga") || name.contains("medi") || name.contains("fruit") || name.contains("run") || name.contains("walk") -> getHealthReminders(userName, rank)
            else -> null
        }
    }

    suspend fun getGeneralMotivation(userName: String = "Champion", rank: String = "Novice"): String {
         return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Write a short, highly emotional and motivational 1-sentence push notification to celebrate user $userName (Rank: $rank) because they have successfully completed ALL their tasks today.
                    CRITICAL: Write the entire notification in Simple HINDI script (Devanagari). 
                    Praise their dedication using simple, real-life words. Keep it under 12 words. Do not use English letters for Hindi words. Include 1-2 emojis.
                    Example style: 'आज आपने कमाल कर दिया, आप सच में एक विनर हैं! 🏆🔥'
                """.trimIndent()
                
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val url = "https://text.pollinations.ai/$encodedPrompt"
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val aiText = response.body?.string()?.trim()
                    if (!aiText.isNullOrEmpty() && aiText.length < 150) {
                        return@withContext aiText
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            
            // Fallback
            listOf(
                "Dedication level infinity! Aaj ke saare goals finish. You are a beast! 🦁🔥",
                "Success follows the disciplined! Sab kuch complete kar diya, proud of you! 🏆⚡",
                "Oye $userName! Aaj ka din aapka tha. Aise hi aage badhte raho! 🚀💪",
                "No excuses, only results! Saare tasks done. Legend status unlocked! 👑✨",
                "Consistency is your superpower! Ab thoda rest karo, kal phir jeetna hai! 🛌🌱"
            ).random()
        }
    }
}
