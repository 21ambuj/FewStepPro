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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

import com.google.android.gms.tasks.Task
import com.example.fewstep.util.NotificationScheduler

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object VerificationLinkSent : AuthState()
    object PasswordResetSent : AuthState()
    data class Success(val user: com.google.firebase.auth.FirebaseUser, val message: String = "") : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthUiEvent {
    data class ShowToast(val message: String) : AuthUiEvent()
    data class ShowSnackbar(val message: String) : AuthUiEvent()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiEvents = Channel<AuthUiEvent>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun checkCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            _authState.value = AuthState.Success(user, "Welcome back!")
        } else {
            _authState.value = AuthState.Idle
        }
    }









    private fun sendUiEvent(event: AuthUiEvent) {
        viewModelScope.launch { _uiEvents.send(event) }
    }

    private var pendingSignupName: String? = null
    private var pendingSignupPass: String? = null

    fun loginUser(email: String, pass: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email not found or invalid format")
            sendUiEvent(AuthUiEvent.ShowToast("Please enter a valid email"))
            return
        }
        if (pass.isEmpty()) {
            _authState.value = AuthState.Error("Password is required")
            sendUiEvent(AuthUiEvent.ShowToast("Password is required"))
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        _authState.value = AuthState.Success(user, "Login Successful! 😊")
                        sendUiEvent(AuthUiEvent.ShowToast("Login Successful! Welcome back."))
                    } else {
                        _authState.value = AuthState.Error("Unknown Auth error.")
                    }
                } else {
                    val error = task.exception
                    val errorMsg = when {
                        error is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Account not found. Please create a new account."
                        error?.message?.contains("no user record") == true -> "Account not found. Please create a new account."
                        error is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password or account not found."
                        error?.message?.lowercase()?.contains("quota exceeded") == true -> "Daily limit reached. Please try again later."
                        else -> error?.message ?: "Login Failed"
                    }
                    _authState.value = AuthState.Error(errorMsg)
                    sendUiEvent(AuthUiEvent.ShowToast(errorMsg))
                }
            }
    }

    fun requestSignupVerification(name: String, email: String, pass: String) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Please fill all fields")
            sendUiEvent(AuthUiEvent.ShowToast("All fields are required!"))
            return
        }

        _authState.value = AuthState.Loading
        
        // Use standard Email/Password Signup + Verification (Free & No strict quota)
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                viewModelScope.launch {
                                    try {
                                        val userObj = User(uid = user.uid, name = name, email = email)
                                        db.collection("users").document(user.uid).set(userObj).await()
                                        _authState.value = AuthState.Success(user, "Welcome to FewStep! 🎒")
                                        sendUiEvent(AuthUiEvent.ShowToast("Verification email sent! You can verify later."))
                                        startVerificationPolling() // Still runs in background
                                    } catch (e: Exception) {
                                        _authState.value = AuthState.Error("Profile setup error: ${e.message}")
                                    }
                                }
                            } else {
                                // Even if email send fails, we let them in to avoid blocking Play Store testers
                                _authState.value = AuthState.Success(user, "Welcome! 🎒")
                                sendUiEvent(AuthUiEvent.ShowToast("Welcome back!"))
                            }
                        }
                } else {
                    val error = task.exception
                    val errorMsg = when {
                        error is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Account already exists! Please Login."
                        error?.message?.lowercase()?.contains("quota exceeded") == true -> "Limit exceeded. Please try Google Sign-in."
                        else -> error?.message ?: "Signup Failed"
                    }
                    _authState.value = AuthState.Error(errorMsg)
                    sendUiEvent(AuthUiEvent.ShowToast(errorMsg))
                }
            }
    }

    private fun startVerificationPolling() {
        viewModelScope.launch {
            var verified = false
            while (!verified) {
                kotlinx.coroutines.delay(3000) // Check every 3 seconds
                val user = auth.currentUser
                if (user != null) {
                    try {
                        user.reload().await()
                        if (user.isEmailVerified) {
                            verified = true
                            _authState.value = AuthState.Success(user, "Email Verified! Building habits... 😊")
                            sendUiEvent(AuthUiEvent.ShowToast("Verified successfully!"))
                        }
                    } catch (e: Exception) {
                        // User might have logged out or network error
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    fun completeEmailLinkSignIn(email: String, link: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailLink(email, link)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Complete profile with cached name
                        val finalName = pendingSignupName ?: email.split("@")[0]
                        val finalPass = pendingSignupPass

                        viewModelScope.launch {
                            try {
                                if (finalPass != null) {
                                    user.updatePassword(finalPass).await()
                                }
                                val newUser = User(uid = user.uid, email = email, name = finalName)
                                db.collection("users").document(user.uid).set(newUser).await()
                                
                                _authState.value = AuthState.Success(user, "Verification Success! Account Created. 🎒")
                                sendUiEvent(AuthUiEvent.ShowToast("Email Verified! Account created successfully."))
                                
                                pendingSignupName = null
                                pendingSignupPass = null
                            } catch (e: Exception) {
                                _authState.value = AuthState.Success(user, "Welcome back!") // fallback if update fails
                            }
                        }
                    }
                } else {
                    _authState.value = AuthState.Error("Magic Link expired or invalid.")
                    sendUiEvent(AuthUiEvent.ShowToast("Verification link has expired. Please try again."))
                }
            }
    }

    fun signupUser(name: String, email: String, pass: String) {
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            _authState.value = AuthState.Error("Please fill all fields")
            sendUiEvent(AuthUiEvent.ShowToast("All fields are required!"))
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
                                val userObj = User(uid = firebaseUser.uid, name = name, email = email)
                                db.collection("users").document(firebaseUser.uid).set(userObj).await()
                                _authState.value = AuthState.Success(firebaseUser, "Account Created Successfully! 🎒")
                                sendUiEvent(AuthUiEvent.ShowToast("Account Created! Welcome to FewStep."))
                            } catch (e: Exception) {
                                _authState.value = AuthState.Error("Profile setup failed: ${e.message}")
                            }
                        }
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Signup Failed"
                    if (errorMsg.contains("already in use")) {
                        _authState.value = AuthState.Error("You already have an account. Please login.")
                        sendUiEvent(AuthUiEvent.ShowToast("Account already exists! Please Login."))
                    } else {
                        _authState.value = AuthState.Error(errorMsg)
                        sendUiEvent(AuthUiEvent.ShowToast(errorMsg))
                    }
                }
            }
    }

    fun loginWithGoogle(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        viewModelScope.launch {
                            try {
                                val doc = db.collection("users").document(firebaseUser.uid).get().await()
                                if (!doc.exists()) {
                                    val newUser = User(
                                        uid = firebaseUser.uid,
                                        name = firebaseUser.displayName ?: "Champion",
                                        email = firebaseUser.email ?: ""
                                    )
                                    db.collection("users").document(firebaseUser.uid).set(newUser).await()
                                }
                                _authState.value = AuthState.Success(firebaseUser, "Login Successful! ✨")
                                sendUiEvent(AuthUiEvent.ShowToast("Login Successful! Welcome."))
                            } catch (e: Exception) {
                                // Fallback success even if profile check fails
                                _authState.value = AuthState.Success(firebaseUser, "Login Successful!")
                            }
                        }
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Google Login failed"
                    _authState.value = AuthState.Error(errorMsg)
                    sendUiEvent(AuthUiEvent.ShowToast(errorMsg))
                }
            }
    }

    fun sendPasswordResetEmail(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email not found or invalid format")
            sendUiEvent(AuthUiEvent.ShowToast("Please enter a valid email"))
            return
        }

        _authState.value = AuthState.Loading
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.PasswordResetSent
                    sendUiEvent(AuthUiEvent.ShowToast("Reset link sent! Please check your email inbox."))
                } else {
                    val error = task.exception
                    val errorMsg = when {
                        error is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Account not found. Please register first."
                        else -> error?.message ?: "Failed to send reset email"
                    }
                    _authState.value = AuthState.Error(errorMsg)
                    sendUiEvent(AuthUiEvent.ShowToast(errorMsg))
                }
            }
    }

    fun deleteAccount() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // MANUAL DELETION REQUEST (Zero-Budget Sync for Spark Plan)
                
                // 1. Create the Request for Admin
                val request = com.example.fewstep.data.model.DeletionRequest(
                    id = currentUser.uid,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Champion",
                    userEmail = currentUser.email ?: "",
                    timestamp = System.currentTimeMillis()
                )
                db.collection("deletion_requests").document(currentUser.uid).set(request).await()
                
                // 2. Block the current session permanently in Firestore
                val blockUpdates = hashMapOf<String, Any>(
                    "isBlocked" to true,
                    "isDeleted" to true,
                    "blockedAt" to System.currentTimeMillis(),
                    "deletionRequestedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(currentUser.uid).update(blockUpdates).await()
                
                sendUiEvent(AuthUiEvent.ShowToast("Request sent! An Admin will process your account deletion shortly."))
            } catch (e: Exception) {
                sendUiEvent(AuthUiEvent.ShowToast("Failed to request: ${e.message}"))
            }
        }
    }

    fun cancelDeletionRequest() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // REVERSING THE DELETION REQUEST
                
                // 1. Remove from Admin's Queue
                db.collection("deletion_requests").document(currentUser.uid).delete().await()
                
                // 2. Clear flags in Firestore
                val restoreUpdates = hashMapOf<String, Any>(
                    "isBlocked" to false,
                    "isDeleted" to false,
                    "deletionRequestedAt" to 0L
                )
                db.collection("users").document(currentUser.uid).update(restoreUpdates).await()
                
                sendUiEvent(AuthUiEvent.ShowToast("Account Restored! Welcome back! 🛡️✨"))
            } catch (e: Exception) {
                sendUiEvent(AuthUiEvent.ShowToast("Failed to restore: ${e.message}"))
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
        sendUiEvent(AuthUiEvent.ShowToast("Logged out successfully."))
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
