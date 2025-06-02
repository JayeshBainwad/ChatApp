package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jsb.chatapp.feature_auth.domain.usecase.LoginUseCase
import com.jsb.chatapp.feature_auth.domain.usecase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jsb.chatapp.core.util.Result
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.domain.model.User
import kotlinx.coroutines.tasks.await

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
    private val googleAuthUiClient: GoogleAuthUiClient // Injected
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _usernameAvailable = MutableStateFlow<Boolean?>(null)
    val usernameAvailable: StateFlow<Boolean?> = _usernameAvailable

    // Add a new state
    private val _googleSignInResult = MutableStateFlow<SignInResult?>(null)
    val googleSignInResult: StateFlow<SignInResult?> = _googleSignInResult.asStateFlow()

    suspend fun launchGoogleSignIn(): IntentSender? {
        return googleAuthUiClient.signIn()
    }

    fun signInWithGoogleIntent(intent: Intent) {
        viewModelScope.launch {
            val result = googleAuthUiClient.signInWithIntent(intent)
            _googleSignInResult.value = result
            if (result.data != null) {
                saveUserToFirestore(result.data)
                _state.update { it.copy(isAuthenticated = true, isLoading = false) }
            } else {
                _state.update { it.copy(error = result.errorMessage, isLoading = false) }
            }
        }
    }

    private suspend fun saveUserToFirestore(user: User) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(user.uid)

            // Save user fields
            val userData = mapOf(
                "userId" to user.uid,
                "name" to user.name,
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "createdAt" to System.currentTimeMillis()
            )

            userDoc.set(userData).await() // ⬅️ Upload to Firestore
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
            is AuthEvent.Signin -> login()
            is AuthEvent.Signup -> signup()
        }
    }

    private fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = loginUseCase(state.value.email, state.value.password)
            _state.update { currentState ->
                when (result) {
                    is Result.Success -> currentState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        error = null
                    )
                    is Result.Error -> {
                        Log.e("AuthViewModel", "Login error", result.exception)
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
            _state.update { it.copy(isLoading = true) }
            val result = signupUseCase(state.value.email, state.value.password, state.value.username)
            _state.update { currentState ->
                when (result) {
                    is Result.Success -> currentState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        error = null
                    )
                    is Result.Error -> {
                        Log.e("AuthViewModel", "Signup error", result.exception)
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