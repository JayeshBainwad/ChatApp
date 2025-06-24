package com.jsb.chatapp.feature_chat.data.chat_datasource

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jsb.chatapp.feature_core.core_domain.main_model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
import com.jsb.chatapp.feature_core.main_util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatDatasourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatDatasource {

    override suspend fun searchUsers(query: String): List<User> {
        if (query.isBlank()) return emptyList()

        val snapshot = firestore.collection("users")
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get()
            .await()

        return snapshot.toObjects(User::class.java)
    }

    override suspend fun sendMessage(chatId: String, message: Message) {
        val chatRef = firestore.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(message.messageId)

        // Use server timestamp instead of client time
        val timestamp = FieldValue.serverTimestamp()

        val messageData = hashMapOf(
            "senderId" to message.senderId,
            "receiverId" to message.receiverId,
            "content" to message.content,
            "senderName" to message.senderName,
            "status" to message.status.name,
            "timestamp" to timestamp // Server timestamp
        )

        firestore.runBatch { batch ->
            batch.set(messageRef, message)
            batch.set(
                chatRef,
                mapOf(
                    "participants" to listOf(message.senderId, message.receiverId),
                    "lastMessage" to message.content,
                    "lastTimestamp" to message.timestamp
                ),
                SetOptions.merge()
            )
        }.await()
    }

    override suspend fun getUserChats(currentUserId: String): List<Chat> {
        val result = firestore.collection("chats").get().await()

        return result.documents.mapNotNull { doc ->
            val chatId = doc.id

            val messages = doc.reference.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastMessageDoc = messages.documents.firstOrNull()
            val lastMessage = lastMessageDoc?.getString("content") ?: ""
            val timestamp = lastMessageDoc?.getLong("timestamp") ?: 0L
            val participants = doc.get("participants") as? List<String> ?: emptyList()

            if (participants.contains(currentUserId)) {
                val otherUserId = participants.firstOrNull { it != currentUserId } ?: return@mapNotNull null

                // Fetch other user info
                val userDoc = firestore.collection("users").document(otherUserId).get().await()
                val user = userDoc.toObject(User::class.java) ?: return@mapNotNull null

                Chat(
                    chatId = chatId,
                    lastMessage = lastMessage,
                    timestamp = timestamp,
                    participants = participants,
                    otherUser = user
                )
            } else null
        }
    }



    override fun listenForMessages(chatId: String, onMessages: (List<Message>) -> Unit) {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val messages = it.documents.mapNotNull { doc -> doc.toObject(Message::class.java) }
                    onMessages(messages)
                }
            }
    }

    override suspend fun markMessagesSeen(chatId: String, messageIds: List<String>) {
        val chatRef = firestore.collection("chats").document(chatId)
        val messagesCollection = chatRef.collection("messages")

        firestore.runBatch { batch ->
            messageIds.forEach { messageId ->
                val messageRef = messagesCollection.document(messageId)
                batch.update(messageRef, "status", MessageStatus.SEEN.name)
            }
        }.await()
    }

    override suspend fun updateFcmToken(userId: String, token: String) {
        firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .await()
        Log.d("FCM", "Token updated in Firestore for userId: $userId")

    }

    override suspend fun getUnreadCount(chatId: String, receiverId: String): Int {
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

    override suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = firestore
            .collection("users")
            .whereEqualTo("username", username)
            .get()
            .await()
        return snapshot.isEmpty
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> = try {
        firestore.collection("users").document(uid).update(
            "username", user.username,
            "name", user.name,
            "email", user.email,
            "avatarUrl", user.avatarUrl,
            "phoneNumber", user.phoneNumber,
            "bio", user.bio,
            "fcmToken", user.fcmToken,
            "lastSeen", user.lastSeen,
            "isOnline", user.isOnline
        ).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)
    }

    override fun getChatsRealtime(currentUserId: String): Flow<List<Chat>> = callbackFlow {
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
}