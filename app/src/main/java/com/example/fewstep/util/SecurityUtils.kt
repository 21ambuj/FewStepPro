package com.example.fewstep.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object SecurityUtils {
    
    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            "secure_admin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            // Known Android Bug: Keystore alias desyncs after reinstall, causing exceptions.
            // Solution: Delete the corrupted SharedPreferences file and retry.
            try {
                context.deleteSharedPreferences("secure_admin_prefs")
                createEncryptedPrefs(context)
            } catch (e2: Exception) {
                // Ultimate fallback to prevent crash: use unencrypted prefs
                context.getSharedPreferences("secure_admin_prefs_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
