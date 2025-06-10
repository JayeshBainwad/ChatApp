package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.domain.model.Chat
import javax.inject.Inject

class GetChatsForUserUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(currentUserId: String): List<Chat> {
        return repository.getChatsForUser(currentUserId)
    }
}