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
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat.UserChatInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatHomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val searchUserUseCase: SearchUserUseCase,
    private val chatUseCases: ChatUseCases,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _firestoreUser = mutableStateOf<User?>(null)
    val firestoreUser: State<User?> = _firestoreUser

    var state by mutableStateOf(ChatState())

    private var searchJob: Job? = null

    private val _chatList = mutableStateOf<List<Chat>>(emptyList())
    val chatList: State<List<Chat>> = _chatList

    init {
        fetchCurrentUserFromFirestore()
    }

    fun onEvent(event: ChatHomeEvent) {
        when (event) {
            is ChatHomeEvent.OnQueryChange -> {
                state = state.copy(query = event.query)
                if (event.query.isBlank()) {
                    // Clear search results when query is empty
                    state = state.copy(userChatInfos = emptyList())
                } else {
                    searchUsersWithChatInfo(event.query)
                }
            }
        }
    }

    fun loadChats(currentUserId: String) {
        viewModelScope.launch {
            val chats = chatUseCases.getChatsForUser(currentUserId)
            _chatList.value = chats.sortedByDescending { it.timestamp }
        }
    }

//    private fun searchUsers(query: String) {
//        searchJob?.cancel()
//        searchJob = viewModelScope.launch {
//            state = state.copy(isLoading = true, error = null)
//            try {
//                val result = searchUserUseCase(query)
//                state = state.copy(users = result, isLoading = false)
//            } catch (e: Exception) {
//                state = state.copy(error = e.message, isLoading = false)
//            }
//        }
//    }

    private fun searchUsersWithChatInfo(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                // 1. Search for users
                val searchedUsers = searchUserUseCase(query)

                // 2. Get current user's chats to find existing conversations
                val currentChats = _chatList.value

                // 3. Combine user info with chat info
                val userChatInfos = searchedUsers.map { user ->
                    // Find existing chat with this user
                    val existingChat = currentChats.find { chat ->
                        chat.otherUser.uid == user.uid
                    }

                    UserChatInfo(
                        user = user,
                        lastMessage = existingChat?.lastMessage,
                        timestamp = existingChat?.timestamp,
                        hasExistingChat = existingChat != null
                    )
                }

                // 4. Sort: existing chats first (by timestamp), then new users (by username)
                val sortedUserChatInfos = userChatInfos.sortedWith(
                    compareByDescending<UserChatInfo> { it.hasExistingChat }
                        .thenByDescending { it.timestamp ?: 0L }
                        .thenBy { it.user.username }
                )

                state = state.copy(userChatInfos = sortedUserChatInfos, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(error = e.message, isLoading = false)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun fetchCurrentUserFromFirestore() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _firestoreUser.value = doc.toObject(User::class.java)
                } else {
                    Log.w("ChatViewModel", "User doc not found for uid=$uid")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Error fetching user doc", e)
            }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            firebaseAuth.signOut()
            userPreferences.clearRememberMe()
            onLoggedOut()
        }
    }

    fun generateChatId(user1: String, user2: String): String {
        return listOf(user1, user2).sorted().joinToString("_")
    }
}