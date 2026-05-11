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

    fun shareImage(context: Context, bitmap: android.graphics.Bitmap) {
        try {
            val cachePath = java.io.File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "weekly_recap.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Here is my FewStep Weekly Recap! 🔥 Join me: https://21ambuj.github.io/FewStep-/")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share Weekly Recap"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text share if image fails
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Here is my FewStep Weekly Recap! 🔥 Join me: https://21ambuj.github.io/FewStep-/")
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Share Weekly Recap"))
        }
    }
}
