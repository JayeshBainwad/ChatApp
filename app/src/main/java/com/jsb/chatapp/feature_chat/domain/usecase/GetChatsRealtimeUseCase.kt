package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import com.jsb.chatapp.feature_chat.domain.model.Chat
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatsRealtimeUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(currentUserId: String): Flow<List<Chat>> {
        return repository.getChatsRealtime(currentUserId)
    }
}