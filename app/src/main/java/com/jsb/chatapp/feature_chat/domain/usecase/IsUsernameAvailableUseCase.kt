package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import javax.inject.Inject

class IsUsernameAvailableUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(username: String): Boolean {
        return repository.isUsernameAvailable(username)
    }
}