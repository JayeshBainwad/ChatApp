package com.jsb.chatapp.feature_chat.presentation.ui.screens.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_auth.presentation.ui.screens.auth.UiEvent
import com.jsb.chatapp.feature_chat.domain.usecase.IsUsernameAvailableUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.ProfileUseCases
import com.jsb.chatapp.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val isUsernameAvailableUseCase: IsUsernameAvailableUseCase,
    private val profileUseCases: ProfileUseCases
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var usernameCheckJob: Job? = null
    private var currentUser: User? = null // Store the current user data

    private val storage = FirebaseStorage.getInstance().reference

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnUsernameChanged -> {
                _state.value = _state.value.copy(username = event.username, isUsernameAvailable = null)
                usernameCheckJob?.cancel()
                usernameCheckJob = viewModelScope.launch {
                    val isAvailable = isUsernameAvailableUseCase(event.username)
                    _state.value = _state.value.copy(isUsernameAvailable = isAvailable)
                }
            }
            is ProfileEvent.OnNameChanged -> {
                _state.value = _state.value.copy(name = event.name)
            }
            is ProfileEvent.OnPhoneChanged -> {
                _state.value = _state.value.copy(phoneNumber = event.phone)
            }
            is ProfileEvent.OnBioChanged -> {
                _state.value = _state.value.copy(bio = event.bio)
            }
            is ProfileEvent.OnAvatarSelected -> {
                uploadAvatar(event.uri)
            }
            is ProfileEvent.UpdateProfile -> {
                updateProfile()
            }
            is ProfileEvent.LoadProfile -> {
                loadCurrentUser()
            }
            is ProfileEvent.ResetSaveFlag -> {
                _state.value = _state.value.copy(isSaved = false)
            }
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val avatarRef = storage.child("avatars/$uid.jpg")

        viewModelScope.launch {
            try {
                avatarRef.putFile(uri).await()
                val downloadUrl = avatarRef.downloadUrl.await().toString()
                _state.value = _state.value.copy(avatarUrl = downloadUrl)
                _uiEvent.emit(UiEvent.ShowSnackbar("Avatar uploaded successfully"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Avatar upload failed: ${e.message}"))
            }
        }
    }

    private fun loadCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = profileUseCases.getCurrentUser(uid)) {
                is Result.Success -> {
                    val user = result.data
                    currentUser = user // Store the current user

                    Log.d(TAG, "Loaded user: ${user.username}, isOnline: ${user.isOnline}")

                    _state.value = _state.value.copy(
                        email = user.email,
                        name = user.name,
                        username = user.username,
                        avatarUrl = user.avatarUrl,
                        phoneNumber = user.phoneNumber ?: "",
                        bio = user.bio ?: "",
                        isLoading = false,
                        error = null
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                    _uiEvent.emit(UiEvent.ShowSnackbar(result.exception.message ?: "Error loading user"))
                }
            }
        }
    }

    private fun updateProfile() {
        val uid = auth.currentUser?.uid ?: return
        val currentState = state.value
        val existingUser = currentUser

        if (currentState.isUsernameAvailable == false) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar("Username already taken."))
            }
            return
        }

        // Create user object with ALL fields, preserving existing values for fields not being updated
        val user = User(
            uid = uid,
            email = currentState.email,
            username = currentState.username,
            name = currentState.name,
            avatarUrl = currentState.avatarUrl,
            phoneNumber = currentState.phoneNumber.ifBlank { null },
            bio = currentState.bio.ifBlank { null },
            fcmToken = existingUser?.fcmToken ?: "", // Preserve existing FCM token
            lastSeen = System.currentTimeMillis(), // Update last seen to current time
            createdAt = existingUser?.createdAt ?: System.currentTimeMillis(), // Preserve original creation time
            isOnline = true // Preserve current online status or default to true
        )

        Log.d(TAG, "Updating profile with user: ${user.username}, isOnline: ${user.isOnline}")

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = profileUseCases.updateUserProfile(uid, user)) {
                is Result.Success -> {
                    currentUser = user // Update the stored current user
                    _state.value = _state.value.copy(isLoading = false, isSaved = true)
                    _uiEvent.emit(UiEvent.ShowSnackbar("Profile updated"))
                    Log.d(TAG, "Profile updated successfully")
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                    _uiEvent.emit(UiEvent.ShowSnackbar(result.exception.message ?: "Unknown error"))
                    Log.e(TAG, "Profile update failed", result.exception)
                }
            }
        }
    }
}