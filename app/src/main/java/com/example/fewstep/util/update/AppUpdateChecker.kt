package com.example.fewstep.util.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false
)

object AppUpdateChecker {

    // Host this version.json on your GitHub Pages site
    private const val VERSION_URL = "https://21ambuj.github.io/FewStep-/version.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(VERSION_URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = JSONObject(body)
            val latestVersionCode = json.getInt("latestVersionCode")

            if (latestVersionCode > currentVersionCode) {
                return@withContext UpdateInfo(
                    latestVersionCode = latestVersionCode,
                    latestVersionName = json.optString("latestVersionName", "New Version"),
                    apkUrl = json.getString("apkUrl"),
                    releaseNotes = json.optString("releaseNotes", "Bug fixes and performance improvements."),
                    isForceUpdate = json.optBoolean("forceUpdate", false)
                )
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateChecker", "Update check failed: ${e.message}")
            null
        }
    }
}
