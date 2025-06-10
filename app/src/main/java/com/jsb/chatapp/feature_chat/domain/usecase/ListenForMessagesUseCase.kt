package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.domain.model.Message
import javax.inject.Inject

class ListenForMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: String, onMessagesReceived: (List<Message>) -> Unit) {
        repository.listenForMessages(chatId, onMessagesReceived)
    }
}