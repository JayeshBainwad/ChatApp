package com.jsb.chatapp.feature_chat.data.chat_datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
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

}