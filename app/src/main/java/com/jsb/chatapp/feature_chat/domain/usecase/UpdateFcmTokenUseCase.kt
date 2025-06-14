package com.jsb.chatapp.feature_chat.domain.usecase

import com.jsb.chatapp.feature_chat.data.chat_repository.ChatRepository
import javax.inject.Inject

class UpdateFcmTokenUseCase @Inject constructor(
    private val userRepository: ChatRepository
) {
    suspend operator fun invoke(userId: String, token: String) {
        userRepository.updateFcmToken(userId, token)
    }
}
