package com.jsb.chatapp.feature_chat.presentation.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_main.main_navigation.Screen
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_chat.domain.usecase.GetChatsRealtimeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val getChatsRealtimeUseCase: GetChatsRealtimeUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "MainScreenViewModel"
    }

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private var chatsJob: Job? = null

    init {
        loadCurrentUserAndChats()
    }

    private fun loadCurrentUserAndChats() {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "No current user found")
            return
        }

        // Load current user from Firestore
        viewModelScope.launch {
            try {
                val userDoc = firestore
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .await()
                if (userDoc.exists()) {
                    val currentUser = userDoc.toObject(User::class.java)
                    _state.value = _state.value.copy(currentUser = currentUser)
                    Log.d(TAG, "Current user loaded: ${currentUser?.username}")

                    // Start listening to chats for real-time updates
                    startChatsListener(currentUserId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current user", e)
            }
        }
    }

    private fun startChatsListener(currentUserId: String) {
        chatsJob?.cancel()
        chatsJob = getChatsRealtimeUseCase(currentUserId)
            .onEach { chats ->
                Log.d(TAG, "Received ${chats.size} chats from real-time updates")

                // Update available chats
                val currentState = _state.value
                _state.value = currentState.copy(availableChats = chats)

                // If we have a selected user, update it with fresh data from the real-time updates
                val selectedUserId = currentState.selectedUserIdForChat
                if (selectedUserId != null) {
                    val updatedChat = chats.find { it.otherUser.uid == selectedUserId }
                    if (updatedChat != null) {
                        Log.d(
                            TAG,
                            "Updating selected user with real-time data: " +
                                    updatedChat.otherUser.username
                        )
                        _state.value = _state.value.copy(
                            selectedOtherUser = updatedChat.otherUser,
                            selectedChatId = updatedChat.chatId
                        )
                    }
                }
            }
            .catch { error ->
                Log.e(TAG, "Error in real-time chat updates", error)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Loads a user from Firestore by their ID
     * This is used when starting a new chat with someone not in available chats
     */
    private suspend fun loadUserById(userId: String): User? {
        return try {
            val userDoc = firestore
                .collection("users")
                .document(userId)
                .get()
                .await()

            if (userDoc.exists()) {
                userDoc.toObject(User::class.java)
            } else {
                Log.w(TAG, "User with ID $userId not found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user with ID $userId", e)
            null
        }
    }

    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ShowSignOutDialog -> {
                _state.value = _state.value.copy(showSignOutDialog = true)
            }
            is MainScreenEvent.HideSignOutDialog -> {
                _state.value = _state.value.copy(showSignOutDialog = false)
            }
            is MainScreenEvent.ConfirmSignOut -> {
                _state.value = _state.value.copy(showSignOutDialog = false)
            }
            is MainScreenEvent.UpdateCurrentRoute -> {
                _state.value = _state.value.copy(currentRoute = event.route)
            }
            is MainScreenEvent.SelectChatUser -> {
                // First, try to find the user from available chats
                val selectedChat = _state.value.availableChats.find {
                    it.otherUser.uid == event.userId
                }

                if (selectedChat != null) {
                    // Chat exists, use the existing chat data
                    _state.value = _state.value.copy(
                        selectedOtherUser = selectedChat.otherUser,
                        selectedChatId = selectedChat.chatId,
                        selectedUserIdForChat = event.userId
                    )
                    Log.d(TAG, "Selected existing chat user: ${selectedChat.otherUser.username}")
                    Log.d("FCM_DEBUGER", "Selected existing user: ${selectedChat.otherUser.fcmToken}")
                } else {
                    // Chat doesn't exist, load the user from Firestore to start a new chat
                    Log.d(TAG, "No existing chat found for user $event.userId, loading user data")
                    viewModelScope.launch {
                        val otherUser = loadUserById(event.userId)
                        if (otherUser != null) {
                            // Generate a new chat ID for the new conversation
                            val currentUserId = _state.value.currentUser?.uid
                            val newChatId = if (currentUserId != null) {
                                listOf(currentUserId, event.userId).sorted().joinToString("_")
                            } else null

                            _state.value = _state.value.copy(
                                selectedOtherUser = otherUser,
                                selectedChatId = newChatId,
                                selectedUserIdForChat = event.userId
                            )
                            Log.d(TAG, "Loaded user for new chat: ${otherUser.username}")
                        } else {
                            Log.e(TAG, "Failed to load user data for ID: ${event.userId}")
                        }
                    }
                }
            }
            is MainScreenEvent.SetChatUsers -> {
                // For direct user setting (like from notifications)
                _state.value = _state.value.copy(
                    selectedOtherUser = event.otherUser,
                    selectedChatId = event.chatId,
                    selectedUserIdForChat = event.otherUser.uid
                )
            }
            is MainScreenEvent.ClearSelectedChat -> {
                _state.value = _state.value.copy(
                    selectedOtherUser = null,
                    selectedChatId = null,
                    selectedUserIdForChat = null
                )
            }
        }
    }

    fun getTitle(currentRoute: String?): String {
        return when (currentRoute) {
            Screen.ChatHome.route -> "Chat App"
            Screen.Profile.route -> "Profile"
            Screen.News.route -> "News"
            Screen.Chat.route -> _state.value.selectedOtherUser?.username ?: "Chat"
            else -> ""
        }
    }

    fun shouldShowBackButton(currentRoute: String?): Boolean {
        return currentRoute == Screen.Chat.route
    }

    fun shouldShowBottomBar(currentRoute: String?): Boolean {
        return currentRoute != Screen.Chat.route
    }

    // Get the selected chat ID for navigation
    fun getSelectedChatId(): String? {
        val currentUser = _state.value.currentUser
        val otherUser = _state.value.selectedOtherUser
        return if (currentUser != null && otherUser != null) {
            listOf(currentUser.uid, otherUser.uid).sorted().joinToString("_")
        } else {
            _state.value.selectedChatId
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, cancelling jobs")
        chatsJob?.cancel()
    }
}