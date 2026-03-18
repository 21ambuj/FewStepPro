package com.example.fewstep.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fewstep.data.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: com.google.firebase.auth.FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Success(user)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun loginUser(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        _authState.value = AuthState.Error("Login successful but user is null")
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signupUser(name: String, email: String, pass: String) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        viewModelScope.launch {
                            try {
                                val userObj = User(
                                    uid = firebaseUser.uid,
                                    name = name,
                                    email = email,
                                    xp = 0,
                                    level = 1,
                                    currentStreak = 0
                                )
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(firebaseUser.uid)
                                    .set(userObj)
                                    .await()
                                
                                _authState.value = AuthState.Success(firebaseUser)
                            } catch (e: Exception) {
                                _authState.value = AuthState.Error("Account created but profile setup failed: ${e.message}")
                            }
                        }
                    } else {
                        _authState.value = AuthState.Error("Signup successful but user is null")
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }

    fun loginWithGoogle(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        _authState.value = AuthState.Error("Google Login successful but user is null")
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google Login failed")
                }
            }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
