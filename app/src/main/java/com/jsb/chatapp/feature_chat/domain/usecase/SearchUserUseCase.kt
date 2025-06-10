package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_auth.domain.model.User
import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import javax.inject.Inject

class SearchUserUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(query: String): List<User> {
        return repository.searchUsers(query)
    }
}