package com.jsb.chatapp.feature_chat.data.chat_repository

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.data.chat_datasource.ChatDatasource
import com.jsb.chatapp.feature_chat.domain.model.Chat
import com.jsb.chatapp.feature_chat.domain.model.Message
import com.jsb.chatapp.feature_chat.domain.model.MessageStatus
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

}