package com.jsb.chatapp.feature_chat.data.chat_repository

import com.jsb.chatapp.feature_chat.data.chat_datasource.ChatDatasource
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_main.main_domain.main_model.User
import com.jsb.chatapp.feature_main.main_util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDatasource: ChatDatasource
) : ChatRepository {
    override suspend fun searchUsers(query: String) = chatDatasource.searchUsers(query)

    override suspend fun sendMessage(chatId: String, message: Message) =
        chatDatasource.sendMessage(chatId, message)

    override suspend fun getChatsForUser(userId: String): List<Chat> =
        chatDatasource.getUserChats(userId)

    override fun listenForMessages(chatId: String, onMessages: (List<Message>) -> Unit) =
        chatDatasource.listenForMessages(chatId, onMessages)

    override suspend fun markMessagesSeen(chatId: String, messageIds: List<String>) {
        chatDatasource.markMessagesSeen(chatId, messageIds)
    }

    override suspend fun updateFcmToken(userId: String, token: String) {
        chatDatasource.updateFcmToken(userId, token)
    }

    override suspend fun getUnreadCount(chatId: String, receiverId: String): Int {
        return chatDatasource.getUnreadCount(chatId, receiverId)
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return chatDatasource.isUsernameAvailable(username)
    }

    override suspend fun updateUserProfile(uid: String, user: User): Result<Unit> {
        return chatDatasource.updateUserProfile(uid, user)
    }

    override fun getChatsRealtime(currentUserId: String): Flow<List<Chat>> {
        return chatDatasource.getChatsRealtime(currentUserId)
    }
}