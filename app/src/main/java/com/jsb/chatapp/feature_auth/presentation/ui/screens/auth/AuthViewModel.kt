package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_auth.domain.usecase.SigninUseCase
import com.jsb.chatapp.feature_auth.domain.usecase.SignupUseCase
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.jsb.chatapp.util.Result

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val SigninUseCase: SigninUseCase,
    private val signupUseCase: SignupUseCase,
    private val googleAuthUiClient: GoogleAuthUiClient,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable

    private val _googleSignInResult = MutableStateFlow<SignInResult?>(null)
    val googleSignInResult: StateFlow<SignInResult?> = _googleSignInResult.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        Log.d("AuthViewModel", "AuthStateListener triggered. Current User: ${user?.displayName ?: "null"} | UID: ${user?.uid ?: "null"}")
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    suspend fun launchGoogleSignIn(): IntentSender? {
        Log.d("AuthViewModel", "Launching Google Sign-In")
        return googleAuthUiClient.signIn()
    }

    fun signInWithGoogleIntent(intent: Intent) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Processing Google Sign-In intent")
            val result = googleAuthUiClient.signInWithIntent(intent)
            _googleSignInResult.value = result
            if (result.data != null) {
                Log.d("AuthViewModel", "Google Sign-In successful, saving user to Firestore")
                saveUserToFirestore(result.data)
                _state.update {
                    if (it.rememberMe) {
                        viewModelScope.launch {
                            userPreferences.saveRememberMe(true)
                            Log.d("AuthViewModel", "Saved rememberMe: true")
                        }
                    }
                    it.copy(isAuthenticated = true, isLoading = false)
                }
            } else {
                Log.e("AuthViewModel", "Google Sign-In failed: ${result.errorMessage}")
                _state.update { it.copy(error = result.errorMessage, isLoading = false) }
            }
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(user.uid)
            val userData = mapOf(
                "uid" to user.uid,
                "username" to user.username,
                "name" to user.name,
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "phoneNumber" to user.phoneNumber,
                "bio" to user.bio,
                "lastSeen" to user.lastSeen,
                "createdAt" to user.createdAt
            )
            userDoc.set(userData).await()
            Log.d("AuthViewModel", "User saved to Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error saving user to Firestore", e)
        }
    }

    fun resetGoogleSignInResult() {
        _googleSignInResult.value = null
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.UpdateEmail -> _state.update { it.copy(email = event.email) }
            is AuthEvent.UpdatePassword -> _state.update { it.copy(password = event.password) }
            is AuthEvent.UpdateUsername -> _state.update { it.copy(username = event.username) }
            is AuthEvent.ToggleRememberMe -> _state.update { it.copy(rememberMe = event.checked) }
            is AuthEvent.Signin -> signin()
            is AuthEvent.Signup -> signup()
            is AuthEvent.ForgotPassword -> sendPasswordResetEmail()
        }
    }

    private fun sendPasswordResetEmail() {
        viewModelScope.launch {
            val email = state.value.email
            if (email.isBlank()) {
                Log.w("AuthViewModel", "Email is blank for password reset")
                _state.update { it.copy(error = "Email cannot be empty for reset") }
                return@launch
            }
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
                Log.d("AuthViewModel", "Password reset email sent to $email")
                _state.update { it.copy(error = "Password reset email sent!") }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset failed", e)
                _state.update { it.copy(error = e.message ?: "Reset failed") }
            }
        }
    }

    private fun signin() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting sign-in with email: ${state.value.email}")
            _state.update { it.copy(isLoading = true) }
            val result = SigninUseCase(state.value.email, state.value.password)
            _state.update { currentState ->
                when (result) {
                    is Result.Success -> {
                        val user = FirebaseAuth.getInstance().currentUser
                        Log.d("AuthViewModel", "Sign-in successful, user: ${user?.uid}, email: ${user?.email}")
                        if (currentState.rememberMe) {
                            viewModelScope.launch {
                                userPreferences.saveRememberMe(true)
                                Log.d("AuthViewModel", "Saved rememberMe: true")
                            }
                        }
                        currentState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        Log.e("AuthViewModel", "Sign-in failed", result.exception)
                        currentState.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                }
            }
        }
    }

    private fun signup() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting sign-up with email: ${state.value.email}, username: ${state.value.username}")
            _state.update { it.copy(isLoading = true) }
            val result = signupUseCase(state.value.email, state.value.password, state.value.username)
            _state.update { currentState ->
                when (result) {
                    is Result.Success -> {
                        val user = FirebaseAuth.getInstance().currentUser
                        Log.d("AuthViewModel", "Sign-up successful, user: ${user?.uid}, email: ${user?.email}")
                        currentState.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        Log.e("AuthViewModel", "Sign-up failed", result.exception)
                        currentState.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun isUsernameAvailable(username: String): Boolean {
        val doc = FirebaseFirestore.getInstance()
            .collection("usernames")
            .document(username)
            .get()
            .await()
        return !doc.exists()
    }

    fun checkUsernameAvailability(username: String) {
        viewModelScope.launch {
            _usernameAvailable.value = isUsernameAvailable(username)
        }
    }
}