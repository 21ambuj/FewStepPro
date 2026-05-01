package com.example.fewstep.util

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareMilestone(context: Context, streak: Int, isMilestone: Boolean) {
        val title = if (isMilestone) "🏆 FewStep Milestone Reached!" else "🔥 FewStep Streak Active!"
        val message = if (isMilestone) {
            "I just hit an incredible $streak day streak on FewStep! 🚀 Building habits is life-changing.\n\nI'd really appreciate it if you join me on this journey and build your own tribe!\nDownload the app: https://21ambuj.github.io/FewStep-/"
        } else {
            "I'm on a $streak day streak on FewStep! 🔥 Building consistency one day at a time.\n\nI'd really appreciate it if you join me on this journey and build your own tribe!\nDownload the app: https://21ambuj.github.io/FewStep-/"
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        
        val chooser = Intent.createChooser(intent, "Share Achievement via")
        context.startActivity(chooser)
    }
}
