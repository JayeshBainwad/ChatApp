package com.jsb.chatapp.feature_chat.domain.usecase

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GetChatsRealtimeUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    operator fun invoke(currentUserId: String): Flow<List<Chat>> = callbackFlow {
        val userListeners = mutableMapOf<String, ListenerRegistration>()
        var chatListener: ListenerRegistration? = null

        // Store current chat data
        val currentChats = mutableMapOf<String, Chat>()

        fun updateAndEmitChats() {
            val sortedChats = currentChats.values.sortedByDescending { it.timestamp }
            trySend(sortedChats)
        }

        // Helper function to manually extract User from document
        fun extractUserFromDocument(userDoc: DocumentSnapshot): User? {
            return if (userDoc.exists()) {
                User(
                    uid = userDoc.getString("uid") ?: "",
                    username = userDoc.getString("username") ?: "",
                    phoneNumber = userDoc.getString("phoneNumber") ?: "",
                    fcmToken = userDoc.getString("fcmToken") ?: "",
                    createdAt = userDoc.getLong("createdAt") ?: 0L,
                    isOnline = userDoc.getBoolean("isOnline") ?: false,
                    lastSeen = userDoc.getLong("lastSeen") ?: 0L,
                    name = userDoc.getString("name") ?: "",
                    email = userDoc.getString("email") ?: "",
                    bio = userDoc.getString("bio") ?: "",
                    avatarUrl = userDoc.getString("avatarUrl") ?: ""
                )
            } else null
        }

        // Listen to chat changes
        chatListener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val newChatIds = mutableSetOf<String>()

                            for (document in snapshot.documents) {
                                val chatId = document.id
                                newChatIds.add(chatId)

                                val lastMessage = document.getString("lastMessage") ?: ""
                                val timestamp = document.getLong("lastTimestamp") ?: 0L
                                val participants = document.get("participants") as? List<String> ?: emptyList()

                                // Get the other user's ID
                                val otherUserId = participants.find { it != currentUserId }

                                if (otherUserId != null) {
                                    // If we don't have a listener for this user yet, create one
                                    if (!userListeners.containsKey(otherUserId)) {
                                        val userListener = firestore.collection("users")
                                            .document(otherUserId)
                                            .addSnapshotListener { userDoc, userError ->
                                                if (userError != null) {
                                                    Log.e(
                                                        "GetChatsRealtimeUseCase",
                                                        "Error listening to user:" +
                                                                otherUserId + userError
                                                    )
                                                    return@addSnapshotListener
                                                }

                                                if (userDoc != null && userDoc.exists()) {
                                                    // Manual extraction for real-time updates
                                                    val otherUser = extractUserFromDocument(userDoc)

                                                    if (otherUser != null) {
                                                        // Update the chat with new user data
                                                        currentChats[chatId]?.let { existingChat ->
                                                            currentChats[chatId] = existingChat.copy(otherUser = otherUser)
                                                            updateAndEmitChats()
                                                        }

                                                        Log.d(
                                                            "GetChatsRealtimeUseCase",
                                                            "User status updated: " +
                                                                    "${otherUser.username}, " +
                                                                    "createdAt: ${otherUser.createdAt}, " +
                                                                    "phoneNumber: ${otherUser.phoneNumber}, " +
                                                                    "uid: ${otherUser.uid}, " +
                                                                    "isOnline: ${otherUser.isOnline}, " +
                                                                    "otherUserFcmToken: ${otherUser.fcmToken}, " +
                                                                    "lastSeen: ${otherUser.lastSeen}"
                                                        )
                                                    }
                                                }
                                            }
                                        userListeners[otherUserId] = userListener //D
                                    }

                                    // Get initial user data if we don't have this chat yet
                                    if (!currentChats.containsKey(chatId)) {
                                        val otherUserDoc = firestore.collection("users")
                                            .document(otherUserId)
                                            .get()
                                            .await()

                                        // Use manual extraction instead of toObject()
                                        val otherUser = extractUserFromDocument(otherUserDoc)

                                        if (otherUser != null) {
                                            // Get unread count
                                            val unreadCount = getUnreadCount(chatId, currentUserId)

                                            currentChats[chatId] = Chat(
                                                chatId = chatId,
                                                lastMessage = lastMessage,
                                                timestamp = timestamp,
                                                participants = participants,
                                                otherUser = otherUser,
                                                unreadCount = unreadCount
                                            )

                                            Log.d("GetChatsRealtimeUseCase",
                                                "Initial chat created: " +
                                                        "${otherUser.username}, " +
                                                        "otherUserFcmToken: ${otherUser.fcmToken}, " +
                                                        "isOnline: ${otherUser.isOnline}")
                                        }
                                    } else {
                                        // Update existing chat data (message, timestamp, unread count)
                                        currentChats[chatId]?.let { existingChat ->
                                            val unreadCount = getUnreadCount(chatId, currentUserId)
                                            currentChats[chatId] = existingChat.copy(
                                                lastMessage = lastMessage,
                                                timestamp = timestamp,
                                                unreadCount = unreadCount
                                            )
                                        }
                                    }
                                }
                            }

                            // Remove chats that no longer exist and their user listeners
                            val removedChatIds = currentChats.keys - newChatIds
                            removedChatIds.forEach { chatId ->
                                val chat = currentChats.remove(chatId)
                                chat?.otherUser?.uid?.let { userId ->
                                    // Only remove user listener if no other chats use this user
                                    val stillHasChatsWithUser = currentChats.values.any { it.otherUser.uid == userId }
                                    if (!stillHasChatsWithUser) {
                                        userListeners.remove(userId)?.remove()
                                    }
                                }
                            }

                            updateAndEmitChats()

                        } catch (e: Exception) {
                            Log.e("GetChatsRealtimeUseCase", "Error processing chat updates", e)
                            close(e)
                        }
                    }
                }
            }

        awaitClose {
            chatListener?.remove()
            userListeners.values.forEach { it.remove() }
        }
    }

    private suspend fun getUnreadCount(chatId: String, receiverId: String): Int {
        return try {
            val snapshot = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", MessageStatus.SENT.name)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            Log.e("GetChatsRealtimeUseCase", "Error getting unread count", e)
            0 // Return 0 if there's an error
        }
    }
}