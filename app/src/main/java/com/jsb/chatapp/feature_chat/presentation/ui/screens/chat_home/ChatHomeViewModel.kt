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
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_auth.presentation.utils.UserPreferences
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.usecase.ChatUseCases
import com.jsb.chatapp.feature_chat.domain.usecase.SearchUserUseCase
import com.jsb.chatapp.feature_chat.domain.usecase.GetChatsRealtimeUseCase
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.UserChatInfo
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
    private val searchUserUseCase: SearchUserUseCase,
    private val chatUseCases: ChatUseCases,
    private val getChatsRealtimeUseCase: GetChatsRealtimeUseCase, // Add this new use case
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "ChatHomeViewModel"
    }

    private val _firestoreUser = mutableStateOf<User?>(null)
    val firestoreUser: State<User?> = _firestoreUser

    var state by mutableStateOf(ChatState())

    private var searchJob: Job? = null
    private var chatsJob: Job? = null // For managing the Flow subscription

    private val _chatList = mutableStateOf<List<Chat>>(emptyList())
    val chatList: State<List<Chat>> = _chatList

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
                } else {
                    searchUsersWithChatInfo(event.query)
                }
            }
        }
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
                            "lastMessage='${chat.lastMessage}', " +
                            "unreadCount=${chat.unreadCount}, " +
                            "timestamp=${chat.timestamp}")
                }

                _chatList.value = chats

                // If we're currently searching, update the search results too
                if (state.query.isNotBlank()) {
                    Log.d(TAG, "Query is active (${state.query}), updating search results")
                    searchUsersWithChatInfo(state.query)
                }
            }
            .catch { error ->
                Log.e(TAG, "Error in real-time chat updates", error)
                // Optionally update state with error
                state = state.copy(error = error.message)
            }
            .launchIn(viewModelScope)
    }

    private fun searchUsersWithChatInfo(query: String) {
        Log.d(TAG, "Searching users with chat info for query: '$query'")

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                // 1. Search for users
                val searchedUsers = searchUserUseCase(query)
                Log.d(TAG, "Found ${searchedUsers.size} users matching query")

                // 2. Get current user's chats to find existing conversations
                val currentChats = _chatList.value
                Log.d(TAG, "Current chats count: ${currentChats.size}")

                // 3. Combine user info with chat info
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
                        unreadCount = existingChat?.unreadCount ?: 0 // Add unread count
                    )

                    // Log each UserChatInfo creation
                    Log.d(TAG, "Created UserChatInfo for ${user.username}: " +
                            "hasExistingChat=${userChatInfo.hasExistingChat}, " +
                            "unreadCount=${userChatInfo.unreadCount}, " +
                            "lastMessage='${userChatInfo.lastMessage}', " +
                            "timestamp=${userChatInfo.timestamp}")

                    userChatInfo
                }

                // 4. Sort: existing chats first (by timestamp), then new users (by username)
                val sortedUserChatInfos = userChatInfos.sortedWith(
                    compareByDescending<UserChatInfo> { it.hasExistingChat }
                        .thenByDescending { it.timestamp ?: 0L }
                        .thenBy { it.user.username }
                )

                Log.d(TAG, "Final sorted UserChatInfos count: ${sortedUserChatInfos.size}")
                sortedUserChatInfos.forEachIndexed { index, userChatInfo ->
                    Log.d(TAG, "Sorted UserChatInfo $index: " +
                            "user=${userChatInfo.user.username}, " +
                            "unreadCount=${userChatInfo.unreadCount}, " +
                            "hasExistingChat=${userChatInfo.hasExistingChat}")
                }

                state = state.copy(userChatInfos = sortedUserChatInfos, isLoading = false)
                Log.d(TAG, "Updated state with ${sortedUserChatInfos.size} userChatInfos")

            } catch (e: Exception) {
                Log.e(TAG, "Error in searchUsersWithChatInfo", e)
                state = state.copy(error = e.message, isLoading = false)
            }
        }
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
            // Cancel real-time updates before logout
            chatsJob?.cancel()
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