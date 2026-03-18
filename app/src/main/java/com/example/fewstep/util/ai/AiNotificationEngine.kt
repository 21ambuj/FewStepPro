package com.example.fewstep.util.ai

import com.example.fewstep.data.model.Habit
import kotlin.random.Random

object AiNotificationEngine {

    private val morningReminders = listOf(
        "Suprabhat! ☀️ %s pending hai? Chalo nashte se pehle khatam karein! 🍳",
        "Arre o Hero! Suraj nikal gaya par %s so raha hai? Utho aur complete karo! 🐯",
        "Good morning ji! %s complete karoge tabhi toh din solid start hoga! 🏃‍♂️💨",
        "Aaj ka pehla win chahiye? %s try karo, maza ayega! 🌟✨"
    )

    private val noonReminders = listOf(
        "Lunch break chal raha hai? %s ke liye sirf 5 min nikaal lo na! 🍱😉",
        "Arre busy man! %s kab karoge? Hum toh raah dekh rahe hain... ❤️",
        "Oye! %s baaki hai. Discipline dikhao, tabhi toh Swag ayega! 😎🔥",
        "Dekho, %s complete karoge toh hi sukoon ki neend ayegi. Chalo shuru ho! ⚡"
    )

    private val eveningReminders = listOf(
        "Shaam ho gayi hai ji! %s skip mat karo, chalo shuru ho jao! 🌆💪",
        "Arre yaar, %s baaki hai aur aap reels dekh rahe ho? Not fair! 😤📱",
        "Suno na... %s kar lo, phir reward milega (Coffee meri taraf se!) ☕💓",
        "Fitness aur health important hai ji, %s complete karo aur Champion ban jao! 🏆👟"
    )

    private val midnightReminders = listOf(
        "Midnight mission! 🌌 %s khatam karo aur chain ki neend so jao. 🛌💤",
        "Raat ho gayi hai par %s abhi bhi akela hai... bechara! Jaldi niptao! 🌙😔",
        "Sone se pehle %s kar lo ji, warna sapne mein AI coach darayega! 👻😂",
        "Oye Champion! Bahut der ho gayi, par tasks check kar lo pehle! 😴💤"
    )

    private val streakProtector = listOf(
        "Oye! %d days ka streak khatre mein hai! %s jaldi karo varna sab zero ho jayega! 💣🔥",
        "Aapka %d days ka mehnat waste mat karo. %s complete karo aur aage badho! 🚀📈",
        "Arre %d days streak is legendary! %s skip karke record mat todo please! 🥺✨",
        "Consistency is key, aur aap toh Lock hi bhul gaye! %d days streak save karo! 🔑🤩"
    )

    fun getMessage(habitName: String, hour: Int, streak: Int): String {
        // High priority: Streak protection if user has 3+ days
        if (streak >= 3 && Random.nextFloat() > 0.5f) {
            val template = streakProtector.random()
            return String.format(template, streak, habitName)
        }

        // Phase based messages
        val list = when (hour) {
            in 5..10 -> morningReminders
            in 11..15 -> noonReminders
            in 16..21 -> eveningReminders
            else -> midnightReminders
        }
        
        val template = list.random()
        return String.format(template, habitName)
    }

    fun getGeneralMotivation(): String {
        return listOf(
            "Life mein habits zaroori hai, varna confuse ho jaoge! Task dekho jaldi! 😂📱",
            "Phone chodo, kaam pe lago! Habits complete karoge tabhi toh level up hoga! 📈🔥",
            "Aapka AI coach aapko miss kar raha hai... Kuch task toh kar lo! 🤖💔",
            "Ab so jao ji, baaki kal karenge. Shaba khair! 😴🌃"
        ).random()
    }
}
