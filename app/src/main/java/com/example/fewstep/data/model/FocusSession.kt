package com.example.fewstep.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FocusSession(
    val id: String = "",
    val userId: String = "",
    val durationSeconds: Int = 0,
    val tag: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)
