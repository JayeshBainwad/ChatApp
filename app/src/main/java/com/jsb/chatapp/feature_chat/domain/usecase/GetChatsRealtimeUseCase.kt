package com.jsb.chatapp.feature_chat.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import com.jsb.chatapp.feature_chat.presentation.ui.screens.chat_home.ChatState
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

        val listener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Launch coroutine to process the data
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val chats = mutableListOf<Chat>()

                            for (document in snapshot.documents) {
                                val chatId = document.id
                                val lastMessage = document.getString("lastMessage") ?: ""
                                val timestamp = document.getLong("lastTimestamp") ?: 0L // Use lastTimestamp instead of timestamp
                                val participants = document.get("participants") as? List<String> ?: emptyList()

                                // Get the other user's ID
                                val otherUserId = participants.find { it != currentUserId }

                                if (otherUserId != null) {
                                    // Fetch other user's details
                                    val otherUserDoc = firestore.collection("users")
                                        .document(otherUserId)
                                        .get()
                                        .await()

                                    val otherUser = otherUserDoc.toObject(User::class.java)

                                    if (otherUser != null) {
                                        // Get unread count
                                        val unreadCount = getUnreadCount(chatId, currentUserId)

                                        chats.add(
                                            Chat(
                                                chatId = chatId,
                                                lastMessage = lastMessage,
                                                timestamp = timestamp,
                                                participants = participants,
                                                otherUser = otherUser,
                                                unreadCount = unreadCount
                                            )
                                        )
                                    }
                                }
                            }

                            // Sort by timestamp and send to flow
                            val sortedChats = chats.sortedByDescending { it.timestamp }
                            trySend(sortedChats)

                        } catch (e: Exception) {
                            close(e)
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
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
            0 // Return 0 if there's an error
        }
    }
}