package com.jsb.chatapp.feature_auth.presentation.ui.screens.auth

import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_auth.domain.usecase.SigninUseCase
import com.jsb.chatapp.feature_auth.domain.usecase.SignupUseCase
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import com.jsb.chatapp.feature_chat.domain.usecase.IsUsernameAvailableUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.UpdateFcmTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.jsb.chatapp.feature_main.main_util.Result
import com.jsb.chatapp.feature_main.main_util.UserStatusManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val SigninUseCase: SigninUseCase,
    private val signupUseCase: SignupUseCase,
    private val googleAuthUiClient: GoogleAuthUiClient,
    private val userPreferences: UserPreferences,
    private val isUsernameAvailableUseCase: IsUsernameAvailableUseCase,
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase,
    private val userStatusManager: UserStatusManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var usernameCheckJob: Job? = null
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

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    // Simplified FCM token management - just get current token, don't delete
    private suspend fun getFcmToken(): String {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d("FCM", "Got FCM token: $token")
            token
        } catch (e: Exception) {
            Log.e("FCM", "Failed to get FCM token", e)
            ""
        }
    }

    private suspend fun updateUserFcmToken(userId: String) {
        try {
            val token = getFcmToken() // Use simple token getter, not fresh generation
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)

            // First check if user document exists
            val snapshot = userDocRef.get().await()
            if (!snapshot.exists()) {
                Log.w("FCM", "User document doesn't exist for userId: $userId, skipping FCM token update")
                return
            }

            // Update both FCM token and online status
            val updates = mapOf(
                "fcmToken" to token,
                "isOnline" to true,
                "lastSeen" to System.currentTimeMillis()
            )

            userDocRef.update(updates).await()
            Log.d("FCM", "Updated FCM token and online status for user: $userId, token: $token")

            // Also update through the status manager
            userStatusManager.setUserOnline(userId)
        } catch (e: Exception) {
            Log.e("FCM", "Failed to update FCM token for user: $userId", e)
            // Don't throw here as this is called after user creation and shouldn't fail the whole process
        }
    }

    suspend fun launchGoogleSignIn(): IntentSender? {
        Log.d("AuthViewModel", "Launching Google Sign-In")
        return googleAuthUiClient.signIn()
    }

    fun signInWithGoogleIntent(intent: Intent) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Processing Google Sign-In intent")
            _state.update { it.copy(isLoading = true) } // Set loading state

            val result = googleAuthUiClient.signInWithIntent(intent)
            _googleSignInResult.value = result

            if (result.data != null) {
                Log.d("AuthViewModel", "Google Sign-In successful, saving user to Firestore")

                try {
                    // Wait for user to be saved to Firestore before updating UI state
                    val isNewUser = saveUserToFirestore(result.data)

                    // Only call updateUserFcmToken if it's an existing user
                    // For new users, FCM token is already set during creation
                    if (!isNewUser) {
                        result.data.uid.let { userId ->
                            updateUserFcmToken(userId)
                        }
                    }

                    // Set user online after successful signup
                    userStatusManager.setUserOnline(result.data.uid)

                    // Only update state to authenticated after everything is complete
                    _state.update { currentState ->
                        if (currentState.rememberMe) {
                            viewModelScope.launch {
                                userPreferences.saveRememberMe(true)
                                Log.d("AuthViewModel", "Saved rememberMe: true")
                            }
                        }
                        currentState.copy(isAuthenticated = true, isLoading = false, error = null)
                    }

                    Log.d("AuthViewModel", "Google Sign-In process completed successfully")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error during Google Sign-In process", e)
                    _state.update {
                        it.copy(
                            error = "Failed to complete sign-in: ${e.message}",
                            isLoading = false,
                            isAuthenticated = false
                        )
                    }
                }
            } else {
                Log.e("AuthViewModel", "Google Sign-In failed: ${result.errorMessage}")
                _state.update {
                    it.copy(
                        error = result.errorMessage,
                        isLoading = false,
                        isAuthenticated = false
                    )
                }
            }
        }
    }

    private suspend fun saveUserToFirestore(user: User): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(user.uid)

            // Check if user exists first
            val snapshot = userDocRef.get().await()
            val userExists = snapshot.exists()
            Log.d("AuthViewModel", "Checking if user exists in Firestore: $userExists")

            if (!userExists) {
                // User doesn't exist, create new user document
                // Get FCM token for new user
                val fcmToken = getFcmToken()

                // Create explicit map to avoid field name conflicts
                val userData = mapOf(
                    "uid" to user.uid,
                    "username" to user.username,
                    "name" to user.name,
                    "email" to user.email,
                    "avatarUrl" to user.avatarUrl,
                    "phoneNumber" to user.phoneNumber,
                    "bio" to (user.bio ?: ""),
                    "fcmToken" to fcmToken,
                    "lastSeen" to System.currentTimeMillis(),
                    "createdAt" to System.currentTimeMillis(),
                    "isOnline" to true // Explicitly use "isOnline" field name
                )

                // Wait for the user to be saved before continuing
                userDocRef.set(userData).await()
                Log.d("AuthViewModel", "New Google user successfully added to Firestore with FCM token")
                false // Return false indicating this is a new user
            } else {
                // User exists, just update FCM token and online status
                val fcmToken = getFcmToken()
                val updates = mapOf(
                    "fcmToken" to fcmToken,
                    "isOnline" to true, // Explicitly use "isOnline"
                    "lastSeen" to System.currentTimeMillis()
                )

                // Wait for the update to complete
                userDocRef.update(updates).await()
                Log.d("AuthViewModel", "Existing user successfully updated with new FCM token and online status")
                true // Return true indicating this is an existing user
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error saving user to Firestore", e)
            // Re-throw the exception so it can be handled in the calling function
            throw e
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

    private fun signup() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting sign-up with email: ${state.value.email}, username: ${state.value.username}")
            _state.update { it.copy(isLoading = true) }
            val result = signupUseCase(state.value.email, state.value.password, state.value.username)
            when (result) {
                is Result.Success -> {
                    // Update FCM token with fresh token
                    FirebaseAuth.getInstance().uid?.let { userId ->
                        updateUserFcmToken(userId)
                        // Set user online after successful signup
                        userStatusManager.setUserOnline(userId)
                    }
                    Log.d("AuthViewModel", "Sign-up successful")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            error = null
                        )
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Sign-up successful! Welcome ${state.value.username ?: ""}"))
                }

                is Result.Error -> {
                    Log.e("AuthViewModel", "Sign-up failed", result.exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar(result.exception.message ?: "Unknown error"))
                }
            }
        }
    }

    private fun signin() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting sign-in with email: ${state.value.email}")
            _state.update { it.copy(isLoading = true) }
            val result = SigninUseCase(state.value.email, state.value.password)

            when (result) {
                is Result.Success -> {
                    val user = FirebaseAuth.getInstance().currentUser
                    _uiEvent.emit(UiEvent.ShowSnackbar("Sign-in successful! Welcome ${state.value.username ?: ""}"))

                    // Update FCM token with fresh token
                    user?.uid?.let { userId ->
                        updateUserFcmToken(userId)
                        // Set user online after successful signup
                        userStatusManager.setUserOnline(userId)
                    }

                    Log.d("AuthViewModel", "Sign-in successful, user: ${user?.uid}, email: ${user?.email}")
                    _state.update { currentState ->
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
                }
                is Result.Error -> {
                    Log.e("AuthViewModel", "Sign-in failed", result.exception)
                    _state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = result.exception.message
                        )
                    }
                    _uiEvent.emit(UiEvent.ShowSnackbar(result.exception.message ?: "Unknown error"))
                }
            }
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

    fun checkUsernameAvailabilityDebounced(username: String) {
        usernameCheckJob?.cancel() // Cancel the previous job
        usernameCheckJob = viewModelScope.launch {
            val isAvailable = isUsernameAvailableUseCase(username)
            _usernameAvailable.value = isAvailable
        }
    }
}