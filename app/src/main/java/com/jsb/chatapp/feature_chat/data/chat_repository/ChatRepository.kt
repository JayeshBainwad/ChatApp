package com.jsb.chatapp.feature_chat.data.chat_repository

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus

interface ChatRepository {
    suspend fun searchUsers(query: String): List<User>
    suspend fun sendMessage(chatId: String, message: Message)
    suspend fun getChatsForUser(userId: String): List<Chat>
    fun listenForMessages(chatId: String, onMessages: (List<Message>) -> Unit)
    suspend fun markMessagesSeen(chatId: String, messageIds: List<String>)
    suspend fun updateFcmToken(userId: String, token: String)
    suspend fun getUnreadCount(chatId: String, receiverId: String): Int
}