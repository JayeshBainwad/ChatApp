package com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.usecase.SearchUserRealtimeUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.GetChatsRealtimeUseCase
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.UserChatInfo
import com.jsb.chatapp.feature_core.main_util.UserStatusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatHomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val searchUserRealtimeUseCase: SearchUserRealtimeUseCase, // Updated to use real-time search
    private val getChatsRealtimeUseCase: GetChatsRealtimeUseCase,
    private val firestore: FirebaseFirestore,
    private val userStatusManager: UserStatusManager
) : ViewModel() {

    companion object {
        private const val TAG = "ChatHomeViewModel"
    }

    private val _firestoreUser = mutableStateOf<User?>(null)
    val firestoreUser: State<User?> = _firestoreUser

    var state by mutableStateOf(ChatState()) // used to display searched users

    private var searchJob: Job? = null
    private var chatsJob: Job? = null

    private val _chatList = mutableStateOf<List<Chat>>(emptyList()) // used to display recent chat
    val chatList: State<List<Chat>> = _chatList

    // Store search results separately for real-time updates
    private val _searchUsers = mutableStateOf<List<User>>(emptyList())

    init {
        fetchCurrentUserFromFirestore()
    }

    fun onEvent(event: ChatHomeEvent) {
        when (event) {
            is ChatHomeEvent.OnQueryChange -> {
                Log.d(TAG, "Query changed to: '${event.query}'")
                state = state.copy(query = event.query)
                if (event.query.isBlank()) {
                    // Clear search results when query is empty
                    Log.d(TAG, "Query is blank, clearing search results")
                    state = state.copy(userChatInfos = emptyList())
                    _searchUsers.value = emptyList()
                    searchJob?.cancel()
                } else {
                    startRealtimeSearch(event.query)
                }
            }
        }
    }

    private fun startRealtimeSearch(query: String) {
        Log.d(TAG, "Starting real-time search for: '$query'")

        searchJob?.cancel()
        searchJob = searchUserRealtimeUseCase(query)
            .onEach { users ->
                Log.d(TAG, "Received ${users.size} users from real-time search")
                _searchUsers.value = users
                updateSearchResults()
            }
            .catch { error ->
                Log.e(TAG, "Error in real-time search", error)
                state = state.copy(error = error.message, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    private fun updateSearchResults() {
        val searchedUsers = _searchUsers.value
        val currentChats = _chatList.value

        Log.d(
            TAG,
            "Updating search results with ${searchedUsers.size} users " +
                    "and ${currentChats.size} chats"
        )

        // Combine user info with chat info
        val userChatInfos = searchedUsers.map { user ->
            // Find existing chat with this user
            val existingChat = currentChats.find { chat ->
                chat.otherUser.uid == user.uid
            }

            val userChatInfo = UserChatInfo(
                user = user,
                lastMessage = existingChat?.lastMessage,
                timestamp = existingChat?.timestamp,
                hasExistingChat = existingChat != null,
                unreadCount = existingChat?.unreadCount ?: 0
            )

            // Log each UserChatInfo creation
            Log.d(TAG, "Created UserChatInfo for ${user.username}: " +
                    "hasExistingChat=${userChatInfo.hasExistingChat}, " +
                    "unreadCount=${userChatInfo.unreadCount}, " +
                    "lastMessage='${userChatInfo.lastMessage}', " +
                    "isOnline='${userChatInfo.user.isOnline}', " +
                    "timestamp=${userChatInfo.timestamp}")

            userChatInfo
        }

        // Sort: existing chats first (by timestamp), then new users (by username)
        val sortedUserChatInfos = userChatInfos.sortedWith(
            compareByDescending<UserChatInfo> { it.hasExistingChat }
                .thenByDescending { it.timestamp ?: 0L }
                .thenBy { it.user.username }
        )

        Log.d(TAG, "Final sorted UserChatInfos count: ${sortedUserChatInfos.size}")
        state = state.copy(userChatInfos = sortedUserChatInfos, isLoading = false)
    }

    // Updated to use Flow-based real-time updates
    fun loadChats(currentUserId: String) {
        Log.d(TAG, "Loading chats for user: $currentUserId")

        // Cancel any existing job
        chatsJob?.cancel()

        // Start collecting real-time updates using Flow
        chatsJob = getChatsRealtimeUseCase(currentUserId)
            .onEach { chats ->
                Log.d(TAG, "Received ${chats.size} chats from real-time updates")

                // Log detailed chat information including unread counts
                chats.forEachIndexed { index, chat ->
                    Log.d(TAG, "Chat $index: " +
                            "otherUser=${chat.otherUser.username}, " +
                            "otherUser is online=${chat.otherUser.isOnline}, " +
                            "lastMessage='${chat.lastMessage}', " +
                            "unreadCount=${chat.unreadCount}, " +
                            "timestamp=${chat.timestamp}")
                }

                _chatList.value = chats

                // If we're currently searching, update the search results too
                if (state.query.isNotBlank()) {
                    Log.d(TAG, "Query is active (${state.query}), updating search results")
                    updateSearchResults()
                }
            }
            .catch { error ->
                Log.e(TAG, "Error in real-time chat updates", error)
                // Optionally update state with error
                state = state.copy(error = error.message)
            }
            .launchIn(viewModelScope)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun fetchCurrentUserFromFirestore() {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            Log.w(TAG, "No current user found when fetching from Firestore")
            return
        }

        Log.d(TAG, "Fetching current user from Firestore for uid: $uid")

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)
                    _firestoreUser.value = user
                    Log.d(TAG, "Successfully fetched user: ${user?.username}")
                } else {
                    Log.w(TAG, "User doc not found for uid=$uid")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user doc", e)
            }
    }

    fun logout(onLoggedOut: () -> Unit) {
        Log.d(TAG, "Logging out user")
        viewModelScope.launch {
            // Set user offline before logout
            firebaseAuth.currentUser?.uid?.let { userId ->
                userStatusManager.setUserOffline(userId)
            }
            // Cancel real-time updates before logout
            chatsJob?.cancel()
            searchJob?.cancel()
            firebaseAuth.signOut()
            userPreferences.clearRememberMe()
            Log.d(TAG, "User logged out successfully")
            onLoggedOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, cancelling jobs")
        // Clean up jobs when ViewModel is destroyed
        chatsJob?.cancel()
        searchJob?.cancel()
    }
}