package com.example.fewstep.data.repository

import com.example.fewstep.data.model.FocusSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class FocusHistoryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val userId: String? get() = auth.currentUser?.uid

    suspend fun saveSession(session: FocusSession) {
        val uid = userId ?: return
        val docRef = firestore.collection("users").document(uid).collection("focus_sessions").document()
        val finalSession = session.copy(id = docRef.id, userId = uid)
        docRef.set(finalSession).await()
    }

    fun getRecentSessions(limit: Int = 50): Flow<List<FocusSession>> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(emptyList())
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid).collection("focus_sessions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { it.toObject(FocusSession::class.java) }
                    trySend(sessions)
                }
            }
        awaitClose { listener.remove() }
    }
}
